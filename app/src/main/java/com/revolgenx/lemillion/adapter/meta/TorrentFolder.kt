package com.revolgenx.lemillion.adapter.meta

import com.github.axet.androidlibrary.widgets.TreeListView
import libtorrent.Libtorrent


class TorrentFolder(private var handler: Long) : TorrentName() {
    var node: TreeListView.TreeNode? = null
    var check = true
        get() {
            for (n in node!!.nodes) {
                val m = n.tag as TorrentFile
                if (!m.check)
                    return false
            }
            return true
        }
        set(value) {
            field = value
            Libtorrent.torrentFilesCheckFilter(handler, "$fullPath/*", value)
            for (n in node!!.nodes) {
                val m = n.tag as TorrentFile
                m.file!!.check = value
            }
        }
}