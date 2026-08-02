package com.easeaudio.engagement

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.easeaudio.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

enum class EngagementPromptType {
    NONE,
    RATE_5_STARS,
    SHARE_APP,
    UPDATE_APP
}

data class UpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val isForceUpdate: Boolean = false,
    val latestVersionName: String = "",
    val updateNotes: String = ""
)

class SmartEngagementManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SmartEngagementManager"
        private const val PREFS_NAME = "neotune_engagement_prefs"

        private const val KEY_SESSION_COUNT = "session_count"
        private const val KEY_LISTENING_TIME_SEC = "listening_time_sec"
        private const val KEY_FAVORITES_ADDED_COUNT = "favorites_added_count"
        private const val KEY_FIRST_LAUNCH_TIMESTAMP = "first_launch_timestamp"

        private const val KEY_LAST_RATE_PROMPT_TIMESTAMP = "last_rate_prompt_timestamp"
        private const val KEY_HAS_RATED_APP = "has_rated_app"

        private const val KEY_LAST_SHARE_PROMPT_TIMESTAMP = "last_share_prompt_timestamp"
        private const val KEY_HAS_SHARED_APP = "has_shared_app"

        private const val KEY_LAST_UPDATE_PROMPT_TIMESTAMP = "last_update_prompt_timestamp"

        @Volatile
        private var instance: SmartEngagementManager? = null

        fun getInstance(context: Context): SmartEngagementManager {
            return instance ?: synchronized(this) {
                instance ?: SmartEngagementManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _activePrompt = MutableStateFlow(EngagementPromptType.NONE)
    val activePrompt: StateFlow<EngagementPromptType> = _activePrompt.asStateFlow()

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    init {
        trackSessionStart()
    }

    private fun trackSessionStart() {
        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_SESSION_COUNT, sessions).apply()

        if (!prefs.contains(KEY_FIRST_LAUNCH_TIMESTAMP)) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH_TIMESTAMP, System.currentTimeMillis()).apply()
        }
        Log.i(TAG, "Smart Engagement session #$sessions recorded.")
    }

    fun recordListeningTime(seconds: Long) {
        val total = prefs.getLong(KEY_LISTENING_TIME_SEC, 0L) + seconds
        prefs.edit().putLong(KEY_LISTENING_TIME_SEC, total).apply()
    }

    fun recordFavoriteAdded() {
        val count = prefs.getInt(KEY_FAVORITES_ADDED_COUNT, 0) + 1
        prefs.edit().putInt(KEY_FAVORITES_ADDED_COUNT, count).apply()
        checkSmartTriggers(eventSource = "favorite_added")
    }

    /**
     * Smart calculation to check whether to present Rate, Share, or Update prompts.
     * Evaluates happiness score, session duration, user loyalty & cool-down windows.
     */
    fun checkSmartTriggers(eventSource: String = "general") {
        // Priority 1: Check for App Updates (highest priority)
        if (checkUpdatePromptAvailable()) {
            return
        }

        // Priority 2: Check for 5-Star Rating Prompt
        if (shouldPromptRating()) {
            Log.i(TAG, "Smart Trigger: Presenting Rate 5 Stars Prompt (Source: $eventSource)")
            _activePrompt.value = EngagementPromptType.RATE_5_STARS
            return
        }

        // Priority 3: Check for Share App Prompt
        if (shouldPromptShare()) {
            Log.i(TAG, "Smart Trigger: Presenting Share App Prompt (Source: $eventSource)")
            _activePrompt.value = EngagementPromptType.SHARE_APP
            return
        }
    }

    private fun shouldPromptRating(): Boolean {
        if (prefs.getBoolean(KEY_HAS_RATED_APP, false)) return false

        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0)
        val listeningTimeMinutes = prefs.getLong(KEY_LISTENING_TIME_SEC, 0L) / 60
        val favoritesCount = prefs.getInt(KEY_FAVORITES_ADDED_COUNT, 0)
        val firstLaunchTime = prefs.getLong(KEY_FIRST_LAUNCH_TIMESTAMP, System.currentTimeMillis())
        val daysSinceFirstLaunch = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - firstLaunchTime)
        val lastPromptTime = prefs.getLong(KEY_LAST_RATE_PROMPT_TIMESTAMP, 0L)
        val daysSinceLastPrompt = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastPromptTime)

        // Cool-down check: don't re-prompt within 14 days
        if (lastPromptTime > 0 && daysSinceLastPrompt < 14) {
            return false
        }

        // Smart Happiness Score Calculation:
        // User is happy if:
        // 1. Session count >= 3 OR Listening time >= 15 min OR Favorites count >= 1
        // AND 2. App has been installed for at least 1 day
        val isHappyUser = (sessions >= 3 || listeningTimeMinutes >= 15 || favoritesCount >= 1) && daysSinceFirstLaunch >= 1

        return isHappyUser
    }

    private fun shouldPromptShare(): Boolean {
        if (prefs.getBoolean(KEY_HAS_SHARED_APP, false)) return false

        val listeningTimeMinutes = prefs.getLong(KEY_LISTENING_TIME_SEC, 0L) / 60
        val favoritesCount = prefs.getInt(KEY_FAVORITES_ADDED_COUNT, 0)
        val lastPromptTime = prefs.getLong(KEY_LAST_SHARE_PROMPT_TIMESTAMP, 0L)
        val daysSinceLastPrompt = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastPromptTime)

        if (lastPromptTime > 0 && daysSinceLastPrompt < 7) {
            return false
        }

        // Smart Share Trigger: User loves the content (listening > 20 min or added 2+ favorites)
        return (listeningTimeMinutes >= 20 || favoritesCount >= 2)
    }

    private fun checkUpdatePromptAvailable(): Boolean {
        val info = _updateInfo.value
        if (!info.isUpdateAvailable) return false

        val lastPromptTime = prefs.getLong(KEY_LAST_UPDATE_PROMPT_TIMESTAMP, 0L)
        val daysSinceLastPrompt = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastPromptTime)

        if (info.isForceUpdate || lastPromptTime == 0L || daysSinceLastPrompt >= 3) {
            _activePrompt.value = EngagementPromptType.UPDATE_APP
            return true
        }
        return false
    }

    fun setRemoteUpdateInfo(
        latestVersionCode: Int,
        minRequiredVersionCode: Int,
        latestVersionName: String = "",
        updateNotes: String = ""
    ) {
        val currentVersionCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) {
            1
        }

        val isAvailable = latestVersionCode > currentVersionCode
        val isForce = currentVersionCode < minRequiredVersionCode

        _updateInfo.value = UpdateInfo(
            isUpdateAvailable = isAvailable,
            isForceUpdate = isForce,
            latestVersionName = latestVersionName.ifBlank { "v$latestVersionCode" },
            updateNotes = updateNotes
        )

        if (isAvailable) {
            checkSmartTriggers(eventSource = "remote_config_update")
        }
    }

    fun onRatingCompleted(stars: Int) {
        prefs.edit()
            .putBoolean(KEY_HAS_RATED_APP, true)
            .putLong(KEY_LAST_RATE_PROMPT_TIMESTAMP, System.currentTimeMillis())
            .apply()
        _activePrompt.value = EngagementPromptType.NONE

        if (stars >= 4) {
            openPlayStoreForRating()
        }
    }

    fun onRatingDismissed() {
        prefs.edit()
            .putLong(KEY_LAST_RATE_PROMPT_TIMESTAMP, System.currentTimeMillis())
            .apply()
        _activePrompt.value = EngagementPromptType.NONE
    }

    fun onShareCompleted() {
        prefs.edit()
            .putBoolean(KEY_HAS_SHARED_APP, true)
            .putLong(KEY_LAST_SHARE_PROMPT_TIMESTAMP, System.currentTimeMillis())
            .apply()
        _activePrompt.value = EngagementPromptType.NONE
        launchShareIntent()
    }

    fun onShareDismissed() {
        prefs.edit()
            .putLong(KEY_LAST_SHARE_PROMPT_TIMESTAMP, System.currentTimeMillis())
            .apply()
        _activePrompt.value = EngagementPromptType.NONE
    }

    fun onUpdateConfirmed() {
        prefs.edit()
            .putLong(KEY_LAST_UPDATE_PROMPT_TIMESTAMP, System.currentTimeMillis())
            .apply()
        _activePrompt.value = EngagementPromptType.NONE
        openPlayStoreForRating()
    }

    fun onUpdateDismissed() {
        prefs.edit()
            .putLong(KEY_LAST_UPDATE_PROMPT_TIMESTAMP, System.currentTimeMillis())
            .apply()
        _activePrompt.value = EngagementPromptType.NONE
    }

    fun openPlayStoreForRating() {
        val packageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun launchShareIntent() {
        val shareText = "📻 Listening to global live radio & ambient streams on NeoTune! Download the app: https://play.google.com/store/apps/details?id=${context.packageName}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "NeoTune - Global Live Radio")
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share NeoTune with Friends").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
