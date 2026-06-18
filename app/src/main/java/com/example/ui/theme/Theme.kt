package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ArtisticPrimary,
    onPrimary = ArtisticOnPrimary,
    primaryContainer = ArtisticPrimaryContainer,
    onPrimaryContainer = ArtisticOnPrimaryContainer,
    secondary = ArtisticRose,
    onSecondary = ArtisticOnPrimary,
    background = ArtisticBg,
    onBackground = ArtisticTextMax,
    surface = ArtisticSurface,
    onSurface = ArtisticTextMax,
    surfaceVariant = ArtisticCardBg,
    onSurfaceVariant = ArtisticTextSecondary,
    outline = ArtisticBorder,
    error = ArtisticRose,
    onError = ArtisticOnPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4), // Elegant light purple
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    background = Color(0xFFFEF7FF), // Soft lavender cream
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFF7F2FA),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    error = Color(0xFFB3261E),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force darkTheme to true if we always want the stunning dark Artistic Flair aesthetic,
    // or let it follow. Let's use dark colors for darkTheme and beautiful light purple for light.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
