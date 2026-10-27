package com.easeaudio.engagement

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.easeaudio.BuildConfig
import com.easeaudio.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

enum class EngagementPromptType {
    NONE,
    RATE_5_STARS,
    SHARE_APP,
    UPDATE_APP,
    DONATION_PROMPT
}

data class UpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val isForceUpdate: Boolean = false,
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val updateNotes: String = ""
)

object EngagementPolicy {
    // Rating:
    // - User must have used the app for at least 3 distinct days and 4 sessions
    // - User must have engaged meaningfully (>= 30 min listening OR >= 2 favorites)
    // - At least 21 days between rating prompt attempts
    // - Never prompt again after 2 dismissals
    const val RATE_MIN_INSTALL_DAYS = 3L
    const val RATE_MIN_SESSIONS = 4
    const val RATE_MIN_LISTENING_MINUTES = 30L
    const val RATE_MIN_FAVORITES = 2
    const val RATE_COOLDOWN_DAYS = 21L
    const val RATE_MAX_DISMISS_COUNT = 2

    // Sharing:
    // - User must have used the app for at least 5 days and 6 sessions
    // - High loyalty: >= 60 min listening OR >= 3 favorites
    // - At least 21 days between share prompt attempts
    // - Never prompt again after 2 dismissals
    const val SHARE_MIN_INSTALL_DAYS = 5L
    const val SHARE_MIN_SESSIONS = 6
    const val SHARE_MIN_LISTENING_MINUTES = 60L
    const val SHARE_MIN_FAVORITES = 3
    const val SHARE_COOLDOWN_DAYS = 21L
    const val SHARE_MAX_DISMISS_COUNT = 2

    // Optional Update:
    // - Cooldown: at least 7 days between optional update prompts
    // - Max 2 dismissals for the exact same target version code
    const val UPDATE_OPTIONAL_COOLDOWN_DAYS = 7L
    const val UPDATE_MAX_DISMISS_COUNT_PER_VERSION = 2

    // Global cooldown across all non-force engagement prompts:
    // Must wait at least 7 days between ANY prompts (Rate, Share, Update)
    const val GLOBAL_ENGAGEMENT_COOLDOWN_DAYS = 7L

    fun shouldPromptRating(
        hasRatedApp: Boolean,
        rateDismissCount: Int,
        daysSinceLastRate: Long?,
        daysSinceAnyPrompt: Long?,
        daysSinceFirstLaunch: Long,
        sessionCount: Int,
        listeningTimeMinutes: Long,
        favoritesCount: Int
    ): Boolean {
        if (hasRatedApp) return false
        if (rateDismissCount >= RATE_MAX_DISMISS_COUNT) return false
        if (daysSinceAnyPrompt != null && daysSinceAnyPrompt < GLOBAL_ENGAGEMENT_COOLDOWN_DAYS) return false
        if (daysSinceLastRate != null && daysSinceLastRate < RATE_COOLDOWN_DAYS) return false

        val hasEnoughExperience = daysSinceFirstLaunch >= RATE_MIN_INSTALL_DAYS && sessionCount >= RATE_MIN_SESSIONS
        val hasDemonstratedEnjoyment = listeningTimeMinutes >= RATE_MIN_LISTENING_MINUTES || favoritesCount >= RATE_MIN_FAVORITES

        return hasEnoughExperience && hasDemonstratedEnjoyment
    }

    fun shouldPromptShare(
        hasSharedApp: Boolean,
        shareDismissCount: Int,
        daysSinceLastShare: Long?,
        daysSinceAnyPrompt: Long?,
        daysSinceFirstLaunch: Long,
        sessionCount: Int,
        listeningTimeMinutes: Long,
        favoritesCount: Int
    ): Boolean {
        if (hasSharedApp) return false
        if (shareDismissCount >= SHARE_MAX_DISMISS_COUNT) return false
        if (daysSinceAnyPrompt != null && daysSinceAnyPrompt < GLOBAL_ENGAGEMENT_COOLDOWN_DAYS) return false
        if (daysSinceLastShare != null && daysSinceLastShare < SHARE_COOLDOWN_DAYS) return false

        val hasSufficientLoyalty = daysSinceFirstLaunch >= SHARE_MIN_INSTALL_DAYS && sessionCount >= SHARE_MIN_SESSIONS
        val hasHighEngagement = listeningTimeMinutes >= SHARE_MIN_LISTENING_MINUTES || favoritesCount >= SHARE_MIN_FAVORITES

        return hasSufficientLoyalty && hasHighEngagement
    }

