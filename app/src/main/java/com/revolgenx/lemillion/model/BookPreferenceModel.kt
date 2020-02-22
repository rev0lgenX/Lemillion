package com.revolgenx.lemillion.model

import android.content.Context
import com.arialyy.aria.core.Aria
import com.revolgenx.lemillion.core.preference.getPrefInt
import com.revolgenx.lemillion.core.preference.putInt

class BookPreferenceModel(private val context: Context) {

    var numThread: Int = 1
        get() = context.getPrefInt(Key.NUM_THREAD.name, field)
        set(value) {
            Aria.get(context).downloadConfig.threadNum = value
            context.putInt(Key.NUM_THREAD.name, value)
        }

    var numRetry: Int = 5
        get() = context.getPrefInt(Key.NUM_RETRY.name, field)
        set(value) {
            Aria.get(context).downloadConfig.reTryNum = value
            context.putInt(Key.NUM_RETRY.name, value)
        }

    var numTask: Int = 3
        get() = context.getPrefInt(Key.NUM_TASK.name, field)
        set(value) {
            Aria.get(context).downloadConfig.maxTaskNum = value
            context.putInt(Key.NUM_TASK.name, value)
        }

    var downloadRateLimit: Int = 0
        get() = context.getPrefInt(Key.DOWNLOAD_LIMIT.name, field)
        set(value) {
            Aria.get(context).downloadConfig.maxSpeed = value
            context.putInt(Key.DOWNLOAD_LIMIT.name, value)
        }

    companion object {
        fun getBookPreferenceInstance(context: Context) = BookPreferenceModel(context)
    }

    enum class Key {
        NUM_THREAD, NUM_RETRY, NUM_TASK, DOWNLOAD_LIMIT
    }
}