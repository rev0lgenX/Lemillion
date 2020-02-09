package com.revolgenx.lemillion.event

import com.revolgenx.lemillion.core.torrent.Torrent

data class TorrentEvent(var torrents: List<Torrent>, var type: TorrentEventType)

enum class TorrentEventType {
    TORRENT_PAUSED, TORRENT_RESUMED, TORRENT_FINISHED, TORRENT_ERROR
}