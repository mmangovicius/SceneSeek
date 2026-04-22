package com.sceneseek.tmdb.api.service

import com.sceneseek.tmdb.api.dto.CreditsResponse
import com.sceneseek.tmdb.api.dto.TvShowDto
import com.sceneseek.tmdb.api.dto.VideosResponse
import com.sceneseek.tmdb.api.model.PagedResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbTvService {
    @GET("tv/popular")
    suspend fun getPopular(@Query("page") page: Int = 1): Response<PagedResponse<TvShowDto>>

    @GET("tv/top_rated")
    suspend fun getTopRated(@Query("page") page: Int = 1): Response<PagedResponse<TvShowDto>>

    @GET("trending/tv/{time_window}")
    suspend fun getTrending(
        @Path("time_window") timeWindow: String = "day",
        @Query("page") page: Int = 1,
    ): Response<PagedResponse<TvShowDto>>

    @GET("tv/{series_id}")
    suspend fun getTvDetail(@Path("series_id") id: Int): Response<TvShowDto>

    @GET("tv/{series_id}/credits")
    suspend fun getCredits(@Path("series_id") id: Int): Response<CreditsResponse>

    @GET("tv/{series_id}/videos")
    suspend fun getVideos(@Path("series_id") id: Int): Response<VideosResponse>

    @GET("tv/{series_id}/similar")
    suspend fun getSimilar(
        @Path("series_id") id: Int,
        @Query("page") page: Int = 1,
    ): Response<PagedResponse<TvShowDto>>
}
