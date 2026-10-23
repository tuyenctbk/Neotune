package com.easeaudio.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppIconManager {
    private const val TAG = "AppIconManager"
    private const val PREFS_NAME = "neotune_icon_prefs"
    private const val KEY_CURRENT_ICON = "current_icon_theme"

    val AVAILABLE_ICONS = listOf(
        "default" to "NeoTune Default",
        "jazz" to "Smooth Jazz",
        "rock" to "Electric Rock",
        "cyberpunk" to "Cyberpunk Neon"
    )

    fun getCurrentIconTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_ICON, "default") ?: "default"
    }

    fun getLaunchIntent(context: Context): android.content.Intent {
        return context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: android.content.Intent(context, com.easeaudio.MainActivity::class.java).apply {
                action = android.content.Intent.ACTION_MAIN
            }
    }

    fun setAppIcon(context: Context, iconKey: String) {
        val current = getCurrentIconTheme(context)
        if (current == iconKey) return

        val pm = context.packageManager
        val packageName = context.packageName

        val mainActivityComponent = ComponentName(packageName, "com.easeaudio.MainActivity")
        val defaultAlias = ComponentName(packageName, "com.easeaudio.MainActivityAliasDefault")
        val aliases = mapOf(
            "default" to defaultAlias,
            "jazz" to ComponentName(packageName, "com.easeaudio.MainActivityAliasJazz"),
            "rock" to ComponentName(packageName, "com.easeaudio.MainActivityAliasRock"),
            "cyberpunk" to ComponentName(packageName, "com.easeaudio.MainActivityAliasCyberpunk")
        )

        val targetComponent = aliases[iconKey] ?: defaultAlias

        try {
            // Keep MainActivity permanently enabled so explicit intents (notifications, widgets,
            // shortcuts) and Android TV LEANBACK_LAUNCHER are never broken.
            pm.setComponentEnabledSetting(
                mainActivityComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            aliases.forEach { (_, component) ->
                val state = if (component == targetComponent) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
                pm.setComponentEnabledSetting(
                    component,
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_ICON, iconKey)
                .apply()

            Log.i(TAG, "Successfully changed app launcher icon to $iconKey")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update app icon alias: ${e.message}", e)
        }
    }
}
