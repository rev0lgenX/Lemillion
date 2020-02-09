package com.revolgenx.lemillion.fragment.torrent.meta

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.revolgenx.lemillion.core.torrent.Torrent
import com.revolgenx.lemillion.fragment.BasePagerFragment

open class TorrentBaseMetaFragment : BasePagerFragment() {
    private val torrentKey = "torrent_key"
    protected lateinit var torrent: Torrent

    companion object {
        fun <T : TorrentBaseMetaFragment> newInstance(torrent: Torrent, clazz: Class<T>) =
            clazz.newInstance().apply {
                arguments = bundleOf(torrentKey to torrent)
            }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        torrent = arguments!!.getParcelable(torrentKey)?:return
    }

    override fun getTitle(context: Context): String = ""

    fun checkValidity() = ::torrent.isInitialized && torrent.handle != null && torrent.handle?.isValid == true


}