package com.revolgenx.lemillion.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.revolgenx.lemillion.core.db.converter.DateConverter
import com.revolgenx.lemillion.core.db.converter.TorrentStatusConverter
import com.revolgenx.lemillion.core.db.torrent.TorrentDao
import com.revolgenx.lemillion.core.db.torrent.TorrentEntity

@Database(entities = [TorrentEntity::class], version = 2)
@TypeConverters(DateConverter::class, TorrentStatusConverter::class)
abstract class WeaverXDatabase : RoomDatabase() {
    abstract fun torrentDao(): TorrentDao
}