package com.easeaudio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RadioRepository(
    private val dao: RadioDao,
    private val favoriteDao: FavoriteDao,
    private val recentSearchDao: RecentSearchDao,
    private val listenLaterDao: ListenLaterDao
) : IRadioRepository {

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

    override fun getFavoriteStations(): Flow<List<RadioStation>> {
        return combine(dao.getFavoriteStations(), favoriteDao.getAllFavorites()) { radioFavs, favEntities ->
            val favEntityMap = favEntities.map { entity ->
                RadioStation(
                    id = entity.id,
                    name = entity.name,
                    genre = entity.genre,
                    country = entity.country,
                    streamUrl = entity.streamUrl,
                    imageUrl = entity.imageUrl,
                    bitrate = entity.bitrate,
                    codec = entity.codec,
                    isFavorite = true,
                    isCustom = entity.isCustom
                )
            }.associateBy { it.id }

            val merged = mutableMapOf<String, RadioStation>()
            radioFavs.forEach { merged[it.id] = it.copy(isFavorite = true) }
            favEntityMap.forEach { (id, station) -> merged[id] = station }
            merged.values.toList()
        }
    }

    override fun getRecentStations(): Flow<List<RadioStation>> = dao.getRecentStations()

    override fun getListenLaterItems(): Flow<List<ListenLaterItem>> = listenLaterDao.getAllListenLater()

    override suspend fun toggleListenLater(station: RadioStation) {
        val isPresent = listenLaterDao.isListenLaterDirect(station.id)
        if (isPresent) {
            listenLaterDao.deleteById(station.id)
        } else {
            val item = ListenLaterItem(
                id = station.id,
                name = station.name,
                genre = station.genre,
                country = station.country,
                streamUrl = station.streamUrl,
                imageUrl = station.imageUrl,
                bitrate = station.bitrate,
                codec = station.codec,
                isCustom = station.isCustom,
                isPodcast = station.isPodcast
            )
            listenLaterDao.insert(item)
        }
    }

    override suspend fun clearListenLater() {
        listenLaterDao.clearAll()
    }

    override suspend fun toggleFavorite(station: RadioStation) {
        val currentlyFav = station.isFavorite || favoriteDao.isFavoriteDirect(station.id)
        if (currentlyFav) {
            favoriteDao.deleteFavoriteById(station.id)
            dao.updateFavoriteStatus(station.id, false)
        } else {
            val favEntity = FavoriteStation(
                id = station.id,
                name = station.name,
                genre = station.genre,
                country = station.country,
                streamUrl = station.streamUrl,
                imageUrl = station.imageUrl,
                bitrate = station.bitrate,
                codec = station.codec,
                isCustom = station.isCustom
            )
            favoriteDao.insertFavorite(favEntity)
            dao.insertOrUpdateStation(station.copy(isFavorite = true))
        }
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

    override fun getRecentSearchQueries(limit: Int): Flow<List<String>> {
        return recentSearchDao.getRecentSearchQueries(limit).map { list ->
            list.map { it.query }
        }
    }

    override suspend fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            recentSearchDao.insertSearchQuery(
                RecentSearchQuery(query = trimmed, timestamp = System.currentTimeMillis())
            )
        }
    }

    override suspend fun deleteSearchQuery(query: String) {
        recentSearchDao.deleteSearchQuery(query)
    }

    override suspend fun clearSearchQueries() {
        recentSearchDao.clearRecentSearchQueries()
    }
}
