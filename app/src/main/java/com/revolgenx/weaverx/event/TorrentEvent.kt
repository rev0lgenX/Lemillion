package com.revolgenx.weaverx.event

import com.revolgenx.weaverx.core.torrent.Torrent

data class TorrentEvent(var torrents: List<Torrent>, var type: TorrentEventType)

enum class TorrentEventType {
    TORRENT_PAUSED, TORRENT_RESUMED
}