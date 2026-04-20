package com.sceneseek.core.domain.model

data class TvShow(
    val id: Int,
    val name: String,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String,
    val voteAverage: Double,
    val firstAirDate: String,
)
