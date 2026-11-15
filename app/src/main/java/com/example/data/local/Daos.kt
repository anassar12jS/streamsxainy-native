package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC LIMIT 200")
    fun observeWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE tmdbId = :tmdbId AND mediaType = :mediaType)")
    fun observeIsInWatchlist(tmdbId: Int, mediaType: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE tmdbId = :tmdbId AND mediaType = :mediaType)")
    suspend fun isInWatchlist(tmdbId: Int, mediaType: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun delete(tmdbId: Int, mediaType: String)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress ORDER BY updatedAt DESC LIMIT 20")
    fun observeRecentProgress(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE tmdbId = :tmdbId AND mediaType = :mediaType ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatestForTitle(tmdbId: Int, mediaType: String): Flow<ProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: ProgressEntity)

    @Query("DELETE FROM progress WHERE tmdbId = :tmdbId AND mediaType = :mediaType")
    suspend fun deleteByTitle(tmdbId: Int, mediaType: String)

    @Query("DELETE FROM progress WHERE key NOT IN (SELECT key FROM progress ORDER BY updatedAt DESC LIMIT 100)")
    suspend fun trimOldProgress()
}
