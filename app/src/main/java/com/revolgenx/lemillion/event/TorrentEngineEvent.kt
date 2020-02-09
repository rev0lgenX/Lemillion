package com.revolgenx.lemillion.event

data class TorrentEngineEvent(var engineEventTypes: TorrentEngineEventTypes)
enum class TorrentEngineEventTypes {
    ENGINE_STARTING, ENGINE_STARTED, ENGINE_STOPPING,ENGINE_STOPPED, ENGINE_FAULT
}