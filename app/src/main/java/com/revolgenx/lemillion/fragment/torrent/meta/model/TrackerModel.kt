package com.revolgenx.lemillion.fragment.torrent.meta.model

data class TrackerModel(var name:String, var working: TrackerStatus, var message:String){
    override fun equals(other: Any?): Boolean {
        return if(other is TrackerModel){
            this.name == other.name && this.working == other.working && this.message == other.message
        }else false
    }
}

enum class TrackerStatus{
    WORKING, NOT_WORKING, UPDATING, NOT_CONTACTED, UNKNOWN
}