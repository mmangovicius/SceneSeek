package com.sceneseek.moviestorage.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sceneseek.moviestorage.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY added_at DESC")
    fun getAll(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE media_id = :mediaId AND media_type = :mediaType")
    suspend fun deleteByMediaId(mediaId: Int, mediaType: String)

    @Query("SELECT COUNT(*) > 0 FROM watchlist WHERE media_id = :mediaId AND media_type = :mediaType")
    fun isWatchlisted(mediaId: Int, mediaType: String): Flow<Boolean>
}
