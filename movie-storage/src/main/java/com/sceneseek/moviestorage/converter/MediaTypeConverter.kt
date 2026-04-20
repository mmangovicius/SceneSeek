package com.sceneseek.moviestorage.converter

import androidx.room.TypeConverter
import com.sceneseek.core.domain.model.MediaType

class MediaTypeConverter {
    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.key

    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.fromKey(value)
}
