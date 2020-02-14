package com.revolgenx.lemillion.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.revolgenx.lemillion.core.util.postEvent
import com.revolgenx.lemillion.event.WifiEvent


class WifiReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action
        if (action != null && action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val info =
                intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)
            if (info != null && info.type == ConnectivityManager.TYPE_WIFI) {
                postEvent(WifiEvent(info.isConnected))
            }
        }
    }
}