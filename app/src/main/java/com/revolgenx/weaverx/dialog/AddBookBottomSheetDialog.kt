package com.revolgenx.weaverx.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.files.folderChooser
import com.arialyy.annotations.Download
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadTask
import com.arialyy.aria.util.CheckUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.revolgenx.weaverx.R
import com.revolgenx.weaverx.activity.MainActivity
import com.revolgenx.weaverx.core.preference.storagePath
import com.revolgenx.weaverx.core.util.makeToast
import com.revolgenx.weaverx.core.util.postEvent
import com.revolgenx.weaverx.core.util.showProgress
import com.revolgenx.weaverx.event.BookAddedEvent
import kotlinx.android.synthetic.main.add_book_bottom_sheet_dialog_layout.*
import timber.log.Timber
import java.io.File
import java.lang.Exception

class AddBookBottomSheetDialog : BottomSheetDialogFragment() {

    private val urlKey = "url_key"
    private val pathKey = "path_key"
    private val nameKey = "name_key"
    private val taskIdKey = "task_id_key"

    private var onPreComplete = false

    private var taskId: Long = -1L
    private var url: String = ""
    private var path: String = ""
    private var name: String = ""
    private val fullPath: String
        get() = "$path/$name"
    private var failCause = TaskFailedCause.NONE


    enum class TaskFailedCause {
        NONE, TASK_EXIST, FILE_EXIST, SAME_URL, LOW_SPACE, UNKNOWN, FINDING
    }

    companion object {
        fun newInstance(url: String) = AddBookBottomSheetDialog().apply {
            Timber.d("url $url")
            arguments = bundleOf(urlKey to url)
            Aria.download(this).register()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.add_book_bottom_sheet_dialog_layout, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        url = arguments!!.getString(urlKey)!!

        addListener()
        if (savedInstanceState == null) {
            name = URLUtil.guessFileName(url, null, null)
            path = storagePath(context!!)!!
            createAriaDownload()
        } else {
            taskId = savedInstanceState.getLong(taskIdKey)
            name = savedInstanceState.getString(nameKey)!!
            path = savedInstanceState.getString(pathKey)!!
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(taskIdKey, taskId)
        outState.putString(nameKey, name)
        outState.putString(pathKey, path)
        super.onSaveInstanceState(outState)
    }

    private fun updateView() {
        bookNameTv.description = name
        bookPathTv.description = path
        bookUrlTv.description = url

        when (failCause) {
            TaskFailedCause.NONE -> {
                showStatusProgress(visibility = false)
                errorTextView.visibility = View.GONE
            }
            TaskFailedCause.TASK_EXIST -> {
                errorTextView.visibility = View.VISIBLE
                errorTextView.text =
                    getString(R.string.task_exists_please_change_the_file_name_or_path_and_continue)
            }
            TaskFailedCause.FILE_EXIST -> {
                errorTextView.visibility = View.VISIBLE
                errorTextView.text =
                    getString(R.string.task_exists_please_change_the_file_name_or_path_and_continue)
            }


            TaskFailedCause.LOW_SPACE -> {

            }
            TaskFailedCause.SAME_URL -> {
                errorTextView.visibility = View.VISIBLE
                errorTextView.text = getString(R.string.task_with_same_url_exists)
            }
            TaskFailedCause.UNKNOWN -> {
                errorTextView.visibility = View.VISIBLE
                errorTextView.text = getString(R.string.unknown)
            }
            TaskFailedCause.FINDING->{

            }
        }
    }

    private fun addListener() {
        bookNameTv.setDrawableClickListener {
            MaterialDialog(this.context!!).show {
                inputDialog(this.context, prefill = name) { materialDialog, charSequence ->
                    name = charSequence.toString()
                    createAriaDownload()
                }
                title(R.string.enter_file_name)
                positiveButton(R.string.ok)
                negativeButton(R.string.cancel)
            }
        }

        bookPathTv.setDrawableClickListener {
            MaterialDialog(this.context!!).show {
                folderChooser { dialog, file ->
                    path = file.path
                    createAriaDownload()
                }
            }
        }

        bookUrlTv.setDrawableClickListener {
            (activity as MainActivity).openLinkInputDialog()
            dialog?.cancel()
        }


        cancelBookTv.setOnClickListener {
            dialog?.cancel()
        }

        addBookTv.setOnClickListener {
            if (!onPreComplete || taskId == -1L) {
                makeToast(resId = R.string.unresolved_issues)
                return@setOnClickListener
            }
            postEvent(BookAddedEvent(taskId))
            dialog?.dismiss()
        }
    }


    @Download.onTaskStart
    fun onTaskStart(task: DownloadTask) {
        showStatusProgress(visibility = false)
        onPreComplete = true
        updateView()
        Timber.d("ontask start")
        Aria.download(this).load(task.entity.id).stop()
    }

    @Download.onTaskFail
    fun onTaskFail(task: DownloadTask?, e: Exception?) {
        failCause = TaskFailedCause.UNKNOWN

        Timber.e(e)
        Timber.d("exception null ${e == null}")

        //2020-01-24 20:28:38.093 1872-1872/com.example.weaverx E/AddBookBottomSheetDialog: com.arialyy.aria.exception.AriaIOException: Aria Net Exception:任务下载失败，errorCode：404, url: http://ipv4.download.thinkbroadband.com/70MB.zip
        showStatusProgress(R.string.failed)
        updateView()
    }

    private fun createAriaDownload() {
        failCause = TaskFailedCause.NONE
        onPreComplete = false

        if (taskId != -1L) {
            Aria.download(this).load(taskId).cancel()
            taskId = -1
        }

        showStatusProgress(R.string.checking, progress = true)

        if (!CheckUtil.checkDPathConflicts(false, fullPath)) {
            makeToast(resId = R.string.task_exists)
            failCause = TaskFailedCause.TASK_EXIST
            showStatusProgress(R.string.failed)
            updateView()
            return
        }

        if (File(path, name).exists()) {
            makeToast(resId = R.string.file_exists)
            failCause = TaskFailedCause.FILE_EXIST
            showStatusProgress(R.string.failed)
            updateView()
            return
        }

        if (Aria.download(this).taskExists(url)) {
            makeToast(resId = R.string.file_exists)
            failCause = TaskFailedCause.SAME_URL
            showStatusProgress(R.string.failed)
            updateView()
            return
        }

        //TODO://check space

        failCause = TaskFailedCause.FINDING
        updateView()
        taskId = Aria.download(this).load(url).setFilePath(fullPath).create()
    }

    private fun showStatusProgress(
        @StringRes resId: Int = 0,
        visibility: Boolean = true,
        progress: Boolean = false
    ) {
        if (!visibility) {
            checkingTv.visibility = View.GONE
            return
        }

        checkingTv.visibility = View.VISIBLE
        checkingTv.showProgress(resId = resId, b = progress)
    }


    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        if (taskId != -1L) {
            Aria.download(this).load(taskId).cancel()
        }
    }

    override fun onDestroy() {
        Aria.download(this).unRegister()
        super.onDestroy()
    }

}