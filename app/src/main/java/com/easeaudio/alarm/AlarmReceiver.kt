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

            // Start foreground service & play station
            val serviceIntent = Intent(context, RadioPlaybackService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            RadioPlayerManager.getInstance(context).playStation(station)
        }
    }
}
