package com.easeaudio.data

import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class SongLyrics(
    val plainLyrics: String?,
    val syncedLyrics: List<LyricLine>?,
    val trackName: String,
    val artistName: String
) {
    val hasLyrics: Boolean
        get() = !plainLyrics.isNullOrBlank() || !syncedLyrics.isNullOrEmpty()
}

object LyricsService {
    private const val TAG = "LyricsService"
    private val lyricsCache = LruCache<String, SongLyrics>(100)
    private val nullCache = LruCache<String, Boolean>(100)

    suspend fun fetchLyrics(rawTitle: String, stationName: String = ""): SongLyrics? = withContext(Dispatchers.IO) {
        val (artist, track) = parseArtistAndTitle(rawTitle, stationName)
        if (track.isBlank()) return@withContext null

        val cacheKey = "$artist - $track".lowercase().trim()
        lyricsCache.get(cacheKey)?.let { return@withContext it }
        if (nullCache.get(cacheKey) == true) return@withContext null

        try {
            // Attempt 1: Query by artist and track name
            var lyrics = queryLrclib(artist, track)
            
            // Attempt 2: Fallback to general query if specific search returned nothing
            if (lyrics == null) {
                val combinedQuery = if (artist.isNotBlank()) "$artist $track" else track
                lyrics = searchLrclib(combinedQuery)
            }

            if (lyrics != null && lyrics.hasLyrics) {
                lyricsCache.put(cacheKey, lyrics)
                Log.d(TAG, "Lyrics found for '$cacheKey'")
                return@withContext lyrics
            }

            nullCache.put(cacheKey, true)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch lyrics for '$rawTitle': ${e.message}")
            null
        }
    }

    private fun queryLrclib(artist: String, track: String): SongLyrics? {
        val encodedTrack = URLEncoder.encode(track, "UTF-8")
        val encodedArtist = URLEncoder.encode(artist, "UTF-8")
        val urlString = if (artist.isNotBlank()) {
            "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTrack"
        } else {
            "https://lrclib.net/api/get?track_name=$encodedTrack"
        }
        return executeLrclibRequest(urlString)
    }

    private fun searchLrclib(query: String): SongLyrics? {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val urlString = "https://lrclib.net/api/search?q=$encodedQuery"
        return executeLrclibSearchRequest(urlString)
    }

    private fun executeLrclibRequest(urlString: String): SongLyrics? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
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
                return parseSongLyrics(json)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Request failed for $urlString: ${e.message}")
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }
        return null
    }

    private fun executeLrclibSearchRequest(urlString: String): SongLyrics? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "NeoTune/1.0 (Android; Radio/Podcast)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonText)
                if (array.length() > 0) {
                    return parseSongLyrics(array.getJSONObject(0))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search request failed for $urlString: ${e.message}")
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }
        return null
    }

    private fun parseSongLyrics(json: JSONObject): SongLyrics {
        val trackName = json.optString("trackName", json.optString("name", ""))
        val artistName = json.optString("artistName", "")
        val plainLyrics = json.optString("plainLyrics", "").trim()
        val rawSyncedLyrics = json.optString("syncedLyrics", "").trim()

        val parsedSynced = if (rawSyncedLyrics.isNotBlank()) {
            parseLrc(rawSyncedLyrics)
        } else {
            null
        }

        return SongLyrics(
            plainLyrics = plainLyrics.ifBlank { null },
            syncedLyrics = if (!parsedSynced.isNullOrEmpty()) parsedSynced else null,
            trackName = trackName,
            artistName = artistName
        )
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val lrcRegex = Regex("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?\\](.*)")

        lrcText.lines().forEach { line ->
            val match = lrcRegex.find(line.trim())
            if (match != null) {
                val (minStr, secStr, msStr, text) = match.destructured
                val minutes = minStr.toLongOrNull() ?: 0L
                val seconds = secStr.toLongOrNull() ?: 0L
                val millis = when (msStr.length) {
                    2 -> (msStr.toLongOrNull() ?: 0L) * 10
                    3 -> msStr.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val timestampMs = (minutes * 60 * 1000) + (seconds * 1000) + millis
                val cleanText = text.trim()
                if (cleanText.isNotBlank()) {
                    lines.add(LyricLine(timestampMs, cleanText))
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    private fun parseArtistAndTitle(raw: String, stationName: String): Pair<String, String> {
        var clean = raw.trim()
        if (stationName.isNotBlank()) {
            clean = clean.replace(stationName, "", ignoreCase = true).trim()
        }

        // Clean out typical promo patterns
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
            clean = pattern.replace(clean, "").trim()
        }

        clean = clean.trim { it == '-' || it == ':' || it == '|' || it <= ' ' }

        return when {
            clean.contains(" - ") -> {
                val parts = clean.split(" - ", limit = 2)
                Pair(parts[0].trim(), parts[1].trim())
            }
            clean.contains(" – ") -> { // En-dash
                val parts = clean.split(" – ", limit = 2)
                Pair(parts[0].trim(), parts[1].trim())
            }
            clean.contains(" — ") -> { // Em-dash
                val parts = clean.split(" — ", limit = 2)
                Pair(parts[0].trim(), parts[1].trim())
            }
            clean.contains(":") -> {
                val parts = clean.split(":", limit = 2)
                Pair(parts[0].trim(), parts[1].trim())
            }
            else -> Pair("", clean)
        }
    }
}
