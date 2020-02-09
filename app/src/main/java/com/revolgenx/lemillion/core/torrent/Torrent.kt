package com.revolgenx.lemillion.core.torrent

import android.os.Parcel
import android.os.Parcelable
import com.revolgenx.lemillion.core.db.torrent.TorrentEntity
import com.revolgenx.lemillion.core.util.postEvent
import com.revolgenx.lemillion.core.util.postStickyEvent
import com.revolgenx.lemillion.event.TorrentEvent
import com.revolgenx.lemillion.event.TorrentEventType
import com.revolgenx.lemillion.event.UpdateDataBase
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.libtorrent4j.*
import org.libtorrent4j.TorrentStatus.State
import org.libtorrent4j.alerts.*
import org.libtorrent4j.swig.add_torrent_params
import timber.log.Timber
import java.io.File
import java.util.*


typealias TorrentProgressListener = (() -> Unit)?

class Torrent() : Parcelable, KoinComponent, AlertListener {

    constructor(parcel: Parcel) : this() {
        hash = parcel.readString()!!
        handle = engine.find(Sha1Hash(hash))
        path = parcel.readString()!!
        createDate = Date(parcel.readLong())
        source = parcel.createByteArray()!!
        fastResumeData = parcel.createByteArray()!!
        hasError = parcel.readInt() == 1
        errorMsg = parcel.readString()!!
    }

    private var lastSaveResumeTime: Long = 0
    private val SAVE_RESUME_SYNC_TIME: Long = 10000 /* ms */


    override fun types(): IntArray = intArrayOf(
        AlertType.ADD_TORRENT.swig(),
        AlertType.BLOCK_DOWNLOADING.swig(),
        AlertType.TORRENT_CHECKED.swig(),
        AlertType.STATE_UPDATE.swig(),
        AlertType.BLOCK_FINISHED.swig(),
        AlertType.STATE_CHANGED.swig(),
        AlertType.TORRENT_FINISHED.swig(),
        AlertType.TORRENT_REMOVED.swig(),
        AlertType.TORRENT_PAUSED.swig(),
        AlertType.TORRENT_RESUMED.swig(),
        AlertType.STATS.swig(),
        AlertType.SAVE_RESUME_DATA.swig(),
        AlertType.STORAGE_MOVED.swig(),
        AlertType.STORAGE_MOVED_FAILED.swig(),
        AlertType.METADATA_RECEIVED.swig(),
        AlertType.PIECE_FINISHED.swig(),
        AlertType.READ_PIECE.swig(),
        AlertType.TORRENT_ERROR.swig(),
        AlertType.METADATA_FAILED.swig(),
        AlertType.FILE_ERROR.swig()
    )

    var listeners = mutableListOf<TorrentProgressListener>()

    private val engine: TorrentEngine by inject()

    init {
        engine.addListener(this)
    }

    var hash: String = ""
    var errorMsg: String = ""
    var hasError = false
    var priorities: Array<Priority>? = null
        get() {
            return if (handle == null || handle?.isValid == false) null
            else if (handle!!.status().hasMetadata()) handle!!.filePriorities() else field
        }

    var fastResumeData: ByteArray = byteArrayOf()
    var source: ByteArray = byteArrayOf()
        set(value) {
            Timber.d("source ${value.contentToString()} fast $fastResumeData")
            if (value.isNotEmpty())
                field = value

            if (value.isNotEmpty() && handle == null) {
                handle =
                    engine.loadTorrent(
                        TorrentInfo(value),
                        File(path),
                        fastResumeData,
                        priorities,
                        null
                    )
            } else if (magnet.isNotEmpty() && handle == null) {
                handle = engine.loadTorrent(magnet, File(path))
            }
        }

    var handle: TorrentHandle? = null
    var magnet: String = ""

    var path: String = ""
    var name: String = ""
        get() {
            if (handle == null) return field
            return handle!!.name()
        }

    var simpleState: Boolean = false //true running else false
    val progress: Float
        get() {
            if (handle == null) return 0f
            return handle!!.status().progress() * 100
        }

    var createDate: Date = Date(System.currentTimeMillis())
    val downloadSpeed: Long
        get() {
            if(!checkValidity())return 0
            return handle!!.status().downloadRate().toLong()
        }

    val uploadSpeed: Long
        get() {
            if(!checkValidity())return 0
            return handle!!.status().uploadRate().toLong()
        }

