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
    val tagline: String = "",
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
    val cardBorder: Color,
    val glowColor: Color = primary
)

object AppThemeState {
    val ThemePresets = listOf(
        ThemePreset(
            id = "nordic_aurora",
            name = "Nordic Aurora",
            tagline = "GLACIAL POLAR NIGHT",
            description = "Calm Scandinavian winter night with deep glacial blues and aurora violet",
            background = Color(0xFF060B14),
            surface = Color(0xFF0E1726),
            surfaceVariant = Color(0xFF17253B),
            primary = Color(0xFF60A5FA),
            secondary = Color(0xFFA78BFA),
            tertiary = Color(0xFF93C5FD),
            accent = Color(0xFF60A5FA),
            textPrimary = Color(0xFFF0F8FF),
            textSecondary = Color(0xFF93C5FD),
            textMuted = Color(0xFF475E7E),
            activePill = Color(0xFF60A5FA),
            cardBorder = Color(0x3360A5FA),
            glowColor = Color(0xFF60A5FA)
        ),
        ThemePreset(
            id = "neo_cyber",
            name = "Neo Cyber",
            tagline = "CYBERPUNK GLOW",
            description = "Signature futuristic cyberpunk with electric cyan glow",
            background = Color(0xFF080B10),
            surface = Color(0xFF111722),
            surfaceVariant = Color(0xFF1B2434),
            primary = Color(0xFF00F0FF),
            secondary = Color(0xFF8B5CF6),
            tertiary = Color(0xFF38BDF8),
            accent = Color(0xFF00F0FF),
            textPrimary = Color(0xFFF1F5F9),
            textSecondary = Color(0xFF94A3B8),
            textMuted = Color(0xFF475569),
            activePill = Color(0xFF00F0FF),
            cardBorder = Color(0x3300F0FF),
            glowColor = Color(0xFF00F0FF)
        ),
        ThemePreset(
            id = "oled_midnight",
            name = "OLED Midnight",
            tagline = "TRUE BLACK OLED",
            description = "Pure pitch-black OLED contrast with crisp monochromatic purity",
            background = Color(0xFF000000),
            surface = Color(0xFF0D0D10),
            surfaceVariant = Color(0xFF1A1A20),
            primary = Color(0xFFFFFFFF),
            secondary = Color(0xFFA1A1AA),
            tertiary = Color(0xFFE4E4E7),
            accent = Color(0xFFFFFFFF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFA1A1AA),
            textMuted = Color(0xFF71717A),
            activePill = Color(0xFFFFFFFF),
            cardBorder = Color(0x28FFFFFF),
            glowColor = Color(0xFFFFFFFF)
        ),
        ThemePreset(
            id = "neon_volt",
            name = "Neon Volt",
            tagline = "HIGH-VOLTAGE ENERGY",
            description = "Electric neon lime punch designed for high energy and athletic focus",
            background = Color(0xFF080A06),
            surface = Color(0xFF13170F),
            surfaceVariant = Color(0xFF1E2518),
            primary = Color(0xFFD6FF57),
            secondary = Color(0xFFA3B18A),
            tertiary = Color(0xFFE9FF70),
            accent = Color(0xFFD6FF57),
            textPrimary = Color(0xFFF7FEE7),
            textSecondary = Color(0xFFA3B18A),
            textMuted = Color(0xFF586249),
            activePill = Color(0xFFD6FF57),
            cardBorder = Color(0x33D6FF57),
            glowColor = Color(0xFFD6FF57)
        ),
        ThemePreset(
            id = "sunset_horizon",
            name = "Sunset Horizon",
            tagline = "WARM COPPER DUSK",
            description = "Warm radiant sunset tones blending rich copper and golden amber",
            background = Color(0xFF0D0704),
            surface = Color(0xFF1C100A),
            surfaceVariant = Color(0xFF2B1910),
            primary = Color(0xFFFF7A30),
            secondary = Color(0xFFF59E0B),
            tertiary = Color(0xFFFF9E7A),
            accent = Color(0xFFFF7A30),
            textPrimary = Color(0xFFFFF7ED),
            textSecondary = Color(0xFFA88D7E),
            textMuted = Color(0xFF6B5548),
            activePill = Color(0xFFFF7A30),
            cardBorder = Color(0x33FF7A30),
            glowColor = Color(0xFFFF7A30)
        ),
        ThemePreset(
            id = "espresso_velvet",
            name = "Espresso Velvet",
            tagline = "ARTISAN COFFEE & JAZZ",
            description = "Cozy artisan coffeehouse with warm roasted mocha and creamy caramel",
            background = Color(0xFF0C0907),
            surface = Color(0xFF19130F),
            surfaceVariant = Color(0xFF261D17),
            primary = Color(0xFFDDB892),
            secondary = Color(0xFFB09E8F),
            tertiary = Color(0xFFF3E5D8),
            accent = Color(0xFFDDB892),
            textPrimary = Color(0xFFFDFBF7),
            textSecondary = Color(0xFFA8988B),
            textMuted = Color(0xFF66584E),
            activePill = Color(0xFFDDB892),
            cardBorder = Color(0x33DDB892),
            glowColor = Color(0xFFDDB892)
        ),
        ThemePreset(
            id = "cyber_magenta",
            name = "Cyber Magenta",
            tagline = "SYNTHWAVE NEON",
            description = "Vibrant neon magenta and dreamy synthwave rose glow",
            background = Color(0xFF0F060D),
            surface = Color(0xFF1C0D19),
            surfaceVariant = Color(0xFF2B1427),
            primary = Color(0xFFFF3385),
            secondary = Color(0xFFC084FC),
            tertiary = Color(0xFFFF70A6),
            accent = Color(0xFFFF3385),
            textPrimary = Color(0xFFFFF0F7),
            textSecondary = Color(0xFFA8849C),
            textMuted = Color(0xFF66495E),
            activePill = Color(0xFFFF3385),
            cardBorder = Color(0x33FF3385),
            glowColor = Color(0xFFFF3385)
        ),
        ThemePreset(
            id = "emerald_botanical",
            name = "Emerald Botanical",
            tagline = "ORGANIC RAINFOREST",
            description = "Lush botanical forest with soothing emerald and organic sage accents",
            background = Color(0xFF060D09),
            surface = Color(0xFF0E1A13),
            surfaceVariant = Color(0xFF17291F),
            primary = Color(0xFF34D399),
            secondary = Color(0xFF86A789),
            tertiary = Color(0xFF6EE7B7),
            accent = Color(0xFF34D399),
            textPrimary = Color(0xFFF0FDF4),
            textSecondary = Color(0xFF86A789),
            textMuted = Color(0xFF3D5745),
            activePill = Color(0xFF34D399),
            cardBorder = Color(0x3334D399),
            glowColor = Color(0xFF34D399)
        ),
        ThemePreset(
            id = "midnight_slate",
            name = "Midnight Slate",
            tagline = "PROFESSIONAL STEALTH",
            description = "Deep slate grey and professional charcoal for refined focus",
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFF334155),
            primary = Color(0xFF38BDF8),
            secondary = Color(0xFF94A3B8),
            tertiary = Color(0xFF7DD3FC),
            accent = Color(0xFF0EA5E9),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFCBD5E1),
            textMuted = Color(0xFF64748B),
            activePill = Color(0xFF38BDF8),
            cardBorder = Color(0x2838BDF8),
            glowColor = Color(0xFF38BDF8)
        ),
        ThemePreset(
            id = "crimson_phantom",
            name = "Crimson Phantom",
            tagline = "INTENSE HIGH-CONTRAST",
            description = "Aggressive phantom black with intense crimson accents",
            background = Color(0xFF0A0202),
            surface = Color(0xFF150505),
            surfaceVariant = Color(0xFF220808),
            primary = Color(0xFFFF1744),
            secondary = Color(0xFFCFD8DC),
            tertiary = Color(0xFFFF5252),
            accent = Color(0xFFFF1744),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFB0BEC5),
            textMuted = Color(0xFF546E7A),
            activePill = Color(0xFFFF1744),
            cardBorder = Color(0x33FF1744),
            glowColor = Color(0xFFFF1744)
        ),
        ThemePreset(
            id = "golden_hour",
            name = "Golden Hour",
            tagline = "RADIANT AMBER GLOW",
            description = "The magical warmth of a fading sun with rich amber and gold",
            background = Color(0xFF120C04),
            surface = Color(0xFF1F160A),
            surfaceVariant = Color(0xFF2E2012),
            primary = Color(0xFFFBBF24),
            secondary = Color(0xFFD97706),
            tertiary = Color(0xFFFDE68A),
            accent = Color(0xFFFBBF24),
            textPrimary = Color(0xFFFFFBEB),
            textSecondary = Color(0xFFF59E0B),
            textMuted = Color(0xFF78350F),
            activePill = Color(0xFFFBBF24),
            cardBorder = Color(0x33FBBF24),
            glowColor = Color(0xFFFBBF24)
        ),
        ThemePreset(
            id = "deep_sea",
            name = "Deep Sea",
            tagline = "ABYSSAL EXPLORATION",
            description = "Immersive ocean depths with deep navy and electric teal",
            background = Color(0xFF020617),
            surface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFF1E293B),
            primary = Color(0xFF2DD4BF),
            secondary = Color(0xFF38BDF8),
            tertiary = Color(0xFF99F6E4),
            accent = Color(0xFF14B8A6),
            textPrimary = Color(0xFFF1F5F9),
            textSecondary = Color(0xFF94A3B8),
            textMuted = Color(0xFF334155),
            activePill = Color(0xFF2DD4BF),
            cardBorder = Color(0x332DD4BF),
            glowColor = Color(0xFF2DD4BF)
        ),
        ThemePreset(
            id = "candy_pop",
            name = "Candy Pop",
            tagline = "PLAYFUL NEON VIBE",
            description = "Explosive candy pink and bubblegum cyan for a high-energy fun UI",
            background = Color(0xFF0D0212),
            surface = Color(0xFF1A0524),
            surfaceVariant = Color(0xFF260936),
            primary = Color(0xFFFF00FF),
            secondary = Color(0xFF00FFFF),
            tertiary = Color(0xFFFF77FF),
            accent = Color(0xFFFF00FF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFCC88FF),
            textMuted = Color(0xFF6622AA),
            activePill = Color(0xFFFF00FF),
            cardBorder = Color(0x33FF00FF),
            glowColor = Color(0xFFFF00FF)
        ),
        ThemePreset(
            id = "toxic_waste",
            name = "Toxic Waste",
            tagline = "ACID RADIOACTIVE",
            description = "Edgy radioactive neon green and toxic deep purple",
            background = Color(0xFF020501),
            surface = Color(0xFF0A0F03),
            surfaceVariant = Color(0xFF141F06),
            primary = Color(0xFF39FF14),
            secondary = Color(0xFFBF00FF),
            tertiary = Color(0xFFCCFF00),
            accent = Color(0xFF39FF14),
            textPrimary = Color(0xFFF0FFF0),
            textSecondary = Color(0xFF7CFC00),
            textMuted = Color(0xFF2E4D00),
            activePill = Color(0xFF39FF14),
            cardBorder = Color(0x3339FF14),
            glowColor = Color(0xFF39FF14)
        ),
        ThemePreset(
            id = "vintage_vinyl",
            name = "Vintage Vinyl",
            tagline = "RETRO ANALOG WARMTH",
            description = "Warm retro brown and aged cream with an analog vinyl aesthetic",
            background = Color(0xFF1A120B),
            surface = Color(0xFF2C1E12),
            surfaceVariant = Color(0xFF3D2B1A),
            primary = Color(0xFFE6D5B8),
            secondary = Color(0xFFD4A373),
            tertiary = Color(0xFFFAEDCD),
            accent = Color(0xFFE6D5B8),
            textPrimary = Color(0xFFFEFAE0),
            textSecondary = Color(0xFFD4A373),
            textMuted = Color(0xFF523D26),
            activePill = Color(0xFFE6D5B8),
            cardBorder = Color(0x28E6D5B8),
            glowColor = Color(0xFFE6D5B8)
        ),
        ThemePreset(
            id = "titanium_industrial",
            name = "Titanium Industrial",
            tagline = "MONOCHROMATIC METAL",
            description = "Sleek industrial titanium grey and monochromatic silver",
            background = Color(0xFF0A0A0A),
            surface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFF262626),
            primary = Color(0xFFD1D5DB),
            secondary = Color(0xFF9CA3AF),
            tertiary = Color(0xFFE5E7EB),
            accent = Color(0xFFD1D5DB),
            textPrimary = Color(0xFFF9FAFB),
            textSecondary = Color(0xFF9CA3AF),
            textMuted = Color(0xFF4B5563),
            activePill = Color(0xFFD1D5DB),
            cardBorder = Color(0x28D1D5DB),
            glowColor = Color(0xFFD1D5DB)
        )
    )

    private val LegacyThemeMap = mapOf(
        "universal_purist" to "oled_midnight",
        "night_drive" to "oled_midnight",
        "high_contrast" to "oled_midnight",
        "concrete_industrial" to "oled_midnight",
        "paper_editorial" to "oled_midnight",
        "executive_focus" to "oled_midnight",
        "youth_neo_lime" to "neon_volt",
        "youth_volt_pop" to "neon_volt",
        "youth_acid_yellow" to "neon_volt",
        "youth_electric_cyan" to "neo_cyber",
        "global_signal" to "neo_cyber",
        "youth_sunset_orange" to "sunset_horizon",
        "bistro_warm" to "sunset_horizon",
        "trattoria" to "sunset_horizon",
        "audiophile_hifi" to "nordic_aurora",
        "nordic_calm" to "nordic_aurora",
        "espresso_bar" to "espresso_velvet",
        "warm_lounge" to "espresso_velvet",
        "minimalist_cafe" to "espresso_velvet",
        "fine_dining_obsidian" to "espresso_velvet",
        "youth_cyber_pink" to "cyber_magenta",
        "wine_bar" to "cyber_magenta",
        "garden_cafe" to "emerald_botanical",
        "youth_cafe" to "emerald_botanical"
    )

    var currentTheme by mutableStateOf(ThemePresets[0])
    var isLightMode by mutableStateOf(false)

    fun loadTheme(context: Context) {
        val prefs = context.getSharedPreferences("neotune_theme_prefs", Context.MODE_PRIVATE)
        val savedThemeId = prefs.getString("selected_theme_id", "nordic_aurora") ?: "nordic_aurora"
        val resolvedId = LegacyThemeMap[savedThemeId] ?: savedThemeId
        val matchedTheme = ThemePresets.firstOrNull { it.id == resolvedId } ?: ThemePresets[0]
        currentTheme = matchedTheme
        isLightMode = prefs.getBoolean("is_light_mode", false)
    }

    fun saveTheme(context: Context, themeId: String) {
        val matchedTheme = ThemePresets.firstOrNull { it.id == themeId } ?: ThemePresets[0]
        currentTheme = matchedTheme
        val prefs = context.getSharedPreferences("neotune_theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("selected_theme_id", themeId).apply()
    }

    fun setLightMode(context: Context, enabled: Boolean) {
        isLightMode = enabled
        val prefs = context.getSharedPreferences("neotune_theme_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_light_mode", enabled).apply()
    }
}

