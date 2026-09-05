package com.safeshade.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LocalBoardColors = staticCompositionLocalOf { LightBoardColors }
private val LocalBoardTypography = staticCompositionLocalOf { BoardType }

/** The board's semantic colours. Read this, never the constants in Color.kt. */
val MaterialTheme.board: BoardColors
    @Composable get() = LocalBoardColors.current

/** Nameplate / readout styles that sit outside the Material type scale. */
val MaterialTheme.boardType: BoardTypography
    @Composable get() = LocalBoardTypography.current

/**
 * Material colour roles mapped onto the board.
 *
 * `primary` is deliberately the *ink*, not a brand hue. In this world a filled
 * button is an engraved plate, not a coloured pill, and letting Material's
 * default components reach for a saturated primary is exactly how the
 * colour-means-state rule gets broken by a stray `Button`. `error` is the one
 * role that keeps a real hue, because there it genuinely means state.
 */
private val LightScheme = lightColorScheme(
    primary = BrandCharcoal,
    onPrimary = Color(0xFFFBF9F5),
    primaryContainer = Color(0xFFE5E1D8),
    onPrimaryContainer = BrandCharcoal,
    secondary = Color(0xFF5A6068),
    onSecondary = Color(0xFFFBF9F5),
    background = Color(0xFFF2EFE9),
    onBackground = BrandCharcoal,
    surface = Color(0xFFFBF9F5),
    onSurface = BrandCharcoal,
    surfaceVariant = Color(0xFFE5E1D8),
    onSurfaceVariant = Color(0xFF5A6068),
    outline = Color(0xFFB9A88A),
    outlineVariant = Color(0xFFD3CDC0),
    error = Color(0xFFB3151A),
    onError = Color(0xFFFBF9F5)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFECEFF1),
    onPrimary = Color(0xFF14171A),
    primaryContainer = Color(0xFF2A3138),
    onPrimaryContainer = Color(0xFFECEFF1),
    secondary = Color(0xFFA0A8B0),
    onSecondary = Color(0xFF14171A),
    background = Color(0xFF14171A),
    onBackground = Color(0xFFECEFF1),
    surface = Color(0xFF1C2126),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF2A3138),
    onSurfaceVariant = Color(0xFFA0A8B0),
    outline = Color(0xFF8A7A5E),
    outlineVariant = Color(0xFF353C43),
    error = Color(0xFFFF8A8D),
    onError = Color(0xFF14171A)
)

/**
 * Dynamic colour is not offered, and this is a product decision rather than an
 * oversight. Material You would repaint the pilot lamps from the user's
 * wallpaper, which destroys the single rule this system is built on: that a
 * coloured element always means a live circuit. Android's guidance to prefer
 * dynamic colour assumes hue is decorative here. It is not.
 */
@Composable
fun SafeShadeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val boardColors = if (darkTheme) DarkBoardColors else LightBoardColors
    val scheme = if (darkTheme) DarkScheme else LightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            // Edge-to-edge: the system bars are transparent and content draws
            // behind them. Bar *icon* colour still has to be set, or light
            // icons land on the bone ground and vanish.
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalBoardColors provides boardColors,
        LocalBoardTypography provides BoardType
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = BoardMaterialTypography,
            shapes = BoardShapes,
            content = content
        )
    }
}
