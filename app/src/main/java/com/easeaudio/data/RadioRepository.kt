package com.easeaudio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RadioRepository(private val dao: RadioDao) {

    val defaultStations = emptyList<RadioStation>()

    fun getAllStations(): Flow<List<RadioStation>> {
        return dao.getAllStations()
    }

    suspend fun discoverOnlineStations(
        query: String = "",
        genre: String = "",
        country: String = "",
        countryCode: String = "",
        offset: Int = 0,
        limit: Int = 40
    ): List<RadioStation> {
        val onlineList = RadioBrowserService.fetchTopStations(
            limit = limit,
            offset = offset,
            searchQuery = query,
            genreTag = genre,
            country = country,
            countryCode = countryCode
        )
        if (onlineList.isNotEmpty()) {
            dao.saveStationsToCache(onlineList)
        }
        return onlineList
    }

    fun getFavoriteStations(): Flow<List<RadioStation>> = dao.getFavoriteStations()
    fun getRecentStations(): Flow<List<RadioStation>> = dao.getRecentStations()

    suspend fun toggleFavorite(station: RadioStation) {
        val newFav = !station.isFavorite
        val updated = station.copy(isFavorite = newFav)
        dao.insertOrUpdateStation(updated)
    }

    suspend fun recordStationListened(station: RadioStation) {
        val updated = station.copy(lastListenedTimestamp = System.currentTimeMillis())
        dao.insertOrUpdateStation(updated)
    }

    suspend fun addCustomStation(station: RadioStation) {
        dao.insertOrUpdateStation(station)
    }

    suspend fun deleteCustomStation(station: RadioStation) {
        dao.deleteStation(station)
    }
}
