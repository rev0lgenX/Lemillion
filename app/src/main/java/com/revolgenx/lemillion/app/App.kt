package com.revolgenx.lemillion.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.arialyy.aria.core.Aria
import com.google.android.gms.ads.MobileAds
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.revolgenx.lemillion.BuildConfig
import com.revolgenx.lemillion.core.coreModules
import com.revolgenx.lemillion.debug.LemillionTree
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

        if (BuildConfig.DEBUG) {
            Timber.plant(LemillionTree(), Timber.DebugTree())
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }


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

        MobileAds.initialize(this)
    }

}