    fun shouldPromptOptionalUpdate(
        isUpdateAvailable: Boolean,
        isForceUpdate: Boolean,
        isPromptShownThisSession: Boolean,
        targetVersionCode: Int,
        lastDismissedVersionCode: Int,
        dismissCountForVersion: Int,
        daysSinceLastUpdate: Long?,
        daysSinceAnyPrompt: Long?
    ): Boolean {
        if (!isUpdateAvailable || isForceUpdate) return false
        if (isPromptShownThisSession) return false
        if (targetVersionCode == lastDismissedVersionCode && dismissCountForVersion >= UPDATE_MAX_DISMISS_COUNT_PER_VERSION) return false
        if (daysSinceLastUpdate != null && daysSinceLastUpdate < UPDATE_OPTIONAL_COOLDOWN_DAYS) return false
        if (daysSinceAnyPrompt != null && daysSinceAnyPrompt < GLOBAL_ENGAGEMENT_COOLDOWN_DAYS) return false
        return true
    }
}

class SmartEngagementManager private constructor(context: Context) {
    private val context: Context = context.applicationContext

    companion object {
        private const val TAG = "SmartEngagementManager"
        private const val PREFS_NAME = "neotune_engagement_prefs"

        private const val KEY_SESSION_COUNT = "session_count"
        private const val KEY_LISTENING_TIME_SEC = "listening_time_sec"
        private const val KEY_FAVORITES_ADDED_COUNT = "favorites_added_count"
        private const val KEY_FIRST_LAUNCH_TIMESTAMP = "first_launch_timestamp"

        // Global engagement tracking
        private const val KEY_LAST_ANY_PROMPT_TIMESTAMP = "last_any_engagement_timestamp"

        // Rate tracking
        private const val KEY_LAST_RATE_PROMPT_TIMESTAMP = "last_rate_prompt_timestamp"
        private const val KEY_HAS_RATED_APP = "has_rated_app"
        private const val KEY_RATE_DISMISS_COUNT = "rate_dismiss_count"

        // Share tracking
        private const val KEY_LAST_SHARE_PROMPT_TIMESTAMP = "last_share_prompt_timestamp"
        private const val KEY_HAS_SHARED_APP = "has_shared_app"
        private const val KEY_SHARE_DISMISS_COUNT = "share_dismiss_count"

        // Update tracking
        private const val KEY_LAST_UPDATE_PROMPT_TIMESTAMP = "last_update_prompt_timestamp"
        private const val KEY_UPDATE_DISMISSED_VERSION_CODE = "update_dismissed_version_code"
        private const val KEY_UPDATE_DISMISS_COUNT_FOR_VERSION = "update_dismiss_count_for_version"

        // Donation & Milestone tracking
        private const val KEY_OPERATIONS_COUNT = "operations_count"
        private const val KEY_IS_SUPPORTER = "is_supporter"
        private const val KEY_LAST_DONATION_BANNER_DISMISS = "last_donation_banner_dismiss"
        private const val KEY_LAST_AUTO_DONATION_PROMPT = "last_auto_donation_prompt"
        private const val KEY_LAST_MILESTONE_SHOWN = "last_milestone_shown"

        // Re-export constants for compatibility
        const val RATE_MIN_INSTALL_DAYS = EngagementPolicy.RATE_MIN_INSTALL_DAYS
        const val RATE_MIN_SESSIONS = EngagementPolicy.RATE_MIN_SESSIONS
        const val RATE_MIN_LISTENING_MINUTES = EngagementPolicy.RATE_MIN_LISTENING_MINUTES
        const val RATE_MIN_FAVORITES = EngagementPolicy.RATE_MIN_FAVORITES
        const val RATE_COOLDOWN_DAYS = EngagementPolicy.RATE_COOLDOWN_DAYS
        const val RATE_MAX_DISMISS_COUNT = EngagementPolicy.RATE_MAX_DISMISS_COUNT

        const val SHARE_MIN_INSTALL_DAYS = EngagementPolicy.SHARE_MIN_INSTALL_DAYS
        const val SHARE_MIN_SESSIONS = EngagementPolicy.SHARE_MIN_SESSIONS
        const val SHARE_MIN_LISTENING_MINUTES = EngagementPolicy.SHARE_MIN_LISTENING_MINUTES
        const val SHARE_MIN_FAVORITES = EngagementPolicy.SHARE_MIN_FAVORITES
        const val SHARE_COOLDOWN_DAYS = EngagementPolicy.SHARE_COOLDOWN_DAYS
        const val SHARE_MAX_DISMISS_COUNT = EngagementPolicy.SHARE_MAX_DISMISS_COUNT

        const val UPDATE_OPTIONAL_COOLDOWN_DAYS = EngagementPolicy.UPDATE_OPTIONAL_COOLDOWN_DAYS
        const val UPDATE_MAX_DISMISS_COUNT_PER_VERSION = EngagementPolicy.UPDATE_MAX_DISMISS_COUNT_PER_VERSION
        const val GLOBAL_ENGAGEMENT_COOLDOWN_DAYS = EngagementPolicy.GLOBAL_ENGAGEMENT_COOLDOWN_DAYS

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: SmartEngagementManager? = null

        fun getInstance(context: Context): SmartEngagementManager {
            return instance ?: synchronized(this) {
                instance ?: SmartEngagementManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // In-memory guard to ensure AT MOST ONE engagement prompt per app session
    @Volatile
    private var isPromptShownThisSession: Boolean = false

    private val _activePrompt = MutableStateFlow(EngagementPromptType.NONE)
    val activePrompt: StateFlow<EngagementPromptType> = _activePrompt.asStateFlow()

    private val _updateInfo = MutableStateFlow(UpdateInfo())
    val updateInfo: StateFlow<UpdateInfo> = _updateInfo.asStateFlow()

    private val _isSupporter = MutableStateFlow(prefs.getBoolean(KEY_IS_SUPPORTER, false))
    val isSupporter: StateFlow<Boolean> = _isSupporter.asStateFlow()

    private val _operationsCount = MutableStateFlow(prefs.getInt(KEY_OPERATIONS_COUNT, 0))
    val operationsCount: StateFlow<Int> = _operationsCount.asStateFlow()

    private val _showHomeDonationBanner = MutableStateFlow(false)
    val showHomeDonationBanner: StateFlow<Boolean> = _showHomeDonationBanner.asStateFlow()

    private val _milestoneToCelebrate = MutableStateFlow<Int?>(null)
    val milestoneToCelebrate: StateFlow<Int?> = _milestoneToCelebrate.asStateFlow()

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
        // Note: Do not interrupt active user interactions with immediate popups.
        // Engagement triggers are evaluated at natural pause points (e.g., app resume).
    }

    /**
     * Smart calculation to check whether to present Rate, Share, or Update prompts.
     * Evaluates happiness score, session duration, user loyalty & cool-down windows.
     * Guarantees non-intrusive presentation with strict frequency limits.
     */
    fun checkSmartTriggers(eventSource: String = "general") {
        // If a prompt is currently active on screen, do not disrupt it
        if (_activePrompt.value != EngagementPromptType.NONE) {
            return
        }

        // Priority 1: Check for App Updates (highest priority)
        if (checkUpdatePromptAvailable()) {
            return
        }

        // Strict Session Rule: Never show more than ONE engagement prompt in a single session
        if (isPromptShownThisSession) {
            return
        }

        // Priority 2: Check for 5-Star Rating Prompt
        if (shouldPromptRating()) {
            Log.i(TAG, "Smart Trigger: Presenting Rate 5 Stars Prompt (Source: $eventSource)")
            _activePrompt.value = EngagementPromptType.RATE_5_STARS
            markPromptDisplayed(EngagementPromptType.RATE_5_STARS)
            return
        }

        // Priority 3: Check for Share App Prompt
        if (shouldPromptShare()) {
            Log.i(TAG, "Smart Trigger: Presenting Share App Prompt (Source: $eventSource)")
            _activePrompt.value = EngagementPromptType.SHARE_APP
            markPromptDisplayed(EngagementPromptType.SHARE_APP)
            return
        }
    }

    private fun markPromptDisplayed(type: EngagementPromptType) {
        isPromptShownThisSession = true
        val now = System.currentTimeMillis()
        val editor = prefs.edit().putLong(KEY_LAST_ANY_PROMPT_TIMESTAMP, now)
        when (type) {
            EngagementPromptType.RATE_5_STARS -> editor.putLong(KEY_LAST_RATE_PROMPT_TIMESTAMP, now)
            EngagementPromptType.SHARE_APP -> editor.putLong(KEY_LAST_SHARE_PROMPT_TIMESTAMP, now)
            EngagementPromptType.UPDATE_APP -> editor.putLong(KEY_LAST_UPDATE_PROMPT_TIMESTAMP, now)
            EngagementPromptType.DONATION_PROMPT -> editor.putLong(KEY_LAST_AUTO_DONATION_PROMPT, now)
            EngagementPromptType.NONE -> {}
        }
        editor.apply()
    }

    private fun shouldPromptRating(): Boolean {
        val now = System.currentTimeMillis()
        val lastAnyPromptTime = prefs.getLong(KEY_LAST_ANY_PROMPT_TIMESTAMP, 0L)
        val daysSinceAnyPrompt = if (lastAnyPromptTime > 0) TimeUnit.MILLISECONDS.toDays(now - lastAnyPromptTime) else null
        val lastRatePromptTime = prefs.getLong(KEY_LAST_RATE_PROMPT_TIMESTAMP, 0L)
        val daysSinceLastRate = if (lastRatePromptTime > 0) TimeUnit.MILLISECONDS.toDays(now - lastRatePromptTime) else null
        val firstLaunchTime = prefs.getLong(KEY_FIRST_LAUNCH_TIMESTAMP, now)
        val daysSinceFirstLaunch = TimeUnit.MILLISECONDS.toDays(now - firstLaunchTime)

        return EngagementPolicy.shouldPromptRating(
            hasRatedApp = prefs.getBoolean(KEY_HAS_RATED_APP, false),
            rateDismissCount = prefs.getInt(KEY_RATE_DISMISS_COUNT, 0),
            daysSinceLastRate = daysSinceLastRate,
            daysSinceAnyPrompt = daysSinceAnyPrompt,
            daysSinceFirstLaunch = daysSinceFirstLaunch,
            sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0),
            listeningTimeMinutes = prefs.getLong(KEY_LISTENING_TIME_SEC, 0L) / 60,
            favoritesCount = prefs.getInt(KEY_FAVORITES_ADDED_COUNT, 0)
        )
    }

    private fun shouldPromptShare(): Boolean {
        val now = System.currentTimeMillis()
        val lastAnyPromptTime = prefs.getLong(KEY_LAST_ANY_PROMPT_TIMESTAMP, 0L)
        val daysSinceAnyPrompt = if (lastAnyPromptTime > 0) TimeUnit.MILLISECONDS.toDays(now - lastAnyPromptTime) else null
        val lastSharePromptTime = prefs.getLong(KEY_LAST_SHARE_PROMPT_TIMESTAMP, 0L)
        val daysSinceLastShare = if (lastSharePromptTime > 0) TimeUnit.MILLISECONDS.toDays(now - lastSharePromptTime) else null
        val firstLaunchTime = prefs.getLong(KEY_FIRST_LAUNCH_TIMESTAMP, now)
        val daysSinceFirstLaunch = TimeUnit.MILLISECONDS.toDays(now - firstLaunchTime)

        return EngagementPolicy.shouldPromptShare(
            hasSharedApp = prefs.getBoolean(KEY_HAS_SHARED_APP, false),
            shareDismissCount = prefs.getInt(KEY_SHARE_DISMISS_COUNT, 0),
            daysSinceLastShare = daysSinceLastShare,
            daysSinceAnyPrompt = daysSinceAnyPrompt,
            daysSinceFirstLaunch = daysSinceFirstLaunch,
            sessionCount = prefs.getInt(KEY_SESSION_COUNT, 0),
            listeningTimeMinutes = prefs.getLong(KEY_LISTENING_TIME_SEC, 0L) / 60,
            favoritesCount = prefs.getInt(KEY_FAVORITES_ADDED_COUNT, 0)
        )
    }

    private fun checkUpdatePromptAvailable(): Boolean {
        val info = _updateInfo.value
        if (!info.isUpdateAvailable) return false

        // Priority Critical / Force Update: Always shown so the app remains usable
        if (info.isForceUpdate) {
            _activePrompt.value = EngagementPromptType.UPDATE_APP
            markPromptDisplayed(EngagementPromptType.UPDATE_APP)
            return true
        }

        val now = System.currentTimeMillis()
        val lastAnyPromptTime = prefs.getLong(KEY_LAST_ANY_PROMPT_TIMESTAMP, 0L)
        val daysSinceAnyPrompt = if (lastAnyPromptTime > 0) TimeUnit.MILLISECONDS.toDays(now - lastAnyPromptTime) else null
        val lastUpdatePromptTime = prefs.getLong(KEY_LAST_UPDATE_PROMPT_TIMESTAMP, 0L)
        val daysSinceLastUpdate = if (lastUpdatePromptTime > 0) TimeUnit.MILLISECONDS.toDays(now - lastUpdatePromptTime) else null

        val shouldPrompt = EngagementPolicy.shouldPromptOptionalUpdate(
            isUpdateAvailable = info.isUpdateAvailable,
            isForceUpdate = info.isForceUpdate,
            isPromptShownThisSession = isPromptShownThisSession,
            targetVersionCode = info.latestVersionCode,
            lastDismissedVersionCode = prefs.getInt(KEY_UPDATE_DISMISSED_VERSION_CODE, 0),
            dismissCountForVersion = prefs.getInt(KEY_UPDATE_DISMISS_COUNT_FOR_VERSION, 0),
            daysSinceLastUpdate = daysSinceLastUpdate,
            daysSinceAnyPrompt = daysSinceAnyPrompt
        )

        if (shouldPrompt) {
            _activePrompt.value = EngagementPromptType.UPDATE_APP
            markPromptDisplayed(EngagementPromptType.UPDATE_APP)
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
            BuildConfig.VERSION_CODE
        }

        val isAvailable = latestVersionCode > currentVersionCode
        val isForce = currentVersionCode < minRequiredVersionCode

        _updateInfo.value = UpdateInfo(
            isUpdateAvailable = isAvailable,
            isForceUpdate = isForce,
            latestVersionCode = latestVersionCode,
            latestVersionName = latestVersionName.ifBlank { "v$latestVersionCode" },
            updateNotes = updateNotes
        )

        if (isAvailable) {
            checkSmartTriggers(eventSource = "remote_config_update")
        }
    }

    fun onRatingCompleted(stars: Int, activity: android.app.Activity? = null) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean(KEY_HAS_RATED_APP, true)
            .putLong(KEY_LAST_RATE_PROMPT_TIMESTAMP, now)
            .putLong(KEY_LAST_ANY_PROMPT_TIMESTAMP, now)
            .apply()
        _activePrompt.value = EngagementPromptType.NONE

        if (stars >= 4) {
            if (activity != null) {
                try {
                    val reviewManager = com.google.android.play.core.review.ReviewManagerFactory.create(activity)
                    val request = reviewManager.requestReviewFlow()
                    request.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val reviewInfo = task.result
                            val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                            flow.addOnCompleteListener { _ ->
                                Log.i(TAG, "Google Play in-app review flow finished")
                            }
                        } else {
                            Log.w(TAG, "In-app review request not successful, falling back to Play Store URL")
                            openPlayStoreForRating()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error initiating Google Play In-App Review: ${e.message}")
                    openPlayStoreForRating()
                }
            } else {
                openPlayStoreForRating()
            }
        }
    }

