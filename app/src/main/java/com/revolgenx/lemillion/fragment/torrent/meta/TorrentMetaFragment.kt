package com.revolgenx.lemillion.fragment.torrent.meta

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.torrent.TorrentProgressListener
import com.revolgenx.lemillion.core.torrent.TorrentState
import com.revolgenx.lemillion.core.util.*
import com.revolgenx.lemillion.view.makeToast
import com.revolgenx.lemillion.view.string
import com.revolgenx.lemillion.view.setNAText
import kotlinx.android.synthetic.main.torrent_meta_fragment.*
import kotlinx.android.synthetic.main.torrent_meta_fragment.torrentPiecesTv
import java.io.File
import java.util.*

class TorrentMetaFragment : TorrentBaseMetaFragment(), TorrentProgressListener {

    private val date = Date()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.torrent_meta_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (!checkValidity()) return

        torrent.addListener(this)
        addListener()
        updateView()
    }

    fun addListener() {
        torrentNameTv.setDrawableClickListener {
            MaterialDialog(this.context!!).show {
                title(R.string.torrent_name)
                input(prefill = this@TorrentMetaFragment.torrentNameTv.description) { _, charSequence ->
                    val newPath = torrent.path + "$charSequence"
                    if (File(newPath).exists()) {
                        makeToast(string(R.string.file_exists))
                        return@input
                    }
                    if (torrent.checkValidity())
                        torrent.handle!!.torrentFile().files().name(charSequence.toString())
                }

                negativeButton()
            }
        }

        torrentHashTv.setDrawableClickListener {
            context!!.copyToClipBoard(torrentHashTv.description)
        }
    }

    override fun invoke() {
        updateView()
    }

    private fun updateView() {
        if (!checkValidity()) return
        if (context == null) return

        val handle = torrent.handle
        val status = torrent.torrentStatus()
        torrentNameTv.description = torrent.name
        torrentHashTv.description = torrent.hash
        torrentPathTv.description = torrent.path
        torrentPiecesTv.descriptionTextView()
            .setNAText(
                if (status.hasMetadata()) {
                    status.numPieces().toString() + "/" + handle!!.torrentFile().numPieces().toString() + " (" +
                            handle.torrentFile().pieceLength().toLong().formatSize() + ")"
                } else ""
            )
        torrentSpeedTv.descriptionTextView().setNAText(
            if (torrent.state == TorrentState.DOWNLOADING || torrent.state == TorrentState.DOWNLOADING_METADATA) {
                "↓ ${torrent.downloadSpeed.formatSpeed()} · ↑ ${torrent.uploadSpeed.formatSpeed()}"
            } else ""
        )

        torrentFileSizeTv.titleTextView().text =
            context!!.string(R.string.size_free).format(getFree(File(torrent.path)).formatSize())
        torrentFileSizeTv.description = torrent.totalSize.formatSize()
        torrentSeedersLeechersTv.description =
            "${torrent.connectedSeeders()} (${torrent.totalSeeders()}) / ${torrent.connectedLeechers()} (${torrent.totalLeechers()})"
        torrentPeersTv.description = "${torrent.connectedPeers()} (${torrent.totalPeers()})"

        torrentDownloadedUploadedTv.description =
            "${status.allTimeDownload().formatSize()} (D)/${status.allTimeUpload().formatSize()} (U)"

        val eta = torrent.eta()
        torrentEtaTv.descriptionTextView()
            .setNAText(
                if (eta == 0L) "" else torrent.eta().formatRemainingTime()
            )

        torrentAddedTv.description = torrent.createDate.toString()

        val createdDate = if (torrent.torrentStatus().hasMetadata()) {
            handle!!.torrentFile().creationDate()
        } else 0

        torrentCreatedTv.descriptionTextView().setNAText(
            if (createdDate == 0L) "" else date.apply { time = createdDate * 1000 }.toString()
        )
    }

    override fun onDestroy() {
        if (checkValidity()) {
            torrent.removeListener(this)
            torrent.removeEngineListener()
        }
        super.onDestroy()
    }


    override fun getTitle(context: Context): String {
        return context.getString(R.string.general)
    }

}