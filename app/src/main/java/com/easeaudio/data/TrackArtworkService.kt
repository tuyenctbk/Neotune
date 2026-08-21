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
    private val artworkCache = LruCache<String, String>(150)
    private val nullCache = LruCache<String, Boolean>(150)

    suspend fun fetchTrackArtwork(rawTrackTitle: String, stationName: String = ""): String? = withContext(Dispatchers.IO) {
        if (rawTrackTitle.isBlank()) return@withContext null

        val cleanQuery = cleanTrackTitle(rawTrackTitle, stationName)
        if (cleanQuery.isBlank() || cleanQuery.length < 3) return@withContext null

        val cacheKey = cleanQuery.lowercase()
        artworkCache.get(cacheKey)?.let { return@withContext it }
        if (nullCache.get(cacheKey) == true) return@withContext null

        var connection: HttpURLConnection? = null
        try {
            val encodedTerm = URLEncoder.encode(cleanQuery, "UTF-8")
            val urlString = "https://itunes.apple.com/search?term=$encodedTerm&entity=song&limit=1"
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
                    val firstItem = json.getJSONArray("results").getJSONObject(0)
                    val rawArtwork = firstItem.optString("artworkUrl100", firstItem.optString("artworkUrl60", ""))
                    if (rawArtwork.isNotBlank()) {
                        val highResArtwork = rawArtwork.replace(Regex("/\\d+x\\d+bb"), "/600x600bb")
                        artworkCache.put(cacheKey, highResArtwork)
                        Log.d(TAG, "Resolved artwork for '$cleanQuery' -> $highResArtwork")
                        return@withContext highResArtwork
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
            Regex("(?i)\\(remastered(\\s*\\d{4})?\\)"),
            Regex("(?i)ft\\.?\\s+[^\\-\\(\\[]+"),
            Regex("(?i)feat\\.?\\s+[^\\-\\(\\[]+"),
            Regex("(?i)\\bnow\\s+playing:?\\b"),
            Regex("(?i)\\blive\\s+on\\s+air\\b")
        )

        for (pattern in junkPatterns) {
            query = pattern.replace(query, "").trim()
        }

        // Remove trailing hyphens or punctuation
        query = query.trim { it == '-' || it == ':' || it == '|' || it <= ' ' }
        return query
    }
}
