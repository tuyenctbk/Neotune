package com.easeaudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StationMonogramAvatar(
    name: String,
    genre: String = "",
    isPodcast: Boolean = false,
    shape: Shape? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    StationGradientPlaceholder(
        stationName = name,
        genre = genre,
        isPodcast = isPodcast,
        shape = shape,
        fontSize = fontSize,
        isPlaying = isPlaying,
        isLoading = false,
        modifier = modifier
    )
}
