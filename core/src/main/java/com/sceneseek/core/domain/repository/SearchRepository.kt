package com.sceneseek.core.domain.repository

import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun search(query: String, page: Int = 1): Flow<Result<List<MediaItem>>>
}
