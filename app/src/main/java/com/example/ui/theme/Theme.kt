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

private val CyberpunkColorScheme = darkColorScheme(
    primary = Color(0xFF00F0FF), // Neon Cyan
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1F002B),
    onPrimaryContainer = Color(0xFF00F0FF),
    secondary = Color(0xFFFF007F), // Neon Pink
    onSecondary = Color.White,
    background = Color(0xFF0A0A0C), // Pure Blackish
    onBackground = Color.White,
    surface = Color(0xFF121216),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E24),
    onSurfaceVariant = Color(0xFF8A8A93),
    outline = Color(0xFF3A3A4A),
    error = Color(0xFFFF0055),
    onError = Color.White
)

private val EmeraldColorScheme = darkColorScheme(
    primary = Color(0xFF81C784), // Emerald Green
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF1B3D2F),
    onPrimaryContainer = Color(0xFF81C784),
    secondary = Color(0xFFAED581), // Light green-yellow
    onSecondary = Color(0xFF1D3300),
    background = Color(0xFF111713), // Deep forest charcoal
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF1B241E),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF2E3D33),
    onSurfaceVariant = Color(0xFFA5B8AC),
    outline = Color(0xFF435C4C),
    error = Color(0xFFE57373),
    onError = Color(0xFF5D0000)
)

private val ClassicColorScheme = darkColorScheme(
    primary = Color(0xFFFFB74D), // Warm Amber
    onPrimary = Color(0xFF5D3200),
    primaryContainer = Color(0xFF3D2100),
    onPrimaryContainer = Color(0xFFFFB74D),
    secondary = Color(0xFFFFD54F), // Gold
    onSecondary = Color(0xFF4A3B00),
    background = Color(0xFF1A1612), // Deep chocolate-grey
    onBackground = Color(0xFFFFF3E0),
    surface = Color(0xFF26201B),
    onSurface = Color(0xFFFFF3E0),
    surfaceVariant = Color(0xFF382F27),
    onSurfaceVariant = Color(0xFFD7CCC8),
    outline = Color(0xFF5D4E43),
    error = Color(0xFFE57373),
    onError = Color(0xFF5D0000)
)

@Composable
fun MyApplicationTheme(
    themeName: String = "ARTISTIC",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "CYBERPUNK" -> CyberpunkColorScheme
        "EMERALD" -> EmeraldColorScheme
        "CLASSIC" -> ClassicColorScheme
        "LIGHT" -> LightColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
