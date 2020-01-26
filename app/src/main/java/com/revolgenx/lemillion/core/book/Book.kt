package com.revolgenx.lemillion.core.book

import com.arialyy.aria.core.download.DownloadEntity

class Book {
    var entity: DownloadEntity? = null
    var listener: (() -> Unit)? = null

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


    override fun equals(other: Any?): Boolean {
        return if (other is Book) {
            entity!!.id == other.entity!!.id
                    && entity!!.fileName == other.entity!!.fileName
                    && entity!!.state == other.entity!!.state
                    && entity!!.percent == other.entity!!.percent
                    && entity!!.convertSpeed == other.entity!!.convertSpeed
        } else false
    }
}