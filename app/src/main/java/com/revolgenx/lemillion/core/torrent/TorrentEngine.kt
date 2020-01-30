package com.revolgenx.lemillion.core.torrent

import com.revolgenx.lemillion.core.util.postEvent
import com.revolgenx.lemillion.event.TorrentEngineEvent
import com.revolgenx.lemillion.event.TorrentEngineEventTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import libtorrent.Libtorrent
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean


//TODO:CHECK ENGINE BEFORE ADDING MAGNET OR TORRENT

class TorrentEngine {

    var isEngineRunning: AtomicBoolean = AtomicBoolean(false)

    fun startEngine() {
        Timber.d("Starting engine engine_running ${isEngineRunning()}")

        if (isEngineRunning()) {
            postEvent(
                TorrentEngineEvent(
                    TorrentEngineEventTypes.ENGINE_STARTED
                )
            )
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (!isEngineRunning.get()) {
                val max = Runtime.getRuntime().maxMemory() / 1024 / 1024
                val sock: Int
                sock = when {
                    max >= 192 -> 20
                    max >= 96 -> 10
                    max >= 64 -> 5
                    else -> 2
                }

                Libtorrent.setSocketsPerTorrent(sock.toLong())
//                Libtorrent.setDownloadRate(-1)
//                Libtorrent.setUploadRate(-1)
                Libtorrent.setBindAddr(":0")

                postEvent(
                    TorrentEngineEvent(
                        TorrentEngineEventTypes.ENGINE_STARTING
                    )
                )
                if (Libtorrent.create()) {
                    Timber.d("engine started")
                    isEngineRunning.set(true)
//                    updateFromPreference()

                    postEvent(
                        TorrentEngineEvent(
                            TorrentEngineEventTypes.ENGINE_STARTED
                        )
                    )
                } else {
                    isEngineRunning.set(false)
                    postEvent(
                        TorrentEngineEvent(
                            TorrentEngineEventTypes.ENGINE_FAULT
                        )
                    )
                    Timber.e(Libtorrent.error())
                }
            }
        }
    }


    fun stopEngine() {
        synchronized(this) {
            if (isEngineRunning.get()) {
                isEngineRunning.set(false)
                Timber.d("torrent engine stopping")
                Libtorrent.close()
                Timber.d("torrent engine stopped")
                postEvent(TorrentEngineEvent(TorrentEngineEventTypes.ENGINE_STOPPED))
            }
        }
    }

//    private fun updateFromPreference() {
//        context.let {
//            sharedPref(context).let {
//                it.getString(context.getString(R.string.download_speed_key), "0")?.toLong()
//                    ?.let { Libtorrent.setDownloadRate(it * 1024) }
//                it.getString(context.getString(R.string.upload_speed_key), "0")?.toLong()
//                    ?.let { Libtorrent.setUploadRate(it * 1024) }
//                it.getString(context.getString(R.string.active_torrent_key), "5")?.toLong()
//                    ?.let { Libtorrent.torrentActive(it) }
//                it.getString(context.getString(R.string.announce_list_key), "")
//                    ?.let { Libtorrent.setDefaultAnnouncesList(it) }
//            }
//        }
//    }

    fun isEngineRunning() = isEngineRunning.get()


}