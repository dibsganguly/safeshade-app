package com.safeshade.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// SAFESHADE BRAND ACCENTS (theme-independent)
// ============================================
val AccentPurple = Color(0xFF6C5CE7)
val AccentOrange = Color(0xFFFF9F1C)
val AccentGreen = Color(0xFF00B894)
val AccentRed = Color(0xFFFF7675)
val AccentBlue = Color(0xFF0984E3)
val AccentPink = Color(0xFFE84393)
val AccentTeal = Color(0xFF00CEC9)

// ============================================
// LIGHT SCHEME
// ============================================
val LightBackground = Color(0xFFF5F5F7)
val LightSurface = Color.White
val LightSurfaceGlass = Color(0xCCFFFFFF)
val LightBorderGlass = Color(0x1F2D3436)
val LightOnSurface = Color(0xFF2D3436)
val LightOnSurfaceMuted = Color(0xFFA4B0BE)
val LightOnSurfaceFaint = Color(0xFFB2BEC3)
val LightNeuHighlight = Color(0xFFFFFFFF)
val LightNeuShadow = Color(0xFFD1D4DA)

// ============================================
// DARK SCHEME
// ============================================
val DarkBackground = Color(0xFF14161A)
val DarkSurface = Color(0xFF1E2126)
val DarkSurfaceGlass = Color(0xB324272E)
val DarkBorderGlass = Color(0x33FFFFFF)
val DarkOnSurface = Color(0xFFECEDEF)
val DarkOnSurfaceMuted = Color(0xFF8B93A1)
val DarkOnSurfaceFaint = Color(0xFF6B7280)
val DarkNeuHighlight = Color(0xFF2A2E35)
val DarkNeuShadow = Color(0xFF0A0B0D)

/**
 * Semantic color tokens, resolved per-theme in Theme.kt and exposed via
 * MaterialTheme's LocalSafeShadeColors. Screens should read from this
 * instead of the raw Light-/Dark- constants above so dark mode is automatic.
 */
data class SafeShadeColors(
    val background: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val borderGlass: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val onSurfaceFaint: Color,
    val neuHighlight: Color,
    val neuShadow: Color,
    val accentPrimary: Color = AccentPurple,
    val accentWarning: Color = AccentOrange,
    val accentSuccess: Color = AccentGreen,
    val accentDanger: Color = AccentRed,
    val accentInfo: Color = AccentBlue,
    val accentSecondary: Color = AccentPink,
    val accentTertiary: Color = AccentTeal
)

val LightSafeShadeColors = SafeShadeColors(
    background = LightBackground,
    surface = LightSurface,
    surfaceGlass = LightSurfaceGlass,
    borderGlass = LightBorderGlass,
    onSurface = LightOnSurface,
    onSurfaceMuted = LightOnSurfaceMuted,
    onSurfaceFaint = LightOnSurfaceFaint,
    neuHighlight = LightNeuHighlight,
    neuShadow = LightNeuShadow
)

val DarkSafeShadeColors = SafeShadeColors(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceGlass = DarkSurfaceGlass,
    borderGlass = DarkBorderGlass,
    onSurface = DarkOnSurface,
    onSurfaceMuted = DarkOnSurfaceMuted,
    onSurfaceFaint = DarkOnSurfaceFaint,
    neuHighlight = DarkNeuHighlight,
    neuShadow = DarkNeuShadow
)

// ============================================
// DEPRECATED ALIASES — kept so not-yet-migrated screens keep compiling.
// New code should use MaterialTheme.safeShadeColors instead.
// ============================================
@Deprecated("Use MaterialTheme.safeShadeColors.background", ReplaceWith("MaterialTheme.safeShadeColors.background"))
val BgColor = LightBackground

@Deprecated("Use MaterialTheme.safeShadeColors.surface", ReplaceWith("MaterialTheme.safeShadeColors.surface"))
val CardColor = LightSurface

@Deprecated("Use MaterialTheme.safeShadeColors.onSurface", ReplaceWith("MaterialTheme.safeShadeColors.onSurface"))
val TextDark = LightOnSurface

@Deprecated("Use MaterialTheme.safeShadeColors.onSurfaceMuted", ReplaceWith("MaterialTheme.safeShadeColors.onSurfaceMuted"))
val TextGray = LightOnSurfaceMuted

@Deprecated("Use MaterialTheme.safeShadeColors.onSurfaceFaint", ReplaceWith("MaterialTheme.safeShadeColors.onSurfaceFaint"))
val IconGray = LightOnSurfaceFaint
