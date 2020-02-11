package com.revolgenx.lemillion.core

import androidx.room.Room
import com.revolgenx.lemillion.core.db.LemillionDatabase
import com.revolgenx.lemillion.core.db.book.BookRepository
import com.revolgenx.lemillion.core.db.torrent.TorrentRepository
import com.revolgenx.lemillion.core.service.ServiceConnector
import com.revolgenx.lemillion.core.torrent.TorrentActiveState
import com.revolgenx.lemillion.core.torrent.TorrentEngine
import org.koin.dsl.module

val coreModules = module {

    //torrent engine
    single { TorrentEngine() }
    single { TorrentActiveState() }

    //service module
    factory { ServiceConnector(get()) }

    //database
    single {
        Room.databaseBuilder(get(), LemillionDatabase::class.java, "weaverx_torrent.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    single {
        get<LemillionDatabase>().torrentDao()
    }

    single {
        get<LemillionDatabase>().bookDao()
    }

    single {
        TorrentRepository(get())
    }

    single {
        BookRepository(get())
    }

}