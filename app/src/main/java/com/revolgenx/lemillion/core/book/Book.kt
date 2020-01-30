package com.revolgenx.lemillion.core.book

import android.os.Parcel
import android.os.Parcelable
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.revolgenx.lemillion.core.db.book.BookEntity

class Book() : Parcelable {

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
    var listener: (() -> Unit)? = null
    var bookProtocol = BookProtocol.UNKNOWN

    val name: String
        get() = entity!!.fileName

    val totalSize: Long
        get() = entity!!.fileSize

    val totalSizeFormatted: String
        get() = entity!!.convertFileSize

    val progress: Long
        get() = entity!!.currentProgress

    val stopTime: Long
        get() = entity!!.stopTime


    fun resume() {
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


    override fun equals(other: Any?): Boolean {
        return if (other is Book) {
            entity!!.id == other.entity!!.id
                    && entity!!.fileName == other.entity!!.fileName
                    && entity!!.state == other.entity!!.state
                    && entity!!.percent == other.entity!!.percent
                    && entity!!.convertSpeed == other.entity!!.convertSpeed
        } else false
    }

    fun toEntity() = BookEntity(id, bookProtocol)


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