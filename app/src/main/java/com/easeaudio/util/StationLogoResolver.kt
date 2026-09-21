package com.easeaudio.util

import java.net.URI
import java.net.URLEncoder

object StationLogoResolver {

    private val KNOWN_STATION_DOMAINS = listOf(
        "cnn" to "https://www.cnn.com",
        "rai radio 1" to "https://www.raiplaysound.it",
        "rai radio 2" to "https://www.raiplaysound.it",
        "rai radio 3" to "https://www.raiplaysound.it",
        "rai" to "https://www.raiplaysound.it",
        "europe 1" to "https://www.europe1.fr",
        "europe 2" to "https://www.europe2.fr",
        "bbc world service" to "https://www.bbc.co.uk",
        "bbc radio" to "https://www.bbc.co.uk",
        "bbc" to "https://www.bbc.co.uk",
        "npr" to "https://www.npr.org",
        "fox news" to "https://radio.foxnews.com",
        "france info" to "https://www.francetvinfo.fr",
        "france inter" to "https://www.radiofrance.fr/franceinter",
        "france culture" to "https://www.radiofrance.fr/franceculture",
        "france musique" to "https://www.radiofrance.fr/francemusique",
        "fip" to "https://www.radiofrance.fr/fip",
        "cadena ser" to "https://cadenaser.com",
        "los 40" to "https://los40.com",
        "cope" to "https://www.cope.es",
        "onda cero" to "https://www.ondacero.es",
        "rne" to "https://www.rtve.es/rne",
        "lbc" to "https://www.lbc.co.uk",
        "capital fm" to "https://www.capitalfm.com",
        "heart" to "https://www.heart.co.uk",
        "classic fm" to "https://www.classicfm.com",
        "virgin radio" to "https://virginradio.co.uk",
        "kiss fm" to "https://www.kissfm.es",
        "deutschlandfunk" to "https://www.deutschlandfunk.de",
        "swr3" to "https://www.swr3.de",
        "wdr" to "https://www1.wdr.de",
        "ndr" to "https://www.ndr.de",
        "antenne bayern" to "https://www.antenne.de",
        "nrj" to "https://www.nrj.fr",
        "rtl" to "https://www.rtl.fr",
        "rfi" to "https://www.rfi.fr",
        "kexp" to "https://www.kexp.org",
        "kcrw" to "https://www.kcrw.com",
        "wnyc" to "https://www.wnyc.org",
        "triple j" to "https://www.abc.net.au/triplej",
        "bloomberg" to "https://www.bloomberg.com",
        "msnbc" to "https://www.msnbc.com",
        "cbs news" to "https://www.cbsnews.com",
        "somafm" to "https://somafm.com"
    )

    private val CLOUDFLARE_BLOCKED_DOMAINS = listOf(
        "europe1.fr",
        "virginradio.fr"
    )

    private val DOMAIN_REGEX = Regex(
        """\b([a-zA-Z0-9-]+\.(?:com|fm|org|net|fr|de|it|uk|es|nl|ca|be|ch|at|io|tv|radio))\b""",
        RegexOption.IGNORE_CASE
    )

    fun getGoogleFaviconUrl(urlOrDomain: String, size: Int = 128): String {
        val cleanUrl = when {
            urlOrDomain.startsWith("http://") || urlOrDomain.startsWith("https://") -> urlOrDomain
            else -> "https://$urlOrDomain"
        }
        val encoded = try {
            URLEncoder.encode(cleanUrl, "UTF-8")
        } catch (e: Exception) {
            cleanUrl
        }
        return "https://t1.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=$encoded&size=$size"
    }

    fun isKnownBroadcaster(name: String): Boolean {
        val lower = name.trim().lowercase()
        return KNOWN_STATION_DOMAINS.any { (key, _) ->
            lower == key || lower.startsWith("$key ") || lower.contains(key)
        }
    }

    fun resolveStationLogo(
        name: String,
        favicon: String,
        homepage: String = "",
        tags: String = ""
    ): String {
        val cleanFavicon = favicon.trim()
        val cleanHomepage = homepage.trim()
        val lowerName = name.trim().lowercase()

        // 0. Dedicated Radio Paradise High-Res Audiophile Channels
        if (lowerName.contains("radio paradise") || cleanFavicon.contains("radioparadise.com", ignoreCase = true)) {
            return when {
                lowerName.contains("mellow") -> "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=600&q=80"
                lowerName.contains("rock") -> "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?auto=format&fit=crop&w=600&q=80"
                lowerName.contains("global") || lowerName.contains("world") -> "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80"
                else -> "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80"
            }
        }

        // 1. Check known broadcaster mappings
        for ((key, domain) in KNOWN_STATION_DOMAINS) {
            if (lowerName == key || lowerName.startsWith("$key ") || lowerName.contains(key)) {
                val isIco = cleanFavicon.contains(".ico", ignoreCase = true)
                val isBlocked = CLOUDFLARE_BLOCKED_DOMAINS.any { cleanFavicon.contains(it, ignoreCase = true) }
                if (cleanFavicon.isBlank() || isIco || isBlocked) {
                    return getGoogleFaviconUrl(domain)
                }
            }
        }

        // 2. If favicon is provided and valid (not an .ico and not blocked)
        if (cleanFavicon.startsWith("http://") || cleanFavicon.startsWith("https://")) {
            val isIco = cleanFavicon.contains(".ico", ignoreCase = true)
            val isBlocked = CLOUDFLARE_BLOCKED_DOMAINS.any { cleanFavicon.contains(it, ignoreCase = true) }
            if (!isIco && !isBlocked) {
                return cleanFavicon
            }
            // If it's an .ico or blocked, resolve via Google Favicon CDN
            if (cleanHomepage.startsWith("http://") || cleanHomepage.startsWith("https://")) {
                return getGoogleFaviconUrl(cleanHomepage)
            }
            val domainFromFavicon = extractDomain(cleanFavicon)
            if (domainFromFavicon.isNotBlank()) {
                return getGoogleFaviconUrl(domainFromFavicon)
            }
        }

        // 3. Favicon is blank or invalid, but homepage is available
        if (cleanHomepage.startsWith("http://") || cleanHomepage.startsWith("https://")) {
            return getGoogleFaviconUrl(cleanHomepage)
        }

        // 4. Try to find a domain in the station name
        val domainInName = extractDomainFromStationName(name)
        if (domainInName != null) {
            return getGoogleFaviconUrl(domainInName)
        }

        // 5. Fallback to empty so StationMonogramAvatar renders custom monogram avatar
        return ""
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = URI(url)
            val host = uri.host ?: ""
            if (host.isNotBlank()) "${uri.scheme ?: "https"}://$host" else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractDomainFromStationName(name: String): String? {
        val match = DOMAIN_REGEX.find(name)
        return match?.value
    }

    fun getRandomDefaultImage(tags: String): String {
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
