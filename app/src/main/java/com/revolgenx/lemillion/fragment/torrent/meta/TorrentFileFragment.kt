package com.revolgenx.lemillion.fragment.torrent.meta

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.adapter.FilesTreeAdapter
import com.revolgenx.lemillion.core.torrent.TorrentProgressListener
import kotlinx.android.synthetic.main.torrent_file_header_layout.*
import kotlinx.android.synthetic.main.torrent_file_meta_layout.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class TorrentFileFragment : TorrentBaseMetaFragment(), TorrentProgressListener, CoroutineScope {

    private lateinit var adapter: FilesTreeAdapter

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.torrent_file_meta_layout, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!checkValidity())
            return

        adapter = FilesTreeAdapter(torrent.handle!!) {
            torrentMetaTotalSizeTv.text = it
        }
        adapter.setHasStableIds(true)
        torrent.addListener(this)
        val div = DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
        treeRecyclerView.addItemDecoration(div)
        treeRecyclerView.adapter = adapter
        torrentMetaFileCheckBox.setOnCheckedChangeListener { _, isChecked ->
            adapter.checkAll(isChecked)
        }
        updateView()
    }

    private fun updateView() {
        if (!checkValidity()) return

        if (!torrent.handle!!.status().hasMetadata()) return

        emptyTv.visibility = View.GONE

        if (adapter.folders.isEmpty()) {

            launch(Dispatchers.IO) {
                adapter.update {
                    launch(Dispatchers.Main) {
                        adapter.load()
                        adapter.updateTotal()
                    }
                }
            }
        } else {
            treeRecyclerView.post {
                adapter.updateItems()
            }
        }

    }

    override fun invoke() {
        updateView()
    }

    override fun onDestroy() {
        if (checkValidity()) {
            torrent.removeEngineListener()
            torrent.removeListener(this)
        }
        job.cancel()
        super.onDestroy()
    }


    override fun getTitle(context: Context): String = context.getString(R.string.files)
}