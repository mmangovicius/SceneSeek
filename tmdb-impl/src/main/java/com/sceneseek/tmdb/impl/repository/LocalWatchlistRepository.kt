package com.sceneseek.tmdb.impl.repository

import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.WatchlistItem
import com.sceneseek.core.domain.repository.WatchlistRepository
import com.sceneseek.moviestorage.dao.WatchlistDao
import com.sceneseek.moviestorage.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalWatchlistRepository @Inject constructor(
    private val watchlistDao: WatchlistDao,
) : WatchlistRepository {

    override fun getAll(): Flow<List<WatchlistItem>> =
        watchlistDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun toggle(item: WatchlistItem) {
        val mediaTypeStr = when (item.mediaType) {
            is MediaType.Movie -> "movie"
            is MediaType.TvShow -> "tv"
        }
        val isWatched = watchlistDao.isWatchlisted(item.mediaId, mediaTypeStr).first()
        if (isWatched) {
            watchlistDao.deleteByMediaId(item.mediaId, mediaTypeStr)
        } else {
            watchlistDao.insert(item.toEntity())
        }
    }

    override fun isWatchlisted(mediaId: Int, type: MediaType): Flow<Boolean> {
        val mediaTypeStr = when (type) {
            is MediaType.Movie -> "movie"
            is MediaType.TvShow -> "tv"
        }
        return watchlistDao.isWatchlisted(mediaId, mediaTypeStr)
    }

    private fun WatchlistEntity.toDomain() = WatchlistItem(
        mediaId = mediaId,
        mediaType = if (mediaType == "movie") MediaType.Movie else MediaType.TvShow,
        title = title,
        posterPath = posterPath,
        addedAt = addedAt,
    )

    private fun WatchlistItem.toEntity() = WatchlistEntity(
        mediaId = mediaId,
        mediaType = when (mediaType) { is MediaType.Movie -> "movie"; is MediaType.TvShow -> "tv" },
        title = title,
        posterPath = posterPath,
        addedAt = addedAt,
    )
}
