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
        var connection: HttpURLConnection? = null
        try {
            Log.d(TAG, "Fetching episode RSS feed from: $feedUrl")
            var currentUrl = feedUrl
            var redirectCount = 0
            var finalStream: java.io.InputStream? = null
            var isGzip = false

            while (redirectCount < 5) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    (this as? javax.net.ssl.HttpsURLConnection)?.apply {
                        sslSocketFactory = com.easeaudio.util.NetworkSecurityHelper.sslSocketFactory
                        hostnameVerifier = com.easeaudio.util.NetworkSecurityHelper.hostnameVerifier
                    }
                    requestMethod = "GET"
                    connectTimeout = 12000
                    readTimeout = 12000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 NeoTune/1.0")
                    setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, application/atom+xml, */*")
                    setRequestProperty("Accept-Encoding", "gzip, deflate")
                }
                connection = conn

                val responseCode = conn.responseCode
                if (responseCode in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                            location
                        } else {
                            URL(URL(currentUrl), location).toString()
                        }
                        redirectCount++
                        continue
                    }
                }

                if (responseCode in 200..299) {
                    isGzip = conn.contentEncoding?.contains("gzip", ignoreCase = true) == true
                    finalStream = conn.inputStream
                    break
                } else {
                    Log.w(TAG, "HTTP $responseCode while requesting RSS feed at $currentUrl")
                    break
                }
            }

            if (finalStream != null) {
                val effectiveStream = if (isGzip) {
                    java.util.zip.GZIPInputStream(finalStream)
                } else {
                    finalStream
                }

                effectiveStream.use { rawStream ->
                    val parser = Xml.newPullParser().apply {
                        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                        setInput(rawStream, null)
                    }

                    var eventType = parser.eventType
                    var insideItem = false
                    var currentTitle = ""
                    var currentDesc = ""
                    var currentPubDate = ""
                    var currentAudioUrl = ""
                    var currentDurationMs = 0L
                    var currentArtwork = show.imageUrl
                    var currentChaptersUrl = ""
                    var currentTranscriptUrl = ""
                    val currentEmbeddedChapters = mutableListOf<EpisodeChapter>()

                    while (eventType != XmlPullParser.END_DOCUMENT && episodes.size < maxEpisodes) {
                        val tagName = parser.name ?: ""
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
                                    currentChaptersUrl = ""
                                    currentTranscriptUrl = ""
                                    currentEmbeddedChapters.clear()
                                } else if (insideItem) {
                                    val cleanTag = tagName.substringAfter(":").lowercase()
                                    when {
                                        cleanTag == "title" || tagName.equals("title", ignoreCase = true) -> {
                                            currentTitle = safeReadText(parser).trim()
                                        }
                                        cleanTag == "description" || cleanTag == "summary" || cleanTag == "encoded" -> {
                                            if (currentDesc.isBlank()) {
                                                currentDesc = cleanHtmlText(safeReadText(parser))
                                            }
                                        }
                                        cleanTag == "pubdate" || tagName.equals("pubDate", ignoreCase = true) -> {
                                            currentPubDate = formatPubDate(safeReadText(parser).trim())
                                        }
                                        cleanTag == "duration" -> {
                                            currentDurationMs = parseDurationToMs(safeReadText(parser).trim())
                                        }
                                        cleanTag == "chapters" -> {
                                            // Podcasting 2.0 <podcast:chapters url="..." type="application/json"/>
                                            for (i in 0 until parser.attributeCount) {
                                                val attrName = parser.getAttributeName(i) ?: ""
                                                if (attrName.equals("url", ignoreCase = true)) {
                                                    currentChaptersUrl = parser.getAttributeValue(i) ?: ""
                                                }
                                            }
                                        }
                                        cleanTag == "transcript" -> {
                                            // Podcasting 2.0 <podcast:transcript url="..." type="..."/>
                                            for (i in 0 until parser.attributeCount) {
                                                val attrName = parser.getAttributeName(i) ?: ""
                                                if (attrName.equals("url", ignoreCase = true)) {
                                                    currentTranscriptUrl = parser.getAttributeValue(i) ?: ""
                                                }
                                            }
                                        }
                                        cleanTag == "chapter" -> {
                                            // Podlove Simple Chapters <psc:chapter start="..." title="..."/>
                                            var startVal = ""
                                            var titleVal = ""
                                            var imgVal: String? = null
                                            for (i in 0 until parser.attributeCount) {
                                                val attrName = parser.getAttributeName(i) ?: ""
                                                when {
                                                    attrName.equals("start", ignoreCase = true) -> startVal = parser.getAttributeValue(i) ?: ""
                                                    attrName.equals("title", ignoreCase = true) -> titleVal = parser.getAttributeValue(i) ?: ""
                                                    attrName.equals("image", ignoreCase = true) || attrName.equals("href", ignoreCase = true) -> imgVal = parser.getAttributeValue(i)
                                                }
                                            }
                                            if (titleVal.isNotBlank()) {
                                                val startMs = parseDurationToMs(startVal)
                                                currentEmbeddedChapters.add(
                                                    EpisodeChapter(
                                                        title = titleVal,
                                                        startTimeMs = startMs,
                                                        durationText = formatMins(startMs),
                                                        imageUrl = imgVal
                                                    )
                                                )
                                            }
                                        }
                                        cleanTag == "enclosure" || cleanTag == "content" -> {
                                            var urlAttr = ""
                                            var typeAttr = ""
                                            for (i in 0 until parser.attributeCount) {
                                                val attrName = parser.getAttributeName(i) ?: ""
                                                if (attrName.equals("url", ignoreCase = true) || attrName.equals("src", ignoreCase = true)) {
                                                    urlAttr = parser.getAttributeValue(i) ?: ""
                                                } else if (attrName.equals("type", ignoreCase = true)) {
                                                    typeAttr = parser.getAttributeValue(i) ?: ""
                                                }
                                            }
                                            if (urlAttr.isNotBlank()) {
                                                val isAudioType = typeAttr.isBlank() || typeAttr.contains("audio", ignoreCase = true) || typeAttr.contains("mpeg", ignoreCase = true) || typeAttr.contains("mp3", ignoreCase = true) || typeAttr.contains("aac", ignoreCase = true) || typeAttr.contains("m4a", ignoreCase = true)
                                                val isAudioUrl = urlAttr.contains(".mp3", ignoreCase = true) || urlAttr.contains(".m4a", ignoreCase = true) || urlAttr.contains(".aac", ignoreCase = true) || urlAttr.contains(".ogg", ignoreCase = true) || urlAttr.contains("audio", ignoreCase = true) || urlAttr.contains("episode", ignoreCase = true) || urlAttr.contains("podcast", ignoreCase = true)
                                                if ((isAudioType || isAudioUrl) && currentAudioUrl.isBlank()) {
                                                    currentAudioUrl = urlAttr.trim().replace("&amp;", "&")
                                                }
                                            }
                                        }
                                        cleanTag == "image" -> {
                                            var hrefAttr = ""
                                            for (i in 0 until parser.attributeCount) {
                                                val attrName = parser.getAttributeName(i) ?: ""
                                                if (attrName.equals("href", ignoreCase = true) || attrName.equals("url", ignoreCase = true)) {
                                                    hrefAttr = parser.getAttributeValue(i) ?: ""
                                                }
                                            }
                                            if (hrefAttr.isNotBlank()) {
                                                currentArtwork = hrefAttr.trim()
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
                                        val initialChapters = if (currentEmbeddedChapters.isNotEmpty()) {
                                            currentEmbeddedChapters
                                        } else {
                                            parseChaptersFromDescription(currentDesc)
                                        }
                                        episodes.add(
                                            PodcastEpisode(
                                                id = episodeId,
                                                showId = show.id,
                                                title = currentTitle,
                                                description = currentDesc,
                                                audioUrl = currentAudioUrl,
                                                pubDate = currentPubDate,
                                                durationMs = currentDurationMs,
                                                artworkUrl = currentArtwork.ifBlank { show.imageUrl },
                                                chaptersUrl = currentChaptersUrl.ifBlank { null },
                                                transcriptUrl = currentTranscriptUrl.ifBlank { null },
                                                chapters = initialChapters
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        eventType = try { parser.next() } catch (e: Exception) { XmlPullParser.END_DOCUMENT }
                    }
                }
                Log.i(TAG, "Successfully parsed ${episodes.size} episodes for show ${show.name}")
                if (episodes.isNotEmpty()) {
                    episodeCache[feedUrl] = episodes
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS feed for ${show.name}: ${e.message}", e)
        } finally {
            connection?.disconnect()
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

    private fun safeReadText(parser: XmlPullParser): String {
        return try {
            var result = ""
            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.text ?: ""
                parser.nextTag()
            }
            result
        } catch (e: Exception) {
            try { parser.nextText() ?: "" } catch (e2: Exception) { "" }
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

    fun parseChaptersFromDescription(description: String): List<EpisodeChapter> {
        val pattern = Regex("""(?:(?:(\d{1,2}):)?(\d{2}):(\d{2}))\s*[-–:]?\s*(.+)""")
        val chapters = mutableListOf<EpisodeChapter>()
        description.lines().forEach { line ->
            val match = pattern.find(line.trim())
            if (match != null) {
                val p1 = match.groupValues[1]
                val p2 = match.groupValues[2]
                val p3 = match.groupValues[3]
                val title = match.groupValues[4].trim()
                val startMs = if (p1.isNotBlank()) {
                    (p1.toLong() * 3600 + p2.toLong() * 60 + p3.toLong()) * 1000L
                } else {
                    (p2.toLong() * 60 + p3.toLong()) * 1000L
                }
                if (title.isNotBlank()) {
                    chapters.add(
                        EpisodeChapter(
                            title = title,
                            startTimeMs = startMs,
                            durationText = formatMins(startMs)
                        )
                    )
                }
            }
        }
        return chapters
    }

    private fun formatMins(ms: Long): String {
        val totalSecs = ms / 1000L
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    suspend fun fetchChaptersForEpisode(episode: PodcastEpisode): List<EpisodeChapter> = withContext(Dispatchers.IO) {
        if (episode.chapters.isNotEmpty()) return@withContext episode.chapters
        val urlStr = episode.chaptersUrl
        if (!urlStr.isNullOrBlank()) {
            try {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "NeoTune/4.4.3")
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(text)
                    val chaptersArray = json.optJSONArray("chapters")
                    if (chaptersArray != null && chaptersArray.length() > 0) {
                        val parsed = mutableListOf<EpisodeChapter>()
                        for (i in 0 until chaptersArray.length()) {
                            val ch = chaptersArray.getJSONObject(i)
                            val startTimeSec = ch.optDouble("startTime", 0.0)
                            val title = ch.optString("title", "Chapter ${i + 1}")
                            val img = ch.optString("img", "").ifBlank { null }
                            val linkUrl = ch.optString("url", "").ifBlank { null }
                            val startMs = (startTimeSec * 1000.0).toLong()
                            parsed.add(
                                EpisodeChapter(
                                    title = title,
                                    startTimeMs = startMs,
                                    durationText = formatMins(startMs),
                                    imageUrl = img,
                                    url = linkUrl
                                )
                            )
                        }
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load Podcasting 2.0 chapters JSON from $urlStr: ${e.message}")
            }
        }
        parseChaptersFromDescription(episode.description)
    }

    suspend fun fetchTranscriptForEpisode(episode: PodcastEpisode): String? = withContext(Dispatchers.IO) {
        val urlStr = episode.transcriptUrl ?: return@withContext null
        try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "NeoTune/4.4.3")
            }
            if (conn.responseCode == 200) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val cleaned = raw.lines()
                    .filterNot { it.startsWith("WEBVTT") || it.contains("-->") || it.trim().matches(Regex("^\\d+$")) }
                    .joinToString("\n")
                    .trim()
                return@withContext cleaned.ifBlank { raw }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load Podcasting 2.0 transcript from $urlStr: ${e.message}")
        }
        null
    }
}
