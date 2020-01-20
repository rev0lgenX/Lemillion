package com.revolgenx.weaverx.core.db.torrent

import com.revolgenx.weaverx.core.db.BaseRepository
import com.revolgenx.weaverx.core.torrent.Torrent
import com.revolgenx.weaverx.core.util.Resource
import timber.log.Timber
import java.lang.Exception

class TorrentRepository(private val torrentDao: TorrentDao) : BaseRepository<Torrent> {
    override suspend fun add(obj: Torrent): Resource<Boolean> {
        return try {
            torrentDao.insert(obj.toEntity())
            Resource.success(true)
        } catch (e: Exception) {
            Timber.e(e)
            Resource.success(false)
        }
    }

    override suspend fun remove(obj: Torrent): Resource<Boolean> {
        return try {
            torrentDao.delete(obj.toEntity())
            Resource.success(true)

        } catch (e: Exception) {
            Timber.e(e)
            Resource.success(false)
        }
    }

    override suspend fun update(obj: Torrent): Resource<Boolean> {
        return try {
            torrentDao.update(obj.toEntity())
            Resource.success(true)
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, null)
        }
    }

    override suspend fun getAll(): Resource<List<Torrent>> {
        return try {
            Resource.success(torrentDao.selectAll().map { it.toTorrent() })
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, null)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <E> getAllNotIn(obj: List<E>): Resource<List<Torrent>> {
        return try {
            Resource.success(torrentDao.selectAllNotIn(obj as List<String>).map { it.toTorrent() })
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, null)
        }
    }

    override suspend fun removeAll(obj: List<Torrent>): Resource<Boolean> {
        return try {
            Resource.success(torrentDao.deleteAll(obj.map { it.toEntity() }))
            Resource.success(true)
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, null)
        }
    }

    override suspend fun removeAllWithIds(obj: List<Torrent>): Resource<Boolean> {
        return try {
            Resource.success(torrentDao.deleteAllWithIds(obj.map { it.hash }))
            Resource.success(true)
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, null)
        }
    }

    override suspend fun updateAll(obj: List<Torrent>): Resource<Boolean> {
        return try {
            Resource.success(torrentDao.updateAll(obj.map { it.toEntity() }))
            Resource.success(true)
        } catch (e: Exception) {
            Timber.e(e)
            Resource.error(e.message!!, null)
        }
    }

}