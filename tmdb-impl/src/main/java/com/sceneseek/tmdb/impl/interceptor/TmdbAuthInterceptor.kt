package com.sceneseek.tmdb.impl.interceptor

import com.sceneseek.tmdb.impl.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TmdbAuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url.newBuilder()
            .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
            .build()
        val request = chain.request().newBuilder()
            .url(url)
            .build()
        return chain.proceed(request)
    }
}
