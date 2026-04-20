package com.sceneseek.core.domain.repository

import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun getAll(): Flow<List<WatchlistItem>>
    suspend fun toggle(item: WatchlistItem)
    fun isWatchlisted(mediaId: Int, type: MediaType): Flow<Boolean>
}
