package com.revolgenx.weaverx.core.sorting.torrent

import com.revolgenx.weaverx.core.torrent.Torrent

class TorrentSortingComparator(private val sorting: TorrentSorting) : Comparator<Torrent> {
    override fun compare(state1: Torrent?, state2: Torrent?): Int {
        return TorrentSorting.SortingColumns.fromValue(sorting.columnName)
            .compare(state1!!, state2!!, sorting.direction)
    }
}