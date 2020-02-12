package com.revolgenx.lemillion.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.revolgenx.lemillion.core.util.postEvent
import com.revolgenx.lemillion.event.ShutdownEvent

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        const val SHUTDOWN_ACTION_KEY = "shutdown_action_key"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.hasExtra(SHUTDOWN_ACTION_KEY) == true) {
            postEvent(ShutdownEvent())
        }
    }

}