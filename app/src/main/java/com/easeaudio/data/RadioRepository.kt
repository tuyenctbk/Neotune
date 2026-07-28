package com.easeaudio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RadioRepository(private val dao: RadioDao) {

    val defaultStations = listOf(
        RadioStation(
            id = "bbc_world_service",
            name = "BBC World Service",
            genre = "News & Reports",
            country = "United Kingdom",
            streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
            imageUrl = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        ),
        RadioStation(
            id = "jazz_groove",
            name = "Smooth Jazz & Lounge",
            genre = "Jazz",
            country = "United States",
            streamUrl = "https://smoothjazz.cdnstream1.com/2585_128.mp3",
            imageUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        ),
        RadioStation(
            id = "lofi_girl_radio",
            name = "Lofi Chill Beats",
            genre = "Lo-Fi & Chill",
            country = "Global",
            streamUrl = "https://stream.zeno.fm/f3wvbbqmdg8uv",
            imageUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        )
    )

    fun getAllStations(): Flow<List<RadioStation>> {
        val favoritesFlow = dao.getFavoriteStations()
        val customFlow = dao.getCustomStations()

        return combine(favoritesFlow, customFlow) { favorites, customList ->
            val favSet = favorites.map { it.id }.toSet()
            val baseList = defaultStations.map { station ->
                station.copy(isFavorite = favSet.contains(station.id))
            }
            baseList + customList
        }
    }

    suspend fun discoverOnlineStations(
        query: String = "",
        genre: String = "",
        offset: Int = 0,
        limit: Int = 40
    ): List<RadioStation> {
        val onlineList = RadioBrowserService.fetchTopStations(
            limit = limit,
            offset = offset,
            searchQuery = query,
            genreTag = genre
        )
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
