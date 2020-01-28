package com.revolgenx.lemillion.fragment.book

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
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.inf.IEntity
import com.arialyy.aria.core.task.DownloadTask
import com.arialyy.aria.util.CommonUtil
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.activity.MainActivity
import com.revolgenx.lemillion.adapter.SelectableAdapter
import com.revolgenx.lemillion.core.book.Book
import com.revolgenx.lemillion.core.sorting.book.BookSortingComparator
import com.revolgenx.lemillion.core.util.Status
import com.revolgenx.lemillion.core.util.makeToast
import com.revolgenx.lemillion.core.util.postEvent
import com.revolgenx.lemillion.event.BookEvent
import com.revolgenx.lemillion.event.BookEventType
import com.revolgenx.lemillion.event.BookRemovedEvent
import com.revolgenx.lemillion.fragment.BaseRecyclerFragment
import com.revolgenx.lemillion.viewmodel.BookViewModel
import kotlinx.android.synthetic.main.base_recycler_view_layout.view.*
import kotlinx.android.synthetic.main.book_recycler_adapter_layout.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

class BookFragment : BaseRecyclerFragment<BookFragment.BookRecyclerAdapter.BookViewHolder, Book>() {

    companion object {
        fun newInstance() = BookFragment().apply {
            Aria.download(this).register()
        }
    }

    private val viewModel by viewModel<BookViewModel>()
    private var iconColor: Int = 0
    private var iconColorInverse: Int = 0

    private var query: String = ""

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
                R.id.bookDelete -> {
                    MaterialDialog(this@BookFragment.context!!).show {
                        var withFiles = false
                        checkBoxPrompt(R.string.delete_with_files) {
                            withFiles = it
                        }
                        message(R.string.are_you_sure)
                        title(R.string.delete_files)
                        positiveButton(R.string.yes) {
                            postEvent(
                                BookRemovedEvent(
                                    (adapter as BookRecyclerAdapter).getSelectedIds(),
                                    withFiles
                                )
                            )
                            inActionMode = false
                        }
                        negativeButton(R.string.no)
                    }
                    true
                }


