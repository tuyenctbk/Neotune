package com.easeaudio.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object RadioBrowserService {

    private const val TAG = "RadioBrowserService"
    
    private var cachedServers: List<String> = emptyList()
    private val cacheMutex = Mutex()

    private suspend fun getActiveServers(): List<String> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            if (cachedServers.isNotEmpty()) {
                return@withContext cachedServers
            }
            
            val servers = mutableListOf<String>()
            try {
                val addresses = java.net.InetAddress.getAllByName("all.api.radio-browser.info")
                for (addr in addresses) {
                    val host = addr.canonicalHostName
                    if (host.isNotBlank() && host != addr.hostAddress && host.contains("radio-browser.info")) {
                        servers.add("https://$host/json/stations")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "DNS resolution of all.api.radio-browser.info failed, using fallback mirrors: ${e.message}")
            }
            
            // If DNS lookup failed or returned empty, use fallback mirrors
            if (servers.isEmpty()) {
                servers.addAll(listOf(
                    "https://de1.api.radio-browser.info/json/stations",
                    "https://nl1.api.radio-browser.info/json/stations",
                    "https://at1.api.radio-browser.info/json/stations",
                    "https://fr1.api.radio-browser.info/json/stations"
                ))
            }
            cachedServers = servers
            return@withContext servers
        }
    }

    suspend fun fetchTopStations(
        limit: Int = 40,
        offset: Int = 0,
        searchQuery: String = "",
        genreTag: String = ""
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val mappedTag = mapGenreToTag(genreTag)
        val activeUrls = getActiveServers()
        
        for (baseUrl in activeUrls) {
            val stations = mutableListOf<RadioStation>()
            try {
                val urlBuilder = StringBuilder("$baseUrl/search?offset=$offset&limit=$limit&order=clickcount&reverse=true&hidebroken=true")
                
                if (searchQuery.isNotBlank()) {
                    val encodedQuery = URLEncoder.encode(searchQuery.trim(), "UTF-8")
                    urlBuilder.append("&name=").append(encodedQuery)
                }
                if (mappedTag.isNotBlank()) {
                    val encodedTag = URLEncoder.encode(mappedTag, "UTF-8")
                    urlBuilder.append("&tag=").append(encodedTag)
                }

                val urlString = urlBuilder.toString()
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "EaseAudioApp/1.0")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    val jsonArray = JSONArray(responseText)

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val id = item.optString("stationuuid", "id_$i")
                        val name = item.optString("name", "Unknown Radio").trim()
                        val rawUrlResolved = item.optString("url_resolved", "").trim()
                        val rawUrl = item.optString("url", "").trim()
                        val streamUrl = when {
                            rawUrlResolved.isNotBlank() -> rawUrlResolved
                            rawUrl.isNotBlank() -> rawUrl
                            else -> ""
                        }
                        val favicon = item.optString("favicon", "")
                        val country = item.optString("country", "Global")
                        val tags = item.optString("tags", "General")
                        val bitrateVal = item.optInt("bitrate", 128)
                        val codec = item.optString("codec", "MP3").uppercase()

                        // Filter out adult/nsfw content to keep the app safe
                        val isAdult = tags.contains("adult", ignoreCase = true) || 
                                      tags.contains("nsfw", ignoreCase = true) || 
                                      tags.contains("explicit", ignoreCase = true)
                        
                        if (!isAdult && name.isNotBlank() && streamUrl.isNotBlank() && (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))) {
                            val imageUrl = if (favicon.startsWith("http")) favicon else getRandomDefaultImage(tags)
                            val formattedGenre = tags.split(",").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Music"

                            stations.add(
                                RadioStation(
                                    id = id,
                                    name = name,
                                    genre = if (formattedGenre.length > 20) "Music" else formattedGenre,
                                    country = if (country.isBlank()) "Global" else country,
                                    streamUrl = streamUrl,
                                    imageUrl = imageUrl,
                                    bitrate = "$bitrateVal kbps",
                                    codec = codec
                                )
                            )
                        }
                    }
                    if (stations.isNotEmpty() || (searchQuery.isNotBlank() || mappedTag.isNotBlank())) {
                        return@withContext stations
                    }
                } else {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetching online stations from $baseUrl: ${e.message}")
                try {
                    val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                    crashlytics.setCustomKey("failed_server_url", baseUrl)
                    crashlytics.setCustomKey("search_query", searchQuery)
                    crashlytics.setCustomKey("genre_tag", genreTag)
                    crashlytics.recordException(e)
                } catch (ce: Exception) {
                    // Firebase Crashlytics not configured or initialized yet
                }
            }
        }
        return@withContext emptyList()
    }

    private fun mapGenreToTag(genreTag: String): String {
        return when (genreTag) {
            "News & Reports" -> "news"
            "Lo-Fi & Chill" -> "chill"
            "Jazz" -> "jazz"
            "Rock" -> "rock"
            "Classical" -> "classical"
            "Ambient" -> "ambient"
            "EDM" -> "edm"
            "Pop" -> "pop"
            "Hip Hop" -> "hip hop"
            "House" -> "house"
            "Country" -> "country"
            "All", "Custom" -> ""
            else -> genreTag.lowercase().replace("&", "").trim()
        }
    }

    private fun getRandomDefaultImage(tags: String): String {
        return when {
            tags.contains("news", true) || tags.contains("talk", true) || tags.contains("report", true) ->
                "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?auto=format&fit=crop&w=600&q=80"
            tags.contains("jazz", true) ->
                "https://images.unsplash.com/photo-1511192336575-5a79af67a629?auto=format&fit=crop&w=600&q=80"
            tags.contains("lofi", true) || tags.contains("chill", true) ->
                "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=600&q=80"
            tags.contains("rock", true) ->
                "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?auto=format&fit=crop&w=600&q=80"
            tags.contains("classical", true) || tags.contains("piano", true) ->
                "https://images.unsplash.com/photo-1520523839897-bd0b52f945a0?auto=format&fit=crop&w=600&q=80"
            else ->
                "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80"
        }
    }
}
