package com.revolgenx.weaverx.dialog

import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onPreShow
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.files.fileChooser
import com.afollestad.materialdialogs.input.InputCallback
import com.afollestad.materialdialogs.utils.MDUtil.textChanged
import com.revolgenx.weaverx.R
import com.revolgenx.weaverx.activity.MainActivity
import com.revolgenx.weaverx.core.preference.storagePath
import com.revolgenx.weaverx.core.torrent.common.MagnetParser
import com.revolgenx.weaverx.core.torrent.common.TorrentParser
import com.revolgenx.weaverx.core.util.makeToast
import kotlinx.android.synthetic.main.input_layout.view.*
import org.apache.commons.io.IOUtils
import timber.log.Timber
import java.io.FileInputStream
import java.io.IOException
import java.text.ParseException

fun MainActivity.openMagnetDialog() {
    MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
        title(R.string.magnet_link)
        inputDialog(this@openMagnetDialog) { _, text ->
            try {
                MagnetParser().parse(text.toString()).let {
                    it.path = storagePath(context)
                    getTorrentTab().addMagnet(it)
                }
                dismiss()
            } catch (e: ParseException) {
                Timber.e(e)
                makeToast(resId = R.string.unable_to_parse_magnet)
            } catch (e: Exception) {
                Timber.e(e)
                makeToast(resId = R.string.unable_to_parse_magnet)
            }
        }

        positiveButton(R.string.ok) {
            it.noAutoDismiss()
        }
        negativeButton(R.string.cancel)
    }
}

fun MainActivity.openFileChooser() {
    MaterialDialog(this).show {
        fileChooser { _, file ->
            if (file.extension == "torrent") {
                try {
                    TorrentParser().parseFromFile(file, storagePath(context))?.let {
                        getTorrentTab().addTorrent(it)
                    }
//                    val meta = TorrentMeta().apply {
//                        dataBlob = IOUtils.toByteArray(FileInputStream(file))
//                        path = storagePath(context)
//                    }
                    dismiss()
                } catch (e: IOException) {
                    Timber.e(e)
                    makeToast(getString(R.string._unable_to_parse_file))
                } catch (e: Exception) {
                    Timber.e(e)
                    makeToast(getString(R.string._unable_to_parse_file))
                }
            } else {
                makeToast(getString(R.string._unable_to_parse_file))
            }
        }
        positiveButton(R.string.ok) { it.noAutoDismiss() }
        negativeButton(R.string.cancel)
    }
}


fun MaterialDialog.inputDialog(
    context: Context,
    allowEmpty: Boolean = false,
    handleActionButton: Boolean = false,
    textChangeCallback: ((CharSequence) -> Unit)? = null,
    callback: InputCallback = null
) {

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    var pasteData: String?

    customView(R.layout.input_layout)
    setActionButtonEnabled(WhichButton.POSITIVE, allowEmpty)

    getCustomView().inputEt.let { et ->
        onPreShow {
            et.post {
                et.requestFocus()
                val imm =
                    windowContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        if (callback != null)
            positiveButton { callback.invoke(this@inputDialog, et.text ?: "") }


        et.textChanged {
            textChangeCallback?.invoke(it)
            if (!allowEmpty && !handleActionButton) {
                setActionButtonEnabled(WhichButton.POSITIVE, it.isNotEmpty())
            }
        }


    }

    getCustomView().apply {
        clipboardIv.setOnClickListener {
            val item = clipboard.primaryClip?.getItemAt(0)
            pasteData = item?.text?.toString()
            if (pasteData != null) {
                inputEt.setText(pasteData)
            } else {
            }
        }
    }
}