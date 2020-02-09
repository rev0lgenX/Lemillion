package com.revolgenx.lemillion.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadTask
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.book.Book
import com.revolgenx.lemillion.core.db.book.BookRepository
import com.revolgenx.lemillion.core.preference.getSorting
import com.revolgenx.lemillion.core.service.ServiceConnector
import com.revolgenx.lemillion.core.service.isServiceRunning
import com.revolgenx.lemillion.core.sorting.BaseSorting
import com.revolgenx.lemillion.core.sorting.book.BookSorting
import com.revolgenx.lemillion.core.sorting.book.BookSortingComparator
import com.revolgenx.lemillion.core.util.*
import com.revolgenx.lemillion.event.BookAddedEvent
import com.revolgenx.lemillion.event.BookEvent
import com.revolgenx.lemillion.event.BookEventType
import com.revolgenx.lemillion.event.BookRemovedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class BookViewModel(
    private val context: Context,
    private val connector: ServiceConnector,
    private val bookRepository: BookRepository
) : ViewModel() {
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
            bookRepository.getAll().takeIf { it.status == Status.SUCCESS }?.data!!.forEach {
                bookHashMap[it.id] = it
            }

            Aria.download(this).taskList?.forEach { entity ->
                bookHashMap[entity.id]?.let { book ->
                    book.entity = entity
                }
            }

            if (context.isServiceRunning()) {
                connector.connect { service, connected ->
                    if (connected) {
                        connector.serviceConnectionListener = null
                        viewModelScope.launch(Dispatchers.IO) {
                            service!!.bookHashMap.forEach {
                                bookHashMap[it.value.entity!!.id] = it.value
                            }
                            updateResource()
                            connector.disconnect()
                        }
                    }
                }
            } else {
                updateResource()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBookEvent(event: BookEvent) {
        if (context.isServiceRunning()) return

        if (event.bookEventType == BookEventType.BOOK_RESUMED || event.bookEventType == BookEventType.BOOK_RESTART) {
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
        viewModelScope.launch(Dispatchers.IO) {
            val book = event.book
            bookRepository.add(book)
            bookHashMap[book.id] = book
            book.resume()
            postEvent(BookEvent(listOf(book), BookEventType.BOOK_RESUMED))
            updateResource()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBookRemovedEvent(event: BookRemovedEvent) {
        synchronized(bookHashMap) {
            viewModelScope.launch {
                event.ids.forEach { id ->
                    bookHashMap[id]?.let {
                        it.remove(event.withFiles)
                        bookRepository.remove(it)
                        bookHashMap.remove(id)
                    }
                }
                updateResource()
            }
        }
    }


    fun resumeAll() {
        if(bookHashMap.isEmpty()) return
        Aria.download(this).resumeAllTask()
        connector.connect { service, connected ->
            connector.serviceConnectionListener = null
            if (connected) {
                postEvent(BookEvent(bookHashMap.values.toList(), BookEventType.BOOK_RESUMED))
            }
            connector.disconnect()
        }
    }

    fun pauseAll() {
        if(bookHashMap.isEmpty()) return
        Aria.download(this).stopAllTask()
        postEvent(BookEvent(bookHashMap.values.toList(), BookEventType.BOOK_PAUSED))
    }

    private fun updateResource() {
        bookResource.postValue(Resource.success(bookHashMap.values.sortedWith(sorting).toList()))
    }

    fun getBook(task: DownloadTask?): Book? {
        if (task == null) return null
        return bookHashMap[task.entity.id]?.also { it.entity = task.entity }
    }

    override fun onCleared() {
        bookHashMap.values.forEach { it.unregister() }
        bookHashMap.clear()
        unregisterClass(this)
        super.onCleared()
    }

}