val DarkBackground: Color get() = if (AppThemeState.isLightMode) Color(0xFFFCFAF7) else AppThemeState.currentTheme.background
val DarkSurface: Color get() = if (AppThemeState.isLightMode) Color(0xFFF3EFE9) else AppThemeState.currentTheme.surface
val DarkSurfaceVariant: Color get() = if (AppThemeState.isLightMode) Color(0xFFE7E2D8) else AppThemeState.currentTheme.surfaceVariant

val NeonCyan: Color get() = if (AppThemeState.isLightMode) Color(0xFF007585) else AppThemeState.currentTheme.primary
val NeonPurple: Color get() = if (AppThemeState.isLightMode) Color(0xFF6B4A8C) else AppThemeState.currentTheme.secondary
val NeonPink: Color get() = if (AppThemeState.isLightMode) Color(0xFF9E3A6B) else AppThemeState.currentTheme.tertiary
val AccentOrange: Color get() = if (AppThemeState.isLightMode) Color(0xFFC26500) else AppThemeState.currentTheme.accent
val NeonYellow: Color = Color(0xFFFFD600)
val NeonOrange: Color = Color(0xFFFF9100)

val TextPrimary: Color get() = if (AppThemeState.isLightMode) Color(0xFF1B1917) else AppThemeState.currentTheme.textPrimary
val TextSecondary: Color get() = if (AppThemeState.isLightMode) Color(0xFF5A5752) else AppThemeState.currentTheme.textSecondary
val TextMuted: Color get() = if (AppThemeState.isLightMode) Color(0xFF8B8881) else AppThemeState.currentTheme.textMuted

val ActivePill: Color get() = if (AppThemeState.isLightMode) Color(0xFFE7E2D8) else AppThemeState.currentTheme.activePill
val CardBorder: Color get() = if (AppThemeState.isLightMode) Color(0x1F000000) else AppThemeState.currentTheme.cardBorder

val PlayButtonContainer: Color get() = if (AppThemeState.isLightMode) Color(0xFF1B1917) else Color.White
val PlayButtonContent: Color get() = if (AppThemeState.isLightMode) Color(0xFFFCFAF7) else Color.Black

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
