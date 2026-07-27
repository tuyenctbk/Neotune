package com.easeaudio.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RadioRepository(private val dao: RadioDao) {

    val defaultStations = listOf(
        RadioStation(
            id = "bbc_world_service",
            name = "BBC World Service (Live & Daily Reports)",
            genre = "News & Reports",
            country = "United Kingdom",
            streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
            imageUrl = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        ),
        RadioStation(
            id = "npr_news_hourly",
            name = "NPR News & Daily Reports",
            genre = "News & Reports",
            country = "United States",
            streamUrl = "https://npr-ice.streamguys1.com/live.mp3",
            imageUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        ),
        RadioStation(
            id = "france_info_reports",
            name = "France Info (Daily Reports & News)",
            genre = "News & Reports",
            country = "France",
            streamUrl = "https://icecast.radiofrance.fr/franceinfo-midfi.mp3",
            imageUrl = "https://images.unsplash.com/photo-1526470608268-f674ce90ebd4?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        ),
        RadioStation(
            id = "somafm_groove_salad",
            name = "SomaFM: Groove Salad",
            genre = "Lo-Fi & Chill",
            country = "United States",
            streamUrl = "https://stream.somafm.com/groovesalad-128-mp3",
            imageUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=600&q=80",
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
            id = "radio_swiss_jazz",
            name = "Radio Swiss Jazz",
            genre = "Jazz",
            country = "Switzerland",
            streamUrl = "https://stream.srg-ssr.ch/m/rsj/mp3_128",
            imageUrl = "https://images.unsplash.com/photo-1415201364774-f6f0bb35f28f?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        ),
        RadioStation(
            id = "radio_swiss_classic",
            name = "Radio Swiss Classic",
            genre = "Classical",
            country = "Switzerland",
            streamUrl = "https://stream.srg-ssr.ch/m/rsc_de/mp3_128",
            imageUrl = "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        ),
        RadioStation(
            id = "somafm_secret_agent",
            name = "SomaFM: Secret Agent",
            genre = "Ambient",
            country = "United States",
            streamUrl = "https://stream.somafm.com/secretagent-128-mp3",
            imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        ),
        RadioStation(
            id = "somafm_drone_zone",
            name = "SomaFM: Drone Zone (Atmospheric)",
            genre = "Ambient",
            country = "United States",
            streamUrl = "https://stream.somafm.com/dronezone-128-mp3",
            imageUrl = "https://images.unsplash.com/photo-1518241353330-0f7941c2d9b5?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        ),
        RadioStation(
            id = "kexp_903",
            name = "KEXP 90.3 FM Seattle",
            genre = "Rock",
            country = "United States",
            streamUrl = "https://kexp-mp3-128.streamguys1.com/kexp128.mp3",
            imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps",
            codec = "MP3"
        ),
        RadioStation(
            id = "lofi_girl_radio",
            name = "Lofi Hip Hop Chill Beats",
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
