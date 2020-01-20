package com.revolgenx.weaverx.app

import androidx.multidex.MultiDexApplication
import com.revolgenx.weaverx.core.coreModules
import com.revolgenx.weaverx.viewmodel.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import timber.log.Timber

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
        startKoin {
            androidContext(this@App)
            loadKoinModules(
                listOf(
                    coreModules,
                    viewModelModule
                )
            )
        }
    }

}
