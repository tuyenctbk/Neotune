# ProGuard & R8 Optimization and Obfuscation Rules for NeoTune
# Designed to maximize DEX obfuscation while preserving reflection, Room, ExoPlayer, and third-party stability.

# ---------------------------------------------------------------------------
# General Optimization & Obfuscation Controls
# ---------------------------------------------------------------------------
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Retain debug stack traces with source file and line numbers
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Retain annotations and generic signatures for runtime and reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ---------------------------------------------------------------------------
# AndroidX & Jetpack Compose
# ---------------------------------------------------------------------------
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

-keepclassmembers class androidx.compose.runtime.Recomposer {
    public *;
}

# ---------------------------------------------------------------------------
# Room Database (Entities, DAOs, Migrations)
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract <methods>;
}
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Media3 ExoPlayer & MediaSession
# ---------------------------------------------------------------------------
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# Maintain MediaPlaybackService and custom AudioProcessors
-keep class com.easeaudio.service.RadioPlaybackService { *; }
-keep class com.easeaudio.service.ParametricEqAudioProcessor { *; }

# ---------------------------------------------------------------------------
# Network & Third-Party HTTP (OkHttp, Okio)
# ---------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepclassmembers class okhttp3.OkHttpClient {
    <fields>;
    <methods>;
}

# ---------------------------------------------------------------------------
# Coil Image Loading & Palette
# ---------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**
-keep class androidx.palette.graphics.** { *; }

# ---------------------------------------------------------------------------
# Firebase & Google Play Services
# ---------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.android.play.core.review.** { *; }
-dontwarn com.google.android.play.core.**

# ---------------------------------------------------------------------------
# App Data Models & Room Entities
# ---------------------------------------------------------------------------
-keepclassmembers class com.easeaudio.data.RadioStation { *; }
-keepclassmembers class com.easeaudio.data.FavoriteStation { *; }
-keepclassmembers class com.easeaudio.data.RecentSearchQuery { *; }
-keepclassmembers class com.easeaudio.data.ListenLaterItem { *; }
-keepclassmembers class com.easeaudio.data.PodcastEpisode { *; }

# ---------------------------------------------------------------------------
# App Widget Provider & Alarm Receivers
# ---------------------------------------------------------------------------
-keep class com.easeaudio.widget.NeoTuneAppWidgetProvider { *; }
-keep class com.easeaudio.alarm.AlarmReceiver { *; }

# ---------------------------------------------------------------------------
# Kotlin Coroutines
# ---------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }
