package com.revolgenx.lemillion.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.net.toFile
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import com.github.axet.androidlibrary.widgets.HeaderRecyclerAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.adapter.FilesTreeAdapter
import com.revolgenx.lemillion.core.exception.TorrentException
import com.revolgenx.lemillion.core.preference.storagePath
import com.revolgenx.lemillion.core.torrent.Torrent
import com.revolgenx.lemillion.core.torrent.TorrentEngine
import com.revolgenx.lemillion.core.util.*
import com.revolgenx.lemillion.view.showProgress
import com.revolgenx.lemillion.view.makeToast
import com.revolgenx.lemillion.event.TorrentAddedEvent
import com.revolgenx.lemillion.event.TorrentAddedEventTypes
import kotlinx.android.synthetic.main.add_torrent_bottom_sheet_layout.*
import kotlinx.android.synthetic.main.torrent_file_header_layout.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.libtorrent4j.AlertListener
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import timber.log.Timber
import java.io.File
import java.lang.Exception
import kotlin.coroutines.CoroutineContext


//TODO:rotation problem
class AddTorrentBottomSheetDialog : BottomSheetDialogFragment(), AlertListener, CoroutineScope {

    companion object {
        fun newInstance(uri: Uri) = AddTorrentBottomSheetDialog().apply {
            arguments = bundleOf(uriKey to uri)
        }
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    private val uriKey = "torrent_hash_key"
    private val torrentPathKey = "torrent_path_key"
    private var handle: TorrentHandle? = null
    private var torrentName = ""
    private var path = ""
    private lateinit var adapter: FilesTreeAdapter
    private lateinit var headerLayout: View
    private val engine by inject<TorrentEngine>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        headerLayout = LayoutInflater.from(context).inflate(
            R.layout.torrent_file_header_layout,
            container,
            false
        )
        return inflater.inflate(R.layout.add_torrent_bottom_sheet_layout, container, false)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener { t ->
            (t as BottomSheetDialog).findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let {
                    BottomSheetBehavior.from(it).let { bh ->
                        bh.isHideable = false
                        bh.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
        }

        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val uri = arguments?.getParcelable<Uri>(uriKey) ?: return

        addListener()

        path = if (savedInstanceState == null) {
            storagePath(context!!)!!
        } else {
            savedInstanceState.getString(torrentPathKey)!!
        }

        when (uri.scheme) {
            FILE_PREFIX -> {
                try {
                    handle =
                        engine.loadTorrent(TorrentInfo(uri.toFile()), File(path), null, null, null)
                } catch (e: TorrentException) {
                    if (savedInstanceState == null) {
                        context!!.showErrorDialog(e.message ?: "error")
                        dismiss()
                        return
                    } else {
                        handle = e.data as? TorrentHandle
                    }
                } catch (e: Exception) {
                    context!!.showErrorDialog(e.message ?: "error")
                    dismiss()
                    return
                }
            }

            MAGNET_PREFIX -> {
                try {
                    handle = engine.fetchMagnet(uri.toString())
                } catch (e: TorrentException) {
                    if (savedInstanceState == null) {
                        context!!.showErrorDialog(e.message ?: "error")
                        dismiss()
                        return
                    } else {
                        handle = e.data as? TorrentHandle
                    }
                } catch (e: Exception) {
                    context!!.showErrorDialog(e.message ?: "error")
                    dismiss()
                    return
                }
                showFetchingMetaData(true)
            }

            CONTENT_PREFIX -> {
                try {
                    handle = engine.loadTorrent(
                        TorrentInfo(uriContentToByteArray(context!!, uri)),
                        File(path),
                        null,
                        null, null
                    )
                } catch (e: TorrentException) {
                    if (savedInstanceState == null) {
                        context!!.showErrorDialog(e.message ?: "error")
                        dismiss()
                        return
                    } else {
                        handle = e.data as? TorrentHandle
                    }
                } catch (e: Exception) {
                    context!!.showErrorDialog(e.message ?: "error")
                    dismiss()
                    return
                }
            }
            else -> {
                context!!.showErrorDialog(getString(R.string.unknown_scheme))
                dismiss()
            }
        }

        if (handle == null) {
            return
        }

        adapter = FilesTreeAdapter(handle!!) {
            headerLayout.torrentMetaTotalSizeTv.text = it
        }

        val div = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        treeRecyclerView.addItemDecoration(div)

        val headerRecyclerAdapter = HeaderRecyclerAdapter(adapter)
        headerRecyclerAdapter.setHeaderView(headerLayout)
        treeRecyclerView.adapter = headerRecyclerAdapter

        updateView()
    }


    private fun showFetchingMetaData(b: Boolean) {
        if (b) {
            downloadMetadataTv.visibility = View.VISIBLE
            downloadMetadataTv.showProgress(R.string.downloading_meta, b)
        } else {
            downloadMetadataTv.visibility = View.GONE
        }
    }

    private fun addListener() {

        engine.addListener(this)

        addTorrentOkTv.setOnClickListener {

            if (handle == null) {
                dismiss()
                return@setOnClickListener
            }

            postEvent(
                TorrentAddedEvent(
                    Torrent().also {
                        it.path = torrentPathMetaTv.description
                        it.hash = torrentMetaHashTv.description
                        it.handle = handle
                        it.simpleState = true
                        if (handle!!.status().hasMetadata()) {
                            it.magnet = handle!!.makeMagnetUri()
                            it.source = handle!!.torrentFile().bencode()!!
                        } else {
                            it.magnet = arguments?.getParcelable<Uri>(uriKey).toString()
                        }
                    },
                    TorrentAddedEventTypes.TORRENT_ADDED
                )
            )
            dismiss()
        }

        addTorrentCancelTv.setOnClickListener {
            if (handle == null) {
                dismiss()
                return@setOnClickListener
            }
            dialog?.cancel()
        }


        torrentMetaNameTv.setDrawableClickListener {
            context!!.showInputDialog(titleRes = R.string.name, prefill = torrentName) {
                handle!!.torrentFile().files().name(it)
            }
            updateView()
        }

        torrentMetaHashTv.setDrawableClickListener {
            val manager =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(
                ClipData.newPlainText(
                    "Text Copied:",
                    torrentMetaHashTv.description
                )
            )
            makeToast("Text Copied")
        }

        torrentPathMetaTv.setDrawableClickListener {
            MaterialDialog(this.context!!).show {
                folderChooser(allowFolderCreation = true) { dialog, file ->
                    handle!!.moveStorage(file.path)
                }
                negativeButton(R.string.cancel)
            }
        }

        headerLayout.torrentMetaFileCheckBox.setOnCheckedChangeListener { _, isChecked ->
            adapter.checkAll(isChecked)
        }

        showFetchingMetaData(true)

    }


    override fun alert(alert: Alert<*>?) {
        CoroutineScope(Dispatchers.Main).launch {
            if (alert == null) return@launch
            when (alert.type()) {
                AlertType.METADATA_RECEIVED -> {
                    showFetchingMetaData(false)
                    updateView()
                }
                AlertType.METADATA_FAILED -> {
                    downloadMetadataTv?.showProgress(R.string.failed, false)
                }
                else -> {
                    Timber.d(alert.type().name)
                }
            }
        }
    }

    override fun types(): IntArray =
        intArrayOf(AlertType.METADATA_RECEIVED.swig(), AlertType.METADATA_FAILED.swig())


    private fun updateView() {
        if (handle == null) return

        val hash = handle!!.infoHash().toHex()
        torrentName = handle!!.name()

        torrentMetaNameTv.description = torrentName
        torrentMetaHashTv.description = hash
        torrentPathMetaTv.description = path

        if (handle!!.status().hasMetadata()) {
            torrentMetaNameTv.showDrawable(true)
            headerLayout.torrentMetaTotalSizeTv.text =
                handle!!.torrentFile().totalSize().formatSize()

            headerLayout.emptyTv.visibility = View.GONE
            downloadMetadataTv.visibility = View.GONE
            torrentPiecesTv.description =
                handle!!.torrentFile().numPieces().toString() + "/" + handle!!.torrentFile().pieceLength().toLong().formatSize()

            torrentSizeTv.description = handle!!.torrentFile().totalSize().formatSize()
        } else {
            downloadMetadataTv.visibility = View.VISIBLE
            torrentPiecesTv.description = "0"
            torrentSizeTv.description = "0"
            torrentMetaNameTv.showDrawable(false)
        }


        launch(Dispatchers.IO) {
            adapter.update {
                launch(Dispatchers.Main) {
                    adapter.load()
                    adapter.updateTotal()
                }
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(torrentPathKey, path)
        super.onSaveInstanceState(outState)
    }


    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (handle == null) return
        engine.cancelFetchMagnet(handle!!.infoHash().toHex())
    }

    override fun onDestroy() {
        job.cancel()
        engine.removeListener(this)
        super.onDestroy()
    }

}