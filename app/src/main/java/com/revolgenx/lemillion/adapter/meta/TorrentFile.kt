package com.revolgenx.lemillion.adapter.meta

import com.github.axet.androidlibrary.widgets.TreeListView
import libtorrent.Libtorrent

class TorrentFile(t: Long, i: Long) : TorrentName() {

    var parent: TorrentFolder? = null
    var index: Long? = null
    var file: libtorrent.File? = null
    var handle: Long = -1
    var node: TreeListView.TreeNode? = null
    var check = true
        set(value) {
            field = value
            Libtorrent.torrentFilesCheck(handle, index!!, value)
            file!!.check = value
        }
        get() = file!!.check

    init {
        handle = t
        index = i
        update()
        size = file!!.length
    }

    fun update() {
        file = Libtorrent.torrentFiles(handle, index!!)
    }
}