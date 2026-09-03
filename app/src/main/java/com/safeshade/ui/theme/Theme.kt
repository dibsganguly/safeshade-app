package com.safeshade.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LocalSafeShadeColors = staticCompositionLocalOf { LightSafeShadeColors }

/** Theme-aware semantic tokens (background/surface/glass/text) — read this, not the raw Color.kt constants. */
val MaterialTheme.safeShadeColors: SafeShadeColors
    @Composable
    get() = LocalSafeShadeColors.current

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    secondary = AccentBlue,
    tertiary = AccentPink,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    error = AccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    secondary = AccentBlue,
    tertiary = AccentPink,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    error = AccentRed
)

/**
 * SafeShade's app-wide theme. Dynamic color is intentionally OFF by default
 * (dynamicColor = false) — SafeShade has its own brand palette and dynamic
 * per-device Material You theming would wash that identity out. It's left
 * as an opt-in parameter rather than removed outright.
 */
@Composable
fun SafeShadeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalView.current.context
            if (darkTheme) {
                androidx.compose.material3.dynamicDarkColorScheme(context)
            } else {
                androidx.compose.material3.dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val safeShadeColors = if (darkTheme) DarkSafeShadeColors else LightSafeShadeColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalSafeShadeColors provides safeShadeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SafeShadeShapes,
            content = content
        )
    }
}
