package com.easeaudio.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioComfortManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("audio_comfort_prefs", Context.MODE_PRIVATE)

    private val _isVolumeSafetyEnabled = MutableStateFlow(prefs.getBoolean(KEY_VOLUME_SAFETY, false))
    val isVolumeSafetyEnabled: StateFlow<Boolean> = _isVolumeSafetyEnabled.asStateFlow()

    private val _isNightAudioModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_NIGHT_MODE, false))
    val isNightAudioModeEnabled: StateFlow<Boolean> = _isNightAudioModeEnabled.asStateFlow()

    // Listening streak and habit stats (100% on-device)
    private val _todayListeningMinutes = MutableStateFlow(prefs.getInt(getTodayKey(), 0))
    val todayListeningMinutes: StateFlow<Int> = _todayListeningMinutes.asStateFlow()

    private val _currentStreakDays = MutableStateFlow(prefs.getInt(KEY_STREAK_DAYS, 1))
    val currentStreakDays: StateFlow<Int> = _currentStreakDays.asStateFlow()

    fun setVolumeSafetyEnabled(enabled: Boolean) {
        _isVolumeSafetyEnabled.value = enabled
        prefs.edit().putBoolean(KEY_VOLUME_SAFETY, enabled).apply()
    }

    fun setNightAudioModeEnabled(enabled: Boolean) {
        _isNightAudioModeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_NIGHT_MODE, enabled).apply()
    }

    fun addListeningTime(seconds: Int) {
        if (seconds <= 0) return
        val todayKey = getTodayKey()
        val currentSeconds = prefs.getInt(todayKey + "_sec", 0) + seconds
        val currentMinutes = currentSeconds / 60
        
        prefs.edit()
            .putInt(todayKey + "_sec", currentSeconds)
            .putInt(todayKey, currentMinutes)
            .apply()
        
        _todayListeningMinutes.value = currentMinutes
        updateStreak()
    }

    private fun updateStreak() {
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val lastDateStr = prefs.getString(KEY_LAST_ACTIVE_DATE, null)

        if (lastDateStr == todayStr) return // Already recorded today — nothing to update

        val yesterdayCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(yesterdayCal.time)

        val newStreak = when {
            lastDateStr == null || lastDateStr.isBlank() -> 1          // First launch ever
            lastDateStr == yesterdayStr -> prefs.getInt(KEY_STREAK_DAYS, 1) + 1  // Consecutive day
            else -> 1                                                   // Missed one or more days — reset
        }
        prefs.edit()
            .putString(KEY_LAST_ACTIVE_DATE, todayStr)
            .putInt(KEY_STREAK_DAYS, newStreak)
            .apply()
        _currentStreakDays.value = newStreak
    }

    private fun getTodayKey(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        return "listening_min_$dateStr"
    }

    companion object {
        private const val KEY_VOLUME_SAFETY = "key_volume_safety_lock"
        private const val KEY_NIGHT_MODE = "key_night_audio_mode"
        private const val KEY_STREAK_DAYS = "key_streak_days"
        private const val KEY_LAST_ACTIVE_DATE = "key_last_active_date"

        const val VOLUME_SAFETY_CAP = 0.85f

        @Volatile
        private var INSTANCE: AudioComfortManager? = null

        fun getInstance(context: Context): AudioComfortManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioComfortManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
