package com.revolgenx.lemillion.core.torrent.util

import org.libtorrent4j.Priority
import org.libtorrent4j.swig.int_vector


fun array2vector(arr: Array<Priority>): int_vector? {
    val v = int_vector()
    for (i in arr.indices) {
        val p = arr[i]
        v.push_back(p.swig())
    }
    return v
}