package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun GangTheme(
    themeMode: GangThemeMode = GangThemeMode.MIDNIGHT_BLUE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        GangThemeMode.LIGHT -> LightScheme
        GangThemeMode.DARK -> DarkScheme
        GangThemeMode.MIDNIGHT_BLUE -> MidnightBlueScheme
        GangThemeMode.EMERALD -> EmeraldScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
