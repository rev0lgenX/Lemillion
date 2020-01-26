package com.revolgenx.lemillion.event

data class TorrentRemovedEvent(var hashes: List<String>, var withFiles:Boolean)