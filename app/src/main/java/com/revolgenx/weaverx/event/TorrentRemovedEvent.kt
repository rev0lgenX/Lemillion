package com.revolgenx.weaverx.event

data class TorrentRemovedEvent(var hashes: List<String>, var withFiles:Boolean)