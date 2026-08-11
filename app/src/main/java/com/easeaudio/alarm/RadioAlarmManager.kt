package com.easeaudio.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object RadioAlarmManager {

    private const val TAG = "RadioAlarmManager"
    private const val PREFS_NAME = "radio_alarm_prefs"
    private const val KEY_ALARM_ENABLED = "alarm_enabled"
    private const val KEY_ALARM_HOUR = "alarm_hour"
    private const val KEY_ALARM_MINUTE = "alarm_minute"
    private const val KEY_STATION_ID = "alarm_station_id"
    private const val KEY_STATION_NAME = "alarm_station_name"
    private const val KEY_STATION_URL = "alarm_station_url"

    data class AlarmConfig(
        val isEnabled: Boolean,
        val hour: Int,
        val minute: Int,
        val stationId: String,
        val stationName: String,
        val stationUrl: String
    )

    fun getAlarmConfig(context: Context): AlarmConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AlarmConfig(
            isEnabled = prefs.getBoolean(KEY_ALARM_ENABLED, false),
            hour = prefs.getInt(KEY_ALARM_HOUR, 7),
            minute = prefs.getInt(KEY_ALARM_MINUTE, 0),
            stationId = prefs.getString(KEY_STATION_ID, "") ?: "",
            stationName = prefs.getString(KEY_STATION_NAME, "Radio") ?: "Radio",
            stationUrl = prefs.getString(KEY_STATION_URL, "") ?: ""
        )
    }

    fun setAlarm(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        stationId: String,
        stationName: String,
        stationUrl: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ALARM_ENABLED, enabled)
            .putInt(KEY_ALARM_HOUR, hour)
            .putInt(KEY_ALARM_MINUTE, minute)
            .putString(KEY_STATION_ID, stationId)
            .putString(KEY_STATION_NAME, stationName)
            .putString(KEY_STATION_URL, stationUrl)
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("station_id", stationId)
            putExtra("station_name", stationName)
            putExtra("station_url", stationUrl)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (enabled) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Log.i(TAG, "Radio Alarm scheduled for ${calendar.time}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed exact alarm, trying fallback: ${e.message}")
                try {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e2: Exception) {
                    Log.e(TAG, "Fallback alarm failed: ${e2.message}")
                }
            }
        } else {
            alarmManager.cancel(pendingIntent)
            Log.i(TAG, "Radio Alarm canceled.")
        }
    }

    fun setWakeUpStation(context: Context, stationId: String, stationName: String, stationUrl: String) {
        val current = getAlarmConfig(context)
        setAlarm(context, current.isEnabled, current.hour, current.minute, stationId, stationName, stationUrl)
    }
}
