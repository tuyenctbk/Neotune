package com.easeaudio.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StationFilterConfig(
    val filterAdultContent: Boolean = true,
    val filterPoliticsContent: Boolean = false,
    val filterReligiousContent: Boolean = false,
    val filterBrokenStreams: Boolean = true,
    val customKeywords: Set<String> = emptySet()
)

class FilterAndBlockManager private constructor(context: Context) {

    companion object {
        private const val TAG = "FilterAndBlockManager"
        private const val PREFS_NAME = "neotune_blocklist_prefs"
        private const val KEY_BLOCKED_IDS = "blocked_station_ids"
        private const val KEY_FILTER_ADULT = "filter_adult_content"
        private const val KEY_FILTER_POLITICS = "filter_politics_content"
        private const val KEY_FILTER_RELIGIOUS = "filter_religious_content"
        private const val KEY_FILTER_BROKEN = "filter_broken_streams"
        private const val KEY_CUSTOM_KEYWORDS = "custom_filter_keywords"

        private val ADULT_KEYWORDS = listOf("adult", "nsfw", "18+", "erotic", "xxx", "porn", "explicit")
        private val POLITICS_KEYWORDS = listOf("politics", "political", "election", "parliament", "democrat", "republican")
        private val RELIGION_KEYWORDS = listOf("religion", "religious", "church", "gospel", "christian", "islam", "quran", "sermon")

        @Volatile
        private var instance: FilterAndBlockManager? = null

        fun getInstance(context: Context): FilterAndBlockManager {
            return instance ?: synchronized(this) {
                instance ?: FilterAndBlockManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _blockedStationIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedStationIds: StateFlow<Set<String>> = _blockedStationIds.asStateFlow()

    private val _filterConfig = MutableStateFlow(StationFilterConfig())
    val filterConfig: StateFlow<StationFilterConfig> = _filterConfig.asStateFlow()

    init {
        loadBlockedStations()
        loadFilterConfig()
    }

    private fun loadBlockedStations() {
        val savedSet = prefs.getStringSet(KEY_BLOCKED_IDS, emptySet()) ?: emptySet()
        _blockedStationIds.value = HashSet(savedSet)
    }

    private fun loadFilterConfig() {
        val adult = prefs.getBoolean(KEY_FILTER_ADULT, true)
        val politics = prefs.getBoolean(KEY_FILTER_POLITICS, false)
        val religious = prefs.getBoolean(KEY_FILTER_RELIGIOUS, false)
        val broken = prefs.getBoolean(KEY_FILTER_BROKEN, true)
        val keywords = prefs.getStringSet(KEY_CUSTOM_KEYWORDS, emptySet()) ?: emptySet()

        _filterConfig.value = StationFilterConfig(
            filterAdultContent = adult,
            filterPoliticsContent = politics,
            filterReligiousContent = religious,
            filterBrokenStreams = broken,
            customKeywords = HashSet(keywords)
        )
    }

    fun blockStation(stationId: String) {
        val current = _blockedStationIds.value.toMutableSet()
        current.add(stationId)
        _blockedStationIds.value = current
        prefs.edit().putStringSet(KEY_BLOCKED_IDS, current).apply()
        Log.i(TAG, "Station $stationId added to block list.")
    }

    fun unblockStation(stationId: String) {
        val current = _blockedStationIds.value.toMutableSet()
        current.remove(stationId)
        _blockedStationIds.value = current
        prefs.edit().putStringSet(KEY_BLOCKED_IDS, current).apply()
        Log.i(TAG, "Station $stationId removed from block list.")
    }

    fun clearBlockList() {
        _blockedStationIds.value = emptySet()
        prefs.edit().remove(KEY_BLOCKED_IDS).apply()
    }

    fun setFilterAdultContent(enabled: Boolean) {
        _filterConfig.value = _filterConfig.value.copy(filterAdultContent = enabled)
        prefs.edit().putBoolean(KEY_FILTER_ADULT, enabled).apply()
    }

    fun setFilterPoliticsContent(enabled: Boolean) {
        _filterConfig.value = _filterConfig.value.copy(filterPoliticsContent = enabled)
        prefs.edit().putBoolean(KEY_FILTER_POLITICS, enabled).apply()
    }

    fun setFilterReligiousContent(enabled: Boolean) {
        _filterConfig.value = _filterConfig.value.copy(filterReligiousContent = enabled)
        prefs.edit().putBoolean(KEY_FILTER_RELIGIOUS, enabled).apply()
    }

    fun setFilterBrokenStreams(enabled: Boolean) {
        _filterConfig.value = _filterConfig.value.copy(filterBrokenStreams = enabled)
        prefs.edit().putBoolean(KEY_FILTER_BROKEN, enabled).apply()
    }

    fun addCustomKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        val current = _filterConfig.value.customKeywords.toMutableSet()
        current.add(trimmed)
        _filterConfig.value = _filterConfig.value.copy(customKeywords = current)
        prefs.edit().putStringSet(KEY_CUSTOM_KEYWORDS, current).apply()
    }

    fun removeCustomKeyword(keyword: String) {
        val current = _filterConfig.value.customKeywords.toMutableSet()
        current.remove(keyword)
        _filterConfig.value = _filterConfig.value.copy(customKeywords = current)
        prefs.edit().putStringSet(KEY_CUSTOM_KEYWORDS, current).apply()
    }

    fun clearCustomKeywords() {
        _filterConfig.value = _filterConfig.value.copy(customKeywords = emptySet())
        prefs.edit().remove(KEY_CUSTOM_KEYWORDS).apply()
    }

    /**
     * Checks if a station should be allowed (returns false if station is blocked by user or matches any filter)
     */
    fun shouldIncludeStation(station: RadioStation): Boolean {
        // 1. User Block List check
        if (_blockedStationIds.value.contains(station.id)) {
            return false
        }

        val config = _filterConfig.value
        val combinedText = "${station.name} ${station.genre} ${station.country}".lowercase()

        // 2. Adult / NSFW Content Filter
        if (config.filterAdultContent) {
            if (ADULT_KEYWORDS.any { combinedText.contains(it) }) {
                return false
            }
        }

        // 3. Politics Filter
        if (config.filterPoliticsContent) {
            if (POLITICS_KEYWORDS.any { combinedText.contains(it) }) {
                return false
            }
        }

        // 4. Religion Filter
        if (config.filterReligiousContent) {
            if (RELIGION_KEYWORDS.any { combinedText.contains(it) }) {
                return false
            }
        }

        // 5. Custom Banned Keywords
        if (config.customKeywords.isNotEmpty()) {
            if (config.customKeywords.any { combinedText.contains(it.lowercase()) }) {
                return false
            }
        }

        return true
    }
}
