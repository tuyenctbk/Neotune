package com.easeaudio.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import android.content.Context

data class ThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val activePill: Color,
    val cardBorder: Color
)

object AppThemeState {
    val ThemePresets = listOf(
        ThemePreset(
            id = "universal_purist",
            name = "Universal Purist",
            description = "Cho người yêu tối giản tuyệt đối",
            background = Color(0xFF0A0A0B),
            surface = Color(0xFF18181B),
            surfaceVariant = Color(0xFF27272A),
            primary = Color(0xFFFFFFFF),
            secondary = Color(0xFFA1A1AA),
            tertiary = Color(0xFFE4E4E7),
            accent = Color(0xFFFFFFFF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFA1A1AA),
            textMuted = Color(0xFF71717A),
            activePill = Color(0xFFFFFFFF),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "executive_focus",
            name = "Executive Focus",
            description = "Cho dân văn phòng cần tập trung",
            background = Color(0xFF111315),
            surface = Color(0xFF1D1F21),
            surfaceVariant = Color(0xFF2B2E31),
            primary = Color(0xFFC8CDD0),
            secondary = Color(0xFF8A8D90),
            tertiary = Color(0xFFA0A5A8),
            accent = Color(0xFFC8CDD0),
            textPrimary = Color(0xFFE8E8E8),
            textSecondary = Color(0xFF8A8D90),
            textMuted = Color(0xFF5E6164),
            activePill = Color(0xFFC8CDD0),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "audiophile_hifi",
            name = "Audiophile Hi-Fi",
            description = "Cho người yêu âm thanh chất lượng cao",
            background = Color(0xFF0A1020),
            surface = Color(0xFF151D2E),
            surfaceVariant = Color(0xFF202B41),
            primary = Color(0xFF8E9BFF),
            secondary = Color(0xFF7D8599),
            tertiary = Color(0xFFE6EAF2),
            accent = Color(0xFF8E9BFF),
            textPrimary = Color(0xFFE6EAF2),
            textSecondary = Color(0xFF7D8599),
            textMuted = Color(0xFF4F5768),
            activePill = Color(0xFF8E9BFF),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "night_drive",
            name = "Night Drive",
            description = "Cho lái xe & Android TV / Auto",
            background = Color(0xFF000000),
            surface = Color(0xFF111111),
            surfaceVariant = Color(0xFF222222),
            primary = Color(0xFFFFFFFF),
            secondary = Color(0xFF888888),
            tertiary = Color(0xFFAAAAAA),
            accent = Color(0xFFFFFFFF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFF888888),
            textMuted = Color(0xFF555555),
            activePill = Color(0xFFFFFFFF),
            cardBorder = Color(0x22FFFFFF)
        ),
        ThemePreset(
            id = "nordic_calm",
            name = "Nordic Calm",
            description = "Cho người dùng nữ & yêu sự nhẹ nhàng",
            background = Color(0xFF101012),
            surface = Color(0xFF1A1A1E),
            surfaceVariant = Color(0xFF26262C),
            primary = Color(0xFFD8D8E8),
            secondary = Color(0xFF8E8E93),
            tertiary = Color(0xFFF0F0F3),
            accent = Color(0xFFD8D8E8),
            textPrimary = Color(0xFFF0F0F3),
            textSecondary = Color(0xFF8E8E93),
            textMuted = Color(0xFF5C5C62),
            activePill = Color(0xFFD8D8E8),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "warm_lounge",
            name = "Warm Lounge",
            description = "Cho người nghe chill buổi tối",
            background = Color(0xFF121110),
            surface = Color(0xFF201E1C),
            surfaceVariant = Color(0xFF2D2B28),
            primary = Color(0xFFE8DDD0),
            secondary = Color(0xFF9A9590),
            tertiary = Color(0xFFF5F2ED),
            accent = Color(0xFFE8DDD0),
            textPrimary = Color(0xFFF5F2ED),
            textSecondary = Color(0xFF9A9590),
            textMuted = Color(0xFF66615C),
            activePill = Color(0xFFE8DDD0),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "high_contrast",
            name = "High Contrast Accessible",
            description = "Cho người lớn tuổi & thị lực kém",
            background = Color(0xFF080808),
            surface = Color(0xFF1C1C1C),
            surfaceVariant = Color(0xFF333333),
            primary = Color(0xFFFFFFFF),
            secondary = Color(0xFFB0B0B0),
            tertiary = Color(0xFFFFFFFF),
            accent = Color(0xFFFFFFFF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFB0B0B0),
            textMuted = Color(0xFF888888),
            activePill = Color(0xFFFFFFFF),
            cardBorder = Color(0x33FFFFFF)
        ),
        ThemePreset(
            id = "global_signal",
            name = "Global Signal",
            description = "Cho dân công nghệ & du lịch",
            background = Color(0xFF0E1219),
            surface = Color(0xFF1A2333),
            surfaceVariant = Color(0xFF253249),
            primary = Color(0xFF7AC8FF),
            secondary = Color(0xFF7E8A9E),
            tertiary = Color(0xFFE6EAF0),
            accent = Color(0xFF7AC8FF),
            textPrimary = Color(0xFFE6EAF0),
            textSecondary = Color(0xFF7E8A9E),
            textMuted = Color(0xFF4F5B70),
            activePill = Color(0xFF7AC8FF),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "concrete_industrial",
            name = "Concrete Industrial",
            description = "Cho người thích kiến trúc thô mộc",
            background = Color(0xFF0F0F0F),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2C2C2C),
            primary = Color(0xFFC4C4C4),
            secondary = Color(0xFF7A7A7A),
            tertiary = Color(0xFFEAEAEA),
            accent = Color(0xFFC4C4C4),
            textPrimary = Color(0xFFEAEAEA),
            textSecondary = Color(0xFF7A7A7A),
            textMuted = Color(0xFF4F4F4F),
            activePill = Color(0xFFC4C4C4),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "paper_editorial",
            name = "Paper Editorial",
            description = "Cho người yêu tạp chí, typography",
            background = Color(0xFF0E0E0E),
            surface = Color(0xFF222222),
            surfaceVariant = Color(0xFF2E2E2E),
            primary = Color(0xFFF2F0E8),
            secondary = Color(0xFF9E9A93),
            tertiary = Color(0xFFF2F0E8),
            accent = Color(0xFFF2F0E8),
            textPrimary = Color(0xFFF2F0E8),
            textSecondary = Color(0xFF9E9A93),
            textMuted = Color(0xFF66635E),
            activePill = Color(0xFFF2F0E8),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "youth_neo_lime",
            name = "Youth Neo Lime",
            description = "Trẻ trung năng động, Gen Z chủ lực",
            background = Color(0xFF0E0E10),
            surface = Color(0xFF1C1C1F),
            surfaceVariant = Color(0xFF2A2A2E),
            primary = Color(0xFFD6FF57),
            secondary = Color(0xFF8E8E93),
            tertiary = Color(0xFFF5F5F5),
            accent = Color(0xFFD6FF57),
            textPrimary = Color(0xFFF5F5F5),
            textSecondary = Color(0xFF8E8E93),
            textMuted = Color(0xFF5C5C60),
            activePill = Color(0xFFD6FF57),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "youth_electric_cyan",
            name = "Youth Electric Cyan",
            description = "Trendy TikTok, hợp tên NeoTune nhất",
            background = Color(0xFF0A1010),
            surface = Color(0xFF151E1E),
            surfaceVariant = Color(0xFF202D2D),
            primary = Color(0xFF6CFFEE),
            secondary = Color(0xFF7A9998),
            tertiary = Color(0xFFE8FFFE),
            accent = Color(0xFF6CFFEE),
            textPrimary = Color(0xFFE8FFFE),
            textSecondary = Color(0xFF7A9998),
            textMuted = Color(0xFF4F6665),
            activePill = Color(0xFF6CFFEE),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "youth_acid_yellow",
            name = "Youth Acid Yellow",
            description = "Streetwear, cá tính mạnh",
            background = Color(0xFF10100A),
            surface = Color(0xFF1E1E15),
            surfaceVariant = Color(0xFF2B2B20),
            primary = Color(0xFFE8FF4A),
            secondary = Color(0xFF999789),
            tertiary = Color(0xFFFFFEF0),
            accent = Color(0xFFE8FF4A),
            textPrimary = Color(0xFFFFFEF0),
            textSecondary = Color(0xFF999789),
            textMuted = Color(0xFF66655B),
            activePill = Color(0xFFE8FF4A),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "youth_sunset_orange",
            name = "Youth Sunset Orange",
            description = "Năng lượng, tích cực",
            background = Color(0xFF120E0A),
            surface = Color(0xFF221C15),
            surfaceVariant = Color(0xFF312920),
            primary = Color(0xFFFF8A3D),
            secondary = Color(0xFF9A8A7A),
            tertiary = Color(0xFFFFF2E6),
            accent = Color(0xFFFF8A3D),
            textPrimary = Color(0xFFFFF2E6),
            textSecondary = Color(0xFF9A8A7A),
            textMuted = Color(0xFF665C51),
            activePill = Color(0xFFFF8A3D),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "youth_cyber_pink",
            name = "Youth Cyber Pink",
            description = "Gen Z nữ, soft trẻ trung",
            background = Color(0xFF120A10),
            surface = Color(0xFF22151E),
            surfaceVariant = Color(0xFF31202C),
            primary = Color(0xFFFF6B9E),
            secondary = Color(0xFF9A7A8E),
            tertiary = Color(0xFFFFE6F2),
            accent = Color(0xFFFF6B9E),
            textPrimary = Color(0xFFFFE6F2),
            textSecondary = Color(0xFF9A7A8E),
            textMuted = Color(0xFF66515E),
            activePill = Color(0xFFFF6B9E),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "youth_volt_pop",
            name = "Youth Volt Pop",
            description = "Tối giản + trẻ nhất, cân bằng nhất",
            background = Color(0xFF0C0C0F),
            surface = Color(0xFF1A1A1E),
            surfaceVariant = Color(0xFF27272D),
            primary = Color(0xFFCCFF00),
            secondary = Color(0xFF8E8E93),
            tertiary = Color(0xFFF5F5F5),
            accent = Color(0xFFCCFF00),
            textPrimary = Color(0xFFF5F5F5),
            textSecondary = Color(0xFF8E8E93),
            textMuted = Color(0xFF5C5C62),
            activePill = Color(0xFFCCFF00),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "espresso_bar",
            name = "Espresso Bar",
            description = "Cho cafe specialty, đen nâu đậm",
            background = Color(0xFF0F0D0B),
            surface = Color(0xFF1F1A17),
            surfaceVariant = Color(0xFF2D2520),
            primary = Color(0xFFD7C4B0),
            secondary = Color(0xFF9A8C81),
            tertiary = Color(0xFFF5EFE8),
            accent = Color(0xFFD7C4B0),
            textPrimary = Color(0xFFF5EFE8),
            textSecondary = Color(0xFF9A8C81),
            textMuted = Color(0xFF635850),
            activePill = Color(0xFFD7C4B0),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "bistro_warm",
            name = "Bistro Warm",
            description = "Cho bistro, nhà hàng ấm cúng",
            background = Color(0xFF12100E),
            surface = Color(0xFF231E1A),
            surfaceVariant = Color(0xFF312A24),
            primary = Color(0xFFE07A5F),
            secondary = Color(0xFFA89A8C),
            tertiary = Color(0xFFFFF6ED),
            accent = Color(0xFFE07A5F),
            textPrimary = Color(0xFFFFF6ED),
            textSecondary = Color(0xFFA89A8C),
            textMuted = Color(0xFF6C6258),
            activePill = Color(0xFFE07A5F),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "fine_dining_obsidian",
            name = "Fine Dining Obsidian",
            description = "Cho nhà hàng fine dining cao cấp",
            background = Color(0xFF070709),
            surface = Color(0xFF1A1A1E),
            surfaceVariant = Color(0xFF29292F),
            primary = Color(0xFFD4C5A0),
            secondary = Color(0xFF8E8E93),
            tertiary = Color(0xFFF5F5F7),
            accent = Color(0xFFD4C5A0),
            textPrimary = Color(0xFFF5F5F7),
            textSecondary = Color(0xFF8E8E93),
            textMuted = Color(0xFF5C5C60),
            activePill = Color(0xFFD4C5A0),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "garden_cafe",
            name = "Garden Cafe",
            description = "Cho cafe sân vườn, healthy",
            background = Color(0xFF0E100F),
            surface = Color(0xFF1A201B),
            surfaceVariant = Color(0xFF262E27),
            primary = Color(0xFFA8C3A0),
            secondary = Color(0xFF8A9A88),
            tertiary = Color(0xFFE8F0E6),
            accent = Color(0xFFA8C3A0),
            textPrimary = Color(0xFFE8F0E6),
            textSecondary = Color(0xFF8A9A88),
            textMuted = Color(0xFF5B665A),
            activePill = Color(0xFFA8C3A0),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "wine_bar",
            name = "Wine Bar",
            description = "Cho quán rượu, wine bar tối",
            background = Color(0xFF120A0E),
            surface = Color(0xFF21151E),
            surfaceVariant = Color(0xFF311F2B),
            primary = Color(0xFFC46B8A),
            secondary = Color(0xFF9A7A8E),
            tertiary = Color(0xFFFCE8F0),
            accent = Color(0xFFC46B8A),
            textPrimary = Color(0xFFFCE8F0),
            textSecondary = Color(0xFF9A7A8E),
            textMuted = Color(0xFF66515E),
            activePill = Color(0xFFC46B8A),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "minimalist_cafe",
            name = "Minimalist Cafe",
            description = "Cho cafe tối giản trắng gỗ",
            background = Color(0xFF101010),
            surface = Color(0xFF1C1C1C),
            surfaceVariant = Color(0xFF2A2A2A),
            primary = Color(0xFFE8DDD0),
            secondary = Color(0xFF8A8A8A),
            tertiary = Color(0xFFF0F0F0),
            accent = Color(0xFFE8DDD0),
            textPrimary = Color(0xFFF0F0F0),
            textSecondary = Color(0xFF8A8A8A),
            textMuted = Color(0xFF5C5C5C),
            activePill = Color(0xFFE8DDD0),
            cardBorder = Color(0xFF2A2A2A)
        ),
        ThemePreset(
            id = "trattoria",
            name = "Trattoria",
            description = "Cho nhà hàng Ý, pizza",
            background = Color(0xFF120E0A),
            surface = Color(0xFF221C15),
            surfaceVariant = Color(0xFF312920),
            primary = Color(0xFFB5A886),
            secondary = Color(0xFF9A8A7A),
            tertiary = Color(0xFFFFF2E6),
            accent = Color(0xFFB5A886),
            textPrimary = Color(0xFFFFF2E6),
            textSecondary = Color(0xFF9A8A7A),
            textMuted = Color(0xFF665C51),
            activePill = Color(0xFFB5A886),
            cardBorder = Color(0x12FFFFFF)
        ),
        ThemePreset(
            id = "youth_cafe",
            name = "Youth Cafe",
            description = "Cho cafe giới trẻ, check-in",
            background = Color(0xFF0E0E10),
            surface = Color(0xFF1C1C1F),
            surfaceVariant = Color(0xFF2A2A2E),
            primary = Color(0xFFFFB18F),
            secondary = Color(0xFF8E8E93),
            tertiary = Color(0xFFF5F5F5),
            accent = Color(0xFFFFB18F),
            textPrimary = Color(0xFFF5F5F5),
            textSecondary = Color(0xFF8E8E93),
            textMuted = Color(0xFF5C5C60),
            activePill = Color(0xFFFFB18F),
            cardBorder = Color(0x12FFFFFF)
        )
    )

    var currentTheme by mutableStateOf(ThemePresets[0])

    fun loadTheme(context: Context) {
        val prefs = context.getSharedPreferences("neotune_theme_prefs", Context.MODE_PRIVATE)
        val savedThemeId = prefs.getString("selected_theme_id", "universal_purist")
        val matchedTheme = ThemePresets.firstOrNull { it.id == savedThemeId } ?: ThemePresets[0]
        currentTheme = matchedTheme
    }

    fun saveTheme(context: Context, themeId: String) {
        val matchedTheme = ThemePresets.firstOrNull { it.id == themeId } ?: ThemePresets[0]
        currentTheme = matchedTheme
        val prefs = context.getSharedPreferences("neotune_theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_theme_id", themeId).apply()
    }
}

