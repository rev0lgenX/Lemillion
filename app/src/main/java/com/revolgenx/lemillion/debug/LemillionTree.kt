package com.revolgenx.lemillion.debug

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class LemillionTree : Timber.DebugTree() {

    private val priorityKey = "priority"
    private val tagKey = "tag"
    private val messageKey = "message"

    override fun log(priority: Int, tag: String?, message: String, throwable: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        val t = throwable ?: Exception(message)

        FirebaseCrashlytics.getInstance().setCustomKey(priorityKey, priority)
        FirebaseCrashlytics.getInstance().setCustomKey(tagKey, tag ?: "tag")
        FirebaseCrashlytics.getInstance().setCustomKey(messageKey, message)
        FirebaseCrashlytics.getInstance().recordException(t)

    }
}