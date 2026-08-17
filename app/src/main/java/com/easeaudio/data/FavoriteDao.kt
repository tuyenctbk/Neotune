package com.easeaudio.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_stations ORDER BY addedTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteStation>>

    @Query("SELECT * FROM favorite_stations ORDER BY addedTimestamp DESC")
    suspend fun getAllFavoritesDirect(): List<FavoriteStation>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stations WHERE id = :stationId)")
    fun isFavorite(stationId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stations WHERE id = :stationId)")
    suspend fun isFavoriteDirect(stationId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteStation)

    @Query("DELETE FROM favorite_stations WHERE id = :stationId")
    suspend fun deleteFavoriteById(stationId: String)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteStation)

    @Query("DELETE FROM favorite_stations")
    suspend fun clearAllFavorites()
}
