package com.revolgenx.lemillion.core.db.converter

import androidx.room.TypeConverter
import com.revolgenx.lemillion.core.torrent.TorrentStatus

class TorrentStatusConverter {
    @TypeConverter
    fun fromStatus(value: TorrentStatus) = value.ordinal

    @TypeConverter
    fun fromInt(value: Int) = TorrentStatus.values()[value]
}