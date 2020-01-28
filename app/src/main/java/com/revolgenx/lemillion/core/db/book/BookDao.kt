package com.revolgenx.lemillion.core.db.book

import androidx.room.*

@Dao
interface BookDao {
    @Insert
    suspend fun insert(entity: BookEntity): Long

    @Insert
    suspend fun insertAll(entities: List<BookEntity>): List<Long>

    @Update
    suspend fun update(entity: BookEntity): Int

    @Update
    suspend fun updateAll(entities: List<BookEntity>): Int

    @Delete
    suspend fun delete(entity: BookEntity): Int

    @Delete
    suspend fun deleteAll(entities: List<BookEntity>): Int

    @Query("SELECT * FROM book_table")
    suspend fun selectAll(): List<BookEntity>

    @Query("SELECT * FROM book_table WHERE book_id NOT IN (:ids)")
    suspend fun selectAllNotIn(ids: List<Long>): List<BookEntity>

    @Query("DELETE FROM book_table WHERE book_id IN (:ids)")
    suspend fun deleteAllWithIds(ids: List<Long>): Int

}