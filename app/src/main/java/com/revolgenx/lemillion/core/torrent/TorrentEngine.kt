package com.revolgenx.lemillion.core.torrent

import com.revolgenx.lemillion.core.exception.TorrentException
import com.revolgenx.lemillion.core.exception.TorrentLoadException
import com.revolgenx.lemillion.core.torrent.util.array2vector
import com.revolgenx.lemillion.core.util.postEvent
import com.revolgenx.lemillion.event.TorrentEngineEvent
import com.revolgenx.lemillion.event.TorrentEngineEventTypes
import org.libtorrent4j.*
import org.libtorrent4j.swig.*
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock


//TODO:CHECK ENGINE BEFORE ADDING MAGNET OR TORRENT

class TorrentEngine : SessionManager() {

    var isEngineRunning: AtomicBoolean = AtomicBoolean(false)
    var settings = Settings()
    private val syncMagnet = ReentrantLock()


    class Settings {
        var cacheSize = DEFAULT_CACHE_SIZE
        var activeDownloads = DEFAULT_ACTIVE_DOWNLOADS
        var activeSeeds = DEFAULT_ACTIVE_SEEDS
        var maxPeerListSize = DEFAULT_MAX_PEER_LIST_SIZE
        var tickInterval = DEFAULT_TICK_INTERVAL
        var inactivityTimeout = DEFAULT_INACTIVITY_TIMEOUT
        var connectionsLimit = DEFAULT_CONNECTIONS_LIMIT
        var connectionsLimitPerTorrent =
            DEFAULT_CONNECTIONS_LIMIT_PER_TORRENT
        var uploadsLimitPerTorrent = DEFAULT_UPLOADS_LIMIT_PER_TORRENT
        var activeLimit = DEFAULT_ACTIVE_LIMIT
        var port = DEFAULT_PORT
        var downloadRateLimit = DEFAULT_DOWNLOAD_RATE_LIMIT
        var uploadRateLimit = DEFAULT_UPLOAD_RATE_LIMIT
        var dhtEnabled = DEFAULT_DHT_ENABLED
        var lsdEnabled = DEFAULT_LSD_ENABLED
        var utpEnabled = DEFAULT_UTP_ENABLED
        var upnpEnabled = DEFAULT_UPNP_ENABLED
        var natPmpEnabled = DEFAULT_NATPMP_ENABLED
        var encryptInConnections = DEFAULT_ENCRYPT_IN_CONNECTIONS
        var encryptOutConnections =
            DEFAULT_ENCRYPT_OUT_CONNECTIONS
        var encryptMode = DEFAULT_ENCRYPT_MODE
        var autoManaged = DEFAULT_AUTO_MANAGED

        companion object {
            const val DEFAULT_CACHE_SIZE = 256
            const val DEFAULT_ACTIVE_DOWNLOADS = 4
            const val DEFAULT_ACTIVE_SEEDS = 4
            const val DEFAULT_MAX_PEER_LIST_SIZE = 200
            const val DEFAULT_TICK_INTERVAL = 1000
            const val DEFAULT_INACTIVITY_TIMEOUT = 60
            const val MIN_CONNECTIONS_LIMIT = 2
            const val DEFAULT_CONNECTIONS_LIMIT = 200
            const val DEFAULT_CONNECTIONS_LIMIT_PER_TORRENT = 40
            const val DEFAULT_UPLOADS_LIMIT_PER_TORRENT = 4
            const val DEFAULT_ACTIVE_LIMIT = 6
            const val DEFAULT_PORT = 6881
            const val MAX_PORT_NUMBER = 65535
            const val MIN_PORT_NUMBER = 49160
            const val DEFAULT_DOWNLOAD_RATE_LIMIT = 0
            const val DEFAULT_UPLOAD_RATE_LIMIT = 0
            const val DEFAULT_DHT_ENABLED = true
            const val DEFAULT_LSD_ENABLED = true
            const val DEFAULT_UTP_ENABLED = true
            const val DEFAULT_UPNP_ENABLED = true
            const val DEFAULT_NATPMP_ENABLED = true
            const val DEFAULT_ENCRYPT_IN_CONNECTIONS = true
            const val DEFAULT_ENCRYPT_OUT_CONNECTIONS = true
            val DEFAULT_ENCRYPT_MODE = settings_pack.enc_policy.pe_enabled.swigValue()
            const val DEFAULT_AUTO_MANAGED = false
        }
    }

