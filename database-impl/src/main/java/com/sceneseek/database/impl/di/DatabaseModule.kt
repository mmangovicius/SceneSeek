package com.sceneseek.database.impl.di

import android.content.Context
import androidx.room.Room
import com.sceneseek.database.impl.AppDatabase
import com.sceneseek.moviestorage.dao.MovieDao
import com.sceneseek.moviestorage.dao.TvShowDao
import com.sceneseek.moviestorage.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "sceneseek.db")
            .build()

    @Provides
    @Singleton
    fun provideMovieDao(db: AppDatabase): MovieDao = db.movieDao()

    @Provides
    @Singleton
    fun provideTvShowDao(db: AppDatabase): TvShowDao = db.tvShowDao()

    @Provides
    @Singleton
    fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()
}
