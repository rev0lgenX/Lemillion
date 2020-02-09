package com.revolgenx.lemillion.fragment.torrent.meta

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.torrent.TorrentProgressListener
import com.revolgenx.lemillion.core.torrent.TorrentState
import com.revolgenx.lemillion.core.util.*
import kotlinx.android.synthetic.main.torrent_meta_fragment.*
import kotlinx.android.synthetic.main.torrent_meta_fragment.torrentPiecesTv
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
        updateView()
    }

    override fun invoke() {
        updateView()
    }

    private fun updateView() {
        if (!checkValidity()) return

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
        val createdDate = handle!!.torrentFile().creationDate()
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