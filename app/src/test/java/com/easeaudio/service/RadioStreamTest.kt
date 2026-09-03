package com.easeaudio.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class RadioStreamTest {

    private fun isHlsStream(resolvedUrl: String): Boolean {
        val path = try { URI.create(resolvedUrl).path ?: "" } catch (_: Exception) { "" }
        return resolvedUrl.contains(".m3u8", ignoreCase = true) ||
                path.endsWith("/hls", ignoreCase = true) ||
                path.contains("/hls/", ignoreCase = true) ||
                path.contains("/zhls/", ignoreCase = true)
    }

    private fun rewriteStreamUrl(rawUrl: String, stationName: String): String {
        var currentUrl = rawUrl.trim()
        if (currentUrl.contains("audio-lss.vov.vn", ignoreCase = true)) {
            val vovMatch = Regex("""\b(vov\d+)\b""", RegexOption.IGNORE_CASE).find(currentUrl)
            if (vovMatch != null) {
                val channel = vovMatch.groupValues[1].lowercase()
                return "https://audio-lss.vov.vn/live/$channel.m3u8"
            }
        }
        if (currentUrl.contains("zmdcdn.me", ignoreCase = true) || currentUrl.contains("zmp3", ignoreCase = true)) {
            if (stationName.contains("xone", ignoreCase = true) || currentUrl.contains("e75e51f26db784e9dda6")) {
                return "https://stream.zeno.fm/dnukwf7q3a0uv"
            }
        }
        if (stationName.contains("xone fm", ignoreCase = true) &&
            (currentUrl.isBlank() || currentUrl.contains("zmdcdn.me", ignoreCase = true) || currentUrl.contains("zmp3", ignoreCase = true))
        ) {
            return "https://stream.zeno.fm/dnukwf7q3a0uv"
        }
        return currentUrl
    }

    @Test
    fun testKiisFmIsNotDetectedAsHls() {
        // KIIS FM stream URL on iHeartRadio infrastructure
        val kiisFmUrl = "https://stream.revma.ihrhls.com/zc185"
        assertFalse(
            "KIIS FM stream should NOT be treated as HLS even though host contains 'ihrhls'",
            isHlsStream(kiisFmUrl)
        )
    }

    @Test
    fun testZ100IsNotDetectedAsHls() {
        val z100Url = "https://stream.revma.ihrhls.com/zc1469"
        assertFalse(
            "Z100 stream on ihrhls.com should NOT be treated as HLS",
            isHlsStream(z100Url)
        )
    }

    @Test
    fun testRealHlsStreamsAreDetected() {
        assertTrue(isHlsStream("https://example.com/stream.m3u8"))
        assertTrue(isHlsStream("https://example.com/live/playlist.m3u8?token=123"))
        assertTrue(isHlsStream("https://example.com/hls/live"))
        assertTrue(isHlsStream("https://example.com/zhls/playback-realtime/audio/123/audio.m3u8"))
    }

    @Test
    fun testXoneFmDeadZingStreamIsRewrittenToWorkingLiveStream() {
        val deadZingUrl = "https://vnno-ne-2-tf-multi-playlist-zmp3.zmdcdn.me/Wbo0diyYozY/zhls/playback-realtime/audio/e75e51f26db784e9dda6/audio.m3u8"
        val rewritten = rewriteStreamUrl(deadZingUrl, "XONE FM")
        assertEquals("https://stream.zeno.fm/dnukwf7q3a0uv", rewritten)
    }

    @Test
    fun testVovStreamRewrite() {
        val oldVovUrl = "https://audio-lss.vov.vn/something/vov1/playlist.m3u8"
        val rewritten = rewriteStreamUrl(oldVovUrl, "VOV1")
        assertEquals("https://audio-lss.vov.vn/live/vov1.m3u8", rewritten)
    }
}
