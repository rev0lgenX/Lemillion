package com.revolgenx.lemillion.core.db.book

import com.revolgenx.lemillion.core.book.Book
import com.revolgenx.lemillion.core.db.BaseRepository
import com.revolgenx.lemillion.core.util.Resource
import timber.log.Timber
import java.lang.Exception

class BookRepository(private val bookDao: BookDao) : BaseRepository<Book> {

    override suspend fun add(obj: Book): Resource<Long> {
        return try {
            Resource.success(bookDao.insert(obj.toEntity()))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", -1)
        }
    }

    override suspend fun remove(obj: Book): Resource<Int> {
        return try {
            Resource.success(bookDao.delete(obj.toEntity()))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", -1)
        }
    }

    override suspend fun removeAll(obj: List<Book>): Resource<Int> {
        return try {
            Resource.success(bookDao.deleteAll(obj.map { it.toEntity() }))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", -1)
        }
    }

    override suspend fun update(obj: Book): Resource<Int> {
        return try {
            Resource.success(bookDao.update(obj.toEntity()))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", -1)
        }
    }

    override suspend fun updateAll(obj: List<Book>): Resource<Int> {
        return try {
            Resource.success(bookDao.updateAll(obj.map { it.toEntity() }))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", -1)
        }

    }

    override suspend fun getAll(): Resource<List<Book>> {
        return try {
            Resource.success(bookDao.selectAll().map { it.toBook() })
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", null)
        }
    }

    override suspend fun removeAllWithIds(obj: List<Book>): Resource<Int> {
        return try {
            Resource.success(bookDao.deleteAllWithIds(obj.map { it.id }))
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <E> getAllNotIn(obj: List<E>): Resource<List<Book>> {
        return try {
            Resource.success(bookDao.selectAllNotIn((obj as List<Long>)).map { it.toBook() })
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message ?: "error", null)
        }
    }

}