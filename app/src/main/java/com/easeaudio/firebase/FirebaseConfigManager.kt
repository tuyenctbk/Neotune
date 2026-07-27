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
    val adsEnabled: Boolean = false, // Default is FALSE as requested by user
    val bannerAdUnitId: String = "ca-app-pub-3940256099942544/6300978111", // Standard AdMob Test Banner
    val autoQualityAdaptive: Boolean = true,
    val minBufferMsCellular: Long = 20000L,
    val maxBufferMsCellular: Long = 60000L,
    val showNetworkQualityBadge: Boolean = true,
    val configSource: String = "Default Local Config"
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
                    val defaults = mapOf(
                        KEY_ADS_ENABLED to false,
                        KEY_BANNER_AD_UNIT_ID to "ca-app-pub-3940256099942544/6300978111",
                        KEY_AUTO_QUALITY to true,
                        KEY_MIN_BUFFER to 20000L,
                        KEY_MAX_BUFFER to 60000L,
                        KEY_SHOW_NETWORK_BADGE to true
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
        val adsEnabled = config.getBoolean(KEY_ADS_ENABLED)
        val bannerId = config.getString(KEY_BANNER_AD_UNIT_ID).ifBlank { "ca-app-pub-3940256099942544/6300978111" }
        val autoQuality = config.getBoolean(KEY_AUTO_QUALITY)
        val minBuf = config.getLong(KEY_MIN_BUFFER)
        val maxBuf = config.getLong(KEY_MAX_BUFFER)
        val showBadge = config.getBoolean(KEY_SHOW_NETWORK_BADGE)

        _configState.value = AppRemoteConfig(
            adsEnabled = adsEnabled,
            bannerAdUnitId = bannerId,
            autoQualityAdaptive = autoQuality,
            minBufferMsCellular = if (minBuf <= 0) 20000L else minBuf,
            maxBufferMsCellular = if (maxBuf <= 0) 60000L else maxBuf,
            showNetworkQualityBadge = showBadge,
            configSource = source
        )
    }

    // Allow user simulation toggle in Settings UI for testing AdMob or Remote Config
    fun toggleSimulatedAds(enabled: Boolean) {
        val current = _configState.value
        _configState.value = current.copy(
            adsEnabled = enabled,
            configSource = if (enabled) "User Settings Override (Ads Enabled)" else "User Settings Override (Ads Disabled)"
        )
    }

    companion object {
        const val KEY_ADS_ENABLED = "ads_enabled"
        const val KEY_BANNER_AD_UNIT_ID = "banner_ad_unit_id"
        const val KEY_AUTO_QUALITY = "auto_quality_adaptive"
        const val KEY_MIN_BUFFER = "min_buffer_ms_cellular"
        const val KEY_MAX_BUFFER = "max_buffer_ms_cellular"
        const val KEY_SHOW_NETWORK_BADGE = "show_network_quality_badge"
    }
}
