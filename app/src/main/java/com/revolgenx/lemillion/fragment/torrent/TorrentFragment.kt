package com.revolgenx.lemillion.fragment.torrent

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.iterator
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.activity.MainActivity
import com.revolgenx.lemillion.activity.TorrentMetaActivity
import com.revolgenx.lemillion.adapter.SelectableAdapter
import com.revolgenx.lemillion.core.exception.TorrentPauseException
import com.revolgenx.lemillion.core.exception.TorrentResumeException
import com.revolgenx.lemillion.core.sorting.torrent.TorrentSortingComparator
import com.revolgenx.lemillion.core.torrent.*
import com.revolgenx.lemillion.core.util.*
import com.revolgenx.lemillion.view.color
import com.revolgenx.lemillion.event.*
import com.revolgenx.lemillion.view.showProgress
import com.revolgenx.lemillion.view.makeToast
import com.revolgenx.lemillion.fragment.BaseRecyclerFragment
import com.revolgenx.lemillion.viewmodel.TorrentViewModel
import kotlinx.android.synthetic.main.base_recycler_view_layout.*
import kotlinx.android.synthetic.main.torrent_recycler_adapter_layout.view.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.Comparator


class TorrentFragment :
    BaseRecyclerFragment<TorrentFragment.TorrentRecyclerAdapter.TorrentViewHolder, Torrent>() {

    companion object {
        fun newInstance() = TorrentFragment()
    }

    private var rotating = false
    private var query = ""

    private val torrentEngine by inject<TorrentEngine>()
    private val torrentActiveState by inject<TorrentActiveState>()
    private val viewModel by viewModel<TorrentViewModel>()

    private var iconColorInverse = -1
    private var iconColor = -1

    private var forceShutdown = false

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
                    viewModel.recheckTorrents((adapter as TorrentRecyclerAdapter).getSelectedHashes())
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

            if (menu is MenuBuilder) {
                menu.setOptionalIconsVisible(true)
            }

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

                    R.id.recheckTorrentItem -> {
                        it.icon = AppCompatDrawableManager.get()
                            .getDrawable(context!!, R.drawable.ic_check)
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

        torrentEngine.start()
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
            TorrentEngineEventTypes.ENGINE_STOPPING -> {
                progressText?.visibility = View.VISIBLE
                progressText?.showProgress(R.string.engine_stopping, false)
                viewModel.removeAllTorrentEngineListener()
            }
            TorrentEngineEventTypes.ENGINE_FAULT -> {
                progressText?.showProgress(R.string.unable_to_start_engine)
                makeToast(getString(R.string.unable_to_start_engine))
            }
            TorrentEngineEventTypes.ENGINE_STOPPED -> {
                progressText?.visibility = View.VISIBLE
                progressText?.showProgress(R.string.engine_stopped, false)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShutdownEvent(event:ShutdownEvent){
        forceShutdown = true
    }


    override fun onDestroy() {
        adapter.currentList.forEach { it.removeAllListener() }
        if ((!rotating && !torrentActiveState.serviceActive) || forceShutdown) {
            torrentEngine.stop()
        }
        unregisterClass(this)
        super.onDestroy()
    }


    override fun resumeAll() {
        viewModel.resumeAll()
    }

    override fun pauseAll() {
        viewModel.pauseAll()
    }

    override fun search(query: String) {
        this.query = query
        adapter.search(query)
    }

    override fun sort(comparator: Comparator<*>) {
        viewModel.sorting = comparator as TorrentSortingComparator
    }

    fun onPageSelected() {
        if (query.isNotEmpty())
            search("")

        if (inActionMode)
            inActionMode = false
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

        override fun performFiltering(constraint: CharSequence?) {
            if (constraint?.length == 0) {
                if (searchTempList.isNotEmpty()) {
                    submitList(mutableListOf<Torrent>().apply { addAll(searchTempList) })
                    searchTempList.clear()
                }
            } else {
                if (searchTempList.isEmpty()) {
                    searchTempList.addAll(currentList)
                }
                submitList(emptyList())
                constraint?.toString()?.toLowerCase()?.trim()?.let { pattern ->
                    searchTempList.filter { it.name.toLowerCase().contains(pattern) }
                        .takeIf { it.isNotEmpty() }?.let {
                            submitList(it)
                        }
                }
            }
        }


        override fun onViewRecycled(holder: TorrentViewHolder) {
            holder.unbind()
            super.onViewRecycled(holder)
        }

        fun getSelectedHashes() = getSelectedItems().map { currentList[it].hash }

        inner class TorrentViewHolder(private val v: View) : RecyclerView.ViewHolder(v),
            TorrentProgressListener {
            private var torrent: Torrent? = null
            private var currentState: TorrentState = TorrentState.UNKNOWN

            fun bind(item: Torrent) {
                torrent = item
                torrent!!.addListener(this)
                v.apply {
                    torrentAdapterConstraintLayout.isSelected = isSelected(adapterPosition)
                    pausePlayIv.setOnClickListener {
                        if (torrent!!.isPausedWithState()) {
                            try {
                                torrent!!.resume()
                            } catch (e: TorrentResumeException) {
                                makeToast(e.message)
                            }
                        } else {
                            try {
                                torrent!!.pause()
                            } catch (e: TorrentPauseException) {
                                makeToast(e.message)
                            }
                        }
                    }

                    setOnClickListener {
                        if (selectedItemCount > 0) {
                            toggleSelection(adapterPosition)
                            return@setOnClickListener
                        }

                        if (selectedItemCount <= 0) {
                            if (inActionMode) {
                                inActionMode = false
                                return@setOnClickListener
                            }
                        }

                        startActivity(Intent(context, TorrentMetaActivity::class.java).apply {
                            putExtra(TorrentMetaActivity.torrentKey, torrent!!)
                        })
                    }

                    setOnLongClickListener {
                        toggleSelection(adapterPosition)

                        if (!inActionMode) {
                            inActionMode = true
                        }

                        if (selectedItemCount <= 0) {
                            if (inActionMode) inActionMode = false
                        }

                        true
                    }
                }
                updateView()
            }

            @SuppressLint("SetTextI18n")
            private fun updateView() {
                v.apply {
                    if(context == null) return
                    torrentNameTv.text = torrent!!.name
                    val progress = torrent!!.progress
                    torrentProgressBar.progress = progress
                    torrentProgressBar.labelText = progress.toInt().toString()

                    if (torrent!!.hasError) {
                        indicatorView.setBackgroundColor(context.color(R.color.errorColor))
                    }
                    val state = torrent!!.state
                    torrentFirstTv.text =
                        "${torrent!!.state.name} · S:${torrent!!.connectedSeeders()} · L:${torrent!!.connectedLeechers()}${
                        if (state == TorrentState.DOWNLOADING) {
                            " · ET: ${torrent!!.eta().formatRemainingTime()}"
                        } else ""}"

                    torrentSecondTv.text =
                        if (state == TorrentState.COMPLETED || state == TorrentState.SEEDING) {
                            "${torrent!!.totalCompleted.formatSize()}/${torrent!!.totalSize.formatSize()} · " +
                                    "↑ ${torrent!!.uploadSpeed.formatSpeed()}"
                        } else
                            "${torrent!!.totalCompleted.formatSize()}/${torrent!!.totalSize.formatSize()} · " +
                                    "↓ ${torrent!!.downloadSpeed.formatSpeed()} · ↑ ${torrent!!.uploadSpeed.formatSpeed()}"

                    if (currentState == state) return

                    currentState = state

                    indicatorView.setBackgroundColor(
                        when (currentState) {
                            TorrentState.PAUSED -> {
                                context.color(R.color.pausedColor)
                            }
                            TorrentState.UNKNOWN -> {
                                context.color(R.color.red)
                            }

                            TorrentState.DOWNLOADING
                                , TorrentState.CHECKING
                                , TorrentState.QUEUE
                                , TorrentState.CHECKING_FILES
                                , TorrentState.DOWNLOADING_METADATA
                                , TorrentState.ALLOCATING
                                , TorrentState.CHECKING_RESUME_DATA -> {
                                context.color(R.color.downloadingColor)
                            }

                            TorrentState.SEEDING -> {
                                context.color(R.color.seedingColor)
                            }
                            TorrentState.COMPLETED -> {
                                context.color(R.color.completedColor)
                            }
                        }
                    )

                    pausePlayIv.setImageResource(
                        if (torrent!!.isPausedWithState()) {
                            R.drawable.ic_play
                        } else {
                            R.drawable.ic_pause
                        }
                    )

                }
            }

            override fun invoke() {
                updateView()
            }


            fun unbind() {
                torrent!!.removeListener(this)
                torrent = null
            }
        }
    }

    override fun getTitle(context: Context): String = context.getString(R.string.torrent)

}