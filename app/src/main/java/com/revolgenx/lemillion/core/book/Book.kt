package com.revolgenx.lemillion.core.book

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.arialyy.aria.core.inf.IEntity
import com.arialyy.aria.core.task.DownloadTask
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.db.book.BookEntity
import com.revolgenx.lemillion.core.util.getFree
import com.revolgenx.lemillion.core.util.postEvent
import com.revolgenx.lemillion.event.BookEvent
import com.revolgenx.lemillion.event.BookEventType
import java.io.File
import java.lang.Exception


typealias BookProgressListener = (() -> Unit)?

class Book() : Parcelable {

    init {
        Aria.download(this).register()
    }

    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        entity = parcel.readParcelable(DownloadEntity::class.java.classLoader)
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.apply {
            writeLong(id)
            writeParcelable(entity, flags)
        }
    }

    var id: Long = -1
    var entity: DownloadEntity? = null
        get() {
            return if (field == null) {
                field = Aria.download(this).load(id).entity
                field
            } else field
        }
    var listeners: MutableList<BookProgressListener> = mutableListOf()
    var bookProtocol = BookProtocol.UNKNOWN

    var hasError: Boolean = false
    var errorMsg: String = ""

    val name: String
        get() = entity!!.fileName

    val totalSize: Long
        get() = entity!!.fileSize

    val progress: Float
        get() = entity!!.percent.toFloat()

    val stopTime: Long
        get() = entity!!.stopTime

    val state
        get() = entity!!.state

    val speed: Long
        get() = entity!!.speed

    fun stateFormat(context: Context) = when (entity!!.state) {
        IEntity.STATE_RUNNING -> {
            context.getString(R.string.downloading)
        }
        IEntity.STATE_CANCEL -> {
            context.getString(R.string.canceled)
        }
        IEntity.STATE_COMPLETE -> {
            context.getString(R.string.completed)
        }
        IEntity.STATE_FAIL -> {
            context.getString(R.string.failed)
        }
        IEntity.STATE_OTHER -> {
            context.getString(R.string.unknown)
        }
        IEntity.STATE_STOP -> {
            context.getString(R.string.paused)
        }
        IEntity.STATE_PRE -> {
            context.getString(R.string.connecting)
        }
        IEntity.STATE_POST_PRE -> {
            context.getString(R.string.connecting)
        }
        IEntity.STATE_WAIT -> {
            context.getString(R.string.waiting)
        }
        else -> ""
    }

    val eta: Long
        get() {
            return entity!!.timeLeft.toLong()
        }

    fun resume() {
        if (!checkValidity()) return

        hasError = false
        errorMsg = ""
        if (state == IEntity.STATE_FAIL) {
            reTry()
            return
        }

        when (bookProtocol) {
            BookProtocol.HTTP -> {
                Aria.download(this).load(id).resume()
            }
            BookProtocol.FTP -> {
                Aria.download(this).loadFtp(id).resume()
            }
            BookProtocol.UNKNOWN -> {
            }
        }
    }

    fun stop() {
        if (!checkValidity()) return

        when (bookProtocol) {
            BookProtocol.HTTP -> {
                Aria.download(this).load(id).stop()
            }
            BookProtocol.FTP -> {
                Aria.download(this).loadFtp(id).stop()
            }
            BookProtocol.UNKNOWN -> {
            }
        }
    }

    fun reStart() {
        when (bookProtocol) {
            BookProtocol.HTTP -> {
                Aria.download(this).load(id).reStart()
            }
            BookProtocol.FTP -> {
                Aria.download(this).loadFtp(id).reStart()
            }
            BookProtocol.UNKNOWN -> {
            }
        }
    }

    fun reTry() {
        when (bookProtocol) {
            BookProtocol.HTTP -> {
                Aria.download(this).load(id).reTry()
            }
            BookProtocol.FTP -> {
                Aria.download(this).loadFtp(id).reTry()
            }
            BookProtocol.UNKNOWN -> {
            }
        }
    }

    fun isPaused(): Boolean {
        return when (state) {
            IEntity.STATE_RUNNING, IEntity.STATE_WAIT, IEntity.STATE_PRE, IEntity.STATE_POST_PRE -> false
            else -> true
        }
    }

    fun remove(withFiles: Boolean = false) {
        when (bookProtocol) {
            BookProtocol.HTTP -> {
                Aria.download(this).load(id).cancel(withFiles)
            }
            BookProtocol.FTP -> {
                Aria.download(this).loadFtp(id).cancel(withFiles)
            }
            BookProtocol.UNKNOWN -> {
            }
        }
    }


    fun checkTaskValidity(task: DownloadTask?) =
        task != null && task.entity?.id == id && task.entity?.id != -1L

    fun checkValidity() = entity != null && id != -1L

    @Download.onWait
    fun onWait(task: DownloadTask?) {
        if (!checkTaskValidity(task)) return
        postEvent(BookEvent(listOf(this), BookEventType.BOOK_RESUMED))
        update(task)
    }

    @Download.onPre
    fun onPre(task: DownloadTask?) {
        if (!checkTaskValidity(task)) return
        postEvent(BookEvent(listOf(this), BookEventType.BOOK_RESUMED))
        update(task)
    }

    @Download.onTaskStart
    fun taskStart(task: DownloadTask?) {
        if (!checkTaskValidity(task)) return
        postEvent(BookEvent(listOf(this), BookEventType.BOOK_RESUMED))
        update(task)
    }

    @Download.onTaskRunning
    fun running(task: DownloadTask?) {
        if (!checkTaskValidity(task)) return
        update(task)
    }

    @Download.onTaskResume
    fun taskResume(task: DownloadTask?) {
        if (!checkTaskValidity(task)) return
        postEvent(BookEvent(listOf(this), BookEventType.BOOK_RESUMED))
        update(task)
    }

    @Download.onTaskStop
    fun taskStop(task: DownloadTask?) {
        if (!checkTaskValidity(task)) return
        postEvent(BookEvent(listOf(this), BookEventType.BOOK_PAUSED))
        update(task)
    }

    @Download.onTaskCancel
    fun taskCancel(task: DownloadTask?) {
        if (!checkTaskValidity(task)) return
        postEvent(BookEvent(listOf(this), BookEventType.BOOK_PAUSED))
        update(task)
    }

    @Download.onTaskFail
    fun taskFail(task: DownloadTask?, e: Exception?) {
        if (!checkTaskValidity(task)) return

        hasError = true
        if (e != null) {
            errorMsg = e.message ?: ""
        }

        postEvent(BookEvent(listOf(this), BookEventType.BOOK_FAILED))
        update(task)
    }


    fun isLowSpace() = entity!!.fileSize >= getFree(File(entity!!.filePath))


    @Download.onTaskComplete
    fun taskComplete(task: DownloadTask?) {
        if (!checkTaskValidity(task)) return
        postEvent(BookEvent(listOf(this), BookEventType.BOOK_COMPLETED))
        update(task)
    }

    fun update(task: DownloadTask?) {
        if (task != null) {
            this.entity = task.entity
        }
        listeners.forEach { it?.invoke() }
    }

    fun addListener(listener: BookProgressListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: BookProgressListener) {
        listeners.remove(listener)
    }

    fun removeAllListener() {
        listeners.clear()
    }

    fun register() {
        Aria.download(this).register()
    }

    fun unregister() {
        Aria.download(this).unRegister()
    }


    fun toEntity() = BookEntity(id, bookProtocol, hasError, errorMsg)

    override fun equals(other: Any?): Boolean {
        return if (other is Book) {
            entity!!.id == other.entity!!.id
                    && entity!!.fileName == other.entity!!.fileName
                    && entity!!.state == other.entity!!.state
                    && entity!!.percent == other.entity!!.percent
                    && entity!!.speed == other.entity!!.speed
        } else false
    }


    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Book> {
        override fun createFromParcel(parcel: Parcel): Book {
            return Book(parcel)
        }

        override fun newArray(size: Int): Array<Book?> {
            return arrayOfNulls(size)
        }
    }
}