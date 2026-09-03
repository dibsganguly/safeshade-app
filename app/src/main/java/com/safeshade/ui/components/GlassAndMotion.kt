/**
 * SafeShade - Universal Safety Companion
 *
 * GlassAndMotion.kt
 *
 * Frosted-glass and neumorphic surface primitives, plus the app's shared
 * motion vocabulary (durations only — actual animation calls stay at each
 * call site since AnimatedVisibility/AnimatedContent are inherently local).
 *
 * @author SafeShade Team
 * @version 1.0.0
 */

package com.safeshade.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.Elevation
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.safeShadeColors

/**
 * Frosted-glass card. Baseline treatment (all API levels): translucent
 * surfaceGlass fill + thin borderGlass stroke + soft shadow. On API 31+ a
 * subtle blurred highlight layer is added on top for extra depth — real
 * backdrop blur-behind (blurring whatever sits under the card) isn't
 * available without a third-party compositor library, so this stays an
 * honest "reads as frosted" treatment rather than true blur-behind.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.lg),
    elevation: Dp = Elevation.level1,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = colors.onSurface.copy(alpha = 0.08f), spotColor = colors.onSurface.copy(alpha = 0.08f))
            .clip(shape)
            .background(colors.surfaceGlass)
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(colors.neuHighlight.copy(alpha = 0.10f), Color.Transparent)
                        )
                    )
                } else Modifier
            )
            .border(1.dp, colors.borderGlass, shape)
    ) {
        content()
    }
}

/**
 * Dual-shadow neumorphic surface for tactile, interactive elements (SOS
 * button, connect toggle, quick-action icon buttons). Kept off full screens
 * intentionally — heavy neumorphism hurts contrast/accessibility.
 */
@Composable
fun NeumorphicSurface(
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    shape: Shape = RoundedCornerShape(Radius.md),
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    val depth = if (pressed) 2.dp else 8.dp
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .then(
                if (!pressed) {
                    Modifier
                        .shadow(depth, shape, ambientColor = colors.neuShadow, spotColor = colors.neuShadow)
                } else Modifier
            )
    ) {
        content()
    }
}

/** Optional extra blur enhancement layer, API 31+ only — used sparingly on decorative accents. */
fun Modifier.safeShadeGlassBlur(radius: Dp = 24.dp): Modifier = this.then(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(radius) else Modifier
)
