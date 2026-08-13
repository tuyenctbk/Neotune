package com.easeaudio.data

import android.content.Context
import android.util.Log

object PodcastCacheManager {

    private const val TAG = "PodcastCacheManager"
    private const val CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour TTL

    // BUG-9 fix: plain mutableMapOf is not thread-safe; concurrent access from
    // multiple Dispatchers.IO coroutines can cause ConcurrentModificationException.
    private val memoryCache = java.util.concurrent.ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val timestamp: Long,
        val podcasts: List<RadioStation>,
        val radioStations: List<RadioStation>
    )

    fun get(key: String): Pair<List<RadioStation>, List<RadioStation>>? {
        val entry = memoryCache[key] ?: return null
        val age = System.currentTimeMillis() - entry.timestamp
        if (age > CACHE_TTL_MS) {
            memoryCache.remove(key)
            Log.d(TAG, "Cache expired for key: $key")
            return null
        }
        Log.d(TAG, "Cache hit for key: $key (podcasts: ${entry.podcasts.size}, radio: ${entry.radioStations.size})")
        return Pair(entry.podcasts, entry.radioStations)
    }

    fun put(key: String, podcasts: List<RadioStation>, radioStations: List<RadioStation>) {
        memoryCache[key] = CacheEntry(
            timestamp = System.currentTimeMillis(),
            podcasts = podcasts,
            radioStations = radioStations
        )
        Log.d(TAG, "Cached entry for key: $key (podcasts: ${podcasts.size}, radio: ${radioStations.size})")
    }

    fun clear() {
        memoryCache.clear()
    }
}
