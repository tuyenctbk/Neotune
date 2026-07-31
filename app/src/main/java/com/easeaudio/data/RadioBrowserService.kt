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
            
            val defaultMirrors = listOf(
                "http://all.api.radio-browser.info/json/stations",
                "https://de1.api.radio-browser.info/json/stations",
                "https://nl1.api.radio-browser.info/json/stations",
                "https://at1.api.radio-browser.info/json/stations",
                "https://fr1.api.radio-browser.info/json/stations"
            )
            
            val servers = mutableListOf<String>()
            try {
                val addresses = java.net.InetAddress.getAllByName("all.api.radio-browser.info")
                for (addr in addresses) {
                    val host = addr.canonicalHostName
                    val ip = addr.hostAddress
                    if (host.isNotBlank() && host != ip && host.endsWith("radio-browser.info")) {
                        val url = "https://$host/json/stations"
                        if (!servers.contains(url)) {
                            servers.add(url)
                        }
                    } else if (!ip.isNullOrBlank()) {
                        // HTTP over direct IP address avoids SSL hostname mismatch completely
                        val url = "http://$ip/json/stations"
                        if (!servers.contains(url)) {
                            servers.add(url)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "DNS resolution of all.api.radio-browser.info failed, using default mirrors: ${e.message}")
            }
            
            // Always ensure valid fallback mirrors are in the server list
            for (mirror in defaultMirrors) {
                if (!servers.contains(mirror)) {
                    servers.add(mirror)
                }
            }

            cachedServers = servers
            return@withContext servers
        }
    }

    private fun executeHttpRequest(urlString: String, redirectCount: Int = 0): String? {
        if (redirectCount > 5) return null
        try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) EaseAudioApp/1.0")
                setRequestProperty("Accept", "application/json")
            }
            
            val status = connection.responseCode
            if (status in 200..299) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                return text
            } else if (status in listOf(301, 302, 303, 307, 308)) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (!location.isNullOrBlank()) {
                    val redirectUrl = if (location.startsWith("http")) location else {
                        val base = URL(urlString)
                        URL(base, location).toString()
                    }
                    return executeHttpRequest(redirectUrl, redirectCount + 1)
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "HTTP Request error for $urlString: ${e.message}")
        }
        return null
    }

    suspend fun fetchTopStations(
        limit: Int = 40,
        offset: Int = 0,
        searchQuery: String = "",
        genreTag: String = "",
        country: String = "",
        countryCode: String = ""
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val mappedTag = mapGenreToTag(genreTag)
        val activeUrls = getActiveServers()
        
        var lastNetworkException: Exception? = null
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
                if (countryCode.isNotBlank()) {
                    val encodedCode = URLEncoder.encode(countryCode.trim(), "UTF-8")
                    urlBuilder.append("&countrycode=").append(encodedCode)
                } else if (country.isNotBlank() && !country.equals("Global", ignoreCase = true) && !country.equals("All", ignoreCase = true)) {
                    val encodedCountry = URLEncoder.encode(country.trim(), "UTF-8")
                    urlBuilder.append("&country=").append(encodedCountry)
                }

                var responseText: String? = executeHttpRequest(urlBuilder.toString())

                // Fallback 1: If countrycode query yielded empty response and country name is available
                if ((responseText.isNullOrBlank() || responseText.trim() == "[]") && countryCode.isNotBlank() && country.isNotBlank() && !country.equals("Global", ignoreCase = true)) {
                    val fallbackUrlBuilder = StringBuilder("$baseUrl/search?offset=$offset&limit=$limit&order=clickcount&reverse=true&hidebroken=true")
                    if (searchQuery.isNotBlank()) fallbackUrlBuilder.append("&name=").append(URLEncoder.encode(searchQuery.trim(), "UTF-8"))
                    if (mappedTag.isNotBlank()) fallbackUrlBuilder.append("&tag=").append(URLEncoder.encode(mappedTag, "UTF-8"))
                    fallbackUrlBuilder.append("&country=").append(URLEncoder.encode(country.trim(), "UTF-8"))

                    responseText = executeHttpRequest(fallbackUrlBuilder.toString())
                }

                // Fallback 2: If country query yielded empty array, query general top stations
                if ((responseText.isNullOrBlank() || responseText.trim() == "[]") && (countryCode.isNotBlank() || country.isNotBlank()) && searchQuery.isBlank() && mappedTag.isBlank()) {
                    val fallbackUrlBuilder = StringBuilder("$baseUrl/search?offset=$offset&limit=$limit&order=clickcount&reverse=true&hidebroken=true")
                    responseText = executeHttpRequest(fallbackUrlBuilder.toString())
                }

                if (!responseText.isNullOrBlank()) {
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
                }
            } catch (e: Exception) {
                lastNetworkException = e
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
        if (lastNetworkException != null) {
            throw lastNetworkException
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
