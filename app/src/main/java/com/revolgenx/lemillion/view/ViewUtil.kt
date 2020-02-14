package com.revolgenx.lemillion.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.fragment.BasePagerFragment


fun AppCompatActivity.makePagerAdapter(fragments: List<BasePagerFragment>) =
    object : FragmentPagerAdapter(this@makePagerAdapter.supportFragmentManager) {
        override fun getItem(position: Int) = fragments[position]
        override fun getCount(): Int = fragments.size
        override fun getPageTitle(position: Int): CharSequence? =
            fragments[position].getTitle(this@makePagerAdapter)
    }


fun Context.makeToast(msg: String? = null, @StringRes resId: Int? = null) {
    Toast.makeText(this, msg ?: getString(resId!!), Toast.LENGTH_SHORT).show()
}

fun Fragment.makeToast(msg: String? = null, resId: Int? = null) {
    context!!.makeToast(msg, resId)
}

fun TextView.setNAText(txt: String) {
    text = if (txt.isEmpty()) "n/a"
    else txt
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




fun Context.color(@ColorRes id: Int) = ContextCompat.getColor(this, id)
fun Context.string(@StringRes id: Int) = getString(id)
fun Fragment.string(@StringRes id: Int) = getString(id)


inline fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
inline fun Context.dp(value: Int): Float = (value * resources.displayMetrics.density)

inline fun View.dip(value: Int) = context.dip(value)
inline fun View.dp(value: Int) = context.dp(value)

