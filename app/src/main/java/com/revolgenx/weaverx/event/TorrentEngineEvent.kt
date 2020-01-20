package com.revolgenx.weaverx.event

data class TorrentEngineEvent(var engineEventTypes: TorrentEngineEventTypes)
enum class TorrentEngineEventTypes {
    ENGINE_STARTING, ENGINE_STARTED, ENGINE_STOPPED, ENGINE_FAULT
}