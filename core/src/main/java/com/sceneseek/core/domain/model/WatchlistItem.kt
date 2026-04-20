package com.sceneseek.core.domain.model

data class WatchlistItem(
    val mediaId: Int,
    val mediaType: MediaType,
    val title: String,
    val posterPath: String?,
    val addedAt: Long,
)
