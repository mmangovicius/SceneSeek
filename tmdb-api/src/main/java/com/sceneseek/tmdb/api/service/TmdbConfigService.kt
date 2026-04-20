package com.sceneseek.tmdb.api.service

import com.sceneseek.tmdb.api.dto.ConfigurationDto
import retrofit2.Response
import retrofit2.http.GET

interface TmdbConfigService {
    @GET("configuration")
    suspend fun getConfiguration(): Response<ConfigurationDto>
}
