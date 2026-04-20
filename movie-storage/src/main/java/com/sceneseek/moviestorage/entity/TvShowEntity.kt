package com.sceneseek.moviestorage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tv_shows", indices = [Index(value = ["id"])])
data class TvShowEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo val name: String,
    @ColumnInfo val overview: String,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "backdrop_path") val backdropPath: String?,
    @ColumnInfo(name = "vote_average") val voteAverage: Double,
    @ColumnInfo(name = "first_air_date") val firstAirDate: String,
    @ColumnInfo val popularity: Double = 0.0,
    @ColumnInfo val category: String = "",
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)
