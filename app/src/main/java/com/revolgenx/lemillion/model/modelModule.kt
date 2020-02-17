package com.revolgenx.lemillion.model

import org.koin.dsl.module

val modelModule = module {
    single { BookPreferenceModel.getBookPreferenceInstance(get()) }
    single { TorrentPreferenceModel.getTorrentPreferenceInstance(get()) }
}