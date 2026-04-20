package com.sceneseek.tmdb.impl.di

import com.sceneseek.tmdb.api.service.TmdbConfigService
import com.sceneseek.tmdb.api.service.TmdbMovieService
import com.sceneseek.tmdb.api.service.TmdbSearchService
import com.sceneseek.tmdb.api.service.TmdbTvService
import com.sceneseek.tmdb.impl.BuildConfig
import com.sceneseek.tmdb.impl.interceptor.TmdbAuthInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: TmdbAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton
    fun provideTmdbMovieService(retrofit: Retrofit): TmdbMovieService =
        retrofit.create(TmdbMovieService::class.java)

    @Provides @Singleton
    fun provideTmdbTvService(retrofit: Retrofit): TmdbTvService =
        retrofit.create(TmdbTvService::class.java)

    @Provides @Singleton
    fun provideTmdbSearchService(retrofit: Retrofit): TmdbSearchService =
        retrofit.create(TmdbSearchService::class.java)

    @Provides @Singleton
    fun provideTmdbConfigService(retrofit: Retrofit): TmdbConfigService =
        retrofit.create(TmdbConfigService::class.java)
}
