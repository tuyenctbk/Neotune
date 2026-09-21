package com.easeaudio.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.easeaudio.MainActivity
import com.easeaudio.R
import com.easeaudio.data.RadioDatabase
import com.easeaudio.service.RadioPlayerManager
import kotlinx.coroutines.*

class NeoTuneAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        Log.d(TAG, "Widget received action: $action")

        val playerManager = RadioPlayerManager.getInstance(context.applicationContext)

        when (action) {
            ACTION_WIDGET_TOGGLE_PLAY -> {
                val current = playerManager.currentStation.value
                if (current != null) {
                    playerManager.togglePlayPause()
                    updateAllWidgets(context)
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = RadioDatabase.getDatabase(context.applicationContext)
                            val recent: com.easeaudio.data.RadioStation? = db.radioDao().getRecentStationsDirect().firstOrNull()
                                ?: db.radioDao().getAllStationsDirect().firstOrNull()
                                ?: db.favoriteDao().getAllFavoritesDirect().firstOrNull()?.let { fav ->
                                    com.easeaudio.data.RadioStation(
                                        id = fav.id,
                                        name = fav.name,
                                        genre = fav.genre,
                                        country = fav.country,
                                        streamUrl = fav.streamUrl,
                                        imageUrl = fav.imageUrl,
                                        bitrate = fav.bitrate,
                                        codec = fav.codec,
                                        isCustom = fav.isCustom
                                    )
                                }
                            withContext(Dispatchers.Main) {
                                if (recent != null) {
                                    playerManager.playStation(recent)
                                } else {
                                    playerManager.playNextStation()
                                }
                                updateAllWidgets(context)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to load recent station for widget: ${e.message}")
                            withContext(Dispatchers.Main) {
                                playerManager.playNextStation()
                                updateAllWidgets(context)
                            }
                        }
                    }
                }
            }
            ACTION_WIDGET_NEXT -> {
                playerManager.playNextStation()
                updateAllWidgets(context)
            }
            ACTION_WIDGET_PREV -> {
                playerManager.playPreviousStation()
                updateAllWidgets(context)
            }
            ACTION_UPDATE_WIDGETS -> {
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        private const val TAG = "NeoTuneAppWidget"
        const val ACTION_WIDGET_TOGGLE_PLAY = "com.easeaudio.widget.ACTION_TOGGLE_PLAY"
        const val ACTION_WIDGET_NEXT = "com.easeaudio.widget.ACTION_NEXT"
        const val ACTION_WIDGET_PREV = "com.easeaudio.widget.ACTION_PREV"
        const val ACTION_UPDATE_WIDGETS = "com.easeaudio.widget.ACTION_UPDATE"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, NeoTuneAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds.isNotEmpty()) {
                    for (widgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, widgetId)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update widgets: ${e.message}")
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            try {
                val playerManager = RadioPlayerManager.getInstance(context.applicationContext)
                val station = playerManager.currentStation.value
                val isPlaying = playerManager.isPlaying.value
                val trackTitle = playerManager.streamTitle.value

                val views = RemoteViews(context.packageName, R.layout.widget_neotune_player)

                // 1. Station Name & Track Title
                if (station != null) {
                    views.setTextViewText(R.id.widget_station_name, station.name)
                    val displayTrack = if (!trackTitle.isNullOrBlank() && trackTitle != "Live Audio Stream") {
                        trackTitle
                    } else {
                        "${station.genre.ifBlank { "Live Radio" }} • ${station.country.ifBlank { "Online" }}"
                    }
                    views.setTextViewText(R.id.widget_track_title, displayTrack)
                } else {
                    views.setTextViewText(R.id.widget_station_name, context.getString(R.string.app_name))
                    views.setTextViewText(R.id.widget_track_title, context.getString(R.string.widget_no_station))
                }

                // 2. Status Badge
                views.setTextViewText(
                    R.id.widget_status_badge,
                    if (isPlaying) "LIVE" else if (station != null) "PAUSED" else "READY"
                )

                // 3. Play / Pause Button icon & accessibility content descriptions
                val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                val playPauseDesc = if (isPlaying) context.getString(R.string.pause) else context.getString(R.string.play)
                views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)
                views.setContentDescription(R.id.widget_btn_play_pause, playPauseDesc)
                views.setContentDescription(R.id.widget_btn_next, context.getString(R.string.next_station))
                views.setContentDescription(R.id.widget_btn_prev, context.getString(R.string.previous_station))

                // 4. Pending Intents for Controls
                val togglePlayIntent = Intent(context, NeoTuneAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_TOGGLE_PLAY
                }
                val togglePlayPendingIntent = PendingIntent.getBroadcast(
                    context,
                    101,
                    togglePlayIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_play_pause, togglePlayPendingIntent)

                val nextIntent = Intent(context, NeoTuneAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_NEXT
                }
                val nextPendingIntent = PendingIntent.getBroadcast(
                    context,
                    102,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

                val prevIntent = Intent(context, NeoTuneAppWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_PREV
                }
                val prevPendingIntent = PendingIntent.getBroadcast(
                    context,
                    103,
                    prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

                // 5. Open App when tapping the body / info container
                val launchAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val launchAppPendingIntent = PendingIntent.getActivity(
                    context,
                    100,
                    launchAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_info_container, launchAppPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_icon, launchAppPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating single widget $appWidgetId: ${e.message}")
            }
        }
    }
}