    override fun onBeforeStart() {
        super.onBeforeStart()
        Timber.d("torrent engine starting")

    }

    override fun onAfterStart() {
        super.onAfterStart()
        Timber.d("torrent engine started")
    }

    override fun start() {
        val params = loadSetting()

        if (isEngineRunning()) {
            postEvent(
                TorrentEngineEvent(
                    TorrentEngineEventTypes.ENGINE_STARTED
                )
            )
            return
        }

        postEvent(
            TorrentEngineEvent(
                TorrentEngineEventTypes.ENGINE_STARTING
            )
        )


        Thread {
            super.start(params)
            isEngineRunning.set(true)
            postEvent(
                TorrentEngineEvent(
                    TorrentEngineEventTypes.ENGINE_STARTED
                )
            )
        }.start()

    }

    override fun onBeforeStop() {
        super.onBeforeStop()
        Timber.d("torrent engine stopping")
        isEngineRunning.set(false)
        postEvent(TorrentEngineEvent(TorrentEngineEventTypes.ENGINE_STOPPING))
    }


    override fun stop() {
        if (isEngineRunning()) {
            Thread {
                super.stop()
            }.start()
        }
    }


    override fun onAfterStop() {
        super.onAfterStop()
        Timber.d("torrent engine stopped")
        postEvent(TorrentEngineEvent(TorrentEngineEventTypes.ENGINE_STOPPED))
    }


    @Throws(Exception::class)
    fun fetchMagnet(uri: String): TorrentHandle {
        val ec = error_code()
        val p = add_torrent_params.parse_magnet_uri(uri, ec)
        require(ec.value() == 0) { ec.message() }
        p.set_disabled_storage()
        val hash = p.info_hash
        val strHash = hash.to_hex()
        var th: torrent_handle? = null
        return try {
            syncMagnet.lock()
            try {
                th = swig().find_torrent(hash)
                if (th != null && th.is_valid) {
                    throw TorrentLoadException("Torrent exists!!!", null)
                }

                if (p.name.isEmpty()) p.name = strHash
                var flags = p.flags
                flags = flags.and_(TorrentFlags.AUTO_MANAGED.inv())
                flags = flags.or_(TorrentFlags.UPLOAD_MODE)
                flags = flags.or_(TorrentFlags.STOP_WHEN_READY)
                p.flags = flags
                ec.clear()
                th = swig().add_torrent(p, ec)
                th.resume()
                TorrentHandle(th)
            } finally {
                syncMagnet.unlock()
            }
        } catch (e: TorrentLoadException) {
            throw e
        } catch (e: Exception) {
            if (th != null && th.is_valid) swig().remove_torrent(th)
            throw Exception(e)
        }
    }

    fun cancelFetchMagnet(infoHash: String) {
        val th = find(Sha1Hash(infoHash))
        if (th != null && th.isValid) remove(th, SessionHandle.DELETE_FILES)
    }


    fun loadTorrent(magnetUri: String?, saveDir: File?): TorrentHandle? {
        if (swig() == null) {
            return null
        }
        val ec = error_code()
        val p = add_torrent_params.parse_magnet_uri(magnetUri, ec)
        require(ec.value() == 0) { ec.message() }
        val info_hash = p.info_hash
        var th = swig().find_torrent(info_hash)
        if (th != null && th.is_valid) { // found a download with the same hash
            return TorrentHandle(th)
        }
        if (saveDir != null) {
            p.save_path = saveDir.absolutePath
        }
        var flags = p.flags
        flags = flags.and_(TorrentFlags.AUTO_MANAGED.inv())
        p.flags = flags
        th = swig().add_torrent(p, ec)
        return TorrentHandle(th)
    }


