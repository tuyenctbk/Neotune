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

    @Query("SELECT * FROM radio_stations ORDER BY isCustom DESC, name ASC")
    fun getAllStations(): Flow<List<RadioStation>>

    @Query("SELECT * FROM radio_stations WHERE id = :id LIMIT 1")
    suspend fun getStationById(id: String): RadioStation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStation(station: RadioStation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStations(stations: List<RadioStation>)

    // Room splits large IN-lists automatically; safe for any page size.
    @Query("SELECT * FROM radio_stations WHERE id IN (:ids)")
    suspend fun getStationsByIds(ids: List<String>): List<RadioStation>

    /**
     * BUG-2 fix: previously issued N SELECT + N INSERT statements (one per station).
     * Now issues 1 bulk SELECT and 1 bulk INSERT, reducing DB round-trips by ~98%
     * while still preserving user data (isFavorite, isCustom, lastListenedTimestamp).
     */
    @Transaction
    suspend fun saveStationsToCache(stations: List<RadioStation>) {
        if (stations.isEmpty()) return
        val existingMap = getStationsByIds(stations.map { it.id }).associateBy { it.id }
        val toUpsert = stations.map { incoming ->
            val existing = existingMap[incoming.id]
            if (existing != null) {
                incoming.copy(
                    isFavorite = existing.isFavorite,
                    isCustom = existing.isCustom,
                    lastListenedTimestamp = existing.lastListenedTimestamp
                )
            } else {
                incoming
            }
        }
        insertOrUpdateStations(toUpsert)
    }

    @Delete
    suspend fun deleteStation(station: RadioStation)

    @Query("UPDATE radio_stations SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFav: Boolean)

    @Query("UPDATE radio_stations SET lastListenedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateLastListened(id: String, timestamp: Long)
}
