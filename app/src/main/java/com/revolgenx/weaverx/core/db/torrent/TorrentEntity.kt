package com.revolgenx.weaverx.core.db.torrent

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.revolgenx.weaverx.core.torrent.Torrent
import com.revolgenx.weaverx.core.torrent.TorrentStatus

@Entity(tableName = "torrent_table")
data class TorrentEntity(
    @PrimaryKey val hash: String,
    val state: String,
    val status: TorrentStatus,
    val path: String
) {
    fun toTorrent() = Torrent().also { t ->
        t.hash = hash
        t.path = path
        t.state = state
        t.status = status
    }
}