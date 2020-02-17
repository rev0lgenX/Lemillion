package com.revolgenx.lemillion.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.arialyy.aria.core.Aria
import com.revolgenx.lemillion.core.coreModules
import com.revolgenx.lemillion.model.BookPreferenceModel
import com.revolgenx.lemillion.model.modelModule
import com.revolgenx.lemillion.viewmodel.viewModelModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import timber.log.Timber

class App : MultiDexApplication() {


    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        Timber.plant(Timber.DebugTree())
        startKoin {
            androidContext(this@App)
            loadKoinModules(
                listOf(
                    coreModules,
                    viewModelModule,
                    modelModule
                )
            )
        }

        val bookPref by inject<BookPreferenceModel>()

        Aria.init(this).apply {
            appConfig.isNetCheck = true
            appConfig.isNotNetRetry = false
            downloadConfig.threadNum = bookPref.numThread
            downloadConfig.maxTaskNum = bookPref.numTask
            downloadConfig.reTryNum = bookPref.numRetry
        }

    }

}
