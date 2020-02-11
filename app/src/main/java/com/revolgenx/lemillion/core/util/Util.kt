package com.revolgenx.lemillion.core.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.view.makeToast
import com.revolgenx.lemillion.view.string
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.greenrobot.eventbus.EventBus



val MAGNET_PREFIX = "magnet"
val HTTP_PREFIX = "http"
val HTTPS_PREFIX = "https"
val FTP_PREFIX = "ftp"
val FILE_PREFIX = "file"
val CONTENT_PREFIX = "content"

//eventbus
fun <T> registerClass(cls: T) {
    if (!EventBus.getDefault().isRegistered(cls))
        EventBus.getDefault().register(cls)
}

fun <T> unregisterClass(cls: T) {
    if (EventBus.getDefault().isRegistered(cls))
        EventBus.getDefault().unregister(cls)
}

fun <T> postEvent(cls: T) {
    EventBus.getDefault().post(cls)
}


fun <T> postStickyEvent(cls: T) {
    EventBus.getDefault().postSticky(cls)
}

fun Long.formatRemainingTime(): String {
    var n = this
    val day = n / (24 * 3600)

    n %= (24 * 3600)
    val hour = n / 3600

    n %= 3600
    val minutes = n / 60

    n %= 60
    val seconds = n
    return "$day:$hour:$minutes:$seconds"
}

fun Float.formatProgress() = String.format("%.1f%%", this)

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

fun Context.copyToClipBoard(str: String) {
    val clipboard: ClipboardManager =
        ContextCompat.getSystemService<ClipboardManager>(this, ClipboardManager::class.java)!!
    val clip = ClipData.newPlainText(string(R.string.app_name), str)
    clipboard.setPrimaryClip(clip)
    makeToast("Text Copied.")
}






