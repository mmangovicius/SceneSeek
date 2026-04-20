package com.sceneseek.tmdb.impl.di

import com.sceneseek.core.domain.repository.MovieRepository
import com.sceneseek.core.domain.repository.SearchRepository
import com.sceneseek.core.domain.repository.TvRepository
import com.sceneseek.core.domain.repository.WatchlistRepository
import com.sceneseek.tmdb.impl.repository.LocalWatchlistRepository
import com.sceneseek.tmdb.impl.repository.RemoteMovieRepository
import com.sceneseek.tmdb.impl.repository.RemoteSearchRepository
import com.sceneseek.tmdb.impl.repository.RemoteTvRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindWatchlistRepository(impl: LocalWatchlistRepository): WatchlistRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: RemoteSearchRepository): SearchRepository

    @Binds @Singleton
    abstract fun bindMovieRepository(impl: RemoteMovieRepository): MovieRepository

    @Binds @Singleton
    abstract fun bindTvRepository(impl: RemoteTvRepository): TvRepository
}
