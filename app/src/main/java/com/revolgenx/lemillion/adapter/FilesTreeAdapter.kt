package com.revolgenx.lemillion.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.axet.androidlibrary.widgets.ThemeUtils
import com.github.axet.androidlibrary.widgets.TreeRecyclerView
import com.github.axet.androidlibrary.widgets.TreeListView
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.adapter.meta.TorrentFile
import com.revolgenx.lemillion.adapter.meta.TorrentFolder
import com.revolgenx.lemillion.core.util.formatSize
import kotlinx.android.synthetic.main.file_holder_adapter_layout.view.*
import kotlinx.android.synthetic.main.folder_holder_adapter_layout.view.*
import libtorrent.Libtorrent
import java.io.File
import com.revolgenx.lemillion.adapter.meta.TorrentName
import java.util.*
import kotlin.Comparator
import kotlin.math.ceil


class FilesTreeAdapter(
    private var handle: Long,
    private val totalCallback: ((total: String) -> Unit)? = null
) :
    TreeRecyclerView.TreeAdapter<FilesTreeAdapter.FileHolder>() {

    var folders = mutableMapOf<String, TorrentFolder>()

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).nodes.isEmpty()) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
        return FileHolder(
            LayoutInflater.from(parent.context).inflate(
                if (viewType == 0)
                    R.layout.file_holder_adapter_layout
                else
                    R.layout.folder_holder_adapter_layout, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: FileHolder, position: Int) {
        val item = getItem(holder.getAdapterPosition(this))
        holder.bind(item)
    }

    inner class FileHolder(private val v: View) : TreeRecyclerView.TreeHolder(v) {

        fun bind(treenode: TreeListView.TreeNode) {
            val item = treenode.tag
            if (item is TorrentFolder) {
                v.apply {
                    torrentFolderNameTv.text = item.name
                    torrentFolderCheckBox.isChecked = item.check
                    torrentFolderSizeTv.text = item.size.formatSize()

                    torrentFolderCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        item.check = isChecked
                        updateTotal()
                    }
                    torrentFolderExpandIv.setImageResource(if (treenode.expanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
                }
            } else if (item is TorrentFile) {
                v.apply {
                    torrentFileNameTv.text = item.name
                    torrentFileCheck.isChecked = item.check
                    torrentFilePercentTv.text =
                        if (item.file!!.length > 0) {
                            (ceil(item.file!!.bytesCompleted * 100f / item.file!!.length)).toString() + "%"
                        } else "100%"

                    torrentFileCheck.setOnCheckedChangeListener { buttonView, isChecked ->
                        item.check = isChecked
                        updateTotal()
                    }

                    torrentFileSizeTv.text = item.size.formatSize()

                    if (item.parent == null) {
                        setBackgroundColor(0)
                    } else {
                        setBackgroundColor(ThemeUtils.getColor(context, R.attr.fileBackgroundDark))
                    }
                }
            }
        }
    }


    fun update() {
        val count = Libtorrent.torrentFilesCount(handle)
        val name = Libtorrent.torrentName(handle)
        if (root.nodes.size == 0 && count > 0) {
            root.nodes.clear()
            folders.clear()
            if (count == 1L) {
                val f = TorrentFile(handle, 0)
                f.name = "./" + f.file!!.path
                val n = TreeListView.TreeNode(root, f)
                f.node = n
                root.nodes.add(n)
            } else {
                for (i in 0 until count) {
                    val f = TorrentFile(handle, i)

                    val p = f.file!!.getPath()
                    f.fullPath = p
                    val fp = p.substring(name.length + 1)
                    f.path = fp
                    val file = File(fp)
                    val parent = file.parent
                    f.name = "./" + file.name

                    if (parent != null) {
                        var folder = folders.get(parent)
                        if (folder == null) {
                            folder = TorrentFolder(handle)
                            folder.fullPath = File(p).parent
                            folder.path = parent
                            folder.name = folder.path
                            val n = TreeListView.TreeNode(root, folder)
                            folder.node = n
                            root.nodes.add(n)
                            folders[parent] = folder
                        }
                        folder.size += f.size
                        val n = TreeListView.TreeNode(folder.node, f)
                        f.node = n
                        folder.node!!.nodes.add(n)
                        f.parent = folder
                    }
                    if (f.parent == null) {
                        val n = TreeListView.TreeNode(root, f)
                        f.node = n
                        root.nodes.add(n)
                    }
                }
                for (n in root.nodes) {
                    if (n.tag is TorrentFolder) {
                        val m = n.tag as TorrentFolder
                        Collections.sort(m.node!!.nodes, SortFiles())
                    }
                }
                Collections.sort(root.nodes, SortFiles())
            }
        }

        load()
        updateTotal()
    }


    fun checkAll(checked: Boolean) {
        if (handle == -1L) return

        Libtorrent.torrentFilesCheckAll(handle, checked)

        for (n in root.nodes) {
            if (n.tag is TorrentFolder) {
                val f = n.tag as TorrentFolder
                for (k in f.node!!.nodes) {
                    val m = k.tag as TorrentFile
                    m.file!!.check = checked // update java side runtime data
                }
            }
            if (n.tag is TorrentFile) {
                val f = n.tag as TorrentFile
                f.file!!.check = checked // update java side runtime data
            }
        }

        updateTotal()
        notifyDataSetChanged()
    }

    private fun updateTotal() {
        if (Libtorrent.metaTorrent(handle))
            totalCallback?.invoke(Libtorrent.torrentPendingBytesLength(handle).formatSize())
    }


    class SortFiles : Comparator<TreeListView.TreeNode> {
        override fun compare(file: TreeListView.TreeNode, file2: TreeListView.TreeNode): Int {
            val f1 = file.tag as TorrentName
            val f2 = file2.tag as TorrentName
            return f1.path!!.compareTo(f2.path!!)
        }
    }

}