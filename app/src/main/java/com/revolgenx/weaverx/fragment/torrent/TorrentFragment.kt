package com.revolgenx.weaverx.fragment.torrent

import android.os.Bundle
import android.util.TypedValue
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.iterator
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.revolgenx.weaverx.R
import com.revolgenx.weaverx.activity.MainActivity
import com.revolgenx.weaverx.adapter.SelectableAdapter
import com.revolgenx.weaverx.core.service.isServiceRunning
import com.revolgenx.weaverx.core.torrent.Torrent
import com.revolgenx.weaverx.core.torrent.TorrentEngine
import com.revolgenx.weaverx.core.torrent.TorrentProgressListener
import com.revolgenx.weaverx.core.torrent.TorrentStatus.*
import com.revolgenx.weaverx.core.util.*
import com.revolgenx.weaverx.event.*
import com.revolgenx.weaverx.fragment.BaseRecyclerFragment
import com.revolgenx.weaverx.viewmodel.TorrentViewModel
import kotlinx.android.synthetic.main.base_recycler_view_layout.*
import kotlinx.android.synthetic.main.torrent_recycler_adapter_layout.view.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class TorrentFragment :
    BaseRecyclerFragment<TorrentFragment.TorrentRecyclerAdapter.TorrentViewHolder, Torrent>() {

    companion object {
        fun newInstance() = TorrentFragment()
    }

    private var rotating = false

    private val torrentEngine by inject<TorrentEngine>()
    private val viewModel by viewModel<TorrentViewModel>()


    private var iconColorInverse = -1
    private var iconColor = -1


    private var actionMode: ActionMode? = null
    private var inActionMode = false
        set(value) {
            field = value

            actionMode = if (value) {
                (activity as? MainActivity)?.startSupportActionMode(actionModeCallback)
            } else {
                actionMode?.finish()
                null
            }
        }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.torrentDeleteItem -> {
                    MaterialDialog(this@TorrentFragment.context!!).show {
                        var withFiles = false
                        checkBoxPrompt(R.string.delete_with_files) {
                            withFiles = it
                        }
                        message(R.string.are_you_sure)
                        title(R.string.delete_files)
                        positiveButton(R.string.yes) {
                            postEvent(
                                TorrentRemovedEvent(
                                    (adapter as TorrentRecyclerAdapter).getSelectedHashes(),
                                    withFiles
                                )
                            )
                            inActionMode = false
                        }
                        negativeButton(R.string.no)
                    }
                    true
                }

                R.id.recheckTorrentItem -> {
//                    adapter.recheckTorrent()
//                    viewModel.recheckTorrent(adapter.getSelectedHash())
                    adapter.clearSelection()
                    inActionMode = false
                    true
                }

                R.id.torrentSelectAllItem -> {
                    adapter.selectAll()
                    true
                }
                android.R.id.home -> {
                    false
                }
                else -> false
            }
        }

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.torrent_action_menu, menu)
            menu?.iterator()?.forEach {
                when (it.itemId) {
                    R.id.torrentDeleteItem -> {
                        it.icon = AppCompatDrawableManager.get()
                            .getDrawable(context!!, R.drawable.ic_delete)
                            .apply { DrawableCompat.setTint(this, iconColor) }
                    }

                    R.id.torrentSelectAllItem -> {
                        it.icon = AppCompatDrawableManager.get()
                            .getDrawable(context!!, R.drawable.ic_select_all)
                            .apply { DrawableCompat.setTint(this, iconColor) }
                    }
                }
            }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode?) {
            inActionMode = false
            adapter.clearSelection()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerClass(this)

        val a = TypedValue()
        context!!.theme.resolveAttribute(R.attr.iconColorInverse, a, true)
        iconColorInverse = a.data

        context!!.theme.resolveAttribute(R.attr.iconColor, a, true)
        iconColor = a.data
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TorrentRecyclerAdapter()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.torrentResource.observe(this, Observer { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    progressText.showProgress(R.string.loading, false)
                    progressText.visibility = View.GONE
                    adapter.submitList(resource.data)
                }

                Status.ERROR -> {
                    progressText.showProgress(R.string.loading, false)
                    progressText.visibility = View.GONE
                    makeToast(resId = R.string.unable_to_load_torrent)
                }

                Status.LOADING -> {
                    progressText.visibility = View.VISIBLE
                    progressText.showProgress(R.string.loading, true)
                }
            }
        })

        torrentEngine.startEngine()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        rotating = true
        super.onSaveInstanceState(outState)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun torrentEngineEvent(event: TorrentEngineEvent) {
        when (event.engineEventTypes) {
            TorrentEngineEventTypes.ENGINE_STARTING -> {
                progressText.showProgress(R.string.starting_engine, true)
            }
            TorrentEngineEventTypes.ENGINE_STARTED -> {
                progressText?.showProgress(R.string.successful, false)
                progressText?.visibility = View.GONE
                viewModel.getAllTorrents()
            }
            TorrentEngineEventTypes.ENGINE_STOPPED -> {
                progressText?.visibility = View.VISIBLE
                progressText?.showProgress(R.string.engine_stopped, false)
            }
            TorrentEngineEventTypes.ENGINE_FAULT -> {
                progressText?.showProgress(R.string.unable_to_start_engine)
                makeToast(getString(R.string.unable_to_start_engine))
            }
        }
    }


    override fun onDestroy() {
        if (!rotating && !context!!.isServiceRunning()) {
            torrentEngine.stopEngine()
        }
        adapter.currentList.forEach { it.torrentProgressListener = null }
        unregisterClass(this)
        super.onDestroy()
    }

    inner class TorrentRecyclerAdapter :
        SelectableAdapter<TorrentRecyclerAdapter.TorrentViewHolder, Torrent>(object :
            DiffUtil.ItemCallback<Torrent>() {
            override fun areItemsTheSame(oldItem: Torrent, newItem: Torrent): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: Torrent, newItem: Torrent): Boolean =
                oldItem == newItem
        }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorrentViewHolder =
            TorrentViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.torrent_recycler_adapter_layout,
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: TorrentViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onViewRecycled(holder: TorrentViewHolder) {
            holder.unbind()
            super.onViewRecycled(holder)
        }

        fun getSelectedHashes() = getSelectedItems().map { currentList[it].hash }

        inner class TorrentViewHolder(private val v: View) : RecyclerView.ViewHolder(v),
            TorrentProgressListener {
            private var torrent: Torrent? = null
            fun bind(item: Torrent) {
                torrent = item
                torrent!!.name
                torrent!!.torrentProgressListener = this

                v.apply {
                    pausePlayIv.setOnClickListener {
                        when (torrent!!.status) {
                            QUEUE, SEEDING, DOWNLOADING, CHECKING -> {
                                torrent!!.stop()
                                postEvent(
                                    TorrentEvent(
                                        listOf(torrent!!),
                                        TorrentEventType.TORRENT_PAUSED
                                    )
                                )
                            }
                            else -> {
                                torrent!!.start()
                                postEvent(
                                    TorrentEvent(
                                        listOf(torrent!!),
                                        TorrentEventType.TORRENT_RESUMED
                                    )
                                )
                            }
                        }
                    }

                    setOnClickListener {
                        if (selectedItemCount > 0) {
                            toggleSelection(adapterPosition)
                        }
                    }

                    setOnLongClickListener {
                        toggleSelection(adapterPosition)
                        if (!inActionMode) {
                            inActionMode = true
                        }
                        true
                    }
                }
                updateView()
            }

            private fun updateView() {
                v.apply {
                    torrentAdapterConstraintLayout.isSelected = isSelected(adapterPosition)

                    torrentNameTv.text = torrent!!.name
                    torrentProgressBar.progress = torrent!!.progress
                    torrentProgressBar.labelText = torrent!!.progress.toInt().toString()
                    torrentFirstTv.text = torrent!!.status.name
                    pausePlayIv.setImageResource(
                        when (torrent!!.status) {
                            QUEUE, SEEDING, DOWNLOADING, CHECKING -> {
                                R.drawable.ic_pause
                            }
                            else -> {
                                R.drawable.ic_play
                            }
                        }
                    )
                }
            }

            override fun invoke(torrent: Torrent) {
                updateView()
            }


            fun unbind() {
                torrent!!.torrentProgressListener = null
                torrent = null
            }
        }
    }

}