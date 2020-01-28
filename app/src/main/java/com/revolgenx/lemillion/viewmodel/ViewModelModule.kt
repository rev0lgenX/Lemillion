package com.revolgenx.lemillion.viewmodel

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        TorrentViewModel(get(), get(), get(), get())
    }
    viewModel {
        BookViewModel(get(), get(), get())
    }
}