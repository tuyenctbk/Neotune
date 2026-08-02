package com.easeaudio.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class iTunesFetchResult(
    val podcasts: List<RadioStation>,
    val liveRadioStations: List<RadioStation>
)

object iTunesPodcastService {

    private const val TAG = "iTunesPodcastService"

    suspend fun fetchPodcasts(
        limit: Int = 40,
        offset: Int = 0,
        searchQuery: String = "",
        genre: String = "",
        country: String = ""
    ): iTunesFetchResult = withContext(Dispatchers.IO) {
        val countryCode = when (country.lowercase().trim()) {
            "vietnam", "vn" -> "VN"
            "united states", "usa", "us" -> "US"
            "united kingdom", "uk", "gb" -> "GB"
            "germany", "de" -> "DE"
            "france", "fr" -> "FR"
            "japan", "jp" -> "JP"
            "canada", "ca" -> "CA"
            "australia", "au" -> "AU"
            else -> if (country.length == 2) country.uppercase() else ""
        }
        val countryParam = if (countryCode.isNotBlank()) "&country=$countryCode" else ""
        val cacheKey = "itunes_${searchQuery.trim().lowercase()}_${genre.trim().lowercase()}_${countryCode}_${offset}_$limit"
        val cached = PodcastCacheManager.get(cacheKey)
        if (cached != null) {
            return@withContext iTunesFetchResult(cached.first, cached.second)
        }

        val podcasts = mutableListOf<RadioStation>()
        val liveRadio = mutableListOf<RadioStation>()

        try {
            val searchTerm = when {
                searchQuery.isNotBlank() -> searchQuery.trim()
                genre.isNotBlank() && !genre.equals("All", ignoreCase = true) && !genre.equals("Podcasts", ignoreCase = true) -> genre.trim()
                else -> "top podcasts"
            }

            val encodedTerm = URLEncoder.encode(searchTerm, "UTF-8")
            val urlString = "https://itunes.apple.com/search?term=$encodedTerm&entity=podcast&limit=$limit&offset=$offset$countryParam"
            
            Log.d(TAG, "Fetching podcasts from iTunes API: $urlString")

            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) NeoTune/1.0")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val jsonObject = JSONObject(jsonText)
                val resultsArray = jsonObject.optJSONArray("results") ?: return@withContext iTunesFetchResult(emptyList(), emptyList())

                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.getJSONObject(i)
                    val collectionId = item.optLong("collectionId", 0L)
                    val trackId = item.optLong("trackId", 0L)
                    
                    val name = item.optString("collectionName", item.optString("trackName", "Podcast")).trim()
                    val artist = item.optString("artistName", "").trim()
                    val primaryGenre = item.optString("primaryGenreName", "Podcast").trim()
                    val feedUrl = item.optString("feedUrl", "").trim()
                    val collectionViewUrl = item.optString("collectionViewUrl", "").trim()
                    val artworkUrl = item.optString("artworkUrl600", item.optString("artworkUrl100", "")).trim()
                    val country = item.optString("country", "USA").trim()

                    val streamUrl = when {
                        feedUrl.isNotBlank() -> feedUrl
                        collectionViewUrl.isNotBlank() -> collectionViewUrl
                        else -> ""
                    }

                    if (name.isNotBlank() && streamUrl.isNotBlank()) {
                        val formattedGenre = if (artist.isNotBlank()) "$primaryGenre • $artist" else primaryGenre
                        val isRadio = isLiveRadioBroadcast(name, artist, streamUrl, primaryGenre)

                        if (isRadio) {
                            val id = "itunes_radio_${if (collectionId != 0L) collectionId else trackId}_$i"
                            liveRadio.add(
                                RadioStation(
                                    id = id,
                                    name = name,
                                    genre = formattedGenre,
                                    country = country,
                                    streamUrl = streamUrl,
                                    imageUrl = artworkUrl,
                                    bitrate = "Live Radio",
                                    codec = "AAC/MP3"
                                )
                            )
                        } else {
                            val id = "itunes_podcast_${if (collectionId != 0L) collectionId else trackId}_$i"
                            podcasts.add(
                                RadioStation(
                                    id = id,
                                    name = name,
                                    genre = formattedGenre,
                                    country = country,
                                    streamUrl = streamUrl,
                                    imageUrl = artworkUrl,
                                    bitrate = "Podcast",
                                    codec = "AAC/MP3"
                                )
                            )
                        }
                    }
                }
                Log.i(TAG, "Successfully fetched from iTunes for '$searchTerm': ${podcasts.size} podcasts, ${liveRadio.size} live radio stations")
                PodcastCacheManager.put(cacheKey, podcasts, liveRadio)
            } else {
                connection.disconnect()
                Log.w(TAG, "iTunes API HTTP failure code $responseCode for URL: $urlString")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying iTunes Podcast API: ${e.message}", e)
        }
        return@withContext iTunesFetchResult(podcasts, liveRadio)
    }

    private fun isLiveRadioBroadcast(name: String, artist: String, streamUrl: String, genre: String): Boolean {
        val lowerName = name.lowercase()
        val lowerArtist = artist.lowercase()
        val lowerUrl = streamUrl.lowercase()
        val lowerGenre = genre.lowercase()

        // 1. Direct stream extension or radio server keywords
        if (lowerUrl.contains(".m3u8") || lowerUrl.contains(".pls") || lowerUrl.contains(".m3u") ||
            lowerUrl.contains("icecast") || lowerUrl.contains("shoutcast") || lowerUrl.contains("streamtheworld") ||
            lowerUrl.contains("radionet") || lowerUrl.contains("live-stream") || lowerUrl.contains("listen.live")) {
            return true
        }

        // 2. Clear Radio / FM / AM / Live indicators in name or artist
        val radioKeywords = listOf("radio", " fm", " am", "live broadcast", "24/7", "broadcasting", "live stream")
        val isNameOrArtistRadio = radioKeywords.any { lowerName.contains(it) || lowerArtist.contains(it) }

        // Must NOT have typical podcast RSS feed extensions if classified as radio
        val isRssFeed = lowerUrl.endsWith(".xml") || lowerUrl.endsWith(".rss") || lowerUrl.contains("/feed") ||
                lowerUrl.contains("megaphone") || lowerUrl.contains("buzzsprout") || lowerUrl.contains("libsyn") ||
                lowerUrl.contains("anchor.fm") || lowerUrl.contains("podbean")

        return isNameOrArtistRadio && !isRssFeed
    }
}
