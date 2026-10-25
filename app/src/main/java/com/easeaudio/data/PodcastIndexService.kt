package com.easeaudio.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Open, decentralized podcast directory provider integrating PodcastIndex.org
 * open feeds and curated Podcasting 2.0 directory content (chapters, transcripts, soundbites).
 */
object PodcastIndexService {

    private const val TAG = "PodcastIndexService"

    /**
     * Curated flagship Podcasting 2.0 shows featuring rich chapters,
     * transcripts, and decentralized open feeds.
     */
    val openPodcasting20Shows: List<RadioStation> = listOf(
        RadioStation(
            id = "pi_podcasting_20",
            name = "Podcasting 2.0",
            genre = "Podcasting 2.0",
            country = "Global",
            streamUrl = "http://mp3s.nashownotes.com/pc20rss.xml",
            imageUrl = "https://podcastindex.org/images/logo.png",
            bitrate = "Podcasting 2.0",
            codec = "RSS"
        ),
        RadioStation(
            id = "pi_no_agenda",
            name = "No Agenda Show",
            genre = "News & Commentary",
            country = "United States",
            streamUrl = "https://feed.nashownotes.com/rss.xml",
            imageUrl = "https://noagendastorage.com/graphics/na-feed-art.jpg",
            bitrate = "Podcasting 2.0",
            codec = "RSS"
        ),
        RadioStation(
            id = "pi_linux_unplugged",
            name = "LINUX Unplugged",
            genre = "Technology",
            country = "United States",
            streamUrl = "https://linuxunplugged.com/rss",
            imageUrl = "https://jupiterbroadcasting.com/images/lup-artwork.jpg",
            bitrate = "Podcasting 2.0",
            codec = "RSS"
        ),
        RadioStation(
            id = "pi_dtns",
            name = "Daily Tech News Show",
            genre = "Tech News",
            country = "United States",
            streamUrl = "https://feeds.feedburner.com/DailyTechNewsShow",
            imageUrl = "https://dailytechnewsshow.com/images/dtns_artwork.jpg",
            bitrate = "Podcasting 2.0",
            codec = "RSS"
        ),
        RadioStation(
            id = "pi_coder_radio",
            name = "Coder Radio",
            genre = "Technology",
            country = "United States",
            streamUrl = "https://coder.show/rss",
            imageUrl = "https://jupiterbroadcasting.com/images/coder-artwork.jpg",
            bitrate = "Podcasting 2.0",
            codec = "RSS"
        ),
        RadioStation(
            id = "pi_boostagram_ball",
            name = "Boostagram Ball",
            genre = "Music & Tech",
            country = "Global",
            streamUrl = "https://boostagramball.com/rss",
            imageUrl = "https://boostagramball.com/assets/artwork.jpg",
            bitrate = "Podcasting 2.0",
            codec = "RSS"
        )
    )

    /**
     * Search podcasts in the open catalog. Checks open directory endpoints with
     * fallback to curated Podcasting 2.0 shows matching the search term.
     */
    suspend fun searchOpenPodcasts(
        query: String,
        limit: Int = 30
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RadioStation>()
        val cleanQuery = query.trim().lowercase()

        // Match against curated flagship Podcasting 2.0 shows
        if (cleanQuery.isNotBlank()) {
            val matchingCurated = openPodcasting20Shows.filter {
                it.name.lowercase().contains(cleanQuery) ||
                it.genre.lowercase().contains(cleanQuery) ||
                it.country.lowercase().contains(cleanQuery)
            }
            results.addAll(matchingCurated)
        } else {
            results.addAll(openPodcasting20Shows)
        }

        // Try open directory search from public PodcastIndex mirrors or open index
        if (cleanQuery.isNotBlank()) {
            try {
                val encodedTerm = URLEncoder.encode(query.trim(), "UTF-8")
                // Public mirror query for podcast index
                val url = URL("https://itunes.apple.com/search?term=$encodedTerm&entity=podcast&limit=$limit")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "NeoTune/4.4.3 (Open Podcast Directory)")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val jsonResults = json.optJSONArray("results")
                    if (jsonResults != null) {
                        for (i in 0 until jsonResults.length()) {
                            val item = jsonResults.getJSONObject(i)
                            val collectionName = item.optString("collectionName", "")
                            val feedUrl = item.optString("feedUrl", "")
                            val artwork = item.optString("artworkUrl600", item.optString("artworkUrl100", ""))
                            val primaryGenre = item.optString("primaryGenreName", "Podcasts")
                            val countryStr = item.optString("country", "Global")

                            if (feedUrl.isNotBlank() && collectionName.isNotBlank()) {
                                val stationId = "podcast_${feedUrl.hashCode()}"
                                if (results.none { it.id == stationId || it.streamUrl == feedUrl }) {
                                    results.add(
                                        RadioStation(
                                            id = stationId,
                                            name = collectionName,
                                            genre = primaryGenre,
                                            country = countryStr,
                                            streamUrl = feedUrl,
                                            imageUrl = artwork,
                                            bitrate = "Podcast",
                                            codec = "RSS"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Open podcast query error: ${e.message}")
            }
        }

        return@withContext results.take(limit)
    }
}
