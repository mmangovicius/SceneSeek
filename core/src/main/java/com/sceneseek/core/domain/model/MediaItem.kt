package com.sceneseek.core.domain.model

sealed class MediaItem {
    data class MovieItem(val movie: Movie) : MediaItem()
    data class TvItem(val tvShow: TvShow) : MediaItem()
}
