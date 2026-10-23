package com.easeaudio.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.easeaudio.data.RadioStation
import com.easeaudio.service.RadioPlaybackService
import com.easeaudio.service.RadioPlayerManager

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val stationId = intent.getStringExtra("station_id") ?: ""
        val stationName = intent.getStringExtra("station_name") ?: "Radio"
        val stationUrl = intent.getStringExtra("station_url") ?: ""

        Log.i("AlarmReceiver", "Radio Alarm Triggered for station: $stationName ($stationUrl)")

        if (stationUrl.isNotBlank()) {
            val station = RadioStation(
                id = stationId.ifBlank { "alarm_station" },
                name = stationName,
                genre = "Alarm",
                country = "Global",
                streamUrl = stationUrl,
                imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80"
            )

            try {
                // Play station directly via singleton RadioPlayerManager.
                // RadioPlayerManager manages player preparation and service attachment safely,
                // avoiding ForegroundServiceDidNotStartInTimeException on Android 8.0+.
                RadioPlayerManager.getInstance(context).playStation(station)
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Could not play alarm station: ${e.message}")
            }
        }
    }
}
