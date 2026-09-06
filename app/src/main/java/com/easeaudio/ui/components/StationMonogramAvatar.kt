package com.easeaudio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StationMonogramAvatar(
    name: String,
    genre: String = "",
    modifier: Modifier = Modifier
) {
    val gradients = remember {
        listOf(
            listOf(Color(0xFF0D9488), Color(0xFF0284C7)), // Teal -> Sky
            listOf(Color(0xFF7C3AED), Color(0xFFC026D3)), // Violet -> Fuchsia
            listOf(Color(0xFFE11D48), Color(0xFFEA580C)), // Rose -> Orange
            listOf(Color(0xFF2563EB), Color(0xFF4F46E5)), // Blue -> Indigo
            listOf(Color(0xFF059669), Color(0xFF10B981)), // Emerald -> Green
            listOf(Color(0xFFD97706), Color(0xFFB45309))  // Amber -> Bronze
        )
    }
    val colorIndex = remember(name) {
        kotlin.math.abs(name.hashCode()) % gradients.size
    }
    val gradientColors = gradients[colorIndex]

    val initials = remember(name) {
        val clean = name.trim().replace(Regex("""\([^)]*\)"""), "").trim()
        val parts = clean.split(Regex("""[\s\-_.]+""")).filter { it.isNotBlank() }
        when {
            parts.isEmpty() -> "♪"
            parts[0].length in 2..4 && parts[0].all { it.isUpperCase() || it.isDigit() } -> parts[0]
            parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            else -> clean.take(2).uppercase()
        }
    }

    Box(
        modifier = modifier
            .background(Brush.linearGradient(colors = gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Radio,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.14f),
            modifier = Modifier
                .fillMaxSize(0.85f)
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp, y = 6.dp)
        )
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            ),
            maxLines = 1
        )
    }
}
