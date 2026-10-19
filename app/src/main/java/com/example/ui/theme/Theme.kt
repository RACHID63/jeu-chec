package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AntiqueColorScheme = darkColorScheme(
    primary = RomanGold,
    onPrimary = ObsidianSurface,
    primaryContainer = RomanGoldDark,
    onPrimaryContainer = RomanGoldLight,
    secondary = VerdigrisPatinaLight,
    onSecondary = ObsidianSurface,
    tertiary = Terracotta,
    background = ObsidianSurface,
    surface = ObsidianCard,
    onBackground = CarraraMarble,
    onSurface = CarraraMarble
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep antique atmospheric palette
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianSurface.toArgb()
                window.navigationBarColor = ObsidianSurface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = AntiqueColorScheme,
        typography = Typography,
        content = content
    )
}
