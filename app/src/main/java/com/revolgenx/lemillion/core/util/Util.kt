package com.revolgenx.lemillion.core.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.revolgenx.lemillion.R
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


fun Context.makeToast(msg: String? = null, @StringRes resId: Int? = null) {
    Toast.makeText(this, msg ?: getString(resId!!), Toast.LENGTH_SHORT).show()
}

fun Fragment.makeToast(msg: String? = null, resId: Int? = null) {
    context!!.makeToast(msg, resId)
}


fun TextView.showProgress(@StringRes resId: Int = 0, b: Boolean = false, progColor: Int? = null) {
    if (b) {
        this.showProgress {
            buttonTextRes = resId
            progressRadiusRes = R.dimen.progress_radius_dimen
            progressStrokeRes = R.dimen.progress_stroke_dimen
            progressColor = progColor ?: ContextCompat.getColor(context, R.color.colorAccent)
        }
    } else {
        this.hideProgress(resId)
    }
}


fun makeTextView(context: Context) = TextView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}

fun TextView.setNAText(txt: String) {
    text = if (txt.isEmpty()) "n/a"
    else txt
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

fun Context.color(@ColorRes id: Int) = ContextCompat.getColor(this, id)
fun Fragment.color(@ColorRes id: Int) = context!!.color(id)

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}


inline fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
inline fun Context.dp(value: Int): Float = (value * resources.displayMetrics.density)

inline fun View.dip(value: Int) = context.dip(value)
inline fun View.dp(value: Int) = context.dp(value)



