package com.revolgenx.weaverx.core.db.converter

import androidx.room.TypeConverter
import com.revolgenx.weaverx.core.torrent.TorrentStatus

class TorrentStatusConverter {
    @TypeConverter
    fun fromStatus(value: TorrentStatus) = value.ordinal

    @TypeConverter
    fun fromInt(value: Int) = TorrentStatus.values()[value]
}