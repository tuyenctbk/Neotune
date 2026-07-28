import re

manifest_content = '''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.EaseAudio"
        android:usesCleartextTraffic="true"
        tools:targetApi="31">

        <provider
            android:name="com.google.android.gms.ads.MobileAdsInitProvider"
            android:authorities="${applicationId}.mobileadsinitprovider"
            tools:node="remove" />

        <provider
            android:name="com.google.firebase.provider.FirebaseInitProvider"
            android:authorities="${applicationId}.firebaseinitprovider"
            tools:node="remove" />

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
            android:theme="@style/Theme.EaseAudio.Starting">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Sample AdMob App ID for testing -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-3940256099942544~3347511713"/>

        <service
            android:name=".service.RadioPlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
                <action android:name="android.intent.action.MEDIA_BUTTON" />
            </intent-filter>
        </service>
    </application>
</manifest>
'''

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(manifest_content)

with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'r') as f:
    rpm_content = f.read()

rpm_content = re.sub(r'    private val attributionContext: Context by lazy \{.*?\n    \}\n', '', rpm_content, flags=re.DOTALL)
rpm_content = rpm_content.replace('ExoPlayer.Builder(attributionContext)', 'ExoPlayer.Builder(context)')
rpm_content = rpm_content.replace('MediaSession.Builder(attributionContext, player)', 'MediaSession.Builder(context, player)')

with open('app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt', 'w') as f:
    f.write(rpm_content)

service_content = '''package com.easeaudio.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class RadioPlaybackService : MediaSessionService() {
    override fun onCreate() {
        super.onCreate()
        val playerManager = RadioPlayerManager.getInstance(this)
        RadioPlayerManager.sharedMediaSession?.let { session ->
            addSession(session)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        RadioPlayerManager.getInstance(this)
        return RadioPlayerManager.sharedMediaSession
    }

    override fun onDestroy() {
        RadioPlayerManager.getInstance(this).release()
        super.onDestroy()
    }
}
'''

with open('app/src/main/java/com/easeaudio/service/RadioPlaybackService.kt', 'w') as f:
    f.write(service_content)

print("Cleaned up attribution and restored standard context handling.")
