package com.revolgenx.lemillion.viewmodel

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.db.torrent.TorrentRepository
import com.revolgenx.lemillion.core.preference.getSorting
import com.revolgenx.lemillion.core.service.ServiceConnector
import com.revolgenx.lemillion.core.service.isServiceRunning
import com.revolgenx.lemillion.core.sorting.BaseSorting
import com.revolgenx.lemillion.core.sorting.torrent.TorrentSorting
import com.revolgenx.lemillion.core.sorting.torrent.TorrentSortingComparator
import com.revolgenx.lemillion.core.torrent.Torrent
import com.revolgenx.lemillion.core.torrent.TorrentActiveState
import com.revolgenx.lemillion.core.torrent.TorrentEngine
import com.revolgenx.lemillion.core.util.*
import com.revolgenx.lemillion.event.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.lang.Exception

class TorrentViewModel(
    private val context: Context,
    private val torrentRepository: TorrentRepository,
    private val engine: TorrentEngine,
    private val connector: ServiceConnector,
    private val torrentActiveState: TorrentActiveState
) : ViewModel() {

    private val torrentHashMap = mutableMapOf<String, Torrent>()
    private val hd: Handler

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
        hd = Handler(Looper.getMainLooper())
        torrentActiveState.fragmentActive = true
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTorrentAddedEvent(event: TorrentAddedEvent) {
        when (event.type) {
            TorrentAddedEventTypes.TORRENT_ADDED -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val torrent = event.torrent
                    val resource = torrentRepository.add(torrent)

                    if (resource.status == Status.SUCCESS) {
                        if (torrent.isPaused()) {
                            try {
                                torrent.resume()
                            } catch (e: Exception) {
                            }
                        }
                        torrentHashMap[torrent.hash] = torrent
                        updateResource()
                    }
                    postEvent(TorrentEvent(listOf(torrent), TorrentEventType.TORRENT_RESUMED))
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
            val torrents =
                event.hashes.filter { torrentHashMap[it] != null }.map { torrentHashMap[it]!! }
            val resource = torrentRepository.removeAllWithIds(torrents)

            if (resource.status == Status.SUCCESS) {
                torrents.forEach { torrent ->
                    torrent.remove(event.withFiles)
                    torrent.removeAllListener()
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
        if (event.torrents.isEmpty()) return

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


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateDatabase(event: UpdateDataBase) {
        if (context.isServiceRunning()) return

        viewModelScope.launch(Dispatchers.IO) {
            val data = event.data
            if (data is Torrent) {
                torrentRepository.update(data)
            }
        }
    }


    fun resumeAll() {
        if (torrentHashMap.isEmpty()) return

        torrentHashMap.values.forEach {
            try {
                it.resume(true)
                it.update()
            } catch (e: Exception) {

            }
        }

        connector.connect { service, connected ->
            connector.serviceConnectionListener = null
            if (connected) {
                postEvent(
                    TorrentEvent(
                        torrentHashMap.values.filter { !it.isPaused() }.toList(),
                        TorrentEventType.TORRENT_RESUMED
                    )
                )
            }
            connector.disconnect()
        }
    }

    fun pauseAll() {
        if (torrentHashMap.isEmpty()) return

        torrentHashMap.values.forEach {
            try {
                it.pause(true)
                it.update()
            } catch (e: Exception) {

            }
        }

        connector.connect { service, connected ->
            connector.serviceConnectionListener = null
            if (connected) {
                postEvent(
                    TorrentEvent(
                        torrentHashMap.values.filter { it.isPaused() }.toList(),
                        TorrentEventType.TORRENT_PAUSED
                    )
                )
            }
            connector.disconnect()
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
                                torrentRepository.getAllNotIn(torrentHashMap.values.pmap{ it.hash })

                            resource.data!!.forEach { torrent ->
                                torrentHashMap[torrent.hash] = torrent
                            }

//                            resource.data.filter { !it.isPaused() }.let {
//                                    postEvent(TorrentEvent(it, TorrentEventType.TORRENT_RESUMED))
//                                }

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
                    updateResource()
                    Timber.d("service not running")
                } else {
                    torrentResource.postValue(resource)
                }
            }
        }
    }


    fun recheckTorrents(selectedHashes: List<String>) {
        val torrents = selectedHashes.mapNotNull { torrentHashMap[it] }.toList()
        torrents.forEach {
            it.forceRecheck()
            it.update()
        }

        connector.connect { service, connected ->
            connector.serviceConnectionListener = null
            if (connected) {
                postEvent(
                    TorrentEvent(
                        torrents,
                        TorrentEventType.TORRENT_RESUMED
                    )
                )
            }

            connector.disconnect()
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
        torrentActiveState.fragmentActive = false
        super.onCleared()
    }

    fun getTorrent(toHex: String?): Torrent? {
        return torrentHashMap[toHex]
    }

    fun removeAllTorrentEngineListener() {
        torrentHashMap.forEach { u ->
            u.value.removeEngineListener()
        }
    }

}