    var state: TorrentState = TorrentState.UNKNOWN
        get() {
            if (handle == null) return TorrentState.UNKNOWN

            val state = handle!!.status()
            return if (isPausedWithState()) {
                TorrentState.PAUSED
            } else if (isPausedWithState() && state.isFinished)
                TorrentState.COMPLETED
            else if (isPausedWithState() && !state.isFinished)
                TorrentState.PAUSED;
            else if (!isPausedWithState() && state.isFinished)
                TorrentState.SEEDING
            else when (state.state()) {
                State.CHECKING_FILES -> {
                    TorrentState.CHECKING_FILES
                }
                State.DOWNLOADING_METADATA -> {
                    TorrentState.DOWNLOADING_METADATA
                }
                State.DOWNLOADING -> {
                    TorrentState.DOWNLOADING
                }
                State.FINISHED -> {
                    TorrentState.COMPLETED
                }
                State.SEEDING -> {
                    TorrentState.SEEDING
                }
                State.ALLOCATING -> {
                    TorrentState.ALLOCATING
                }
                State.CHECKING_RESUME_DATA -> {
                    TorrentState.CHECKING_RESUME_DATA
                }
                State.UNKNOWN -> {
                    TorrentState.UNKNOWN
                }
                else -> {
                    TorrentState.UNKNOWN
                }
            }
        }


    val totalCompleted: Long
        get() {
            return if (!checkValidity()) 0
            else handle!!.status().allTimeDownload()
        }
    val totalSize: Long
        get() {
            if (handle == null) return 0
            return if (handle!!.status().hasMetadata()) {
                handle!!.torrentFile().totalSize()
            } else 0
        }

    fun connectedPeers() =
        if (!checkValidity()) 0
        else torrentStatus().numPeers()

    fun connectedSeeders() =
        if (!checkValidity()) 0
        else torrentStatus().numSeeds()

    fun connectedLeechers() =
        if (!checkValidity()) 0
        else connectedPeers() - connectedSeeders()


    fun totalPeers() =
        if (!checkValidity()) 0
        else (torrentStatus().numComplete() + torrentStatus().numIncomplete()).let { if (it > 0) it else torrentStatus().listPeers() }

    fun totalSeeders() =
        if (!checkValidity()) 0
        else torrentStatus().numComplete().let { if (it > 0) it else torrentStatus().listSeeds() }

    fun totalLeechers() =
        if (!checkValidity()) 0
        else torrentStatus().numIncomplete().let { if (it > 0) it else totalPeers() - totalSeeders() }


    fun checkValidity() = handle != null && handle?.isValid == true

    fun torrentStatus() = handle!!.status()

    fun eta(): Long {
        return if (!checkValidity() && state != TorrentState.DOWNLOADING && !torrentStatus().hasMetadata()) 0
        else
            handle!!.torrentFile().let {
                val left = it.totalSize() - torrentStatus().totalDone()
                val rate = torrentStatus().downloadPayloadRate()
                if (left <= 0 || rate <= 0) 0 else left / rate
            }
    }

    fun isPausedWithState(): Boolean {
        return isPaused() && !simpleState
    }

    fun isPaused(): Boolean {
        if (handle == null || handle?.isValid == false) return true
        return handle!!.isValid && (isPaused(handle!!.status(true)))
    }


    private fun isPaused(s: TorrentStatus): Boolean {
        return s.flags().and_(TorrentFlags.PAUSED).nonZero()
    }


    //TODO: CHECK FILE EXITS | CHECK STORAGE EJECTED CONDITION| READONLY | TORRENT_ALTERED | FREE_SPACE
    fun start() {
        if (handle == null || !handle!!.isValid) return

        if (engine.settings.autoManaged)
            handle!!.setFlags(TorrentFlags.AUTO_MANAGED)
        else
            handle!!.unsetFlags(TorrentFlags.AUTO_MANAGED)
        try {
            hasError = false
            errorMsg = ""
            handle!!.resume()
        } catch (e: Exception) {

        }
        saveResumeData(true)
    }

    fun stop() {
        if (!checkValidity()) return

        handle!!.unsetFlags(TorrentFlags.AUTO_MANAGED)
        try {
            handle!!.pause()
        } catch (e: Exception) {

        }
        saveResumeData(true)
    }

    fun remove(withFiles: Boolean) {
        if (handle != null) {
            removeAllListener()
            if (withFiles)
                engine.remove(handle, SessionHandle.DELETE_FILES)
            else
                engine.remove(handle)

            removeEngineListener()
            handle = null
        }

    }

    fun update() {
        if (handle == null) return
        listeners.forEach { it?.invoke() }
    }


    /*
     * Generate fast-resume data for the torrent, see libtorrent documentation
     */

    /*
     * Generate fast-resume data for the torrent, see libtorrent documentation
     */
    fun saveResumeData(force: Boolean) {
        val now = System.currentTimeMillis()
        if (force || now - lastSaveResumeTime >= SAVE_RESUME_SYNC_TIME) {
            lastSaveResumeTime = now
        } else { /* Skip, too fast, see SAVE_RESUME_SYNC_TIME */
            return
        }
        try {
            if (handle != null && handle!!.isValid) {
                handle!!.saveResumeData(TorrentHandle.SAVE_INFO_DICT)
            }
        } catch (e: Exception) {
            Timber.w("%s:", "Error triggering resume data of " + hash)
            Timber.e(e)
        }
    }


    fun check() {
        if (handle == null) return
        handle!!.forceRecheck()
        start()
        update()
    }

