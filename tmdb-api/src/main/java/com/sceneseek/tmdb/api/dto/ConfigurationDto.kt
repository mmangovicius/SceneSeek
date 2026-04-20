package com.sceneseek.tmdb.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConfigurationDto(
    @Json(name = "images") val images: ImageConfigurationDto,
)

@JsonClass(generateAdapter = true)
data class ImageConfigurationDto(
    @Json(name = "base_url") val baseUrl: String,
    @Json(name = "secure_base_url") val secureBaseUrl: String,
    @Json(name = "poster_sizes") val posterSizes: List<String>,
    @Json(name = "backdrop_sizes") val backdropSizes: List<String>,
    @Json(name = "profile_sizes") val profileSizes: List<String>,
)
