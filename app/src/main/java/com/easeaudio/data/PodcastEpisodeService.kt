package com.easeaudio.data

import android.util.Log
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

object PodcastEpisodeService {

    private const val TAG = "PodcastEpisodeService"
    private val episodeCache = mutableMapOf<String, List<PodcastEpisode>>()

    suspend fun fetchEpisodes(
        show: RadioStation,
        maxEpisodes: Int = 1000
    ): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        val feedUrl = show.streamUrl
        if (feedUrl.isBlank()) return@withContext emptyList()

        if (episodeCache.containsKey(feedUrl)) {
            val cached = episodeCache[feedUrl] ?: emptyList()
            if (cached.size >= maxEpisodes || cached.isNotEmpty()) {
                Log.d(TAG, "Returning ${cached.size} cached episodes for ${show.name}")
                return@withContext cached.take(maxEpisodes)
            }
        }

        val episodes = mutableListOf<PodcastEpisode>()
        try {
            Log.d(TAG, "Fetching episode RSS feed from: $feedUrl")
            val connection = (URL(feedUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) NeoTune/1.0")
                setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
            }

            if (connection.responseCode in 200..299) {
                connection.inputStream.use { stream ->
                    val parser = Xml.newPullParser().apply {
                        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                        setInput(stream, null)
                    }

                    var eventType = parser.eventType
                    var insideItem = false
                    var currentTitle = ""
                    var currentDesc = ""
                    var currentPubDate = ""
                    var currentAudioUrl = ""
                    var currentDurationMs = 0L
                    var currentArtwork = show.imageUrl

                    while (eventType != XmlPullParser.END_DOCUMENT && episodes.size < maxEpisodes) {
                        val tagName = parser.name
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (tagName.equals("item", ignoreCase = true)) {
                                    insideItem = true
                                    currentTitle = ""
                                    currentDesc = ""
                                    currentPubDate = ""
                                    currentAudioUrl = ""
                                    currentDurationMs = 0L
                                    currentArtwork = show.imageUrl
                                } else if (insideItem) {
                                    when {
                                        tagName.equals("title", ignoreCase = true) -> {
                                            currentTitle = parser.nextText().trim()
                                        }
                                        tagName.equals("description", ignoreCase = true) || tagName.equals("itunes:summary", ignoreCase = true) -> {
                                            if (currentDesc.isBlank()) {
                                                currentDesc = cleanHtmlText(parser.nextText())
                                            }
                                        }
                                        tagName.equals("pubDate", ignoreCase = true) -> {
                                            currentPubDate = formatPubDate(parser.nextText().trim())
                                        }
                                        tagName.equals("itunes:duration", ignoreCase = true) -> {
                                            currentDurationMs = parseDurationToMs(parser.nextText().trim())
                                        }
                                        tagName.equals("enclosure", ignoreCase = true) || tagName.equals("media:content", ignoreCase = true) -> {
                                            val url = parser.getAttributeValue(null, "url")
                                            val type = parser.getAttributeValue(null, "type")
                                            if (!url.isNullOrBlank()) {
                                                val isAudioType = type == null || type.contains("audio", ignoreCase = true) || type.contains("mpeg", ignoreCase = true) || type.contains("mp3", ignoreCase = true)
                                                val isAudioUrl = url.contains(".mp3", ignoreCase = true) || url.contains(".m4a", ignoreCase = true) || url.contains(".aac", ignoreCase = true) || url.contains(".ogg", ignoreCase = true) || url.contains("audio", ignoreCase = true) || url.contains("stream", ignoreCase = true)
                                                if ((isAudioType || isAudioUrl) && currentAudioUrl.isBlank()) {
                                                    currentAudioUrl = url.trim()
                                                }
                                            }
                                        }
                                        tagName.equals("itunes:image", ignoreCase = true) -> {
                                            val href = parser.getAttributeValue(null, "href")
                                            if (!href.isNullOrBlank()) {
                                                currentArtwork = href.trim()
                                            }
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (tagName.equals("item", ignoreCase = true)) {
                                    insideItem = false
                                    if (currentTitle.isNotBlank() && currentAudioUrl.isNotBlank()) {
                                        val episodeId = "ep_${show.id}_${episodes.size}_${currentAudioUrl.hashCode()}"
                                        episodes.add(
                                            PodcastEpisode(
                                                id = episodeId,
                                                showId = show.id,
                                                title = currentTitle,
                                                description = currentDesc,
                                                audioUrl = currentAudioUrl,
                                                pubDate = currentPubDate,
                                                durationMs = currentDurationMs,
                                                artworkUrl = currentArtwork.ifBlank { show.imageUrl }
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
                connection.disconnect()
                Log.i(TAG, "Successfully parsed ${episodes.size} episodes for show ${show.name}")
                episodeCache[feedUrl] = episodes
            } else {
                connection.disconnect()
                Log.w(TAG, "HTTP ${connection.responseCode} while parsing RSS feed for ${show.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS feed for ${show.name}: ${e.message}", e)
        }

        // Fallback: If feed parsing fails or yields no episodes, treat the show streamUrl as single main episode
        if (episodes.isEmpty() && show.streamUrl.isNotBlank()) {
            episodes.add(
                PodcastEpisode(
                    id = "ep_${show.id}_0",
                    showId = show.id,
                    title = show.name,
                    description = show.genre,
                    audioUrl = show.streamUrl,
                    pubDate = "Latest",
                    durationMs = 0L,
                    artworkUrl = show.imageUrl
                )
            )
        }

        return@withContext episodes
    }

    private fun cleanHtmlText(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .trim()
            .take(300)
    }

    private fun formatPubDate(rawDate: String): String {
        return try {
            if (rawDate.length >= 16) {
                rawDate.substring(0, 16)
            } else {
                rawDate
            }
        } catch (e: Exception) {
            rawDate
        }
    }

    private fun parseDurationToMs(durationStr: String): Long {
        if (durationStr.isBlank()) return 0L
        return try {
            val parts = durationStr.split(":")
            when (parts.size) {
                3 -> (parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()) * 1000L
                2 -> (parts[0].toLong() * 60 + parts[1].toLong()) * 1000L
                1 -> parts[0].toLong() * 1000L
                else -> 0L
            }
        } catch (e: Exception) {
            0L
        }
    }
}
