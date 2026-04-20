package com.sceneseek.tmdb.api.service

import com.sceneseek.tmdb.api.dto.SearchResultDto
import com.sceneseek.tmdb.api.model.PagedResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbSearchService {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
    ): Response<PagedResponse<SearchResultDto>>
}
