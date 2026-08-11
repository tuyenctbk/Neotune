package com.easeaudio.ui.theme

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ArtworkPaletteState(
    val dominantColor: Color = NeonCyan,
    val vibrantColor: Color = NeonPurple,
    val darkMutedColor: Color = DarkBackground,
    val lightVibrantColor: Color = NeonCyan
)

@Composable
fun rememberArtworkPalette(imageUrl: String?): ArtworkPaletteState {
    val context = LocalContext.current
    var paletteState by remember(imageUrl) { mutableStateOf(ArtworkPaletteState()) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            paletteState = ArtworkPaletteState()
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        val bitmap = drawable.bitmap
                        val palette = Palette.from(bitmap).generate()

                        val dominantRgb = palette.getDominantColor(0xFF00E5FF.toInt())
                        val vibrantRgb = palette.getVibrantColor(palette.getLightVibrantColor(0xFF9D4EDD.toInt()))
                        val darkMutedRgb = palette.getDarkMutedColor(palette.getDarkVibrantColor(0xFF0D0E15.toInt()))
                        val lightVibrantRgb = palette.getLightVibrantColor(palette.getVibrantColor(0xFF00E5FF.toInt()))

                        paletteState = ArtworkPaletteState(
                            dominantColor = Color(dominantRgb),
                            vibrantColor = Color(vibrantRgb),
                            darkMutedColor = Color(darkMutedRgb),
                            lightVibrantColor = Color(lightVibrantRgb)
                        )
                    }
                }
            } catch (_: Exception) {
                // Fallback default theme colors on network or decode error
            }
        }
    }
    return paletteState
}
