package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand colors
val GangIndigoPrimary = Color(0xFF6366F1)
val GangIndigoSecondary = Color(0xFF818CF8)
val GangIndigoDarkBg = Color(0xFF0F172A)
val GangIndigoSurface = Color(0xFF1E293B)
val GangIndigoCard = Color(0xFF283548)

// Midnight Blue
val MidnightNavyBg = Color(0xFF0A0F1D)
val MidnightNavySurface = Color(0xFF131C31)
val MidnightNavyCard = Color(0xFF1B2845)
val MidnightCyanAccent = Color(0xFF06B6D4)
val MidnightPurpleAccent = Color(0xFF8B5CF6)

// Emerald Theme
val EmeraldDarkBg = Color(0xFF06231A)
val EmeraldDarkSurface = Color(0xFF0B3327)
val EmeraldDarkCard = Color(0xFF104737)
val EmeraldPrimary = Color(0xFF10B981)
val EmeraldAccent = Color(0xFF34D399)

// Standard Dark
val StandardDarkBg = Color(0xFF121212)
val StandardDarkSurface = Color(0xFF1E1E1E)
val StandardDarkCard = Color(0xFF2C2C2C)

// Standard Light
val StandardLightBg = Color(0xFFF8FAFC)
val StandardLightSurface = Color(0xFFFFFFFF)
val StandardLightCard = Color(0xFFF1F5F9)

enum class GangThemeMode {
    LIGHT,
    DARK,
    MIDNIGHT_BLUE,
    EMERALD
}

val LightScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    background = StandardLightBg,
    surface = StandardLightSurface,
    surfaceVariant = StandardLightCard,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

val DarkScheme = darkColorScheme(
    primary = GangIndigoSecondary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    background = StandardDarkBg,
    surface = StandardDarkSurface,
    surfaceVariant = StandardDarkCard,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

val MidnightBlueScheme = darkColorScheme(
    primary = MidnightCyanAccent,
    onPrimary = Color(0xFF00363F),
    primaryContainer = Color(0xFF0E4A56),
    onPrimaryContainer = Color(0xFFB5E8F7),
    secondary = MidnightPurpleAccent,
    onSecondary = Color.White,
    background = MidnightNavyBg,
    surface = MidnightNavySurface,
    surfaceVariant = MidnightNavyCard,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color(0xFF94A3B8)
)

val EmeraldScheme = darkColorScheme(
    primary = EmeraldAccent,
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF003731),
    background = EmeraldDarkBg,
    surface = EmeraldDarkSurface,
    surfaceVariant = EmeraldDarkCard,
    onBackground = Color(0xFFF0FDF4),
    onSurface = Color(0xFFF0FDF4),
    onSurfaceVariant = Color(0xFFA7F3D0)
)
