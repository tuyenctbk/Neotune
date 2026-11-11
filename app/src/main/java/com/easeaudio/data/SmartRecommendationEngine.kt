package com.easeaudio.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector
import com.easeaudio.R
import java.util.Calendar

/**
 * Contextual time-of-day categories for smart zero-cost on-device recommendations.
 */
enum class TimeOfDayContext(
    val titleResId: Int,
    val subtitleResId: Int,
    val icon: ImageVector,
    val targetKeywords: List<String>
) {
    MORNING(
        titleResId = R.string.smart_picks_morning_title,
        subtitleResId = R.string.smart_picks_morning_desc,
        icon = Icons.Filled.Coffee,
        targetKeywords = listOf("news", "talk", "morning", "pop", "classic", "acoustic", "energy", "hits")
    ),
    AFTERNOON(
        titleResId = R.string.smart_picks_afternoon_title,
        subtitleResId = R.string.smart_picks_afternoon_desc,
        icon = Icons.Filled.LightMode,
        targetKeywords = listOf("lo-fi", "chill", "focus", "study", "jazz", "electronic", "work", "ambient", "indie")
    ),
    EVENING(
        titleResId = R.string.smart_picks_evening_title,
        subtitleResId = R.string.smart_picks_evening_desc,
        icon = Icons.Filled.WbTwilight,
        targetKeywords = listOf("chillout", "jazz", "lounge", "rock", "lo-fi", "groove", "sunset", "blues")
    ),
    NIGHT(
        titleResId = R.string.smart_picks_night_title,
        subtitleResId = R.string.smart_picks_night_desc,
        icon = Icons.Filled.Bedtime,
        targetKeywords = listOf("sleep", "ambient", "lo-fi", "meditation", "piano", "classical", "relax", "rain", "drone")
    );

    companion object {
        fun current(): TimeOfDayContext {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> MORNING
                in 12..17 -> AFTERNOON
                in 18..22 -> EVENING
                else -> NIGHT
            }
        }
    }
}

object SmartRecommendationEngine {

    /**
     * Scores and ranks stations based on:
     * 1. Time-of-day genre affinity (40 points)
     * 2. User favorites and genre history (30 points)
     * 3. Audio stream quality / bitrate (20 points)
     * 4. Station popularity / click count (10 points)
     */
    fun computeSmartPicks(
        availableStations: List<RadioStation>,
        favorites: List<RadioStation>,
        recentStations: List<RadioStation>,
        limit: Int = 18
    ): List<RadioStation> {
        if (availableStations.isEmpty()) return emptyList()

        val context = TimeOfDayContext.current()
        val favGenres = favorites.map { it.genre.lowercase() }.toSet()
        val favNames = favorites.map { it.name.lowercase() }.toSet()

        return availableStations
            .filter { it.streamUrl.isNotBlank() }
            .distinctBy { it.id }
            .map { station ->
                var score = 0
                val genreLower = station.genre.lowercase()
                val nameLower = station.name.lowercase()

                // 1. Time-of-day contextual alignment
                for (kw in context.targetKeywords) {
                    if (genreLower.contains(kw) || nameLower.contains(kw)) {
                        score += 35
                        break
                    }
                }

                // 2. User preference alignment
                if (favGenres.any { genreLower.contains(it) }) {
                    score += 25
                }
                if (favNames.contains(nameLower)) {
                    score += 15
                }

                // 3. Audio quality boost
                val bitrateNum = station.bitrate.filter { it.isDigit() }.toIntOrNull() ?: 128
                if (bitrateNum >= 256) score += 20
                else if (bitrateNum >= 128) score += 10

                // 4. User listening affinity
                if (station.playCount > 0) score += 10

                station to score
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)
    }
}
