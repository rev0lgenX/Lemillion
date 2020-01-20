package com.revolgenx.weaverx.core.util

import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import java.io.File

fun getDefualtStoragePath(): String {
    val path = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
        .absolutePath

    val dir = File(path)
    return if (dir.exists() && dir.isDirectory)
        path
    else
        if (dir.mkdirs()) path else ""
}