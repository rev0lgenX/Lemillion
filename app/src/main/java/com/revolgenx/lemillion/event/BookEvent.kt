package com.revolgenx.lemillion.event

import com.revolgenx.lemillion.core.book.Book

data class BookEvent(var books:List<Book>, var bookEventType: BookEventType)

enum class BookEventType {
    BOOK_PAUSED, BOOK_RESUMED
}

