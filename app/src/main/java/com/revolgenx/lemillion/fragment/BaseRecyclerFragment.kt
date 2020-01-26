package com.revolgenx.lemillion.fragment

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.adapter.SelectableAdapter
import kotlinx.android.synthetic.main.base_recycler_view_layout.*

abstract class BaseRecyclerFragment<VH : RecyclerView.ViewHolder, T : Any> : Fragment() {

    protected lateinit var adapter: SelectableAdapter<VH, T>
    private val recyclerStateKey = "recycler_state_key"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.base_recycler_view_layout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        baseRecyclerView.layoutManager = LinearLayoutManager(this.context)
        baseRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this.context,
                DividerItemDecoration.VERTICAL
            )
        )
        baseRecyclerView.itemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                return true
            }
        }
        baseRecyclerView.adapter = adapter

        savedInstanceState?.let {
            it.getParcelable<Parcelable>(recyclerStateKey)?.let { parcel ->
                baseRecyclerView.layoutManager?.onRestoreInstanceState(parcel)
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(
            recyclerStateKey,
            baseRecyclerView.layoutManager?.onSaveInstanceState()
        )
        super.onSaveInstanceState(outState)
    }

    abstract fun resumeAll()
    abstract fun pauseAll()
    abstract fun search(query:String)
    abstract fun sort(comparator:Comparator<*>)

}
