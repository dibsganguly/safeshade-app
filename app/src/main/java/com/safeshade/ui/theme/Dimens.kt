package com.safeshade.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 4dp-based spacing scale. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Screen gutter. */
    val gutter = 20.dp
    /** Minimum touch target; every interactive row must reach this. */
    val touchTarget = 48.dp
}

/**
 * Corners are machined, not soft. A panel plate has a small milled radius —
 * the 20-28dp pill corners of a consumer wellness app read as the wrong
 * material entirely, so the scale tops out at 14dp and most surfaces sit at
 * 4-8dp. Even the lamp is square since v2.8.0; [Radius.lamp] scales with its size.
 */
object Radius {
    val none = 0.dp
    val tight = 2.dp
    val plate = 4.dp
    val card = 8.dp
    val prominent = 14.dp
}

/**
 * Line weights. The board is built from rules, not shadows — a hairline is a
 * structural separator, a brass rule is emphasis under a section nameplate.
 */
object Stroke {
    val hairline = 1.dp
    val rule = 1.5.dp
    val brass = 2.dp
    val heavy = 3.dp
}

/**
 * Elevation is used sparingly and never as decoration. A plate sits on the
 * ground; only genuinely floating things (sheets, dialogs, the trip banner)
 * lift. Tonal separation does most of the work.
 */
object Elevation {
    val flat = 0.dp
    val plate = 1.dp
    val floating = 6.dp
    val overlay = 12.dp
}

/**
 * Motion. Two rules that matter more than the numbers:
 *
 *  1. Nothing on a safety path is ever slow. Reaching SOS, dismissing a trip,
 *     and placing a call are immediate.
 *  2. A switch **throws**; it does not glide. [switchThrow] overshoots and
 *     settles, which is what makes a toggle feel mechanical rather than
 *     animated. A pilot lamp **warms up** rather than cross-fading — hence
 *     [lampWarmUp]'s asymmetric easing.
 */
object Motion {
    const val instant = 0
    const val fast = 120
    const val normal = 200
    const val switchThrow = 400
    const val lampWarmUp = 260
    const val lampCoolDown = 160
    /** How long a finger stays on a hold-to-confirm plate before it fires. Long enough that a slip cannot. */
    const val holdToConfirm = 1100

    /** Decisive in, gentle settle — the feel of a rocker switch landing. */
    val ThrowEasing: Easing = CubicBezierEasing(0.2f, 1.4f, 0.4f, 1f)
    /** Filament coming up: slow to start, then quick. */
    val WarmUpEasing: Easing = CubicBezierEasing(0.6f, 0f, 0.9f, 0.6f)
    val Standard: Easing = FastOutSlowInEasing
}

val BoardShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.tight),
    small = RoundedCornerShape(Radius.plate),
    medium = RoundedCornerShape(Radius.card),
    large = RoundedCornerShape(Radius.card),
    extraLarge = RoundedCornerShape(Radius.prominent)
)
