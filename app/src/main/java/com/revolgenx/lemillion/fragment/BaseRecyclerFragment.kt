package com.revolgenx.lemillion.fragment

import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setMargins
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.adapter.SelectableAdapter
import com.revolgenx.lemillion.view.dip
import kotlinx.android.synthetic.main.base_recycler_view_layout.*
import kotlinx.android.synthetic.main.base_recycler_view_layout.view.*

abstract class BaseRecyclerFragment<VH : RecyclerView.ViewHolder, T : Any> : BasePagerFragment() {

    protected lateinit var adapter: SelectableAdapter<VH, T>
    private val recyclerStateKey = "recycler_state_key"
    protected lateinit var mBaseRecyclerView: RecyclerView
    protected lateinit var progressText: AppCompatTextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        progressText = AppCompatTextView(context!!).also {
            it.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context!!.dip(30)
            ).also { params ->
                params.setMargins(context!!.dip(3))
                params.gravity = Gravity.CENTER
            }
            it.gravity = Gravity.CENTER
        }

        progressText.typeface = ResourcesCompat.getFont(context!!, R.font.open_sans_regular)
        val a = TypedValue()
        context!!.theme.resolveAttribute(android.R.attr.textColorPrimary, a, true)
        @ColorInt val textColor = a.data

        progressText.setTextColor(textColor)

        mBaseRecyclerView = RecyclerView(context!!).also {
            it.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val v = inflater.inflate(R.layout.base_recycler_view_layout, container, false)
        v.baseLayout.addView(progressText)
        v.baseLayout.addView(mBaseRecyclerView)
        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mBaseRecyclerView.layoutManager = LinearLayoutManager(this.context)
        mBaseRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this.context,
                DividerItemDecoration.VERTICAL
            )
        )
        mBaseRecyclerView.itemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                return true
            }
        }
        mBaseRecyclerView.adapter = adapter

        savedInstanceState?.let {
            it.getParcelable<Parcelable>(recyclerStateKey)?.let { parcel ->
                mBaseRecyclerView.layoutManager?.onRestoreInstanceState(parcel)
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(
            recyclerStateKey,
            mBaseRecyclerView.layoutManager?.onSaveInstanceState()
        )
        super.onSaveInstanceState(outState)
    }

    abstract fun resumeAll()
    abstract fun pauseAll()
    abstract fun search(query: String)
    abstract fun sort(comparator: Comparator<*>)

}
