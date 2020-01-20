package com.revolgenx.weaverx.core.torrent

import com.revolgenx.weaverx.core.util.postEvent
import com.revolgenx.weaverx.core.torrent.common.MagnetParser
import com.revolgenx.weaverx.core.torrent.common.TorrentMetadata
import com.revolgenx.weaverx.event.TorrentEngineEvent
import com.revolgenx.weaverx.event.TorrentEngineEventTypes
import com.revolgenx.weaverx.event.TorrentAddedEvent
import com.revolgenx.weaverx.event.TorrentAddedEventTypes
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
                    max >= 192 -> 40
                    max >= 96 -> 10
                    max >= 64 -> 5
                    else -> 2
                }

                Libtorrent.setSocketsPerTorrent(sock.toLong())
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
                    Libtorrent.setDownloadRate(5 * 1024)
                    Libtorrent.setUploadRate(5 * 1024)
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

    fun addMagnet(magnet: MagnetParser) {
        if (isEngineRunning.get()) {
            val t = Libtorrent.addMagnet(magnet.path, magnet.write())
            if (t == -1L) {
                Timber.e(Libtorrent.error())
                postEvent(
                    TorrentAddedEvent(
                        t,
                        magnet.infoHash!!,
                        magnet.path!!,
                        TorrentAddedEventTypes.MAGNET_ADD_ERROR
                    )
                )
            } else {
                postEvent(
                    TorrentAddedEvent(
                        t,
                        magnet.infoHash!!,
                        magnet.path!!,
                        TorrentAddedEventTypes.MAGNET_ADDED
                    )
                )
            }
        }
    }


    fun addTorrent(meta: TorrentMetadata) {
        if (isEngineRunning.get()) {
            val t = Libtorrent.addTorrentFromBytes(meta.path(), meta.blobs)

            if (t == -1L) {
                postEvent(
                    TorrentAddedEvent(
                        t,
                        meta.infoHash.toString(),
                        meta.path(),
                        TorrentAddedEventTypes.TORRENT_ADD_ERROR
                    )
                )
                return
            }
            postEvent(
                TorrentAddedEvent(
                    t,
                    meta.infoHash.toString(),
                    meta.path(),
                    TorrentAddedEventTypes.TORRENT_ADDED
                )
            )
        }
        return
    }


}