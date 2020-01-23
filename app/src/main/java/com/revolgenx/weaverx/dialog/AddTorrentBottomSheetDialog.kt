package com.revolgenx.weaverx.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import com.github.axet.androidlibrary.widgets.HeaderRecyclerAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.revolgenx.weaverx.R
import com.revolgenx.weaverx.adapter.FilesTreeAdapter
import com.revolgenx.weaverx.core.preference.storagePath
import com.revolgenx.weaverx.core.util.*
import com.revolgenx.weaverx.event.TorrentAddedEvent
import com.revolgenx.weaverx.event.TorrentAddedEventTypes
import kotlinx.android.synthetic.main.add_torrent_bottom_sheet_layout.*
import kotlinx.android.synthetic.main.torrent_add_header_layout.view.*
import libtorrent.Libtorrent


//TODO:CHECK STORAGE
class AddTorrentBottomSheetDialog : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(t: Long) = AddTorrentBottomSheetDialog().apply {
            arguments = bundleOf(torrentHandleKey to t)
        }
    }

    private val torrentHandleKey = "torrent_handle_key"
    private val torrentPathKey = "torrent_path_key"
    private var handle = -1L
    private var hash = ""
    private var torrentName = ""
    private var path = ""
    private lateinit var adapter: FilesTreeAdapter
    private lateinit var headerLayout: View
    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            if (handle == -1L) return
            if (Libtorrent.metaTorrent(handle)) {
                downloadMetadataTv.visibility = View.GONE
                updateView()
                return
            }

            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        headerLayout = LayoutInflater.from(context).inflate(
            R.layout.torrent_add_header_layout,
            container,
            false
        )
        return inflater.inflate(R.layout.add_torrent_bottom_sheet_layout, container, false)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener { t ->
            (t as BottomSheetDialog).findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let { BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED }
        }

        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        handle = arguments?.getLong(torrentHandleKey) ?: -1

        addListener()

        if (handle == -1L) return

        adapter = FilesTreeAdapter(handle) {
            headerLayout.torrentMetaTotalSizeTv.text = it
        }

        val div = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        treeRecyclerView.addItemDecoration(div)

        val headerRecyclerAdapter = HeaderRecyclerAdapter(adapter)
        headerRecyclerAdapter.setHeaderView(headerLayout)
        treeRecyclerView.adapter = headerRecyclerAdapter

        path = if (savedInstanceState == null) {
            storagePath(context!!)!!
        } else {
            savedInstanceState.getString(torrentPathKey)!!
        }

        updateView()
    }

    private fun addListener() {
        addTorrentOkTv.setOnClickListener {

            if (handle == -1L) {
                dismiss()
                return@setOnClickListener
            }

            postEvent(
                TorrentAddedEvent(
                    handle,
                    torrentMetaHashTv.description,
                    torrentPathMetaTv.description,
                    TorrentAddedEventTypes.TORRENT_ADDED
                )
            )

            dismiss()
        }

        addTorrentCancelTv.setOnClickListener {
            if (handle == -1L) {
                dismiss()
                return@setOnClickListener
            }
            dialog?.cancel()
        }


        torrentMetaNameTv.setDrawableClickListener {
            context!!.showInputDialog(titleRes = R.string.name, prefill = torrentName) {
                Libtorrent.torrentSetName(handle, it)
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
                    val buf = Libtorrent.getTorrent(handle)
                    Libtorrent.removeTorrent(handle)
                    handle = Libtorrent.addTorrentFromBytes(file.path, buf)
                    if (handle != -1L) {
                        path = file.path
                        arguments = bundleOf(torrentHandleKey to handle)
                        updateView()
                    } else {
                        context.showErrorDialog(Libtorrent.error())
                    }
                }

                negativeButton(R.string.cancel)
            }
        }

        headerLayout.torrentMetaFileCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (Libtorrent.metaTorrent(handle))
                adapter.checkAll(isChecked)
        }

        downloadMetadataTv.setOnClickListener {
            downloadMetadataTv.isEnabled = false
            Libtorrent.downloadMetadata(handle)
            downloadMetadataTv.showProgress(R.string.downloading, true)
            handler.postDelayed(runnable, 1000L)
        }


    }


    private fun updateView() {
        if (handle == -1L) return

        hash = Libtorrent.torrentHash(handle)
        torrentName = Libtorrent.torrentName(handle).takeIf { it.isNotEmpty() } ?: hash

        torrentMetaNameTv.description = torrentName
        torrentMetaHashTv.description = hash
        torrentPathMetaTv.description = path

        if (Libtorrent.metaTorrent(handle)) {
            torrentMetaNameTv.showDrawable(true)
            headerLayout.torrentMetaTotalSizeTv.text =
                Libtorrent.torrentPendingBytesLength(handle).formatSize()

            headerLayout.emptyTv.visibility = View.GONE
            downloadMetadataTv.visibility = View.GONE
            torrentPiecesTv.description =
                Libtorrent.torrentPiecesCount(handle).toString() + "/" + Libtorrent.torrentPieceLength(
                    handle
                ).formatSize()
            piecesView.drawPieces(handle)
            torrentSizeTv.description = Libtorrent.torrentBytesLength(handle).formatSize()
        } else {
            downloadMetadataTv.visibility = View.VISIBLE
            torrentPiecesTv.description = "0"
            torrentSizeTv.description = "0"
            torrentMetaNameTv.showDrawable(false)
        }

        adapter.update()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(torrentPathKey, path)
        super.onSaveInstanceState(outState)
    }


    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (handle == -1L) return
        Libtorrent.removeTorrent(handle)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}