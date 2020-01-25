package com.revolgenx.weaverx.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.revolgenx.weaverx.R
import com.revolgenx.weaverx.core.db.torrent.TorrentRepository
import com.revolgenx.weaverx.core.preference.getSorting
import com.revolgenx.weaverx.core.service.ServiceConnector
import com.revolgenx.weaverx.core.service.isServiceRunning
import com.revolgenx.weaverx.core.sorting.BaseSorting
import com.revolgenx.weaverx.core.sorting.torrent.TorrentSorting
import com.revolgenx.weaverx.core.sorting.torrent.TorrentSortingComparator
import com.revolgenx.weaverx.core.torrent.Torrent
import com.revolgenx.weaverx.core.torrent.TorrentEngine
import com.revolgenx.weaverx.core.util.*
import com.revolgenx.weaverx.event.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

class TorrentViewModel(
    private val context: Context,
    private val torrentRepository: TorrentRepository,
    private val engine: TorrentEngine,
    private val connector: ServiceConnector
) : ViewModel() {

    private val torrentHashMap = mutableMapOf<String, Torrent>()

    val torrentResource = MutableLiveData<Resource<List<Torrent>>>()
    var sorting =
        context.resources.getStringArray(R.array.sort_array)[getSorting(context)].split(" ").let {
            TorrentSortingComparator(
                TorrentSorting(
                    TorrentSorting.SortingColumns.fromValue(it[0]),
                    BaseSorting.Direction.fromValue(it[1])
                )
            )
        }
        set(value) {
            field = value
            updateResource()
        }

    init {
        registerClass(this)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTorrentAddedEvent(event: TorrentAddedEvent) {
        when (event.type) {
            TorrentAddedEventTypes.MAGNET_ADDED -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val torrent = Torrent().apply {
                        hash = event.hash
                        path = event.path
                        handle = event.handle
                    }

                    val resource = torrentRepository.add(torrent)

                    //TODO SERVICES
                    if (resource.status == Status.SUCCESS) {
                        torrentHashMap[torrent.hash] = torrent
                        updateResource()
                    }
                }

            }
            TorrentAddedEventTypes.MAGNET_ADD_ERROR -> {
                context.makeToast(context.getString(R.string.unable_to_add_magnet))
            }

            TorrentAddedEventTypes.TORRENT_ADDED -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val torrent = Torrent().apply {
                        hash = event.hash
                        path = event.path
                        handle = event.handle
                    }

                    val resource = torrentRepository.add(torrent)

                    if (resource.status == Status.SUCCESS) {
                        torrentHashMap[torrent.hash] = torrent
                        updateResource()
                    }
                }
            }
            TorrentAddedEventTypes.TORRENT_ADD_ERROR -> {
                context.makeToast(context.getString(R.string.unable_to_add_torrent_file))
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTorrentRemovedEvent(event: TorrentRemovedEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            val torrents = event.hashes.map { torrentHashMap[it]!! }
            val resource = torrentRepository.removeAllWithIds(torrents)

            if (resource.status == Status.SUCCESS) {
                torrents.forEach { torrent ->
                    torrent.torrentProgressListener = null
                    torrent.remove(event.withFiles)
                    torrentHashMap.remove(torrent.hash)
                }
                updateResource()
            } else {
                launch(Dispatchers.Main) {
                    context.makeToast(context.getString(R.string.failed_to_remove_torrent))
                }
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun torrentEvent(event: TorrentEvent) {
        if (context.isServiceRunning()) return
        when (event.type) {
            TorrentEventType.TORRENT_RESUMED -> {
                connector.connect { service, connected ->
                    connector.serviceConnectionListener = null
                    if (connected) {
                        postEvent(event)
                    }
                    connector.disconnect()
                }
            }
        }
    }


    fun getAllTorrents() {
        viewModelScope.launch(Dispatchers.IO) {
            if (torrentHashMap.isNotEmpty()) {
                return@launch
            } else {
                torrentResource.postValue(Resource.loading(null))
            }

            if (context.isServiceRunning()) {
                connector.connect { service, connected ->
                    if (connected) {

                        connector.serviceConnectionListener = null

                        service!!.torrentHashMap.forEach {
                            torrentHashMap[it.value.hash] = it.value
                        }
                        viewModelScope.launch(Dispatchers.IO) {
                            val resource =
                                torrentRepository.getAllNotIn(service.torrentHashMap.values.map { it.hash })
                            resource.data!!.forEach { torrent ->
                                torrentHashMap[torrent.hash] = torrent
                            }
                            updateResource()
                        }
                        connector.disconnect()
                    }
                }
                Timber.d("service running")
            } else {
                val resource = torrentRepository.getAll()
                if (resource.status == Status.SUCCESS) {
                    resource.data!!.forEach { torrent -> torrentHashMap[torrent.hash] = torrent }
                    torrentResource.postValue(resource)
                    Timber.d("service not running")
                } else {
                    torrentResource.postValue(resource)
                }
            }
        }
    }

    private fun updateResource() {
        torrentResource.postValue(
            Resource.success(torrentHashMap.values.sortedWith(sorting).toMutableList())
        )
    }


    override fun onCleared() {
        unregisterClass(this)
        torrentHashMap.clear()
        connector.disconnect()
        super.onCleared()
    }
}