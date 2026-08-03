package com.easeaudio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RadioRepository(private val dao: RadioDao) : IRadioRepository {

    override val defaultStations = emptyList<RadioStation>()

    override fun getAllStations(): Flow<List<RadioStation>> {
        return dao.getAllStations()
    }

    override suspend fun discoverOnlineStations(
        query: String,
        genre: String,
        country: String,
        countryCode: String,
        offset: Int,
        limit: Int
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

    override suspend fun discoverOnlinePodcasts(
        query: String,
        genre: String,
        country: String,
        offset: Int,
        limit: Int
    ): List<RadioStation> {
        val result = iTunesPodcastService.fetchPodcasts(
            limit = limit,
            offset = offset,
            searchQuery = query,
            genre = genre,
            country = country
        )
        val allItems = result.podcasts + result.liveRadioStations
        if (allItems.isNotEmpty()) {
            dao.saveStationsToCache(allItems)
        }
        return result.podcasts
    }

    override suspend fun getiTunesLiveRadioStations(
        query: String,
        genre: String,
        country: String
    ): List<RadioStation> {
        val result = iTunesPodcastService.fetchPodcasts(
            limit = 30,
            offset = 0,
            searchQuery = query,
            genre = genre,
            country = country
        )
        return result.liveRadioStations
    }

    override fun getFavoriteStations(): Flow<List<RadioStation>> = dao.getFavoriteStations()
    override fun getRecentStations(): Flow<List<RadioStation>> = dao.getRecentStations()

    override suspend fun toggleFavorite(station: RadioStation) {
        val newFav = !station.isFavorite
        val updated = station.copy(isFavorite = newFav)
        dao.insertOrUpdateStation(updated)
    }

    override suspend fun recordStationListened(station: RadioStation) {
        val updated = station.copy(lastListenedTimestamp = System.currentTimeMillis())
        dao.insertOrUpdateStation(updated)
    }

    override suspend fun addCustomStation(station: RadioStation) {
        dao.insertOrUpdateStation(station)
    }

    override suspend fun deleteCustomStation(station: RadioStation) {
        dao.deleteStation(station)
    }
}
