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
                (this as? javax.net.ssl.HttpsURLConnection)?.apply {
                    sslSocketFactory = com.easeaudio.util.NetworkSecurityHelper.sslSocketFactory
                    hostnameVerifier = com.easeaudio.util.NetworkSecurityHelper.hostnameVerifier
                }
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
                val resultsArray = jsonObject.optJSONArray("results")
                if (resultsArray != null && resultsArray.length() > 0) {
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
                        val itemCountry = item.optString("country", "USA").trim()

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
                                        country = itemCountry,
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
                                        country = itemCountry,
                                        streamUrl = streamUrl,
                                        imageUrl = artworkUrl,
                                        bitrate = "Podcast",
                                        codec = "AAC/MP3"
                                    )
                                )
                            }
                        }
                    }
                }
                
                if (podcasts.isNotEmpty()) {
                    Log.i(TAG, "Successfully fetched from iTunes for '$searchTerm': ${podcasts.size} podcasts, ${liveRadio.size} live radio stations")
                    PodcastCacheManager.put(cacheKey, podcasts, liveRadio)
                }
            } else {
                connection.disconnect()
                Log.w(TAG, "iTunes API HTTP failure code $responseCode for URL: $urlString")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying iTunes Podcast API: ${e.message}", e)
        }

        // If no podcasts fetched from network, provide rich curated fallback
        if (podcasts.isEmpty()) {
            val fallback = getFallbackPodcasts(searchQuery, genre)
            podcasts.addAll(fallback)
            Log.i(TAG, "Using ${fallback.size} curated fallback podcasts for '$genre' / '$searchQuery'")
        }

        return@withContext iTunesFetchResult(podcasts, liveRadio)
    }

    val fallbackCuratedPodcasts: List<RadioStation> = listOf(
        RadioStation(
            id = "itunes_podcast_huberman_lab",
            name = "Huberman Lab",
            genre = "Health & Fitness • Scicomm Media",
            country = "USA",
            streamUrl = "https://feeds.megaphone.fm/hubermanlab",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts116/v4/36/53/78/3653787a-b9c2-bb52-f67e-79012a68a5bc/mza_10793617300762923594.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_the_daily",
            name = "The Daily",
            genre = "News • The New York Times",
            country = "USA",
            streamUrl = "https://feeds.simplecast.com/54nAGcIl",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/44/72/71/44727196-8575-cf6b-3129-ae78f6575193/mza_15509930491950294157.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_behind_the_bastards",
            name = "Behind the Bastards",
            genre = "Society & Culture • Cool Zone Media",
            country = "USA",
            streamUrl = "https://www.omnycontent.com/d/playlist/e73c998e-6e60-432f-8610-ae210140c5b1/e5f91208-cc7e-4726-a312-ae280140ad11/d64f756d-6d5e-4fae-b24f-ae280140ad36/podcast.rss",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts115/v4/65/d6/3b/65d63b22-8094-1a83-1e42-70b1cb3b3208/mza_16377727173151815147.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_stuff_you_should_know",
            name = "Stuff You Should Know",
            genre = "Society & Culture • iHeartPodcasts",
            country = "USA",
            streamUrl = "https://www.omnycontent.com/d/playlist/e73c998e-6e60-432f-8610-ae210140c5b1/aaea4e69-e6ee-4dd5-98e2-ae3200618037/8009a7b7-5426-4fa2-9f37-ae320061804f/podcast.rss",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts115/v4/37/f3/09/37f309aa-67c2-19c2-faea-714a51eec86c/mza_5439401777085731215.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_waveform",
            name = "Waveform: The MKBHD Podcast",
            genre = "Technology • Studio71",
            country = "USA",
            streamUrl = "https://feeds.megaphone.fm/waveform",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/c6/3e/dc/c63edc28-9418-f2b1-561b-93ff519aa1ba/mza_12239458284566373721.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_lex_fridman",
            name = "Lex Fridman Podcast",
            genre = "Technology • Lex Fridman",
            country = "USA",
            streamUrl = "https://lexfridman.com/feed/podcast/",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/91/97/99/9197992a-3305-64f3-8b77-cfb55462cf69/mza_12411993214532296766.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_hard_fork",
            name = "Hard Fork",
            genre = "Technology • The New York Times",
            country = "USA",
            streamUrl = "https://feeds.simplecast.com/2s_w0iZq",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts112/v4/d9/39/10/d939103e-ecb4-6a98-8422-55db6f5825ee/mza_14620857321557008169.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_crime_junkie",
            name = "Crime Junkie",
            genre = "True Crime • audiochuck",
            country = "USA",
            streamUrl = "https://feeds.simplecast.com/qm_9xx0g",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts115/v4/71/c5/4b/71c54b6b-c743-1628-98e3-0c460a5e8c18/mza_13203923363363318991.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_planet_money",
            name = "Planet Money",
            genre = "Business • NPR",
            country = "USA",
            streamUrl = "https://feeds.npr.org/510289/podcast.xml",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts115/v4/80/7e/cb/807ecb87-7da6-38d5-1b48-735f49d21c5b/mza_17871615174620138547.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_freakonomics",
            name = "Freakonomics Radio",
            genre = "Business • Freakonomics Radio + Stitcher",
            country = "USA",
            streamUrl = "https://feeds.simplecast.com/y1G3sVoa",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts115/v4/ca/84/c8/ca84c8a2-2e6f-cbca-d380-4cf8e51532c2/mza_11677335122709795078.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_conan_obrien",
            name = "Conan O'Brien Needs A Friend",
            genre = "Comedy • Team Coco & Earwolf",
            country = "USA",
            streamUrl = "https://feeds.simplecast.com/dHoohavC",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/58/b6/2d/58b62d31-41ee-d419-74d3-13834371ff15/mza_15516946660144673641.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_smartless",
            name = "SmartLess",
            genre = "Comedy • Jason Bateman, Sean Hayes, Will Arnett",
            country = "USA",
            streamUrl = "https://rss.art19.com/smartless",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts115/v4/1b/4f/2e/1b4f2e96-a979-3732-e092-212a95c96ba3/mza_4454955745196568478.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_radiolab",
            name = "Radiolab",
            genre = "Science • WNYC Studios",
            country = "USA",
            streamUrl = "https://feeds.simplecast.com/nvdq_w7S",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/80/7e/61/807e61ef-ca09-fc26-5b72-f67210214a1a/mza_11956627065961633519.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_science_vs",
            name = "Science Vs",
            genre = "Science • Spotify Studios",
            country = "USA",
            streamUrl = "https://feeds.megaphone.fm/sciencevs",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/21/2e/29/212e2933-4fec-d830-ecfc-f5aeb00cb75d/mza_14691456209538357121.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_startalk",
            name = "StarTalk Radio",
            genre = "Science • Neil deGrasse Tyson",
            country = "USA",
            streamUrl = "https://feeds.megaphone.fm/startalkradio",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/36/6a/c7/366ac778-d5e1-cfeb-90b5-779872589578/mza_16694695029367468641.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        ),
        RadioStation(
            id = "itunes_podcast_hidden_brain",
            name = "Hidden Brain",
            genre = "Science • Hidden Brain Media",
            country = "USA",
            streamUrl = "https://feeds.simplecast.com/kWuxvDfy",
            imageUrl = "https://is1-ssl.mzstatic.com/image/thumb/Podcasts125/v4/aa/62/ca/aa62caa1-ebc9-7e43-e69d-c7cb1d7e2e5c/mza_17208154694468508973.jpg/600x600bb.jpg",
            bitrate = "Podcast",
            codec = "AAC/MP3"
        )
    )

    fun getFallbackPodcasts(searchQuery: String, genre: String): List<RadioStation> {
        val q = searchQuery.trim().lowercase()
        val g = genre.trim().lowercase()
        val filtered = fallbackCuratedPodcasts.filter { podcast ->
            val matchQ = q.isBlank() || podcast.name.lowercase().contains(q) || podcast.genre.lowercase().contains(q)
            val matchG = g.isBlank() || g == "all" || g == "podcast" || g == "podcasts" || podcast.genre.lowercase().contains(g) ||
                    (g.contains("tech") && podcast.genre.lowercase().contains("tech")) ||
                    (g.contains("crime") && podcast.genre.lowercase().contains("crime")) ||
                    (g.contains("business") && (podcast.genre.lowercase().contains("business") || podcast.genre.lowercase().contains("money"))) ||
                    (g.contains("comedy") && podcast.genre.lowercase().contains("comedy")) ||
                    (g.contains("health") && (podcast.genre.lowercase().contains("health") || podcast.genre.lowercase().contains("fitness"))) ||
                    (g.contains("society") && (podcast.genre.lowercase().contains("society") || podcast.genre.lowercase().contains("culture"))) ||
                    (g.contains("science") && podcast.genre.lowercase().contains("science")) ||
                    (g.contains("news") && podcast.genre.lowercase().contains("news"))
            matchQ && matchG
        }
        return if (filtered.isNotEmpty()) filtered else fallbackCuratedPodcasts
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
