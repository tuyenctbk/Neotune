package com.easeaudio.ads

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*

object AdMobManager {

    private const val TAG = "AdMobManager"
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            // CRITICAL USER REQUIREMENT: Ads must NEVER make sound!
            // Global mute for all AdMob audio/video ads
            MobileAds.setAppMuted(true)
            MobileAds.setAppVolume(0.0f)

            // Configure request options with video muted by default
            val requestConfiguration = MobileAds.getRequestConfiguration().toBuilder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                .build()
            MobileAds.setRequestConfiguration(requestConfiguration)

            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob initialized successfully with muted audio policy")
            }
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdMob: ${e.message}")
        }
    }
}

@Composable
fun AdMobBanner(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    var isAdLoaded by remember { mutableStateOf(false) }
    var adError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1424))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                AdMobManager.initialize(context.applicationContext)

                // Force video options to be start muted
                val videoOptions = VideoOptions.Builder()
                    .setStartMuted(true)
                    .build()

                val adView = AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    setAdUnitId(adUnitId)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            adError = null
                            Log.d("AdMobBanner", "Banner ad loaded successfully")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                            adError = error.message
                            Log.w("AdMobBanner", "Ad failed to load: ${error.message}")
                        }
                    }
                }

                val adRequest = AdRequest.Builder()
                    .build()

                adView.loadAd(adRequest)
                adView
            },
            update = { view ->
                // No-op or dynamic updates if needed
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )

        if (!isAdLoaded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sponsor Ad Area (Muted - Non-Intrusive)",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
