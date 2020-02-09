package com.revolgenx.lemillion.core.db.book

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.revolgenx.lemillion.core.book.Book
import com.revolgenx.lemillion.core.book.BookProtocol

@Entity(tableName = "book_table")
data class BookEntity(
    @PrimaryKey
    var book_id: Long,
    var book_protocol: BookProtocol,
    var has_error: Boolean,
    var error_msg: String
) {
    fun toBook(): Book = Book().apply {
        id = book_id
        bookProtocol = book_protocol
        hasError = has_error
        errorMsg = error_msg
    }
}