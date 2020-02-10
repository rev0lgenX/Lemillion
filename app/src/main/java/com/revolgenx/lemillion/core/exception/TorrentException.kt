package com.revolgenx.lemillion.core.exception

import timber.log.Timber
import java.lang.Exception

class TorrentException(msg: String, var data:Any? = null) : Exception(msg) {
    init {
        Timber.e(msg)
    }
}

class TorrentResumeException(msg: String, e: Throwable?) : Exception(msg, e) {
    init {
        Timber.e(e, msg)
    }
}

class TorrentPauseException(msg: String, e: Throwable?) : Exception(msg, e) {
    init {
        Timber.e(e, msg)
    }
}

class TorrentLoadException(msg: String, e: Throwable?) : Exception(msg, e){
//    init {
//        Timber.e(e, msg)
//    }
}


