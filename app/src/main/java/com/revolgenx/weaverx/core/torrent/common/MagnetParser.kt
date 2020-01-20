package com.revolgenx.weaverx.core.torrent.common

import android.net.Uri
import java.io.UnsupportedEncodingException
import java.net.URISyntaxException
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.ParseException


class MagnetParser {

    var infoHash: String? = null
    var name: String? = null
    var announce: MutableList<Uri>? = null
    var path:String? = null


    @Throws(ParseException::class, UnsupportedEncodingException::class, URISyntaxException::class)
    fun parse(magUri: String): MagnetParser {
        var magnetUri = magUri
        if (!magnetUri.startsWith("magnet:?")) throw ParseException(magnetUri,0)

        magnetUri = magnetUri.substring(8)

        val args = magnetUri.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (arg in args) {
            when {
                arg.startsWith("xt=urn:btih:") -> infoHash = arg.substring(12)
                arg.startsWith("dn=") -> name = URLDecoder.decode(arg.substring(3), "UTF-8")
                arg.startsWith("tr=") -> addAnnounce(Uri.parse(URLDecoder.decode(arg.substring(3), "UTF-8")))
            }
        }
        return this
    }

    fun addAnnounce(uri: Uri) {
        if (announce == null) announce = ArrayList()
        announce!!.add(uri)
    }

    @Throws(UnsupportedEncodingException::class)
    fun write(): String {
        var magnet = ("magnet:?xt=urn:btih:" + infoHash
                + "&dn=" + URLEncoder.encode(name, "UTF-8"))
        for (uri in announce!!) {
            magnet += "&tr=" + URLEncoder.encode(uri.toString(), "UTF-8")
        }
        return magnet
    }
}