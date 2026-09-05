package com.safeshade.ui.board

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.board

/**
 * What a circuit is doing, as a lamp can express it.
 *
 * Deliberately small. A lamp that can say six things says nothing at a glance,
 * and glanceability is the entire job — a guardian should know whether the
 * person they are responsible for is covered without reading a word.
 */
enum class LampState {
    /** Working and armed. */
    LIVE,

    /** On, but wants attention: degraded, unconfirmed, or nearly out. */
    ATTENTION,

    /** Tripped. Something happened and it needs a person. */
    TRIP,

    /** Deliberately off. Not a fault. */
    OFF,

    /** We cannot currently tell — no link, never synced. Honest, not alarming. */
    UNKNOWN;

    val isLit: Boolean get() = this == LIVE || this == ATTENTION || this == TRIP

    val defaultDescription: String
        get() = when (this) {
            LIVE -> "Live"
            ATTENTION -> "Needs attention"
            TRIP -> "Tripped"
            OFF -> "Off"
            UNKNOWN -> "Unknown"
        }
}

/**
 * A panel pilot lamp.
 *
 * Drawn rather than assembled from Material parts, for two reasons. It has to
 * light with a filament warm-up rather than a cross-fade, which no stock
 * component does; and it is the one place in the whole app where saturated
 * colour is allowed, so it needs to be a single, auditable implementation
 * instead of a colour applied ad hoc across screens.
 *
 * Colour alone never carries the meaning: every caller pairs a lamp with a
 * state word, and the semantics below give a screen reader the same
 * information a sighted user gets from the hue. Red-versus-teal is precisely
 * the pairing that colour vision deficiency costs.
 */
@Composable
fun PilotLamp(
    state: LampState,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    /** Spoken description. Null uses the state's own wording. */
    description: String? = null
) {
    val colors = MaterialTheme.board

    val glass = when (state) {
        LampState.LIVE -> colors.lampLive
        LampState.ATTENTION -> colors.lampAttention
        LampState.TRIP -> colors.lampTrip
        LampState.OFF, LampState.UNKNOWN -> colors.lampOff
    }

    // A filament comes up slowly then quickly, and dies faster than it lights.
    val litness by animateFloatAsState(
        targetValue = if (state.isLit) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (state.isLit) Motion.lampWarmUp else Motion.lampCoolDown,
            easing = if (state.isLit) Motion.WarmUpEasing else Motion.Standard
        ),
        label = "lamp-litness"
    )

    // A trip breathes. Nothing else on the board moves on its own, so this is
    // the one element that draws the eye across a still screen. Held steady in
    // previews and screenshots so captured evidence is deterministic.
    val animateTrip = state == LampState.TRIP && !LocalInspectionMode.current
    val transition = rememberInfiniteTransition(label = "trip")
    val pulseAnim by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trip-pulse"
    )
    val pulse = if (animateTrip) pulseAnim else 1f

    val spoken = description ?: state.defaultDescription

    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { stateDescription = spoken }
    ) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Halo: a flat low-alpha ring, not a radial gradient. The world is flat
        // unmodulated colour throughout, and a glow gradient here would be the
        // one soft edge on an otherwise machined surface.
        if (litness > 0f) {
            drawCircle(
                color = glass.copy(alpha = 0.18f * litness * pulse),
                radius = radius * 1.55f,
                center = center
            )
        }

        // Glass.
        drawCircle(
            color = lerpFlat(colors.lampOff, glass, litness * pulse),
            radius = radius * 0.72f,
            center = center
        )

        // Bezel: the machined ring the lamp sits in. Always present, so an
        // unlit lamp still reads as a lamp rather than as a smudge.
        drawCircle(
            color = colors.ink.copy(alpha = if (colors.isDark) 0.55f else 0.28f),
            radius = radius * 0.86f,
            center = center,
            style = Stroke(width = radius * 0.22f)
        )
    }
}

/** Flat interpolation. Kept local so no caller reaches for a gradient brush. */
private fun lerpFlat(from: Color, to: Color, t: Float): Color = Color(
    red = from.red + (to.red - from.red) * t,
    green = from.green + (to.green - from.green) * t,
    blue = from.blue + (to.blue - from.blue) * t,
    alpha = from.alpha + (to.alpha - from.alpha) * t
)
