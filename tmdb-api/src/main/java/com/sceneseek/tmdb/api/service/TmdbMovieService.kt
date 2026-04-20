package com.sceneseek.tmdb.api.service

import com.sceneseek.tmdb.api.dto.CreditsResponse
import com.sceneseek.tmdb.api.dto.MovieDto
import com.sceneseek.tmdb.api.dto.VideosResponse
import com.sceneseek.tmdb.api.model.PagedResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbMovieService {
    @GET("movie/popular")
    suspend fun getPopular(@Query("page") page: Int = 1): Response<PagedResponse<MovieDto>>

    @GET("movie/top_rated")
    suspend fun getTopRated(@Query("page") page: Int = 1): Response<PagedResponse<MovieDto>>

    @GET("trending/movie/{time_window}")
    suspend fun getTrending(@Path("time_window") timeWindow: String = "day"): Response<PagedResponse<MovieDto>>

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(@Path("movie_id") id: Int): Response<MovieDto>

    @GET("movie/{movie_id}/credits")
    suspend fun getCredits(@Path("movie_id") id: Int): Response<CreditsResponse>

    @GET("movie/{movie_id}/videos")
    suspend fun getVideos(@Path("movie_id") id: Int): Response<VideosResponse>

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilar(
        @Path("movie_id") id: Int,
        @Query("page") page: Int = 1,
    ): Response<PagedResponse<MovieDto>>
}
