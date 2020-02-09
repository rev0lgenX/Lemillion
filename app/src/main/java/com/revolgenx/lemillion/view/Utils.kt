package com.revolgenx.lemillion.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentPagerAdapter
import com.revolgenx.lemillion.core.util.makeToast
import com.revolgenx.lemillion.fragment.BasePagerFragment


fun AppCompatActivity.makePagerAdapter(fragments: List<BasePagerFragment>) =
    object : FragmentPagerAdapter(this@makePagerAdapter.supportFragmentManager) {
        override fun getItem(position: Int) = fragments[position]
        override fun getCount(): Int = fragments.size
        override fun getPageTitle(position: Int): CharSequence? =
            fragments[position].getTitle(this@makePagerAdapter)
    }

fun Context.copyToClipBoard(str: String) {
    val clipboard: ClipboardManager =
        ContextCompat.getSystemService<ClipboardManager>(this, ClipboardManager::class.java)!!
    val clip = ClipData.newPlainText("lemillion", str)
    clipboard.setPrimaryClip(clip)
    makeToast("Text Copied.")
}