    override fun equals(other: Any?): Boolean {
        return if ((other is Torrent)) {
            other.hash == hash
        } else false
    }

    override fun alert(alert: Alert<*>?) {
        if (alert !is TorrentAlert<*>) return
        if (handle == null) return
        if (!alert.handle().swig().op_eq(handle!!.swig())) return

        if (listeners.isEmpty()) return

        when (alert.type()) {
            AlertType.BLOCK_FINISHED -> {

            }
            AlertType.STATE_CHANGED -> {
                update()
            }
            AlertType.TORRENT_FINISHED -> {
                saveResumeData(true)
                postEvent(TorrentEvent(listOf(this), TorrentEventType.TORRENT_FINISHED))
                update()
            }
            AlertType.TORRENT_REMOVED -> {

            }
            AlertType.TORRENT_PAUSED -> {
                postStickyEvent(
                    TorrentEvent(
                        listOf(this),
                        TorrentEventType.TORRENT_PAUSED
                    )
                )
                simpleState = false
                update()
            }
            AlertType.TORRENT_RESUMED -> {
                postStickyEvent(
                    TorrentEvent(
                        listOf(this),
                        TorrentEventType.TORRENT_RESUMED
                    )
                )
                simpleState = true
                update()
            }
            AlertType.STATS -> {
                update()
            }
            AlertType.SAVE_RESUME_DATA -> {
                fastResumeData =
                    Vectors.byte_vector2bytes(add_torrent_params.write_resume_data((alert as SaveResumeDataAlert).params().swig()).bencode())
                update()
            }
            AlertType.STORAGE_MOVED -> {
                saveResumeData(true)
                update()
            }
            AlertType.STORAGE_MOVED_FAILED -> {
                saveResumeData(true)
                update()
            }
            AlertType.PIECE_FINISHED -> {
                saveResumeData(false)
//                update()
            }
            AlertType.METADATA_RECEIVED -> {
                val metadataAlert = alert as MetadataReceivedAlert
                source = metadataAlert.torrentData()
                postEvent(UpdateDataBase(this))
                update()
            }
            else -> checkError(alert)
        }
    }

    private fun checkError(alert: Alert<*>) {
        when (alert.type()) {
            AlertType.TORRENT_ERROR -> {
                val errorAlert = alert as TorrentErrorAlert
                val error = errorAlert.error()
                if (error.isError) {
                    val filename = errorAlert.filename().substring(
                        errorAlert.filename().lastIndexOf("/") + 1
                    )
                    if (errorAlert.filename() != null) errorMsg = "[$filename] "
                    errorMsg += error.message() ?: ""
                    hasError = true
                    postEvent(
                        TorrentEvent(
                            listOf(this),
                            TorrentEventType.TORRENT_ERROR
                        )
                    )
                    update()
                }
            }
            AlertType.METADATA_FAILED -> {
                val metadataFailedAlert = alert as MetadataFailedAlert
                val error = metadataFailedAlert.error
                if (error.isError) {
                    hasError = true
                    errorMsg = error.message() ?: ""
                    postEvent(
                        TorrentEvent(
                            listOf(this),
                            TorrentEventType.TORRENT_ERROR
                        )
                    )
                    update()
                }
            }
            AlertType.FILE_ERROR -> {
                val fileErrorAlert = alert as FileErrorAlert
                val error = fileErrorAlert.error()
                val filename = fileErrorAlert.filename().substring(
                    fileErrorAlert.filename().lastIndexOf("/") + 1
                )
                if (error.isError) {
                    hasError = true
                    errorMsg = "[" + filename + "] " + error.message() ?: ""
                    postEvent(
                        TorrentEvent(
                            listOf(this),
                            TorrentEventType.TORRENT_ERROR
                        )
                    )
                    update()
                }
            }
        }
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }

    fun toEntity(): TorrentEntity {
        return TorrentEntity(
            hash,
            state,
            path,
            magnet,
            createDate,
            fastResumeData,
            source,
            priorities,
            hasError,
            errorMsg
        )
    }


    fun addListener(torrentProgressListener: TorrentProgressListener) {
        listeners.add(torrentProgressListener)
    }


    fun addEngineListener() {
        engine.addListener(this)
    }

    fun removeEngineListener() {
        engine.removeListener(this)
    }

    fun removeListener(torrentProgressListener: TorrentProgressListener) {
        listeners.remove(torrentProgressListener)
    }

    fun removeAllListener() {
        listeners.clear()
    }


    fun forceReannounce() {
        if (!checkValidity()) return
        handle!!.forceReannounce()
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.apply {
            writeString(hash)
            writeString(path)
            writeLong(createDate.time)
            writeByteArray(source)
            writeByteArray(fastResumeData)
            writeInt(if (hasError) 1 else 0)
            writeString(errorMsg)
        }
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Torrent> {
        override fun createFromParcel(parcel: Parcel): Torrent {
            return Torrent(parcel)
        }

        override fun newArray(size: Int): Array<Torrent?> {
            return arrayOfNulls(size)
        }
    }
}