package com.easeaudio.service

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SystemThemeService observes system-wide dark/light theme configuration changes
 * at the Android application level and exposes a reactive flow to update the
 * UI dynamically without requiring manual activity restart or refresh.
 */
class SystemThemeService private constructor(private val application: Application) : ComponentCallbacks2 {

    private val _isSystemDarkTheme = MutableStateFlow(checkIsSystemDarkTheme(application.resources.configuration))
    val isSystemDarkTheme: StateFlow<Boolean> = _isSystemDarkTheme.asStateFlow()

    init {
        application.registerComponentCallbacks(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val isDark = checkIsSystemDarkTheme(newConfig)
        if (_isSystemDarkTheme.value != isDark) {
            _isSystemDarkTheme.value = isDark
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onLowMemory() {
        // No-op
    }

    override fun onTrimMemory(level: Int) {
        // No-op
    }

    fun unregister() {
        application.unregisterComponentCallbacks(this)
    }

    companion object {
        @Volatile
        private var INSTANCE: SystemThemeService? = null

        fun getInstance(context: Context): SystemThemeService {
            return INSTANCE ?: synchronized(this) {
                val app = context.applicationContext as Application
                val instance = SystemThemeService(app)
                INSTANCE = instance
                instance
            }
        }

        private fun checkIsSystemDarkTheme(config: Configuration): Boolean {
            return (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
