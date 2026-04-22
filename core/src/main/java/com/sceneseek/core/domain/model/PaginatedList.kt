package com.sceneseek.core.domain.model

data class PaginatedList<T>(
    val items: List<T> = emptyList(),
    val page: Int = 1,
    val canLoadMore: Boolean = true,
)
