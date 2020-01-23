package com.revolgenx.weaverx.core.book

import com.arialyy.aria.core.download.DownloadEntity

class Book {
    var entity: DownloadEntity? = null
    var listener: (() -> Unit)? = null

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