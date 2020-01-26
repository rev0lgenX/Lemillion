package com.revolgenx.lemillion.core.sorting.book

import com.revolgenx.lemillion.core.book.Book
import com.revolgenx.lemillion.core.sorting.BaseSorting


class BookSorting(columnName: SortingColumns, direction: Direction) :
    BaseSorting(columnName.name, direction) {

    enum class SortingColumns : SortingColumnsInterface<Book> {
        NAME {
            override fun compare(
                item1: Book,
                item2: Book, direction: Direction
            ): Int {
                return if (direction === Direction.ASC)
                    item1.name.compareTo(item2.name)
                else
                    item2.name.compareTo(item1.name)
            }
        },
        SIZE {
            override fun compare(
                item1: Book,
                item2: Book, direction: Direction
            ): Int {
                return if (direction === Direction.ASC)
                    item2.totalSize.compareTo(item1.totalSize)
                else
                    item1.totalSize.compareTo(item2.totalSize)
            }
        },
        PROGRESS {
            override fun compare(
                item1: Book,
                item2: Book, direction: Direction
            ): Int {
                return if (direction === Direction.ASC)
                    item2.progress.compareTo(item1.progress)
                else
                    item1.progress.compareTo(item2.progress)
            }
        },
        DATE {
            override fun compare(
                item1: Book,
                item2: Book, direction: Direction
            ): Int {
                return if (direction === Direction.ASC) {
                    item2.stopTime.compareTo(item1.stopTime)
                } else {
                    item1.stopTime.compareTo(item2.stopTime)
                }
            }
        };


        companion object {
            fun fromValue(value: String): SortingColumns {
                for (column in SortingColumns::class.java.enumConstants!!) {
                    if (column.toString().equals(value, ignoreCase = true)) {
                        return column
                    }
                }
                return NAME
            }
        }
    }
}