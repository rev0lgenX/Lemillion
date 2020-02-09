package com.revolgenx.lemillion.adapter.meta

import com.github.axet.androidlibrary.widgets.TreeListView
import org.libtorrent4j.Priority
import timber.log.Timber

//TODO://
class TorrentFolder : TorrentName() {
    var node: TreeListView.TreeNode? = null
    var check = true
        get() {
            for (n in node!!.nodes) {
                val m = n.tag as TorrentFile
                if (m.priority == Priority.IGNORE)
                    return false
            }
            return true
        }
        set(value) {
            field = value
            for (n in node!!.nodes) {
                val m = n.tag as TorrentFile

                Timber.d("${m.name}")

                m.priority = if (value) {
                    Priority.DEFAULT
                } else {
                    Priority.IGNORE
                }
            }
        }
}