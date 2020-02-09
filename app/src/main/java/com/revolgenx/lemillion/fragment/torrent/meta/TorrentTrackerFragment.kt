package com.revolgenx.lemillion.fragment.torrent.meta

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.torrent.TorrentProgressListener
import com.revolgenx.lemillion.core.util.pmap
import com.revolgenx.lemillion.fragment.torrent.meta.model.TrackerModel
import com.revolgenx.lemillion.fragment.torrent.meta.model.TrackerStatus
import kotlinx.android.synthetic.main.tracker_adapter_layout.view.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class TorrentTrackerFragment :
    TorrentMetaBaseRecyclerView<TrackerModel, TorrentTrackerFragment.TrackerRecyclerAdapter.TrackerViewHolder>(),
    TorrentProgressListener, CoroutineScope {
    override fun getTitle(context: Context): String = context.getString(R.string.tracker)

    private val lsd = "LSD"
    private val dht = "DHT"
    private val pex = "PeX"

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val trackerModels = mutableMapOf<String, TrackerModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.torrent_base_recycler_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TrackerRecyclerAdapter()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!checkValidity()) return

        torrent.addListener(this)
        updateView()
    }


    private fun updateView() {
        if (!canUpdateView()) return

        runBlocking {
            torrent.handle!!.status().let {
                trackerModels[lsd] = trackerModels[lsd]?.apply {
                    working =
                        if (it.announcingToLsd()) TrackerStatus.WORKING else TrackerStatus.NOT_WORKING
                } ?: TrackerModel(
                    lsd,
                    if (it.announcingToLsd()) TrackerStatus.WORKING else TrackerStatus.NOT_WORKING,
                    ""
                )

                trackerModels[dht] = trackerModels[dht]?.apply {
                    working =
                        if (it.announcingToDht()) TrackerStatus.WORKING else TrackerStatus.NOT_WORKING
                } ?: TrackerModel(
                    dht,
                    if (it.announcingToDht()) TrackerStatus.WORKING else TrackerStatus.NOT_WORKING,
                    ""
                )

                trackerModels[pex] = trackerModels[pex]?.apply {
                    working = TrackerStatus.WORKING
                } ?: TrackerModel(
                    pex,
                    TrackerStatus.WORKING,
                    ""
                )

            }
            torrent.handle!!.trackers().pmap { entry ->
                val name = entry.url()
                var message = ""
                var status = TrackerStatus.NOT_WORKING
                if (entry.endpoints().isNotEmpty()) {
                    entry.endpoints().sortedWith(compareBy { it.fails() }).first().let { endP ->
                        message = endP.message()
                        status = if (entry.isVerified && endP.isWorking) TrackerStatus.WORKING
                        else if (endP.fails() == 0 && endP.updating()) TrackerStatus.UPDATING
                        else if (endP.fails() == 0) TrackerStatus.NOT_CONTACTED
                        else TrackerStatus.NOT_WORKING
                    }
                }

                trackerModels[name]?.let {
                    it.name = name
                    it.working = status
                    it.message = message
                    it
                } ?: let {
                    trackerModels[name] =
                        TrackerModel(
                            name,
                            status,
                            message
                        )
                }

                trackerModels[name]

            }

            adapter.submitList(trackerModels.values.toList())
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
        super.onDestroy()
    }

    inner class TrackerRecyclerAdapter :
        ListAdapter<TrackerModel, TrackerRecyclerAdapter.TrackerViewHolder>(object :
            DiffUtil.ItemCallback<TrackerModel>() {
            override fun areItemsTheSame(oldItem: TrackerModel, newItem: TrackerModel): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TrackerModel, newItem: TrackerModel): Boolean {
                return oldItem == newItem
            }
        }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackerViewHolder {
            return TrackerViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.tracker_adapter_layout,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: TrackerViewHolder, position: Int) {
            holder.bind(getItem(position))
        }


        inner class TrackerViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            fun bind(tracker: TrackerModel) {
                itemView.trackerTv.let {
                    it.titleTextView().text = tracker.name
                    it.description = tracker.working.name
                }
            }
        }
    }
}
