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

    private fun sanitizeStation(station: RadioStation): RadioStation {
        val currentUrl = station.imageUrl.trim()
        val isBrokenOrIco = currentUrl.isBlank() ||
                currentUrl.contains(".ico", ignoreCase = true) ||
                currentUrl.contains("europe1.fr", ignoreCase = true) ||
                com.easeaudio.util.StationLogoResolver.isKnownBroadcaster(station.name)

        return if (isBrokenOrIco) {
            val resolved = com.easeaudio.util.StationLogoResolver.resolveStationLogo(
                name = station.name,
                favicon = currentUrl,
                homepage = "",
                tags = station.genre
            )
            if (resolved != currentUrl) station.copy(imageUrl = resolved) else station
        } else {
            station
        }
    }

    override fun getAllStations(): Flow<List<RadioStation>> {
        return dao.getAllStations().map { list -> list.map { sanitizeStation(it) } }
    }

    override suspend fun discoverOnlineStations(
        query: String,
        genre: String,
        country: String,
        countryCode: String,
        offset: Int,
        limit: Int
    ): List<RadioStation> {
        return try {
            val onlineList = RadioBrowserService.fetchTopStations(
                limit = limit,
                offset = offset,
                searchQuery = query,
                genreTag = genre,
                country = country,
                countryCode = countryCode
            ).map { sanitizeStation(it) }
            if (onlineList.isNotEmpty()) {
                dao.saveStationsToCache(onlineList)
            }
            onlineList
        } catch (e: Exception) {
            // Offline fallback: retrieve cached stations from Room database
            dao.getCachedStations(
                query = query,
                genre = genre,
                country = country,
                limit = limit,
                offset = offset
            ).map { sanitizeStation(it) }
        }
    }

    override suspend fun discoverOnlinePodcasts(
        query: String,
        genre: String,
        country: String,
        offset: Int,
        limit: Int
    ): List<RadioStation> {
        return try {
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
            result.podcasts
        } catch (e: Exception) {
            // Offline fallback: retrieve cached podcasts from Room database
            dao.getCachedStations(
                query = query,
                genre = genre,
                country = country,
                limit = limit,
                offset = offset
            ).filter { it.isPodcast }
        }
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
            merged.values.map { sanitizeStation(it) }
        }
    }

    override fun getRecentStations(): Flow<List<RadioStation>> = dao.getRecentStations().map { list -> list.map { sanitizeStation(it) } }

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

    override fun getMostPlayedStations(): Flow<List<RadioStation>> = dao.getMostPlayedStations().map { list -> list.map { sanitizeStation(it) } }

    override suspend fun recordStationListened(station: RadioStation) {
        val existing = dao.getStationById(station.id)
        val currentPlayCount = existing?.playCount ?: station.playCount
        val updated = station.copy(
            lastListenedTimestamp = System.currentTimeMillis(),
            playCount = currentPlayCount + 1
        )
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
