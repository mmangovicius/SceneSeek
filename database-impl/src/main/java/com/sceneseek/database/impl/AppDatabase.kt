package com.sceneseek.database.impl

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sceneseek.moviestorage.converter.DateConverter
import com.sceneseek.moviestorage.converter.MediaTypeConverter
import com.sceneseek.moviestorage.dao.MovieDao
import com.sceneseek.moviestorage.dao.TvShowDao
import com.sceneseek.moviestorage.dao.WatchlistDao
import com.sceneseek.moviestorage.entity.MovieEntity
import com.sceneseek.moviestorage.entity.TvShowEntity
import com.sceneseek.moviestorage.entity.WatchlistEntity

@Database(
    entities = [MovieEntity::class, TvShowEntity::class, WatchlistEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DateConverter::class, MediaTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun tvShowDao(): TvShowDao
    abstract fun watchlistDao(): WatchlistDao
}
