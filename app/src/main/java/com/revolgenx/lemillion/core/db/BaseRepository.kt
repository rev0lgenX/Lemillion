package com.revolgenx.lemillion.core.db

import com.revolgenx.lemillion.core.util.Resource


interface BaseRepository<T> {
    suspend fun add(obj: T): Resource<Long>
    suspend fun remove(obj: T): Resource<Int>
    suspend fun removeAll(obj: List<T>): Resource<Int>
    suspend fun removeAllWithIds(obj: List<T>): Resource<Int>
    suspend fun update(obj: T): Resource<Int>
    suspend fun updateAll(obj: List<T>): Resource<Int>
    suspend fun getAll(): Resource<List<T>>
    suspend fun <E> getAllNotIn(obj: List<E>): Resource<List<T>>
}