    @Throws(TorrentLoadException::class)
    fun loadTorrent(
        ti: TorrentInfo,
        saveDir: File?,
        resumeFile: ByteArray?,
        pri: Array<Priority>?,
        peers: MutableList<TcpEndpoint>?
    ): TorrentHandle? {
        if (swig() == null) {
            return null
        }
        var th = swig().find_torrent(ti.swig().info_hash())
        var priorities = pri
        if (th != null && th.is_valid) { // found a download with the same hash, just adjust the priorities if needed
//            if (priorities != null) {
//                require(ti.numFiles() == priorities.size) { "priorities count should be equals to the number of files" }
//                th.prioritize_files2(array2vector(priorities))
//            } else { // did they just add the entire torrent (therefore not selecting any priorities)
//                priorities = Priority.array(
//                    Priority.DEFAULT,
//                    ti.numFiles()
//                )
//                th.prioritize_files2(array2vector(priorities))
//            }
            throw TorrentException("Torrent Exists!!!", TorrentHandle(th))
        }

        var p: add_torrent_params? = null

        if (resumeFile != null) {
            try {
                val ec = error_code()
                p = add_torrent_params.read_resume_data(Vectors.bytes2byte_vector(resumeFile), ec)
                require(ec.value() == 0) { "Unable to read the resume data: " + ec.message() }
            } catch (e: Throwable) {
                Timber.e(e, "Unable to set resume data")
            }
        }

        if (p == null) {
            p = add_torrent_params.create_instance()
        }

        p!!.set_ti(ti.swig())
        if (saveDir != null) {
            p.save_path = saveDir.absolutePath
        }

        if (priorities != null) {
            require(ti.files().numFiles() == priorities.size) { "priorities count should be equals to the number of files" }
            val v = byte_vector()
            for (i in priorities.indices) {
                v.push_back(priorities[i].swig().toByte())
            }
            p.set_file_priorities2(v)
        }

        if (peers != null && peers.isNotEmpty()) {
            val v = tcp_endpoint_vector()
            for (endp in peers) {
                v.push_back(endp.swig())
            }
            p._peers = v
        }

        var flags = p.flags

        flags = flags.and_(TorrentFlags.AUTO_MANAGED.inv())

        p.flags = flags
        val ec = error_code()
        th = swig().add_torrent(p, ec)
        return TorrentHandle(th)
    }


    private fun loadSetting(): SessionParams {
        return SessionParams(defaultSettingsPack())
    }

    private fun defaultSettingsPack(): SettingsPack {
        val sp = SettingsPack()
        val maxQueuedDiskBytes = sp.maxQueuedDiskBytes()
        sp.maxQueuedDiskBytes(maxQueuedDiskBytes / 2)
        val sendBufferWatermark = sp.sendBufferWatermark()
        sp.sendBufferWatermark(sendBufferWatermark / 2)
        sp.seedingOutgoingConnections(false)
        settingsToSettingsPack(settings, sp)
        return sp
    }


    private fun settingsToSettingsPack(settings: Settings, sp: SettingsPack) {
        sp.cacheSize(settings.cacheSize)
        sp.activeDownloads(settings.activeDownloads)
        sp.activeSeeds(settings.activeSeeds)
        sp.activeLimit(settings.activeLimit)
        sp.maxPeerlistSize(settings.maxPeerListSize)
        sp.tickInterval(settings.tickInterval)
        sp.inactivityTimeout(settings.inactivityTimeout)
        sp.connectionsLimit(settings.connectionsLimit)
        sp.setString(
            settings_pack.string_types.listen_interfaces.swigValue(),
            "0.0.0.0:" + settings.port
        )
        sp.enableDht(settings.dhtEnabled)
        sp.broadcastLSD(settings.lsdEnabled)
        sp.setBoolean(settings_pack.bool_types.enable_incoming_utp.swigValue(), settings.utpEnabled)
        sp.setBoolean(settings_pack.bool_types.enable_outgoing_utp.swigValue(), settings.utpEnabled)
        sp.setBoolean(settings_pack.bool_types.enable_upnp.swigValue(), settings.upnpEnabled)
        sp.setBoolean(settings_pack.bool_types.enable_natpmp.swigValue(), settings.natPmpEnabled)
        sp.setInteger(settings_pack.int_types.in_enc_policy.swigValue(), settings.encryptMode)
        sp.setInteger(settings_pack.int_types.out_enc_policy.swigValue(), settings.encryptMode)
        sp.uploadRateLimit(settings.uploadRateLimit)
        sp.downloadRateLimit(settings.downloadRateLimit)
    }


    fun isEngineRunning() = isEngineRunning.get()


}