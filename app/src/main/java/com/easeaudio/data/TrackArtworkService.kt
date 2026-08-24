package com.easeaudio.data

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TrackArtworkService {
    private const val TAG = "TrackArtworkService"
    private val artworkCache = LruCache<String, String>(200)
    private val nullCache = LruCache<String, Boolean>(200)

    private val GENERIC_TITLES = setOf(
        "live", "stream", "live stream", "live audio stream", "audio stream",
        "unknown", "unknown artist", "unknown track", "various artists",
        "commercial", "commercial break", "advertisement", "ad",
        "promo", "station promo", "jingle", "ident", "station ident",
        "radio", "music", "news", "news update", "weather", "traffic",
        "top 40", "hits", "station", "on air", "live on air", "now playing"
    )

    suspend fun fetchTrackArtwork(rawTrackTitle: String, stationName: String = ""): String? = withContext(Dispatchers.IO) {
        if (rawTrackTitle.isBlank()) return@withContext null

        val cleanQuery = cleanTrackTitle(rawTrackTitle, stationName)
        if (cleanQuery.isBlank() || isGenericOrInvalid(cleanQuery, stationName)) return@withContext null

        val cacheKey = cleanQuery.lowercase()
        artworkCache.get(cacheKey)?.let { return@withContext it }
        if (nullCache.get(cacheKey) == true) return@withContext null

        var connection: HttpURLConnection? = null
        try {
            val encodedTerm = URLEncoder.encode(cleanQuery, "UTF-8")
            val urlString = "https://itunes.apple.com/search?term=$encodedTerm&entity=song&limit=3"
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "NeoTune/1.0 (Android; Radio/Podcast)")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val resultsCount = json.optInt("resultCount", 0)
                if (resultsCount > 0) {
                    val results = json.getJSONArray("results")
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val artistName = item.optString("artistName", "")
                        val trackName = item.optString("trackName", "")
                        val collectionName = item.optString("collectionName", "")

                        if (isResultValidMatch(cleanQuery, artistName, trackName, collectionName)) {
                            val rawArtwork = item.optString("artworkUrl100", item.optString("artworkUrl60", ""))
                            if (rawArtwork.isNotBlank()) {
                                val highResArtwork = rawArtwork.replace(Regex("/\\d+x\\d+bb"), "/600x600bb")
                                artworkCache.put(cacheKey, highResArtwork)
                                Log.d(TAG, "Resolved artwork for '$cleanQuery' -> $highResArtwork (matched: $artistName - $trackName)")
                                return@withContext highResArtwork
                            }
                        }
                    }
                }
            }
            nullCache.put(cacheKey, true)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve track artwork for '$cleanQuery': ${e.message}")
            null
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    private fun isResultValidMatch(
        query: String,
        artistName: String,
        trackName: String,
        collectionName: String
    ): Boolean {
        val q = query.lowercase().trim()
        val a = artistName.lowercase().trim()
        val t = trackName.lowercase().trim()
        val c = collectionName.lowercase().trim()

        // If query has "Artist - Title" format
        if (q.contains(" - ")) {
            val parts = q.split(" - ", limit = 2)
            val qArtist = parts[0].trim()
            val qTitle = parts[1].trim()

            val artistMatches = a.contains(qArtist) || qArtist.contains(a) ||
                    wordsOverlap(a, qArtist)
            val titleMatches = t.contains(qTitle) || qTitle.contains(t) ||
                    wordsOverlap(t, qTitle) || c.contains(qTitle)

            return artistMatches && titleMatches
        }

        // Single query without separator - require strong match with track or artist
        val queryWords = q.split(Regex("[\\s\\-_/]+")).filter { it.length > 2 }
        if (queryWords.isEmpty()) return false

        val trackMatches = t.contains(q) || q.contains(t) || wordsOverlap(t, q)
        val artistMatches = a.contains(q) || q.contains(a)
        return trackMatches || artistMatches
    }

    private fun wordsOverlap(s1: String, s2: String): Boolean {
        val w1 = s1.split(Regex("[\\s\\-_/.,;:'\"()\\[\\]]+")).filter { it.length > 2 }.map { it.lowercase() }.toSet()
        val w2 = s2.split(Regex("[\\s\\-_/.,;:'\"()\\[\\]]+")).filter { it.length > 2 }.map { it.lowercase() }.toSet()
        if (w1.isEmpty() || w2.isEmpty()) return false
        val intersection = w1.intersect(w2)
        return intersection.isNotEmpty()
    }

    private fun isGenericOrInvalid(query: String, stationName: String): Boolean {
        val q = query.lowercase().trim()
        if (q.length < 3) return true
        if (GENERIC_TITLES.contains(q)) return true

        // URLs or domain names
        if (q.contains(".com") || q.contains(".fm") || q.contains(".org") ||
            q.contains(".net") || q.contains("http://") || q.contains("https://") ||
            q.contains("www.")
        ) return true

        // If only digits, frequencies, and punctuation (e.g., "101.5 FM", "98.7")
        if (q.replace(Regex("[0-9.\\s/\\-–|fmFM]+"), "").isBlank()) return true

        // If query is identical to station name
        if (stationName.isNotBlank()) {
            val s = stationName.lowercase().trim()
            if (q == s || q.contains(s) && q.length - s.length < 4) return true
        }

        return false
    }

    private fun cleanTrackTitle(raw: String, stationName: String): String {
        var query = raw.trim()

        // Remove station name if prepended/appended
        if (stationName.isNotBlank()) {
            query = query.replace(stationName, "", ignoreCase = true).trim()
        }

        // Clean out typical radio promo text & junk
        val junkPatterns = listOf(
            Regex("(?i)\\(official\\s*(music)?\\s*video\\)"),
            Regex("(?i)\\[official\\s*(music)?\\s*video\\]"),
            Regex("(?i)\\(official\\s*audio\\)"),
            Regex("(?i)\\[official\\s*audio\\]"),
            Regex("(?i)\\(lyric\\s*video\\)"),
            Regex("(?i)\\[lyric\\s*video\\]"),
            Regex("(?i)\\(radio\\s*edit\\)"),
            Regex("(?i)\\(album\\s*version\\)"),
            Regex("(?i)\\(remastered(\\s*\\d{4})?\\)"),
            Regex("(?i)\\[remastered(\\s*\\d{4})?\\]"),
            Regex("(?i)\\(live(\\s*at\\s*[^)]+)?\\)"),
            Regex("(?i)\\[live(\\s*at\\s*[^]]+)?\\]"),
            Regex("(?i)ft\\.?\\s+[^\\-\\(\\[]+"),
            Regex("(?i)feat\\.?\\s+[^\\-\\(\\[]+"),
            Regex("(?i)\\bnow\\s+playing:?\\b"),
            Regex("(?i)\\bplaying:?\\b"),
            Regex("(?i)\\blive\\s+on\\s+air\\b"),
            Regex("(?i)\\bon\\s+air\\b"),
            Regex("(?i)\\blive\\s+stream\\b"),
            Regex("(?i)\\baudio\\s+stream\\b")
        )

        for (pattern in junkPatterns) {
            query = pattern.replace(query, "").trim()
        }

        // Remove trailing hyphens or punctuation
        query = query.trim { it == '-' || it == ':' || it == '|' || it == '/' || it == '\\' || it <= ' ' }
        return query
    }
}
