package com.revolgenx.weaverx.core.util

import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import java.io.File

val GB: Long = 1073741824 // 1024 * 1024 * 1024
val MB: Long = 1048576 // 1024 * 1024
val KB: Long = 1024


fun getDefualtStoragePath(): String {
    val path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
        .absolutePath

    val dir = File(path)
    return if (dir.exists() && dir.isDirectory)
        path
    else
        if (dir.mkdirs()) path else ""
}

fun Long.formatSize(): String {
    if (this >= GB) {
        return String.format("%.2f GB", this * 1.0 / GB);
    } else if (this >= MB) {
        return String.format("%.2f MB", this * 1.0 / MB);
    } else {
        return String.format("%.2f KB", this * 1.0 / KB);
    }
}