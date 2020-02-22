package com.revolgenx.lemillion.dialog

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
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.activity.MainActivity
import com.revolgenx.lemillion.core.book.Book
import com.revolgenx.lemillion.core.book.BookProtocol
import com.revolgenx.lemillion.core.preference.storagePath
import com.revolgenx.lemillion.core.util.*
import com.revolgenx.lemillion.view.string
import com.revolgenx.lemillion.view.makeToast
import com.revolgenx.lemillion.view.showProgress
import com.revolgenx.lemillion.event.BookAddedEvent
import kotlinx.android.synthetic.main.add_book_bottom_sheet_dialog_layout.*
import timber.log.Timber
import java.io.File
import java.lang.Exception
import java.net.HttpURLConnection
import java.util.regex.Pattern

//TODO:rotation problem
class AddBookBottomSheetDialog : BottomSheetDialogFragment() {

    private val urlKey = "url_key"
    private val pathKey = "path_key"
    private val nameKey = "name_key"
    private val taskKey = "task_key"
    private val failedCauseKey = "failed_cause_key"

    private var taskId = -1L

    private var url: String = ""
    private var path: String = ""
    private var name: String = ""
    private var size: Long = 0L
    private val fullPath: String
        get() = "$path/$name"
    private var failCause = TaskFailedCause.NONE

    private var done = false

    enum class TaskFailedCause {
        NONE, HTTP_NOT_FOUND, TASK_EXIST, FILE_EXIST, SAME_URL, LOW_SPACE, UNKNOWN, FINDING
    }