    fun onRatingDismissed() {
        val dismissCount = prefs.getInt(KEY_RATE_DISMISS_COUNT, 0) + 1
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_RATE_DISMISS_COUNT, dismissCount)
            .putLong(KEY_LAST_RATE_PROMPT_TIMESTAMP, now)
            .putLong(KEY_LAST_ANY_PROMPT_TIMESTAMP, now)
            .apply()
        _activePrompt.value = EngagementPromptType.NONE
    }

    fun onShareCompleted() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean(KEY_HAS_SHARED_APP, true)
            .putLong(KEY_LAST_SHARE_PROMPT_TIMESTAMP, now)
            .putLong(KEY_LAST_ANY_PROMPT_TIMESTAMP, now)
            .apply()
        _activePrompt.value = EngagementPromptType.NONE
        launchShareIntent()
    }

    fun onShareDismissed() {
        val dismissCount = prefs.getInt(KEY_SHARE_DISMISS_COUNT, 0) + 1
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_SHARE_DISMISS_COUNT, dismissCount)
            .putLong(KEY_LAST_SHARE_PROMPT_TIMESTAMP, now)
            .putLong(KEY_LAST_ANY_PROMPT_TIMESTAMP, now)
            .apply()
        _activePrompt.value = EngagementPromptType.NONE
    }

    fun onUpdateConfirmed() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_LAST_UPDATE_PROMPT_TIMESTAMP, now)
            .putLong(KEY_LAST_ANY_PROMPT_TIMESTAMP, now)
            .apply()
        _activePrompt.value = EngagementPromptType.NONE
        openPlayStoreForRating()
    }

    fun onUpdateDismissed() {
        val currentTargetVersion = _updateInfo.value.latestVersionCode
        val lastDismissedVersion = prefs.getInt(KEY_UPDATE_DISMISSED_VERSION_CODE, 0)
        val dismissCount = if (currentTargetVersion == lastDismissedVersion) {
            prefs.getInt(KEY_UPDATE_DISMISS_COUNT_FOR_VERSION, 0) + 1
        } else {
            1
        }
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_UPDATE_DISMISSED_VERSION_CODE, currentTargetVersion)
            .putInt(KEY_UPDATE_DISMISS_COUNT_FOR_VERSION, dismissCount)
            .putLong(KEY_LAST_UPDATE_PROMPT_TIMESTAMP, now)
            .putLong(KEY_LAST_ANY_PROMPT_TIMESTAMP, now)
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
        val shareText = context.getString(R.string.share_app_text, "https://play.google.com/store/apps/details?id=${context.packageName}")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.share_app_header)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun evaluateHomeDonationBanner() {
        if (_isSupporter.value) {
            _showHomeDonationBanner.value = false
            return
        }
        val ops = prefs.getInt(KEY_OPERATIONS_COUNT, 0)
        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0)
        val firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH_TIMESTAMP, System.currentTimeMillis())
        val daysSinceFirstLaunch = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - firstLaunch)
        val lastDismiss = prefs.getLong(KEY_LAST_DONATION_BANNER_DISMISS, 0L)
        val daysSinceLastDismiss = if (lastDismiss > 0) TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastDismiss) else 100L

        val meetsCriteria = ops >= 3 && sessions >= 3 && daysSinceFirstLaunch >= 2 && daysSinceLastDismiss >= 14
        _showHomeDonationBanner.value = meetsCriteria
    }

    fun recordCoreOperation(opType: String = "general") {
        val newCount = prefs.getInt(KEY_OPERATIONS_COUNT, 0) + 1
        prefs.edit().putInt(KEY_OPERATIONS_COUNT, newCount).apply()
        _operationsCount.value = newCount
        Log.i(TAG, "Core operation recorded ($opType), total: $newCount")

        if (_isSupporter.value) return

        // Check Milestones (10, 25, 50)
        val lastMilestone = prefs.getInt(KEY_LAST_MILESTONE_SHOWN, 0)
        val targetMilestone = when {
            newCount >= 50 && lastMilestone < 50 -> 50
            newCount >= 25 && lastMilestone < 25 -> 25
            newCount >= 10 && lastMilestone < 10 -> 10
            else -> null
        }

        if (targetMilestone != null) {
            prefs.edit().putInt(KEY_LAST_MILESTONE_SHOWN, targetMilestone).apply()
            _milestoneToCelebrate.value = targetMilestone
            return
        }

        // Check Auto Donation Prompt (7+ ops, 30 days cooldown)
        if (newCount >= 7 && !isPromptShownThisSession) {
            val now = System.currentTimeMillis()
            val lastAuto = prefs.getLong(KEY_LAST_AUTO_DONATION_PROMPT, 0L)
            val daysSinceLastAuto = if (lastAuto > 0) TimeUnit.MILLISECONDS.toDays(now - lastAuto) else 100L
            if (daysSinceLastAuto >= 30) {
                prefs.edit().putLong(KEY_LAST_AUTO_DONATION_PROMPT, now).apply()
                _activePrompt.value = EngagementPromptType.DONATION_PROMPT
                isPromptShownThisSession = true
                return
            }
        }

        evaluateHomeDonationBanner()
    }

    fun dismissHomeDonationBanner() {
        prefs.edit().putLong(KEY_LAST_DONATION_BANNER_DISMISS, System.currentTimeMillis()).apply()
        _showHomeDonationBanner.value = false
    }

    fun dismissMilestone() {
        _milestoneToCelebrate.value = null
    }

    fun showDonationDialogDirectly() {
        _activePrompt.value = EngagementPromptType.DONATION_PROMPT
    }

    fun dismissDonationDialog() {
        if (_activePrompt.value == EngagementPromptType.DONATION_PROMPT) {
            _activePrompt.value = EngagementPromptType.NONE
        }
    }

    fun setSupporterStatus(supporter: Boolean) {
        prefs.edit().putBoolean(KEY_IS_SUPPORTER, supporter).apply()
        _isSupporter.value = supporter
        if (supporter) {
            _showHomeDonationBanner.value = false
            _milestoneToCelebrate.value = null
            if (_activePrompt.value == EngagementPromptType.DONATION_PROMPT) {
                _activePrompt.value = EngagementPromptType.NONE
            }
        }
    }
}
