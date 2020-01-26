package com.revolgenx.lemillion.core.sorting.book

import com.revolgenx.lemillion.core.book.Book

class BookSortingComparator(private val sorting: BookSorting) : Comparator<Book> {
    override fun compare(state1: Book?, state2: Book?): Int {
        return BookSorting.SortingColumns.fromValue(sorting.columnName)
            .compare(state1!!, state2!!, sorting.direction)
    }
}