    companion object {
        fun newInstance(url: String) = AddBookBottomSheetDialog().apply {
            arguments = bundleOf(urlKey to url)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Aria.download(this).register()
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
            name = savedInstanceState.getString(nameKey)!!
            path = savedInstanceState.getString(pathKey)!!
            createAriaDownload()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(nameKey, name)
        outState.putString(pathKey, path)
//        if (taskId != -1L) {
//            Aria.download(this).load(taskId).cancel()
//        }

        outState.putLong(taskKey, taskId)
        super.onSaveInstanceState(outState)
    }

    private fun updateView() {
        if (context == null) return
        bookNameTv.description = name
        bookPathTv.description = path
        bookUrlTv.description = url
        bookFileSizeTv.titleTextView().text =
            context!!.string(R.string.size_free).format(getFree(File(path)).formatSize())
        bookFileSizeTv.description = size.formatSize()

        when (failCause) {
            TaskFailedCause.NONE -> {
                showStatusProgress(visibility = false)
                errorTextView.visibility = View.GONE
                showDrawables(true)
            }
            TaskFailedCause.TASK_EXIST -> {
                errorTextView.visibility = View.VISIBLE
                errorTextView.text =
                    getString(R.string.task_exists_please_change_the_file_name_or_path_and_continue)
                showDrawables(true)
            }
            TaskFailedCause.FILE_EXIST -> {
                errorTextView.visibility = View.VISIBLE
                errorTextView.text =
                    getString(R.string.task_exists_please_change_the_file_name_or_path_and_continue)
                showDrawables(true)
            }
            TaskFailedCause.HTTP_NOT_FOUND -> {
                errorTextView.visibility = View.VISIBLE
                errorTextView.text = getString(R.string.http_not_found)
                showDrawables(false)
            }
            TaskFailedCause.LOW_SPACE -> {
                errorTextView.visibility = View.VISIBLE
                errorTextView.text = getString(R.string.low_space)
                showDrawables(true)
            }
            TaskFailedCause.SAME_URL -> {
                errorTextView.visibility = View.VISIBLE
                errorTextView.text = getString(R.string.task_with_same_url_exists)
                showDrawables(false)
            }
            TaskFailedCause.UNKNOWN -> {
                errorTextView.visibility = View.VISIBLE
                errorTextView.text = getString(R.string.unknown)
                showDrawables(false)
            }
            TaskFailedCause.FINDING -> {
                showDrawables(false)
            }
        }
    }

    private fun showDrawables(b: Boolean = false) {
        bookNameTv.showDrawable(b)
        bookPathTv.showDrawable(b)
    }

    private fun addListener() {
        bookNameTv.setDrawableClickListener {
            MaterialDialog(this.context!!).show {
                inputDialog(this.context, prefill = name) { _, charSequence ->
                    val tempName = charSequence.toString()
                    if (File("$path/$tempName").exists()) {
                        context.showErrorDialog(getString(R.string.file_exists))
                        return@inputDialog
                    }
                    if (!possibleError("$path/$tempName")) {
                        name = tempName
                    }
                    updateView()
                }
                title(R.string.enter_file_name)
                positiveButton(R.string.ok)
                negativeButton(R.string.cancel)
            }
        }

        bookPathTv.setDrawableClickListener {
            MaterialDialog(this.context!!).show {
                folderChooser { dialog, file ->
                    val tempPath = file.path
                    if (File("$tempPath/$name").exists()) {
                        context.showErrorDialog(getString(R.string.file_exists))
                        return@folderChooser
                    }
                    if (!possibleError("$tempPath/$name")) {
                        path = file.path
                    }
                    updateView()
                }
            }
        }

        bookUrlTv.setDrawableClickListener {
            (activity as MainActivity).openLinkInputDialog()
            dismiss()
        }


        cancelBookTv.setOnClickListener {
            if (taskId != -1L) {
                val downloadTask = Aria.download(this).load(taskId)
                if (downloadTask.taskState == 5 || downloadTask.taskState == 6) {
                    makeToast(getString(R.string.please_wait_collecting_data))
                    return@setOnClickListener
                }
            }
            dismiss()
        }

        addBookTv.setOnClickListener {

            when (failCause) {
                TaskFailedCause.NONE -> {
                    if ((size >= getFree(File(path)))) {
                        failCause = TaskFailedCause.LOW_SPACE
                        updateView()
                        return@setOnClickListener
                    }

                    val protocol = when {
                        url.startsWith(HTTP_PREFIX) || url.startsWith(HTTPS_PREFIX) -> BookProtocol.HTTP
                        url.startsWith(FTP_PREFIX) -> BookProtocol.FTP
                        else -> {
                            context?.showErrorDialog("unsupported url")
                            return@setOnClickListener
                        }
                    }

                    postEvent(BookAddedEvent(Book().also { book ->
                        book.id = Aria.download(this).load(url).setFilePath(fullPath).create()
                        book.bookProtocol = protocol
                    }))
                    done = true
                    dialog?.dismiss()
                }
                TaskFailedCause.HTTP_NOT_FOUND -> {
                    makeToast(resId = R.string.unresolved_issues)
                }
                TaskFailedCause.TASK_EXIST -> {
                    makeToast(resId = R.string.unresolved_issues)
                }
                TaskFailedCause.FILE_EXIST -> {
                    makeToast(resId = R.string.unresolved_issues)
                }
                TaskFailedCause.SAME_URL -> {
                    makeToast(resId = R.string.unresolved_issues)
                }
                TaskFailedCause.LOW_SPACE -> {
                    makeToast(resId = R.string.unresolved_issues)
                }
                TaskFailedCause.UNKNOWN -> {
                    makeToast(resId = R.string.unresolved_issues)
                }
                TaskFailedCause.FINDING -> {
                    makeToast(resId = R.string.please_wait)
                }
            }
        }
    }

    private fun possibleError(newPath: String): Boolean {
        failCause = TaskFailedCause.NONE

        if (!CheckUtil.checkDPathConflicts(false, newPath)) {
            makeToast(resId = R.string.task_exists)
            failCause = TaskFailedCause.TASK_EXIST
            showStatusProgress(R.string.failed)
            return true
        }

        if (Aria.download(this).taskExists(url)) {
            makeToast(resId = R.string.file_exists)
            failCause = TaskFailedCause.SAME_URL
            showStatusProgress(R.string.failed)
            return true
        }

        //todo check space

        return false
    }

    private fun checkFileExists(newPath: String): Boolean {
        if (File(newPath).exists()) {
            makeToast(resId = R.string.file_exists)
            failCause = TaskFailedCause.FILE_EXIST
            showStatusProgress(R.string.failed)
            return true
        }
        return false
    }

    @Download.onTaskRunning
    fun onTaskRunning(task: DownloadTask?) {
        if (task?.entity?.id != taskId) return

        showStatusProgress(visibility = false)
        failCause = TaskFailedCause.NONE
        name = task.downloadEntity.serverFileName ?: name
        size = task.fileSize

        if ((task.fileSize >= getFree(File(path)))) {
            failCause = TaskFailedCause.LOW_SPACE
        }
        checkFileExists(fullPath)
        Aria.download(this).load(task.entity.id).stop()
        Aria.download(this).load(task.entity.id).cancel(true)
        taskId = -1
        updateView()
    }

    @Download.onTaskFail
    fun onTaskFail(task: DownloadTask?, e: Exception?) {
        failCause = TaskFailedCause.UNKNOWN

        Timber.e(e)
        Timber.d("exception null ${e == null} message ${e?.message}")

        if (e != null) {
            val pattern = Pattern.compile("errorCode：([0-9]*)")
            val matcher = pattern.matcher(e.message ?: "")
            if (matcher.find()) {
                e.message
                val code = matcher.group(1)
                if (code == HttpURLConnection.HTTP_NOT_FOUND.toString()) {
                    failCause = TaskFailedCause.HTTP_NOT_FOUND
                }
            }
        }

        if (taskId != -1L) {
            Aria.download(this).load(taskId).cancel()
            taskId = -1L
        }
        showStatusProgress(R.string.failed)
        updateView()
    }

    private fun createAriaDownload() {

        if (taskId != -1L) {
            Aria.download(this).load(taskId).cancel()
            taskId = -1
        }

        showStatusProgress(R.string.checking, progress = true)

        //TODO://check space

        if (possibleError(fullPath)) {
            updateView()
            return
        }

        failCause = TaskFailedCause.FINDING
        updateView()

        if (url.startsWith(HTTP_PREFIX) || url.startsWith(HTTPS_PREFIX)) {
            taskId = Aria.download(this).load(url).setFilePath(fullPath).setHighestPriority()
            if (taskId != -1L) {
                Aria.download(this).load(taskId).resume()
            }
//        } else if (url.startsWith(FTP_PREFIX)) {
//            val option = FtpOption()
//            option.setUrlEntity(FtpUrlEntity())
//
//            taskId = Aria.download(this).loadFtp(url).option(option).setFilePath(fullPath).setHighestPriority()
//            if (taskId != -1L) {
//                Aria.download(this).load(taskId).resume()
//            }
        } else {
            context!!.showErrorDialog(getString(R.string.unsupported_url))
            dismiss()
        }
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

    override fun onDestroy() {
        if (taskId != -1L) {
            val task = Aria.download(this).load(taskId)
            task.cancel(true)
            task.removeRecord()
        }
        Aria.download(this).unRegister()
        super.onDestroy()
    }

}