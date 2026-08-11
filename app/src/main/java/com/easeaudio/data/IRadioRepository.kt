package com.easeaudio.data

import kotlinx.coroutines.flow.Flow

interface IRadioRepository {
    val defaultStations: List<RadioStation>

    fun getAllStations(): Flow<List<RadioStation>>
    
    suspend fun discoverOnlineStations(
        query: String = "",
        genre: String = "",
        country: String = "",
        countryCode: String = "",
        offset: Int = 0,
        limit: Int = 40
    ): List<RadioStation>

    suspend fun discoverOnlinePodcasts(
        query: String = "",
        genre: String = "",
        country: String = "",
        offset: Int = 0,
        limit: Int = 40
    ): List<RadioStation>

    suspend fun getiTunesLiveRadioStations(
        query: String = "",
        genre: String = "",
        country: String = ""
    ): List<RadioStation>

    fun getFavoriteStations(): Flow<List<RadioStation>>
    fun getRecentStations(): Flow<List<RadioStation>>

    suspend fun toggleFavorite(station: RadioStation)
    suspend fun recordStationListened(station: RadioStation)
    suspend fun addCustomStation(station: RadioStation)
    suspend fun deleteCustomStation(station: RadioStation)

    fun getRecentSearchQueries(limit: Int = 10): Flow<List<String>>
    suspend fun saveSearchQuery(query: String)
    suspend fun deleteSearchQuery(query: String)
    suspend fun clearSearchQueries()
}
