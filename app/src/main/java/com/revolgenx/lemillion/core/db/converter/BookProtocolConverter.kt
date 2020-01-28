package com.revolgenx.lemillion.core.db.converter

import androidx.room.TypeConverter
import com.revolgenx.lemillion.core.book.BookProtocol

class BookProtocolConverter {
    @TypeConverter
    fun fromProtocolTo(bookProtocol: BookProtocol): Int = bookProtocol.ordinal

    @TypeConverter
    fun fromdbToProtocol(prot: Int) = BookProtocol.values()[prot]
}