package com.easeaudio.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppRemoteConfig(
    val autoQualityAdaptive: Boolean = true,
    val minBufferMsCellular: Long = 20000L,
    val maxBufferMsCellular: Long = 60000L,
    val showNetworkQualityBadge: Boolean = true,
    val configSource: String = "Default Local Config",
    val latestVersionCode: Int = 1,
    val minRequiredVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val updateNotes: String = ""
)

class FirebaseConfigManager(private val context: Context) {

    private val TAG = "FirebaseConfigManager"

    private val _configState = MutableStateFlow(AppRemoteConfig())
    val configState: StateFlow<AppRemoteConfig> = _configState.asStateFlow()

    private var remoteConfig: FirebaseRemoteConfig? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        initFirebaseAndRemoteConfig()
    }

    private fun initFirebaseAndRemoteConfig() {
        scope.launch {
            try {
                val app: FirebaseApp? = try {
                    if (FirebaseApp.getApps(context).isEmpty()) {
                        try {
                            FirebaseApp.initializeApp(context)
                        } catch (e: Exception) {
                            initializeFallbackFirebase(context)
                        }
                    } else {
                        FirebaseApp.getInstance()
                    }
                } catch (e: Exception) {
                    try {
                        initializeFallbackFirebase(context)
                    } catch (e2: Exception) {
                        null
                    }
                }

                if (app != null) {
                    val instance = FirebaseRemoteConfig.getInstance(app)
                    remoteConfig = instance

                    // Set default map
                    val defaults: Map<String, Any> = mapOf(
                        KEY_AUTO_QUALITY to true,
                        KEY_MIN_BUFFER to 20000L,
                        KEY_MAX_BUFFER to 60000L,
                        KEY_SHOW_NETWORK_BADGE to true,
                        KEY_LATEST_VERSION_CODE to 1,
                        KEY_MIN_REQUIRED_VERSION_CODE to 1,
                        KEY_LATEST_VERSION_NAME to "1.0.0",
                        KEY_UPDATE_NOTES to ""
                    )
                    instance.setDefaultsAsync(defaults)

                    val configSettings = FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(3600)
                        .build()
                    instance.setConfigSettingsAsync(configSettings)

                    // Fetch and activate
                    instance.fetchAndActivate()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val updated = task.result
                                Log.d(TAG, "Remote config fetched successfully, updated: $updated")
                                applyRemoteValues(instance, if (updated) "Firebase Remote Config (Active)" else "Firebase Local Defaults")
                            } else {
                                Log.d(TAG, "Fetch offline or default, using local defaults")
                                applyRemoteValues(instance, "Local Defaults (Firebase Offline)")
                            }
                        }
                } else {
                    Log.d(TAG, "Firebase unavailable, using local default config")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Firebase Remote Config not active: ${e.message}")
            }
        }
    }

    private fun initializeFallbackFirebase(context: Context): FirebaseApp {
        val options = FirebaseOptions.Builder()
            .setApplicationId("1:100000000000:android:easeaudio123")
            .setApiKey("AIzaSyEaseAudioDemoKeyForRemoteConfig")
            .setProjectId("easeaudio-app")
            .build()
        return FirebaseApp.initializeApp(context, options)
    }

    private fun applyRemoteValues(config: FirebaseRemoteConfig, source: String) {
        val autoQuality = config.getBoolean(KEY_AUTO_QUALITY)
        val minBuf = config.getLong(KEY_MIN_BUFFER)
        val maxBuf = config.getLong(KEY_MAX_BUFFER)
        val showBadge = config.getBoolean(KEY_SHOW_NETWORK_BADGE)

        val latestCode = config.getLong(KEY_LATEST_VERSION_CODE).toInt()
        val minCode = config.getLong(KEY_MIN_REQUIRED_VERSION_CODE).toInt()
        val latestName = config.getString(KEY_LATEST_VERSION_NAME)
        val notes = config.getString(KEY_UPDATE_NOTES)

        _configState.value = AppRemoteConfig(
            autoQualityAdaptive = autoQuality,
            minBufferMsCellular = if (minBuf <= 0) 20000L else minBuf,
            maxBufferMsCellular = if (maxBuf <= 0) 60000L else maxBuf,
            showNetworkQualityBadge = showBadge,
            configSource = source,
            latestVersionCode = if (latestCode <= 0) 1 else latestCode,
            minRequiredVersionCode = if (minCode <= 0) 1 else minCode,
            latestVersionName = latestName,
            updateNotes = notes
        )

        // Pass Remote Config version info to SmartEngagementManager for update checks
        com.easeaudio.engagement.SmartEngagementManager.getInstance(context).setRemoteUpdateInfo(
            latestVersionCode = if (latestCode <= 0) 1 else latestCode,
            minRequiredVersionCode = if (minCode <= 0) 1 else minCode,
            latestVersionName = latestName,
            updateNotes = notes
        )
    }

    companion object {
        const val KEY_AUTO_QUALITY = "auto_quality_adaptive"
        const val KEY_MIN_BUFFER = "min_buffer_ms_cellular"
        const val KEY_MAX_BUFFER = "max_buffer_ms_cellular"
        const val KEY_SHOW_NETWORK_BADGE = "show_network_quality_badge"
        const val KEY_LATEST_VERSION_CODE = "latest_version_code"
        const val KEY_MIN_REQUIRED_VERSION_CODE = "min_required_version_code"
        const val KEY_LATEST_VERSION_NAME = "latest_version_name"
        const val KEY_UPDATE_NOTES = "update_notes"
    }
}
