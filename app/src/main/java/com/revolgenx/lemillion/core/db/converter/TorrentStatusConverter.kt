package com.revolgenx.lemillion.core.db.converter

import androidx.room.TypeConverter
import com.revolgenx.lemillion.core.torrent.TorrentState

class TorrentStatusConverter {
    @TypeConverter
    fun fromStatus(value: TorrentState) = value.ordinal

    @TypeConverter
    fun fromInt(value: Int) = TorrentState.values()[value]
}