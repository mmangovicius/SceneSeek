package com.sceneseek.tmdb.impl.repository

import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.WatchlistItem
import com.sceneseek.core.domain.repository.WatchlistRepository
import com.sceneseek.moviestorage.dao.WatchlistDao
import com.sceneseek.core.di.DispatcherProvider
import com.sceneseek.moviestorage.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalWatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val dispatchers: DispatcherProvider,
) : WatchlistRepository {

    override fun getAll(): Flow<List<WatchlistItem>> =
        watchlistDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun toggle(item: WatchlistItem) {
        withContext(dispatchers.io) {
            val mediaTypeStr = item.mediaType.key
            val isWatched = watchlistDao.isWatchlisted(item.mediaId, mediaTypeStr).first()
            if (isWatched) {
                watchlistDao.deleteByMediaId(item.mediaId, mediaTypeStr)
            } else {
                watchlistDao.insert(item.toEntity())
            }
        }
    }

    override fun isWatchlisted(mediaId: Int, type: MediaType): Flow<Boolean> =
        watchlistDao.isWatchlisted(mediaId, type.key)

    private fun WatchlistEntity.toDomain() = WatchlistItem(
        mediaId = mediaId,
        mediaType = MediaType.fromKey(mediaType),
        title = title,
        posterPath = posterPath,
        addedAt = addedAt,
    )

    private fun WatchlistItem.toEntity() = WatchlistEntity(
        mediaId = mediaId,
        mediaType = mediaType.key,
        title = title,
        posterPath = posterPath,
        addedAt = addedAt,
    )
}
