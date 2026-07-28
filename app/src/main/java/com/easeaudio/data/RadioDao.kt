package com.easeaudio.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RadioDao {
    @Query("SELECT * FROM radio_stations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteStations(): Flow<List<RadioStation>>

    @Query("SELECT * FROM radio_stations WHERE lastListenedTimestamp > 0 ORDER BY lastListenedTimestamp DESC LIMIT 20")
    fun getRecentStations(): Flow<List<RadioStation>>

    @Query("SELECT * FROM radio_stations WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustomStations(): Flow<List<RadioStation>>

    @Query("SELECT * FROM radio_stations")
    fun getAllStations(): Flow<List<RadioStation>>

    @Query("SELECT * FROM radio_stations WHERE id = :id LIMIT 1")
    suspend fun getStationById(id: String): RadioStation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStation(station: RadioStation)

    @Transaction
    suspend fun saveStationsToCache(stations: List<RadioStation>) {
        stations.forEach { station ->
            val existing = getStationById(station.id)
            if (existing != null) {
                val updated = station.copy(
                    isFavorite = existing.isFavorite,
                    isCustom = existing.isCustom,
                    lastListenedTimestamp = existing.lastListenedTimestamp
                )
                insertOrUpdateStation(updated)
            } else {
                insertOrUpdateStation(station)
            }
        }
    }

    @Delete
    suspend fun deleteStation(station: RadioStation)

    @Query("UPDATE radio_stations SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFav: Boolean)

    @Query("UPDATE radio_stations SET lastListenedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastListened(id: String, timestamp: Long)
}
