package com.easeaudio.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object RadioBrowserService {

    private const val TAG = "RadioBrowserService"
    private const val BASE_URL = "https://all.api.radio-browser.info/json/stations"

    suspend fun fetchTopStations(
        limit: Int = 40,
        offset: Int = 0,
        searchQuery: String = "",
        genreTag: String = ""
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val stations = mutableListOf<RadioStation>()
        try {
            val mappedTag = mapGenreToTag(genreTag)
            val urlBuilder = StringBuilder("$BASE_URL/search?offset=$offset&limit=$limit&order=clickcount&reverse=true&hidebroken=true&is_https=true")
            
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
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "EaseAudioApp/1.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.optString("stationuuid", "id_$i")
                    val name = item.optString("name", "Unknown Radio").trim()
                    val streamUrl = item.optString("url_resolved", item.optString("url", ""))
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch online radio stations: ${e.message}")
        }
        return@withContext stations
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
