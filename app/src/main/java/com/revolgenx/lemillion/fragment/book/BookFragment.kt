package com.revolgenx.lemillion.fragment.book

import android.content.Context
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
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.inf.IEntity
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.activity.MainActivity
import com.revolgenx.lemillion.adapter.SelectableAdapter
import com.revolgenx.lemillion.core.book.Book
import com.revolgenx.lemillion.core.book.BookProgressListener
import com.revolgenx.lemillion.core.sorting.book.BookSortingComparator
import com.revolgenx.lemillion.core.util.*
import com.revolgenx.lemillion.dialog.BookMetaBottomSheetDialog
import com.revolgenx.lemillion.event.BookEvent
import com.revolgenx.lemillion.event.BookEventType
import com.revolgenx.lemillion.event.BookRemovedEvent
import com.revolgenx.lemillion.fragment.BaseRecyclerFragment
import com.revolgenx.lemillion.viewmodel.BookViewModel
import kotlinx.android.synthetic.main.base_recycler_view_layout.view.*
import kotlinx.android.synthetic.main.book_recycler_adapter_layout.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class BookFragment : BaseRecyclerFragment<BookFragment.BookRecyclerAdapter.BookViewHolder, Book>() {
    companion object {
        fun newInstance() = BookFragment()
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

                R.id.bookRestart -> {
                    postEvent(
                        BookEvent(
                            (adapter as BookRecyclerAdapter).getSelectedBooks(),
                            BookEventType.BOOK_RESTART
                        )
                    )
                    inActionMode = false
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
            if (menu is MenuBuilder) {
                menu.setOptionalIconsVisible(true)
            }
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
                    R.id.bookRestart -> {
                        it.icon = AppCompatDrawableManager.get()
                            .getDrawable(context!!, R.drawable.ic_restart)
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

        fun getSelectedBooks(): List<Book> {
            return getSelectedItems().map { currentList[it] }
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
            adapter.currentList.forEach {
                it.removeAllListener()
            }
        }

        inner class BookViewHolder(private val v: View) : RecyclerView.ViewHolder(v),
            BookProgressListener {
            private var book: Book? = null

            init {
                addListener()
            }

            override fun invoke() {
                updateView()
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

                        if (book!!.isPaused()) {
                            book!!.resume()
                            postEvent(BookEvent(listOf(book!!), BookEventType.BOOK_RESUMED))
                        } else {
                            book!!.stop()
                            postEvent(BookEvent(listOf(book!!), BookEventType.BOOK_PAUSED))
                        }
                    }
                }
            }

            fun bind(book: Book) {
                this.book = book

                book.addListener(this)

                v.setOnClickListener {
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

                    BookMetaBottomSheetDialog.newInstance(book)
                        .show(fragmentManager!!, "book_meta_dialog")
                }
                v.setOnLongClickListener {
                    toggleSelection(adapterPosition)
                    if (!inActionMode) {
                        inActionMode = true
                    }

                    if (selectedItemCount <= 0) {
                        if (inActionMode) inActionMode = false
                    }

                    true
                }
                updateView()
            }

            fun unbind() {
                book?.removeListener(this)
                book = null
            }

            private fun updateView() {
                v.apply {
                    if (book == null) return

                    bookConstraintLayout.isSelected = isSelected(adapterPosition)
                    pageNameTv.text = book!!.entity!!.fileName
                    pageProgressBar.progress = book!!.entity!!.percent.toFloat()
                    pageProgressBar.labelText = book!!.entity!!.percent.toString()

                    indicatorView.setBackgroundColor(
                        when (book!!.state) {
                            IEntity.STATE_RUNNING, IEntity.STATE_PRE, IEntity.STATE_POST_PRE, IEntity.STATE_WAIT -> {
                                color(R.color.downloadingColor)
                            }
                            IEntity.STATE_CANCEL, IEntity.STATE_STOP -> {
                                color(R.color.pausedColor)
                            }
                            IEntity.STATE_COMPLETE -> {
                                color(R.color.completedColor)
                            }
                            IEntity.STATE_FAIL -> {
                                color(R.color.errorColor)
                            }
                            else -> {
                                color(R.color.gray)
                            }
                        }
                    )

                    pausePlayIv.setImageResource(
                        if (book!!.isPaused()) {
                            R.drawable.ic_play
                        } else {
                            R.drawable.ic_pause
                        }
                    )
                    pageStatusTv.text =
                        if (book!!.state == IEntity.STATE_RUNNING) {
                            "${book!!.stateFormat(context)} · ET: ${book!!.eta.formatRemainingTime()}"
                        } else {
                            book!!.stateFormat(context)
                        }
                    pageSpeedTv.text =
                        "${book!!.totalSize.formatSize()} · ${book!!.speed.formatSpeed()}"
                }
            }
        }
    }

    override fun onDestroy() {
        (adapter as BookRecyclerAdapter).clearListener()
        super.onDestroy()
    }

    override fun getTitle(context: Context): String = context.getString(R.string.file)

}