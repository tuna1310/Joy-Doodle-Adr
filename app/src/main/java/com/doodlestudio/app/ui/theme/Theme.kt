package com.doodlestudio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCoralButton,
    onPrimary = Color.White,
    primaryContainer = NeonCoralButton.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = NeonCyan.copy(alpha = 0.2f),
    onSecondaryContainer = Color.White,
    tertiary = NeonUndoButton,
    onTertiary = Color.Black,
    background = NeonBackgroundDark,
    onBackground = Color.White,
    surface = Color(0xFF101010),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFBBBBBB),
    outline = Color(0xFF333333)
)

private val LightColorScheme = lightColorScheme(
    primary = NeonCoralButton,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5EB),
    onPrimaryContainer = Color(0xFF680023),
    secondary = NeonPlayButton,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E4FF),
    onSecondaryContainer = Color(0xFF001B3D),
    tertiary = NeonUndoButton,
    onTertiary = Color.White,
    background = NeonBackgroundGallery,
    onBackground = DarkText,
    surface = LightSurface,
    onSurface = DarkText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = MutedText,
    outline = DividerColor
)

@Composable
fun DoodleStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