val DarkBackground: Color get() = AppThemeState.currentTheme.background
val DarkSurface: Color get() = AppThemeState.currentTheme.surface
val DarkSurfaceVariant: Color get() = AppThemeState.currentTheme.surfaceVariant

val NeonCyan: Color get() = AppThemeState.currentTheme.primary
val NeonPurple: Color get() = AppThemeState.currentTheme.secondary
val NeonPink: Color get() = AppThemeState.currentTheme.tertiary
val AccentOrange: Color get() = AppThemeState.currentTheme.accent
val NeonYellow: Color = Color(0xFFFFD600)
val NeonOrange: Color = Color(0xFFFF9100)

val TextPrimary: Color get() = AppThemeState.currentTheme.textPrimary
val TextSecondary: Color get() = AppThemeState.currentTheme.textSecondary
val TextMuted: Color get() = AppThemeState.currentTheme.textMuted

val ActivePill: Color get() = AppThemeState.currentTheme.activePill
val CardBorder: Color get() = AppThemeState.currentTheme.cardBorder

val FavoriteRed = Color(0xFFFF3B30)

private fun isVibrantColor(color: Color): Boolean {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    if (max <= 0.05f) return false
    val saturation = (max - min) / max
    return saturation > 0.18f && max > 0.25f
}

val FavoriteHeartColor: Color
    get() {
        val theme = AppThemeState.currentTheme
        val primary = theme.primary
        val tertiary = theme.tertiary
        val accent = theme.accent
        return when {
            isVibrantColor(tertiary) -> tertiary
            isVibrantColor(primary) -> primary
            isVibrantColor(accent) -> accent
            else -> FavoriteRed
        }
    }

val WaveformAnimationColors: List<Color>
    get() {
        val theme = AppThemeState.currentTheme
        val primary = theme.primary
        val secondary = theme.secondary
        val tertiary = theme.tertiary
        val accent = theme.accent

        val candidates = listOf(primary, secondary, tertiary, accent).filter { isVibrantColor(it) }.distinct()
        return if (candidates.isNotEmpty()) {
            candidates
        } else {
            listOf(
                primary,
                primary.copy(alpha = 0.82f),
                secondary.copy(alpha = 0.65f),
                primary.copy(alpha = 0.9f)
            )
        }
    }
