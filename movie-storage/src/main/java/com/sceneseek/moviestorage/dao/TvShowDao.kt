package com.sceneseek.moviestorage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceneseek.moviestorage.entity.TvShowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TvShowDao {
    @Query("SELECT * FROM tv_shows ORDER BY popularity DESC")
    fun getAll(): Flow<List<TvShowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shows: List<TvShowEntity>)

    @Query("DELETE FROM tv_shows")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(shows: List<TvShowEntity>) {
        deleteAll()
        insertAll(shows)
    }
}
