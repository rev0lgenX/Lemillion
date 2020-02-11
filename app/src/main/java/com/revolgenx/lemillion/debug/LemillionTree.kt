package com.revolgenx.lemillion.debug

import android.util.Log
import com.github.axet.androidlibrary.app.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class LemillionTree : Timber.DebugTree() {

    private val CRASHLYTICS_KEY_PRIORITY = "priority"
    private val CRASHLYTICS_KEY_TAG = "tag"
    private val CRASHLYTICS_KEY_MESSAGE = "message"

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        val t = throwable ?: Exception(message)

        FirebaseCrashlytics.getInstance().setCustomKey(CRASHLYTICS_KEY_PRIORITY, priority)
        FirebaseCrashlytics.getInstance().setCustomKey(CRASHLYTICS_KEY_TAG, tag ?: "tag")
        FirebaseCrashlytics.getInstance().setCustomKey(CRASHLYTICS_KEY_MESSAGE, message)
        FirebaseCrashlytics.getInstance().recordException(t)

    }
}