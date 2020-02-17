package com.revolgenx.lemillion.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.arialyy.aria.core.Aria
import com.google.android.gms.ads.MobileAds
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.revolgenx.lemillion.BuildConfig
import com.revolgenx.lemillion.core.coreModules
import com.revolgenx.lemillion.debug.LemillionTree
import com.revolgenx.lemillion.viewmodel.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import timber.log.Timber

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        Aria.init(this).apply {
            appConfig.isNetCheck = true
            appConfig.isNotNetRetry = false
            downloadConfig.threadNum = 2
//            downloadConfig.maxTaskNum = sharePreference().getInt(Key.MAX_TASK_NUM, 3)
            downloadConfig.maxTaskNum = 2
            downloadConfig.reTryNum = 5
        }

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
                    viewModelModule
                )
            )
        }

        MobileAds.initialize(this)
    }

}
