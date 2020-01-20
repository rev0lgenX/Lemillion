package com.revolgenx.weaverx.core.util

import android.app.Activity
import android.content.Context
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.revolgenx.weaverx.R
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import org.greenrobot.eventbus.EventBus


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

fun Context.makeToast(msg: String? = null, @StringRes resId: Int? = null) {
    Toast.makeText(this, msg ?: getString(resId!!), Toast.LENGTH_SHORT).show()
}

fun Fragment.makeToast(msg: String? = null, resId: Int? = null) {
    context!!.makeToast(msg, resId)
}


fun TextView.showProgress(@StringRes str: Int = 0, b: Boolean = false, progColor: Int? = null) {
    if (b) {
        this.showProgress {
            buttonTextRes = str
            progressRadiusRes = R.dimen.progress_radius_dimen
            progressStrokeRes = R.dimen.progress_stroke_dimen
            progressColor = progColor ?: ContextCompat.getColor(context, R.color.colorAccent)
        }
    } else {
        this.hideProgress(str)
    }
}


