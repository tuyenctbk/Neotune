package com.easeaudio.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CuratedStationsService {
    private const val TAG = "CuratedStationsService"

    // Default ultra-reliable curated stations with HD stream URLs and artwork
    val defaultCuratedStations: List<RadioStation> = listOf(
        RadioStation(
            id = "curated_rp_main",
            name = "Radio Paradise (Main Mix)",
            genre = "Audiophile Eclectic Rock",
            country = "United States",
            streamUrl = "https://stream.radioparadise.com/aac-320",
            imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80",
            bitrate = "320 kbps (Lossless Master)",
            codec = "AAC-HD"
        ),
        RadioStation(
            id = "curated_rp_mellow",
            name = "Radio Paradise (Mellow Mix)",
            genre = "Chillout & Acoustic",
            country = "United States",
            streamUrl = "https://stream.radioparadise.com/mellow-320",
            imageUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=600&q=80",
            bitrate = "320 kbps (Lossless Master)",
            codec = "AAC-HD"
        ),
        RadioStation(
            id = "curated_rp_rock",
            name = "Radio Paradise (Rock Mix)",
            genre = "Classic & Modern Rock",
            country = "United States",
            streamUrl = "https://stream.radioparadise.com/rock-320",
            imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?auto=format&fit=crop&w=600&q=80",
            bitrate = "320 kbps (Lossless Master)",
            codec = "AAC-HD"
        ),
        RadioStation(
            id = "curated_rp_global",
            name = "Radio Paradise (Global Mix)",
            genre = "World & Fusion",
            country = "United States",
            streamUrl = "https://stream.radioparadise.com/global-320",
            imageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&w=600&q=80",
            bitrate = "320 kbps (Lossless Master)",
            codec = "AAC-HD"
        ),
        RadioStation(
            id = "curated_soma_groovesalad",
            name = "SomaFM: Groove Salad",
            genre = "Downtempo & Ambient Chill",
            country = "San Francisco, USA",
            streamUrl = "https://ice6.somafm.com/groovesalad-256-mp3",
            imageUrl = "https://somafm.com/img3/groovesalad-400.jpg",
            bitrate = "256 kbps (High Quality)",
            codec = "MP3"
        ),
        RadioStation(
            id = "curated_soma_defcon",
            name = "SomaFM: DEF CON Radio",
            genre = "Hacker Electronic & Synth",
            country = "San Francisco, USA",
            streamUrl = "https://ice6.somafm.com/defcon-256-mp3",
            imageUrl = "https://somafm.com/img3/defcon-400.jpg",
            bitrate = "256 kbps (High Quality)",
            codec = "MP3"
        ),
        RadioStation(
            id = "curated_soma_dronezone",
            name = "SomaFM: Drone Zone",
            genre = "Deep Space Ambient",
            country = "San Francisco, USA",
            streamUrl = "https://ice6.somafm.com/dronezone-256-mp3",
            imageUrl = "https://somafm.com/img3/dronezone-400.jpg",
            bitrate = "256 kbps (High Quality)",
            codec = "MP3"
        ),
        RadioStation(
            id = "curated_soma_secretagent",
            name = "SomaFM: Secret Agent",
            genre = "Spy & Lounge Vintage",
            country = "San Francisco, USA",
            streamUrl = "https://ice6.somafm.com/secretagent-256-mp3",
            imageUrl = "https://somafm.com/img3/secretagent-400.jpg",
            bitrate = "256 kbps (High Quality)",
            codec = "MP3"
        ),
        RadioStation(
            id = "curated_soma_vaporwaves",
            name = "SomaFM: Vaporwaves",
            genre = "Vaporwave & Chillwave",
            country = "San Francisco, USA",
            streamUrl = "https://ice6.somafm.com/vaporwaves-256-mp3",
            imageUrl = "https://somafm.com/img3/vaporwaves-400.jpg",
            bitrate = "256 kbps (High Quality)",
            codec = "MP3"
        ),
        RadioStation(
            id = "curated_soma_lush",
            name = "SomaFM: Lush",
            genre = "Sensuous Dream-Pop & Vocals",
            country = "San Francisco, USA",
            streamUrl = "https://ice6.somafm.com/lush-256-mp3",
            imageUrl = "https://somafm.com/img3/lush-400.jpg",
            bitrate = "256 kbps (High Quality)",
            codec = "MP3"
        ),
        RadioStation(
            id = "curated_soma_indiepop",
            name = "SomaFM: Indie Pop Rocks",
            genre = "Independent Pop & Rock",
            country = "San Francisco, USA",
            streamUrl = "https://ice6.somafm.com/indiepop-256-mp3",
            imageUrl = "https://somafm.com/img3/indiepop-400.jpg",
            bitrate = "256 kbps (High Quality)",
            codec = "MP3"
        ),
        RadioStation(
            id = "curated_soma_spacestation",
            name = "SomaFM: Space Station Soma",
            genre = "Ambient Electronic Beats",
            country = "San Francisco, USA",
            streamUrl = "https://ice6.somafm.com/spacestation-256-mp3",
            imageUrl = "https://somafm.com/img3/spacestation-400.jpg",
            bitrate = "256 kbps (High Quality)",
            codec = "MP3"
        )
    )

    private var cachedLiveSomaStations: List<RadioStation>? = null

    suspend fun getCuratedStations(): List<RadioStation> = withContext(Dispatchers.IO) {
        if (!cachedLiveSomaStations.isNullOrEmpty()) {
            return@withContext cachedLiveSomaStations!!
        }

        val dynamicList = mutableListOf<RadioStation>()
        // Add Radio Paradise master feeds
        dynamicList.addAll(defaultCuratedStations.filter { it.id.startsWith("curated_rp_") })

        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://api.somafm.com/channels.json")
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "NeoTune/1.0 (Android; Radio/Podcast)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonText)
                val channels = json.getJSONArray("channels")

                for (i in 0 until channels.length()) {
                    val ch = channels.getJSONObject(i)
                    val chId = ch.optString("id", "")
                    val title = ch.optString("title", "SomaFM")
                    val desc = ch.optString("description", "")
                    val genre = ch.optString("genre", "Ambient / Lo-Fi").replace("|", " • ")
                    val img = ch.optString("largeimage", ch.optString("image", "https://somafm.com/img3/$chId-400.jpg"))
                    
                    // High quality MP3/AAC stream
                    val streamUrl = "https://ice6.somafm.com/$chId-256-mp3"

                    if (chId.isNotBlank()) {
                        dynamicList.add(
                            RadioStation(
                                id = "curated_soma_$chId",
                                name = "SomaFM: $title",
                                genre = genre,
                                country = "San Francisco, USA",
                                streamUrl = streamUrl,
                                imageUrl = img,
                                bitrate = "256 kbps (High Quality)",
                                codec = "MP3"
                            )
                        )
                    }
                }
                cachedLiveSomaStations = dynamicList
                Log.i(TAG, "Successfully fetched ${dynamicList.size} curated stations from SomaFM")
                return@withContext dynamicList
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load live SomaFM channels, using default curated: ${e.message}")
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }

        return@withContext defaultCuratedStations
    }
}
