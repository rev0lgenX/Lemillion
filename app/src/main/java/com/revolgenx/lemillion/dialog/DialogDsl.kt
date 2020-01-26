package com.revolgenx.lemillion.dialog

import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
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
import com.obsez.android.lib.filechooser.ChooserDialog
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.activity.MainActivity
import com.revolgenx.lemillion.core.preference.*
import kotlinx.android.synthetic.main.input_layout.view.*
import kotlinx.android.synthetic.main.setting_layout.*


fun Context.showErrorDialog(error: String) {
    MaterialDialog(this).show {
        title(R.string.error)
        message(text = error)
        positiveButton(R.string.ok)
    }
}

fun MainActivity.makeSettingDialog() {
    MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
        val view = customView(R.layout.setting_layout, scrollable = true)
        view.apply {
            darkThemeSwitch.isChecked = getThemePref(context) == 1
            darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
                setThemePref(if (isChecked) 1 else 0, context)
                recreate()
            }

            storageSettingTv.description = storagePath(context)!!
            storageSettingTv.setOnClickListener {
                ChooserDialog(
                    this@makeSettingDialog,
                    if (getThemePref(context) == 1) R.style.FileChooserStyle_Dark else R.style.CustomFileChooserStyle
                )
                    .withFilter(true, false)
                    .withStartFile(storagePath(context)!!)
                    .withChosenListener { dir, _ ->
                        setStoragePath(context, dir)
                        storageSettingTv.description = dir
                    }
                    .withResources(R.string.choose_a_folder, R.string.done, R.string.cancel)
                    .titleFollowsDir(true)
                    .withFileIcons(
                        false,
                        AppCompatDrawableManager.get().getDrawable(
                            context,
                            R.drawable.ic_file
                        ).also { DrawableCompat.setTint(it, iconColor) },
                        AppCompatDrawableManager.get().getDrawable(
                            context,
                            R.drawable.ic_folder
                        ).also { DrawableCompat.setTint(it, iconColor) }
                    )
                    .enableOptions(true)
                    .build()
                    .show()
            }


        }
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
            } else {
                showErrorDialog(getString(R.string.invalid_url))
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