package com.revolgenx.weaverx.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadTask
import com.revolgenx.weaverx.R
import com.revolgenx.weaverx.core.book.Book
import com.revolgenx.weaverx.core.preference.getSorting
import com.revolgenx.weaverx.core.service.ServiceConnector
import com.revolgenx.weaverx.core.service.isServiceRunning
import com.revolgenx.weaverx.core.sorting.BaseSorting
import com.revolgenx.weaverx.core.sorting.book.BookSorting
import com.revolgenx.weaverx.core.sorting.book.BookSortingComparator
import com.revolgenx.weaverx.core.util.Resource
import com.revolgenx.weaverx.core.util.postEvent
import com.revolgenx.weaverx.core.util.registerClass
import com.revolgenx.weaverx.core.util.unregisterClass
import com.revolgenx.weaverx.event.BookAddedEvent
import com.revolgenx.weaverx.event.BookEvent
import com.revolgenx.weaverx.event.BookEventType
import com.revolgenx.weaverx.event.BookRemovedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class BookViewModel(private val context: Context, private val connector: ServiceConnector) :
    ViewModel() {

    val bookResource = MutableLiveData<Resource<List<Book>>>()
    private val bookHashMap = mutableMapOf<Long, Book>()
    var sorting = context.resources.getStringArray(R.array.sort_array)[
            getSorting(context)].split(" ").let {
        BookSortingComparator(
            BookSorting(
                BookSorting.SortingColumns.fromValue(it[0]),
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

    fun getBooks() {
        viewModelScope.launch(Dispatchers.IO) {

            if (context.isServiceRunning()) {

                connector.connect { service, connected ->
                    if (connected) {

                        connector.serviceConnectionListener = null

                        viewModelScope.launch(Dispatchers.IO) {
                            Aria.download(this).taskList?.map {
                                bookHashMap[it.id] = Book().apply { entity = it }
                            }

                            service!!.bookHashMap.forEach {
                                bookHashMap[it.value.entity!!.id] = it.value
                            }

                            updateResource()
                            connector.disconnect()
                        }
                    }
                }
            } else {
                Aria.download(this).taskList?.map {
                    bookHashMap[it.id] = Book().apply { entity = it }
                }
                updateResource()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBookEvent(event: BookEvent) {
        if (context.isServiceRunning()) return

        if (event.bookEventType == BookEventType.BOOK_RESUMED) {
            connector.connect { service, connected ->
                connector.serviceConnectionListener = null
                if (connected) {
                    postEvent(event)
                }
                connector.disconnect()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBookAddedEvent(event: BookAddedEvent) {
        val task = Aria.download(this).load(event.id).entity
        bookHashMap[event.id] = Book().apply { entity = task }
        updateResource()
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBookRemovedEvent(event: BookRemovedEvent) {
        synchronized(bookHashMap) {
            val download = Aria.download(this)
            event.ids.forEach {
                bookHashMap.remove(it)
                download.load(it).cancel(event.withFiles)
            }
            updateResource()
        }
    }

    private fun updateResource() {
        bookResource.postValue(Resource.success(bookHashMap.values.sortedWith(sorting).toList()))
    }

    override fun onCleared() {
        bookHashMap.clear()
        unregisterClass(this)
        super.onCleared()
    }

    fun getBook(task: DownloadTask?): Book? {
        if (task == null) return null
        return bookHashMap[task.entity.id]?.also { it.entity = task.entity }
    }
}
