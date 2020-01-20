package com.revolgenx.weaverx.core.torrent

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.core.net.toUri
import com.github.axet.androidlibrary.app.Storage
import com.github.axet.wget.SpeedInfo
import com.revolgenx.weaverx.core.db.torrent.TorrentEntity
import libtorrent.Libtorrent
import timber.log.Timber
import java.io.File

typealias TorrentProgressListener = ((torrent: Torrent) -> Unit)?

class Torrent {

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
            return Base64.encodeToString(Libtorrent.saveTorrent(handle), Base64.DEFAULT)
        }
        set(value) {
            field = value
            handle = Libtorrent.loadTorrent(path, Base64.decode(value, Base64.DEFAULT))
            Timber.d("set state")
        }

    var path: String = ""

    val name: String
        get() {
            Timber.d("string handle $handle")
            return Libtorrent.torrentName(handle)
        }

    val progress: Float
        get() {
            Timber.d("progress $handle")
            return if (Libtorrent.metaTorrent(handle)) {
                val p = (Libtorrent.torrentPendingBytesLength(handle).takeIf { it != 0L } ?: 1L)
                val r = Libtorrent.torrentPendingBytesCompleted(handle) * 100f / p
                r
            } else 0f
        }

    val downloaded = SpeedInfo()
    val uploaded = SpeedInfo()

    val downloadSpeed: Int
        get() {
            return downloaded.currentSpeed
        }

    val uploadSpeed: Int
        get() {
            return uploaded.currentSpeed
        }

    var status: TorrentStatus = TorrentStatus.UNKNOWN
        get() = when (Libtorrent.torrentStatus(handle)) {
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

    val totalSize: Long
        get() {
            return Libtorrent.torrentPendingBytesLength(handle)
        }

    //TODO: CHECK FILE EXITS | CHECK STORAGE EJECTED CONDITION| READONLY | TORRENT_ALTERED | FREE_SPACE
    fun start() {
        if (!Libtorrent.startTorrent(handle)) {
            Timber.e(Libtorrent.error())
            return
        }

        val b = Libtorrent.torrentStats(handle)
        downloaded.start(b.downloaded)
        uploaded.start(b.uploaded)
    }

    fun stop() {
        Libtorrent.stopTorrent(handle)
    }

    fun remove(withFiles: Boolean) {
        if (handle != -1L) {
            if (withFiles)
                for (i in 0 until Libtorrent.torrentFilesCount(handle)) {
                    Storage.delete(File(path, Libtorrent.torrentFiles(handle, i).path))
                }

            Handler(Looper.getMainLooper()).postDelayed({
                Libtorrent.removeTorrent(handle)
            }, 300L)
        }

    }

    fun update() {
        val b = Libtorrent.torrentStats(handle)
        downloaded.step(b.downloaded)
        uploaded.step(b.uploaded)
        torrentProgressListener?.invoke(this)
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

    fun toEntity() = TorrentEntity(hash, state, status, path)
}