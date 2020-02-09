package com.revolgenx.lemillion.fragment.torrent.meta

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.revolgenx.lemillion.R
import kotlinx.android.synthetic.main.torrent_base_recycler_layout.*

abstract class TorrentMetaBaseRecyclerView<T, VH : RecyclerView.ViewHolder> :
    TorrentBaseMetaFragment() {
    lateinit var adapter: ListAdapter<T, VH>
    private val recyclerStateKey = "recycler_state_key"
    protected lateinit var baseMetaRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.torrent_base_recycler_layout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!checkValidity()) return

        baseMetaRecyclerView = metaRecyclerView
        metaRecyclerView.layoutManager = LinearLayoutManager(context)
        metaRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this.context,
                DividerItemDecoration.VERTICAL
            )
        )
        metaRecyclerView.itemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                return true
            }
        }
        metaRecyclerView.adapter = adapter
        savedInstanceState?.let {
            it.getParcelable<Parcelable>(recyclerStateKey)?.let { parcel ->
                metaRecyclerView.layoutManager?.onRestoreInstanceState(parcel)
            }
        }
    }

    fun canUpdateView(): Boolean {
        return ::adapter.isInitialized || checkValidity()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(
            recyclerStateKey,
            baseMetaRecyclerView.layoutManager?.onSaveInstanceState()
        )
        super.onSaveInstanceState(outState)
    }

}