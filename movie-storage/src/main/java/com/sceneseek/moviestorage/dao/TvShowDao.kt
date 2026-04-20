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

    @Query("SELECT * FROM tv_shows WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): TvShowEntity?

    @Query("SELECT * FROM tv_shows WHERE category = :category ORDER BY popularity DESC")
    fun getByCategory(category: String): Flow<List<TvShowEntity>>

    @Query("DELETE FROM tv_shows WHERE category = :category")
    suspend fun deleteByCategory(category: String)

    @Transaction
    suspend fun replaceByCategory(category: String, shows: List<TvShowEntity>) {
        deleteByCategory(category)
        insertAll(shows)
    }
}
