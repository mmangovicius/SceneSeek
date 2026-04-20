package com.sceneseek.tmdb.impl.config

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sceneseek.core.util.TmdbImageUrlBuilder
import com.sceneseek.tmdb.api.service.TmdbConfigService
import com.sceneseek.tmdb.impl.util.toResult
import com.sceneseek.core.domain.util.Result
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val configService: TmdbConfigService,
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val IMAGE_BASE_URL_KEY = stringPreferencesKey("image_base_url")
        const val DEFAULT_BASE_URL = "https://image.tmdb.org/t/p/"
    }

    suspend fun bootstrap() {
        val prefs = dataStore.data.first()
        val cached = prefs[IMAGE_BASE_URL_KEY]
        if (cached != null) {
            TmdbImageUrlBuilder.setBaseUrl(cached)
            return
        }
        when (val result = configService.getConfiguration().toResult()) {
            is Result.Success -> {
                val url = result.data.images.secureBaseUrl
                TmdbImageUrlBuilder.setBaseUrl(url)
                dataStore.edit { it[IMAGE_BASE_URL_KEY] = url }
            }
            else -> TmdbImageUrlBuilder.setBaseUrl(DEFAULT_BASE_URL)
        }
    }
}
