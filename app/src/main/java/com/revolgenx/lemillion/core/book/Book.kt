package com.revolgenx.lemillion.core.book

import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.download.DownloadEntity
import com.revolgenx.lemillion.core.db.book.BookEntity

class Book {
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
            BookProtocol.UNKNOWN -> { }
        }
    }

    fun remove(withFiles: Boolean = false) {
        when(bookProtocol){
            BookProtocol.HTTP -> {
                Aria.download(this).load(id).cancel(withFiles)
            }
            BookProtocol.FTP -> {
                Aria.download(this).loadFtp(id).cancel(withFiles)
            }
            BookProtocol.UNKNOWN -> {}
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
}