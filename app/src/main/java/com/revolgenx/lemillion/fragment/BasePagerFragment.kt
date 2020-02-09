package com.revolgenx.lemillion.fragment

import android.content.Context
import androidx.fragment.app.Fragment

abstract class BasePagerFragment : Fragment() {
    abstract fun getTitle(context: Context):String
}