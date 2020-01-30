package com.revolgenx.lemillion.core.torrent

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import com.github.axet.androidlibrary.app.Storage
import com.revolgenx.lemillion.core.db.torrent.TorrentEntity
import com.revolgenx.lemillion.core.torrent.util.SpeedInfo
import libtorrent.Libtorrent
import timber.log.Timber
import java.io.File
import java.util.*

typealias TorrentProgressListener = ((torrent: Torrent) -> Unit)?


class Torrent() : Parcelable {

    constructor(parcel: Parcel) : this() {
        handle = parcel.readLong()
        hash = parcel.readString()!!
        name = parcel.readString()!!
        state = parcel.readString()!!
        path = parcel.readString()!!
        status = TorrentStatus.values()[parcel.readInt()]
        createDate = Date(parcel.readLong())
    }

    var torrentProgressListener: TorrentProgressListener = null

    var hash: String = ""
    var handle: Long = -1
        set(value) {
            field = value
            Timber.d("handle $value")
        }

    var state: String = ""
        get() {
            Timber.d("get state")
            if (handle == -1L) return field
            return Base64.encodeToString(Libtorrent.saveTorrent(handle), Base64.DEFAULT)
        }
        set(value) {
            field = value
            if (handle == -1L) {
                handle = Libtorrent.loadTorrent(path, Base64.decode(value, Base64.DEFAULT))
            }
            Timber.d("set state")
        }

    var path: String = ""

    var name: String = ""
        get() {
            Timber.d("string handle $handle")
            if (handle == -1L) return field

            return if (field.isEmpty() || field == hash)
                Libtorrent.torrentName(handle).takeIf { it.isNotEmpty() } ?: hash
            else field
        }

    val progress: Float
        get() {
            Timber.d("progress $handle")
            if (handle == -1L) return 0f
            return if (Libtorrent.metaTorrent(handle)) {
                if (status == TorrentStatus.SEEDING || completed) {
                    100f
                } else {
                    val p = Libtorrent.torrentPendingBytesLength(handle)
                    if (p == 0L) {
                        0f
                    } else {
                        Libtorrent.torrentPendingBytesCompleted(handle) * 100f / p
                    }
                }
            } else 0f
        }

    val completed: Boolean
        get() {
            return Libtorrent.pendingCompleted(handle)
        }

    val downloaded = SpeedInfo()
    val uploaded = SpeedInfo()

    var createDate: Date = Date(System.currentTimeMillis())

    val downloadSpeed: Long
        get() {
            return downloaded.currentSpeed.toLong()
        }

    val uploadSpeed: Long
        get() {
            return uploaded.currentSpeed.toLong()
        }

    var status: TorrentStatus = TorrentStatus.UNKNOWN
        get() {
            if (handle == -1L) return TorrentStatus.UNKNOWN
            return when (Libtorrent.torrentStatus(handle)) {
                Libtorrent.StatusPaused -> {
                    TorrentStatus.PAUSED
                }
                Libtorrent.StatusChecking -> {
                    TorrentStatus.CHECKING
                }
                Libtorrent.StatusDownloading -> {
                    TorrentStatus.DOWNLOADING
                }
                Libtorrent.StatusQueued -> {
                    TorrentStatus.QUEUE
                }
                Libtorrent.StatusSeeding -> {
                    TorrentStatus.SEEDING
                }
                else -> TorrentStatus.UNKNOWN
            }
        }

    val totalSize: Long
        get() {
            if (handle == -1L) return 0
            return if (Libtorrent.metaTorrent(handle)) Libtorrent.torrentPendingBytesLength(handle)
            else 0
        }

    //TODO: CHECK FILE EXITS | CHECK STORAGE EJECTED CONDITION| READONLY | TORRENT_ALTERED | FREE_SPACE
    fun start() {
        if (handle == -1L) return

        Timber.d("sampleStart $handle")
        if (!Libtorrent.startTorrent(handle)) {
            Timber.e(Libtorrent.error())
            return
        }

        update()
    }

    fun stop() {
        if (handle == -1L) return
        Libtorrent.stopTorrent(handle)
        update()
    }

    fun remove(withFiles: Boolean) {
        if (handle != -1L) {
            if (withFiles)
                for (i in 0 until Libtorrent.torrentFilesCount(handle)) {
                    Storage.delete(File(path, Libtorrent.torrentFiles(handle, i).path))
                }
            Libtorrent.removeTorrent(handle)
            handle = -1
        }

    }

    fun update() {
        if (handle == -1L) return

        val b = Libtorrent.torrentStats(handle)
        downloaded.step(b.downloaded)
        uploaded.step(b.uploaded)
        torrentProgressListener?.invoke(this)
    }


    fun check() {
        if (handle == -1L) return

        stop()
        Libtorrent.checkTorrent(handle)
    }

//    private fun getDownloadUploadSpeedText(context: Context) = if (!isReady()) ""
//    else {
//        var str = "· "
//        when (Libtorrent.torrentStatus(handle)) {
//            Libtorrent.StatusQueued, Libtorrent.StatusChecking, Libtorrent.StatusPaused, Libtorrent.StatusSeeding -> {
//                str += "↓ " + MainApplication.formatSize(
//                    context,
//                    downloaded.currentSpeed.toLong()
//                ) + context.getString(R.string.per_second)
//                str += " · ↑ " + MainApplication.formatSize(
//                    context,
//                    uploaded.currentSpeed.toLong()
//                ) + context.getString(
//                    R.string.per_second
//                )
//                str
//            }
//            Libtorrent.StatusDownloading -> {
//                str = " · ↓ " + MainApplication.formatSize(
//                    context,
//                    downloaded.currentSpeed.toLong()
//                ) + context.getString(R.string.per_second)
//                str += " · ↑ " + MainApplication.formatSize(
//                    context,
//                    uploaded.currentSpeed.toLong()
//                ) + context.getString(
//                    R.string.per_second
//                )
//
//                str
//            }
//            else -> ""
//        }
//    }

    override fun equals(other: Any?): Boolean {
        return if ((other is Torrent)) {
            other.hash == hash
        } else false
    }


    override fun hashCode(): Int {
        return hash.hashCode()
    }

    fun toEntity(): TorrentEntity {
        Timber.d("to entity")
        return TorrentEntity(hash, state, status, path, createDate)
    }


    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.apply {
            writeLong(handle)
            writeString(hash)
            writeString(name)
            writeString(state)
            writeString(path)
            writeInt(status.ordinal)
            writeLong(createDate.time)
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