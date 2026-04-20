package com.sceneseek.tmdb.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TrailerDto(
    @Json(name = "key") val key: String,
    @Json(name = "name") val name: String,
    @Json(name = "site") val site: String,
    @Json(name = "type") val type: String,
)

@JsonClass(generateAdapter = true)
data class VideosResponse(
    @Json(name = "id") val id: Int,
    @Json(name = "results") val results: List<TrailerDto>,
)
