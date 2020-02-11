package com.revolgenx.lemillion.core.service

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.content.Context.ACTIVITY_SERVICE
import android.app.ActivityManager
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import com.revolgenx.lemillion.core.util.pmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber


typealias ServiceConnectionListener = ((service: MainService?, connected: Boolean) -> Unit)?

class ServiceConnector(private val context: Context) : ServiceConnection {

    private var service: MainService? = null
    private var connected = false

    var serviceConnectionListener: ServiceConnectionListener = null

    override fun onServiceConnected(name: ComponentName?, p0: IBinder?) {
        connected = true
        service = (p0 as? MainService.LocalBinder)?.service
        serviceConnectionListener?.invoke(service, connected)
        Timber.d("connected")
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        connected = false
        serviceConnectionListener?.invoke(service, connected)
        Timber.d("disconnected")
    }

    fun startService() {
        if (context.isServiceRunning().not()) {
            context.startService(Intent(context, MainService::class.java))
        }
    }

    fun stopService() {
        if (context.isServiceRunning()) {
            context.stopService(Intent(context, MainService::class.java))
        }
    }

    fun connect(mServiceConnectionListener: ServiceConnectionListener = null) {
        this.serviceConnectionListener = mServiceConnectionListener
        if (service == null && !connected) {
            Timber.d("connect")
            startService()
            context.bindService(Intent(context, MainService::class.java), this, BIND_AUTO_CREATE)
        } else {
            serviceConnectionListener?.invoke(service, connected)
        }
    }

    fun disconnect() {
        if (service != null && connected) {
            context.unbindService(this)
            service = null
            connected = false
        }
    }
}

fun Context.isServiceRunning(): Boolean {
    return runBlocking {
        return@runBlocking (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .pmap { if (MainService::class.java.name == it.service.className) it.service.className else null }
            .filterNotNull()
            .isNotEmpty()
    }
}
