package com.revolgenx.lemillion.core.db.torrent

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.revolgenx.lemillion.core.torrent.Torrent
import com.revolgenx.lemillion.core.torrent.TorrentState
import org.libtorrent4j.Priority
import java.util.*

@Suppress("ArrayInDataClass")
@Entity(tableName = "torrent_table")
data class TorrentEntity(
    @PrimaryKey val hash: String,
    val path: String,
    val magnet: String,
    val createDate: Date,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val fastResume: ByteArray,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val source: ByteArray,
    val priority: Array<Priority>?,
    val hasError: Boolean,
    val errorMsg: String
) {
    fun toTorrent() = Torrent().also { t ->
        t.hash = hash
        t.path = path
        t.magnet = magnet
        t.priorities = priority
        t.fastResumeData = fastResume
        t.source = source
        t.createDate = createDate
        t.hasError = hasError
        t.errorMsg = errorMsg
    }
}