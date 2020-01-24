package com.revolgenx.weaverx.fragment.book

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.inf.IEntity
import com.arialyy.aria.core.task.DownloadTask
import com.arialyy.aria.util.CommonUtil
import com.revolgenx.weaverx.R
import com.revolgenx.weaverx.adapter.SelectableAdapter
import com.revolgenx.weaverx.core.book.Book
import com.revolgenx.weaverx.core.util.Status
import com.revolgenx.weaverx.fragment.BaseRecyclerFragment
import com.revolgenx.weaverx.viewmodel.BookViewModel
import kotlinx.android.synthetic.main.base_recycler_view_layout.view.*
import kotlinx.android.synthetic.main.book_recycler_adapter_layout.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.io.File

class BookFragment : BaseRecyclerFragment<BookFragment.BookRecyclerAdapter.BookViewHolder, Book>() {

    companion object {
        fun newInstance() = BookFragment()
    }

    private val viewModel by viewModel<BookViewModel>()
    private var iconColor: Int = 0
    private var iconColorInverse: Int = 0

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

        inner class BookViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
            private var book: Book? = null
            fun bind(book: Book) {
                this.book = book
                book.listener = {
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

}