package com.safeshade.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 4/8/12/16/24/32dp spacing scale. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** 4-step elevation scale for resting/raised/floating/overlay surfaces. */
object Elevation {
    val level0 = 0.dp
    val level1 = 2.dp
    val level2 = 6.dp
    val level3 = 12.dp
}

/** 4-step corner-radius scale matching values already in use across the app. */
object Radius {
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 28.dp
}

val SafeShadeShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.sm),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl)
)

/** Short, consistent motion durations — never long enough to slow down reaching SOS. */
object Motion {
    const val fast = 120
    const val normal = 200
    const val slow = 250
}
