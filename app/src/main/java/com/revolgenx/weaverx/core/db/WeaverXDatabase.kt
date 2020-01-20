package com.revolgenx.weaverx.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.revolgenx.weaverx.core.db.converter.DateConverter
import com.revolgenx.weaverx.core.db.converter.TorrentStatusConverter
import com.revolgenx.weaverx.core.db.torrent.TorrentDao
import com.revolgenx.weaverx.core.db.torrent.TorrentEntity

@Database(entities = [TorrentEntity::class], version = 1)
@TypeConverters(DateConverter::class, TorrentStatusConverter::class)
abstract class WeaverXDatabase : RoomDatabase() {
    abstract fun torrentDao(): TorrentDao
}