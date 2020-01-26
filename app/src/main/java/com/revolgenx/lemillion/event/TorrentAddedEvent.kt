package com.revolgenx.lemillion.event

data class TorrentAddedEvent(var handle: Long, var hash: String,var path:String, var type: TorrentAddedEventTypes)

enum class TorrentAddedEventTypes {
    MAGNET_ADDED, MAGNET_ADD_ERROR, TORRENT_ADDED, TORRENT_ADD_ERROR
}