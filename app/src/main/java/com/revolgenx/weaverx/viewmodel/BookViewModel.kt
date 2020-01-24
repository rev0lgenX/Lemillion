package com.revolgenx.weaverx.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arialyy.aria.core.Aria
import com.arialyy.aria.core.task.DownloadTask
import com.revolgenx.weaverx.core.book.Book
import com.revolgenx.weaverx.core.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BookViewModel() : ViewModel() {

    val bookResource = MutableLiveData<Resource<List<Book>>>()
    private val bookHashMap = mutableMapOf<Long, Book>()
    fun getBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            Aria.download(this).taskList?.map {
                bookHashMap[it.id] = Book().apply { entity = it }
            }
            bookResource.postValue(Resource.success(bookHashMap.values.toList()))
        }
    }

    override fun onCleared() {
        bookHashMap.clear()
        super.onCleared()
    }

    fun getBook(task: DownloadTask?): Book? {
        if (task == null) return null
        return bookHashMap[task.entity.id]
    }
}
