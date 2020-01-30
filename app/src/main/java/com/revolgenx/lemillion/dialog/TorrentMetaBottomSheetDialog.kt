package com.revolgenx.lemillion.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DividerItemDecoration
import com.github.axet.androidlibrary.widgets.HeaderRecyclerAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.adapter.FilesTreeAdapter
import com.revolgenx.lemillion.core.torrent.Torrent
import com.revolgenx.lemillion.core.util.*
import com.revolgenx.lemillion.event.UpdateTorrentEvent
import kotlinx.android.synthetic.main.torrent_file_header_layout.view.*
import kotlinx.android.synthetic.main.torrent_meta_bottom_sheet_dialog_layout.*
import libtorrent.Libtorrent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class TorrentMetaBottomSheetDialog : BottomSheetDialogFragment() {
    private val torrentKey = "torrent_key"
    private var torrent: Torrent? = null
    private lateinit var headerLayout: View
    private lateinit var adapter: FilesTreeAdapter


    companion object {
        fun newInstance(torrent: Torrent) = TorrentMetaBottomSheetDialog().apply {
            arguments = bundleOf(torrentKey to torrent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerClass(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        headerLayout = inflater.inflate(R.layout.torrent_file_header_layout, container, false)

        return inflater.inflate(R.layout.torrent_meta_bottom_sheet_dialog_layout, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener { t ->
            (t as BottomSheetDialog).findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let { BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED }
        }

        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        torrent = arguments!!.getParcelable(torrentKey) ?: return
        if (torrent!!.handle == -1L) return

        addListener()

        val handle = torrent!!.handle
        adapter = FilesTreeAdapter(handle) {
            headerLayout.torrentMetaTotalSizeTv.text = it
        }

        val div = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        treeRecyclerView.addItemDecoration(div)

        val headerRecyclerAdapter = HeaderRecyclerAdapter(adapter)
        headerRecyclerAdapter.setHeaderView(headerLayout)
        treeRecyclerView.adapter = headerRecyclerAdapter

        updateView()
    }

    private fun addListener() {
        generalViewTv.setOnClickListener {
            scrollViewLayout.smoothScrollTo(
                0, torrentGeneralLayout.y.toInt()
            )
        }

        headerLayout.torrentMetaFileCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (Libtorrent.metaTorrent(torrent!!.handle))
                adapter.checkAll(isChecked)
        }

        piecesViewTv.setOnClickListener {
            scrollViewLayout.smoothScrollTo(
                0, torrentPiecesLayout.y.toInt()
            )
        }

        filesViewTv.setOnClickListener {
            scrollViewLayout.smoothScrollTo(
                0, torrentFilesLayout.y.toInt()
            )
        }
    }

    private fun updateView() {
        torrentNameTv.description = torrent!!.name
        torrentHashTv.description = torrent!!.hash
        torrentPathTv.description = torrent!!.path
        torrentPiecesTv.descriptionTextView()
            .setNAText(
                if (!Libtorrent.metaTorrent(torrent!!.handle)) ""
                else Libtorrent.torrentPiecesCount(torrent!!.handle).toString()
                        + "/" + Libtorrent.torrentPieceLength(torrent!!.handle).formatSize()
            )
        piecesView.drawPieces(torrent!!.handle)
        torrentSpeedTv.description =
            "↓ ${torrent!!.downloadSpeed.formatSpeed()} · ↑ ${torrent!!.uploadSpeed.formatSpeed()}"

        torrentFileSizeTv.description = torrent!!.totalSize.formatSize()

        val trackersCount = Libtorrent.torrentTrackersCount(torrent!!.handle)
        var seeders = 0L
        var leechers = 0L
        var peers = 0L
        for (i in 0 until trackersCount) {
            seeders += Libtorrent.torrentTrackers(torrent!!.handle, i).seeders
            leechers += Libtorrent.torrentTrackers(torrent!!.handle, i).leechers
            peers += Libtorrent.torrentTrackers(torrent!!.handle, i).peers
        }

        if (Libtorrent.metaTorrent(torrent!!.handle)) {
            headerLayout.emptyTv.visibility = View.GONE
        }

        torrentSeedersLeechersTv.description = "S:$seeders / L:$leechers"
        torrentPeersTv.description = peers.toString()
        adapter.update()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun update(event: UpdateTorrentEvent) {
        if ((view == null || torrent == null || torrent?.handle == -1L) || event.hash != torrent?.hash) {
            return
        }

        if (!Libtorrent.metaTorrent(torrent!!.handle)) return

        updateView()
    }


    override fun onDestroy() {
        unregisterClass(this)
        super.onDestroy()
    }


}