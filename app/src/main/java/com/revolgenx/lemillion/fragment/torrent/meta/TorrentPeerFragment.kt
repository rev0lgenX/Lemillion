package com.revolgenx.lemillion.fragment.torrent.meta

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.torrent.TorrentProgressListener
import com.revolgenx.lemillion.core.util.formatSpeed
import kotlinx.android.synthetic.main.peer_adapter_layout.view.*
import org.libtorrent4j.PeerInfo
import timber.log.Timber

class TorrentPeerFragment :
    TorrentMetaBaseRecyclerView<PeerInfo, TorrentPeerFragment.PeerRecyclerAdapter.PeerViewHolder>(),
    TorrentProgressListener {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PeerRecyclerAdapter()
        adapter.setHasStableIds(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!checkValidity()) return

        torrent.addListener(this)
        updateView()
    }


    override fun invoke() {
        updateView()
    }

    private fun updateView() {
        if (!canUpdateView()) return
        val info = torrent.handle!!.peerInfo()
        Timber.d("peer infp ${info.size}")
        adapter.submitList(info)
    }

    override fun getTitle(context: Context): String = context.getString(R.string.peer)

    override fun onDestroy() {
        if (checkValidity()) {
            torrent.removeEngineListener()
            torrent.removeListener(this)
        }
        super.onDestroy()
    }

    inner class PeerRecyclerAdapter :
        ListAdapter<PeerInfo, PeerRecyclerAdapter.PeerViewHolder>(object :
            DiffUtil.ItemCallback<PeerInfo>() {
            override fun areContentsTheSame(oldItem: PeerInfo, newItem: PeerInfo): Boolean {
                return oldItem.client() == newItem.client() && oldItem.progress() == newItem.progress()
                        && newItem.upSpeed() == oldItem.upSpeed()
                        && newItem.downSpeed() == oldItem.downSpeed()
                        && newItem.ip() == oldItem.ip()
                        && newItem.flags() == newItem.flags()
            }

            override fun areItemsTheSame(oldItem: PeerInfo, newItem: PeerInfo): Boolean {
                return oldItem == newItem
            }
        }) {


        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
            return PeerViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.peer_adapter_layout,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class PeerViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            fun bind(item: PeerInfo) {
                itemView.apply {
                    peerInfoTv.titleTextView().text = item.ip()
                    peerInfoTv.description =
                        "${item.connectionType().name} ↓ ${item.downSpeed().toLong().formatSpeed()} · ↑ ${item.upSpeed().toLong().formatSpeed()}"
                }
            }
        }
    }
}
