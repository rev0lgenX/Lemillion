package com.revolgenx.lemillion.event

import com.revolgenx.lemillion.core.torrent.Torrent

data class TorrentAddedEvent(var torrent: Torrent, var type: TorrentAddedEventTypes)

enum class TorrentAddedEventTypes {
    TORRENT_ADDED, TORRENT_ADD_ERROR
}