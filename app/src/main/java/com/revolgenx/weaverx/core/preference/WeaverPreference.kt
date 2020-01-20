package com.revolgenx.weaverx.core.preference

import android.content.Context
import androidx.preference.PreferenceManager
import com.revolgenx.weaverx.core.util.getDefualtStoragePath


object DEFAULT {
    val DEFAULT_STORAGE = getDefualtStoragePath()
}

object KEY {
    val STORAGE_KEY = "key_storage"
}

fun sharedPreference(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

fun storagePath(context: Context) =
    sharedPreference(context).getString(KEY.STORAGE_KEY, DEFAULT.DEFAULT_STORAGE)
