package com.revolgenx.lemillion.core.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*


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
    return when {
        this >= GB -> {
            String.format("%.1f GB", this * 1.0 / GB)
        }
        this >= MB -> {
            String.format("%.1f MB", this * 1.0 / MB)
        }
        else -> {
            String.format("%.1f KB", this * 1.0 / KB)
        }
    }
}

fun Long.formatSpeed(): String = this.formatSize() + "/s"

fun makeTempFile(context: Context, postfix: String): File {
    return File(getTempDir(context), UUID.randomUUID().toString() + postfix)
}

private fun getTempDir(context: Context): File? {
    val tmpDir = File(context.getExternalFilesDir(null), "temp")
    if (!tmpDir.exists()) {
        if (!tmpDir.mkdirs()) {
            return null
        }
    }

    return tmpDir
}

@Throws(Exception::class)
fun copyContentURIToFile(context: Context, uri: Uri, file: File) {
    FileUtils.copyInputStreamToFile(context.contentResolver.openInputStream(uri), file)
}