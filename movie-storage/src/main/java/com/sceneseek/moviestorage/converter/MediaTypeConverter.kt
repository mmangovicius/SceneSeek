package com.sceneseek.moviestorage.converter

import androidx.room.TypeConverter
import com.sceneseek.core.domain.model.MediaType

class MediaTypeConverter {
    @TypeConverter
    fun fromMediaType(value: MediaType): String = when (value) {
        is MediaType.Movie -> "movie"
        is MediaType.TvShow -> "tv"
    }

    @TypeConverter
    fun toMediaType(value: String): MediaType = when (value) {
        "movie" -> MediaType.Movie
        else -> MediaType.TvShow
    }
}
