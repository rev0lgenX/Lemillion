package com.revolgenx.lemillion.core

import androidx.room.Room
import com.revolgenx.lemillion.core.db.WeaverXDatabase
import com.revolgenx.lemillion.core.db.torrent.TorrentRepository
import com.revolgenx.lemillion.core.service.ServiceConnector
import com.revolgenx.lemillion.core.torrent.TorrentEngine
import org.koin.dsl.module

val coreModules = module {

    //torrent engine
    single { TorrentEngine() }

    //service module
    factory { ServiceConnector(get()) }

    //database
    single {
        Room.databaseBuilder(get(), WeaverXDatabase::class.java, "weaverx_torrent.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    single {
        get<WeaverXDatabase>().torrentDao()
    }

    single {
        TorrentRepository(get())
    }

}