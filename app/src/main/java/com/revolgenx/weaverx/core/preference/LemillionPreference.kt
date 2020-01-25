package com.revolgenx.weaverx.core.preference

import android.content.Context
import androidx.preference.PreferenceManager
import com.revolgenx.weaverx.core.preference.DEFAULT.DEFAULT_SORTING
import com.revolgenx.weaverx.core.preference.KEY.KEY_SORTING
import com.revolgenx.weaverx.core.util.getDefualtStoragePath



fun Context.putBoolean(key: String, value: Boolean = false) =
    sharedPreference().edit().putBoolean(key, value).apply()

fun Context.putPrefString(key: String, value: String? = "") =
    sharedPreference().edit().putString(key, value).apply()

fun Context.putInt(key: String, value: Int = -1) =
    sharedPreference().edit().putInt(key, value).apply()

fun Context.getPrefBoolean(key: String, def: Boolean = false) =
    sharedPreference().getBoolean(key, def)

fun Context.getPrefInt(key: String, def: Int) = sharedPreference().getInt(key, def)
fun Context.getPrefString(key: String, def: String = "") = sharedPreference().getString(key, def)


object DEFAULT {
    val DEFAULT_STORAGE = getDefualtStoragePath()
    const val DEFAULT_SORTING = 0
}

object KEY {
    const val STORAGE_KEY = "key_storage"
    const val KEY_THEME = "key_theme"
    const val KEY_SORTING = "key_sorting"
}

fun Context.sharedPreference() = PreferenceManager.getDefaultSharedPreferences(this)

fun storagePath(context: Context) =
    context.sharedPreference().getString(KEY.STORAGE_KEY, DEFAULT.DEFAULT_STORAGE)


fun getSorting(context: Context) = context.getPrefInt(KEY_SORTING, DEFAULT_SORTING)
fun setSorting(context: Context, sorted:Int) = context.putInt(KEY_SORTING, sorted)
