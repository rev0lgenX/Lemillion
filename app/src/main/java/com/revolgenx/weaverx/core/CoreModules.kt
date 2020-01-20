package com.revolgenx.weaverx.core

import androidx.room.Room
import com.revolgenx.weaverx.core.db.WeaverXDatabase
import com.revolgenx.weaverx.core.db.torrent.TorrentRepository
import com.revolgenx.weaverx.core.service.ServiceConnector
import com.revolgenx.weaverx.core.torrent.TorrentEngine
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