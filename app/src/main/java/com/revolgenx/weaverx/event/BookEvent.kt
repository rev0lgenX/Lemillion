package com.revolgenx.weaverx.event

import com.revolgenx.weaverx.core.book.Book

data class BookEvent(var book:Book, var bookEventType: BookEventType)

enum class BookEventType {
    BOOK_PAUSED, BOOK_RESUMED
}