                R.id.bookSelectAllItem -> {
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
            mode?.menuInflater?.inflate(R.menu.book_action_menu, menu)
            menu?.iterator()?.forEach {
                when (it.itemId) {
                    R.id.bookDelete -> {
                        it.icon = AppCompatDrawableManager.get()
                            .getDrawable(context!!, R.drawable.ic_delete)
                            .apply { DrawableCompat.setTint(this, iconColor) }
                    }

                    R.id.bookSelectAllItem -> {
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

        val a = TypedValue()
        context!!.theme.resolveAttribute(R.attr.iconColorInverse, a, true)
        iconColorInverse = a.data

        context!!.theme.resolveAttribute(R.attr.iconColor, a, true)
        iconColor = a.data

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.progressText.visibility = View.GONE
        adapter = BookRecyclerAdapter()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.bookResource.observe(this, Observer {
            if (it.status == Status.SUCCESS) {
                adapter.submitList(it.data)
            }
        })

        if (savedInstanceState == null) {
            viewModel.getBooks()
        }

    }


    private fun invokeListener(task: DownloadTask?) {
        viewModel.getBook(task)?.listener?.invoke()
    }

    @Download.onWait
    fun onWait(task: DownloadTask?) {
        Timber.d("wait ==> " + task?.downloadEntity!!.fileName)
        invokeListener(task)
    }

    @Download.onPre
    fun onPre(task: DownloadTask?) {
        Timber.d("onPre")
        invokeListener(task)
    }

    @Download.onTaskStart
    fun taskStart(task: DownloadTask?) {
        Timber.d("onStart")
        invokeListener(task)
    }

    @Download.onTaskRunning
    fun running(task: DownloadTask?) {
        Timber.d("running")
        invokeListener(task)
    }

    @Download.onTaskResume
    fun taskResume(task: DownloadTask?) {
        Timber.d("resume")
        invokeListener(task)
    }

    @Download.onTaskStop
    fun taskStop(task: DownloadTask?) {
        Timber.d("stop")
        invokeListener(task)
    }

    @Download.onTaskCancel
    fun taskCancel(task: DownloadTask?) {
        Timber.d("cancel")
        invokeListener(task)
    }

    @Download.onTaskFail
    fun taskFail(task: DownloadTask?) {
        Timber.d("fail")
        invokeListener(task)
    }


    @Download.onTaskComplete
    fun taskComplete(task: DownloadTask?) {
        Timber.d("path ==> " + task!!.downloadEntity.filePath)
        Timber.d("md5Code ==> " + CommonUtil.getFileMD5(File(task.filePath)))
        invokeListener(task)
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
        viewModel.sorting = comparator as BookSortingComparator
    }

    fun onPageSelected() {
        if (query.isNotEmpty())
            search("")


        if (inActionMode)
            inActionMode = false

    }

    inner class BookRecyclerAdapter :
        SelectableAdapter<BookRecyclerAdapter.BookViewHolder, Book>(object :
            DiffUtil.ItemCallback<Book>() {
            override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean = oldItem == newItem
            override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean =
                oldItem == newItem
        }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder =
            BookViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.book_recycler_adapter_layout,
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onViewRecycled(holder: BookViewHolder) {
            holder.unbind()
            super.onViewRecycled(holder)
        }

        fun getSelectedIds(): List<Long> {
            return getSelectedItems().map { currentList[it].entity!!.id }
        }

        override fun performFiltering(constraint: CharSequence?) {
            if (constraint?.length == 0) {
                if (searchTempList.isNotEmpty()) {
                    submitList(mutableListOf<Book>().apply { addAll(searchTempList) })
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

        fun clearListener() {
            adapter.currentList.forEach { it.listener = null }
        }

        inner class BookViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
            private var book: Book? = null

            init {
                addListener()
            }

            private fun addListener() {
                v.apply {
                    pausePlayIv.setOnClickListener {
                        if (book == null) return@setOnClickListener

                        if (book!!.entity!!.isComplete) {
                            makeToast(getString(R.string.task_completed))
                            return@setOnClickListener
                        }

                        if (book!!.entity!!.state == IEntity.STATE_PRE || book!!.entity!!.state == IEntity.STATE_POST_PRE) {
                            makeToast(getString(R.string.please_wait_collecting_data))
                            return@setOnClickListener
                        }

                        when (book!!.entity!!.state) {
                            IEntity.STATE_WAIT, IEntity.STATE_RUNNING -> {
                                book!!.stop()
                                postEvent(BookEvent(listOf(book!!), BookEventType.BOOK_PAUSED))
                            }
                            else -> {
                                book!!.resume()
                                postEvent(BookEvent(listOf(book!!), BookEventType.BOOK_RESUMED))
                            }
                        }
                    }
                }
            }

            fun bind(book: Book) {
                this.book = book

                book.listener = {
                    Timber.d("listener invoked")
                    updateView()
                }

                updateView()
            }

            fun unbind() {
                book?.listener = null
                book = null
            }


            private fun updateView() {
                v.apply {
                    if (book == null) return

                    bookConstraintLayout.isSelected = isSelected(adapterPosition)
                    pageNameTv.text = book!!.entity!!.fileName
                    pageSpeedTv.text = book!!.entity!!.convertSpeed
                    pageProgressBar.progress = book!!.entity!!.percent.toFloat()
                    pageProgressBar.labelText = book!!.entity!!.percent.toString()
                    pausePlayIv.setImageResource(
                        when (book!!.entity!!.state) {
                            IEntity.STATE_RUNNING, IEntity.STATE_WAIT, IEntity.STATE_PRE, IEntity.STATE_POST_PRE -> {
                                R.drawable.ic_pause
                            }
                            else -> {
                                R.drawable.ic_play
                            }
                        }
                    )

                    setOnClickListener {
                        if (selectedItemCount > 0) {
                            toggleSelection(adapterPosition)
                            return@setOnClickListener
                        }
                        if (selectedItemCount <= 0) {
                            if (inActionMode) inActionMode = false
                        }
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

                    pageStatusTv.text = when (book!!.entity!!.state) {
                        IEntity.STATE_RUNNING -> {
                            getString(R.string.downloading)
                        }
                        IEntity.STATE_CANCEL -> {
                            getString(R.string.canceled)
                        }
                        IEntity.STATE_COMPLETE -> {
                            getString(R.string.completed)
                        }
                        IEntity.STATE_FAIL -> {
                            getString(R.string.failed)
                        }
                        IEntity.STATE_OTHER -> {
                            getString(R.string.unknown)
                        }
                        IEntity.STATE_STOP -> {
                            getString(R.string.paused)
                        }
                        IEntity.STATE_PRE -> {
                            getString(R.string.connecting)
                        }

                        IEntity.STATE_POST_PRE -> {
                            getString(R.string.connecting)
                        }
                        IEntity.STATE_WAIT -> {
                            getString(R.string.waiting)
                        }
                        else -> ""
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        (adapter as BookRecyclerAdapter).clearListener()
        super.onDestroy()
    }

}