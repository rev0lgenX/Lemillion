package com.revolgenx.weaverx.app

import androidx.multidex.MultiDexApplication
import com.arialyy.aria.core.Aria
import com.revolgenx.weaverx.core.coreModules
import com.revolgenx.weaverx.viewmodel.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import timber.log.Timber

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        Aria.init(this).apply {
            appConfig.isNetCheck = true
            appConfig.isNotNetRetry = false
            downloadConfig.threadNum = 6
//            downloadConfig.maxTaskNum = sharePreference().getInt(Key.MAX_TASK_NUM, 3)
            downloadConfig.maxTaskNum = 2
            downloadConfig.reTryNum = 5
            downloadConfig.isConvertSpeed = true
        }

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
