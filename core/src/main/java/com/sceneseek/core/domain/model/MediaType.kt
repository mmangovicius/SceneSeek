package com.sceneseek.core.domain.model

sealed class MediaType {
    object Movie : MediaType()
    object TvShow : MediaType()

    val key: String get() = when (this) {
        is Movie -> KEY_MOVIE
        is TvShow -> KEY_TV
    }

    companion object {
        const val KEY_MOVIE = "movie"
        const val KEY_TV = "tv"

        fun fromKey(key: String): MediaType = when (key) {
            KEY_MOVIE -> Movie
            KEY_TV -> TvShow
            else -> Movie
        }
    }
}
