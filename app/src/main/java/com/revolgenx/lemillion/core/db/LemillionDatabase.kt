package com.revolgenx.lemillion.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.revolgenx.lemillion.core.db.book.BookDao
import com.revolgenx.lemillion.core.db.book.BookEntity
import com.revolgenx.lemillion.core.db.converter.BookProtocolConverter
import com.revolgenx.lemillion.core.db.converter.DateConverter
import com.revolgenx.lemillion.core.db.converter.TorrentStatusConverter
import com.revolgenx.lemillion.core.db.torrent.TorrentDao
import com.revolgenx.lemillion.core.db.torrent.TorrentEntity

@Database(entities = [TorrentEntity::class, BookEntity::class], version = 3)
@TypeConverters(DateConverter::class, TorrentStatusConverter::class, BookProtocolConverter::class)
abstract class LemillionDatabase : RoomDatabase() {
    abstract fun torrentDao(): TorrentDao
    abstract fun bookDao():BookDao
}