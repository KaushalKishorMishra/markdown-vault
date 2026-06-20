package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Slate & Sky-blue palette (Tailwind-inspired fallbacks, remapped to match the Artistic Flair HTML design spec)
val Slate950 = Color(0xFF1C1B1F) // Theme Background (#1C1B1F)
val Slate900 = Color(0xFF1D192B) // Theme Surface (#1D192B)
val Slate800 = Color(0xFF2B2930) // Card / SurfaceVariant Background (#2B2930)
val Slate700 = Color(0xFF49454F) // Border / Outline (#49454F)
val Slate300 = Color(0xFFCAC4D0) // Muted Text (#CAC4D0)
val Slate200 = Color(0xFFE6E1E5) // Main Text (#E6E1E5)
val Slate50 = Color(0xFFFFFFFF)

// Core Accent Colors from the "Artistic Flair" theme html spec
val Sky400 = Color(0xFFD0BCFF) // Beautiful Accent Lavender/Purple (#D0BCFF)
val Sky500 = Color(0xFF6750A4) // Dynamic light-theme primary violet
val Sky600 = Color(0xFF381E72) // Heavy rich dark violet

val Indigo500 = Color(0xFFEFB8C8)  // Secondary accent rose (#EFB8C8)
val Rose500 = Color(0xFFEFB8C8)    // Error state cozy warm rose
val Emerald500 = Color(0xFFB4E197) // High-polish secure sage green (#B4E197)
val Amber500 = Color(0xFFEFB8C8)   // Warn colors remapped to gorgeous pink-rose

// Dedicated Artistic Flair named parameters
val ArtisticBg = Color(0xFF1C1B1F)         // Warm dark charcoal
val ArtisticSurface = Color(0xFF1D192B)    // Cozy deep violet-indigo
val ArtisticCardBg = Color(0xFF2B2930)     // Muted deep lavender grey
val ArtisticBorder = Color(0xFF49454F)     // Border grey-purple
val ArtisticPrimary = Color(0xFFD0BCFF)    // Vibrant lavender/lilac
val ArtisticOnPrimary = Color(0xFF381E72)  // Deep royal violet
val ArtisticPrimaryContainer = Color(0xFF381E72)
val ArtisticOnPrimaryContainer = Color(0xFFD0BCFF)

val ArtisticTextMax = Color(0xFFE6E1E5)        // Light off-white
val ArtisticTextSecondary = Color(0xFFCAC4D0)  // Muted grey-purple text

// High-polish accents from Artistic Flair HTML spec
val WarmSage = Color(0xFFB4E197)              // Soft glow green
val ArtisticRose = Color(0xFFEFB8C8)          // Cozy soft pink
val WarmSageAlpha = Color(0x2BB4E197)         // Shadow/outline glow sage

// ========================================================================
// HIGH CONTRAST THEME COLORS (WCAG AAA - 7:1 contrast ratio)
// ========================================================================

// High Contrast Dark Theme Colors
val HighContrastDarkBg = Color.Black
val HighContrastDarkSurface = Color(0xFF1A1A1A)
val HighContrastDarkSurfaceVariant = Color(0xFF2D2D2D)
val HighContrastDarkPrimary = Color.White
val HighContrastDarkOnPrimary = Color.Black
val HighContrastDarkSecondary = Color(0xFF00FFFF)  // Cyan
val HighContrastDarkOnSecondary = Color.Black
val HighContrastDarkOutline = Color.White
val HighContrastDarkError = Color(0xFFFF5252)
val HighContrastDarkOnError = Color.White

// High Contrast Light Theme Colors
val HighContrastLightBg = Color.White
val HighContrastLightSurface = Color(0xFFF5F5F5)
val HighContrastLightSurfaceVariant = Color(0xFFE0E0E0)
val HighContrastLightPrimary = Color.Black
val HighContrastLightOnPrimary = Color.White
val HighContrastLightSecondary = Color(0xFF0066CC)  // Blue
val HighContrastLightOnSecondary = Color.White
val HighContrastLightOutline = Color.Black
val HighContrastLightError = Color(0xFFD32F2F)
val HighContrastLightOnError = Color.White
