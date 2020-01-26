package com.revolgenx.lemillion.core.db.torrent

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.revolgenx.lemillion.core.torrent.Torrent
import com.revolgenx.lemillion.core.torrent.TorrentStatus
import java.util.*

@Entity(tableName = "torrent_table")
data class TorrentEntity(
    @PrimaryKey val hash: String,
    val state: String,
    val status: TorrentStatus,
    val path: String,
    val createDate: Date
) {
    fun toTorrent() = Torrent().also { t ->
        t.hash = hash
        t.path = path
        t.state = state
        t.status = status
        t.createDate = createDate
    }
}