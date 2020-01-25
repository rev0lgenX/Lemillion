package com.revolgenx.weaverx.core.sorting.torrent

import com.revolgenx.weaverx.core.sorting.BaseSorting
import com.revolgenx.weaverx.core.torrent.Torrent


class TorrentSorting(columnName: SortingColumns, direction: Direction) :
    BaseSorting(columnName.name, direction) {

    enum class SortingColumns : SortingColumnsInterface<Torrent> {
        NAME {
            override fun compare(
                item1: Torrent,
                item2: Torrent, direction: Direction
            ): Int {
                return if (direction === Direction.ASC)
                    item1.name.compareTo(item2.name)
                else
                    item2.name.compareTo(item1.name)
            }
        },
        SIZE {
            override fun compare(
                item1: Torrent,
                item2: Torrent, direction: Direction
            ): Int {
                return if (direction === Direction.ASC)
                    item2.totalSize.compareTo(item1.totalSize)
                else
                    item1.totalSize.compareTo(item2.totalSize)
            }
        },
        PROGRESS {
            override fun compare(
                item1: Torrent,
                item2: Torrent, direction: Direction
            ): Int {
                return if (direction === Direction.ASC)
                    item2.progress.compareTo(item1.progress)
                else
                    item1.progress.compareTo(item2.progress)
            }
        },
        DATE {
            override fun compare(
                item1: Torrent,
                item2: Torrent, direction: Direction
            ): Int {
                return if (direction === Direction.ASC) {
                    item2.createDate.time.compareTo(item1.createDate.time)
                } else {
                    item1.createDate.time.compareTo(item2.createDate.time)
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