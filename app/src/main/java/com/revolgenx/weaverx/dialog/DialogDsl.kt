package com.revolgenx.weaverx.dialog

import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.core.content.res.ResourcesCompat
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onPreShow
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.InputCallback
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.afollestad.materialdialogs.utils.MDUtil.textChanged
import com.arialyy.aria.util.CheckUtil
import com.revolgenx.weaverx.R
import com.revolgenx.weaverx.activity.MainActivity
import com.revolgenx.weaverx.core.preference.getSorting
import kotlinx.android.synthetic.main.input_layout.view.*


fun Context.showErrorDialog(error: String) {
    MaterialDialog(this).show {
        title(R.string.error)
        message(text = error)
        positiveButton(R.string.ok)
    }
}


fun MainActivity.openLinkInputDialog(prefill: String? = null) {
    MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
        title(R.string.input_url)
        inputDialog(this.context, prefill) { _, text ->
            if (CheckUtil.checkUrl(text.toString())) {
                AddBookBottomSheetDialog.newInstance(text.toString())
                    .show(supportFragmentManager, "add_book_fragment_tag")
                dismiss()
            }
        }

        noAutoDismiss()
        positiveButton(R.string.ok)
        negativeButton(R.string.cancel) {
            dismiss()
        }
    }
}

fun Context.showInputDialog(
    titleRes: Int? = null,
    prefill: CharSequence? = null,
    callback: (text: String) -> Unit?
) {
    MaterialDialog(this).show {
        title(titleRes)
        input(prefill = prefill) { materialDialog, charSequence ->
            callback.invoke(charSequence.toString())
        }
        getInputLayout().apply {
            typeface = ResourcesCompat.getFont(context, R.font.open_sans_regular)
        }
        positiveButton(res = R.string.done)
        negativeButton(R.string.cancel)
    }
}

fun MaterialDialog.inputDialog(
    context: Context,
    prefill: CharSequence? = null,
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


        if (prefill != null) {
            et.setText(prefill)
        }

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

inline fun MainActivity.makeSortDialog(crossinline callback: ((String, Int) -> Unit)) {
    MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
        title(R.string.sort)
        val sorting = getSorting(context)
        listItemsSingleChoice(
            R.array.sort_array,
            initialSelection = sorting
        ) { dialog, index, text ->
            callback.invoke(text.toString(), index)
        }
    }
}