package com.safeshade.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Material motion for navigation.
 *
 * Three patterns, applied consistently, because inconsistent transitions are
 * how an app stops feeling like one place:
 *
 *  - **Fade through** between the four bottom destinations. They are peers with
 *    no spatial relationship, so nothing should slide.
 *  - **Shared axis (X)** from a destination into one of its sub-screens. The
 *    forward/back direction carries the hierarchy.
 *  - **Shared axis (Z)** for overlays that sit *above* everything — the map
 *    picker, the QR card, the trip screen.
 *
 * Durations are Material's own asymmetric pairing: the outgoing element leaves
 * in 90ms and the incoming arrives over 210ms after a 90ms hold, so the two
 * never cross-dissolve into mush. They are also short enough that no transition
 * in this app ever delays reaching an emergency control.
 *
 * These specs also drive predictive back: navigation-compose seeks the pop
 * transitions as the user drags, so the pop specs are written to look correct
 * when scrubbed at an arbitrary fraction, not only when played start to finish.
 */
object NavMotion {

    private const val OUT = 90
    private const val IN = 210
    private const val HOLD = 90

    // ============================================
    // Fade through — peer destinations
    // ============================================

    val fadeThroughEnter: EnterTransition =
        fadeIn(tween(IN, delayMillis = HOLD)) +
            scaleIn(initialScale = 0.94f, animationSpec = tween(IN, delayMillis = HOLD))

    val fadeThroughExit: ExitTransition = fadeOut(tween(OUT))

    // ============================================
    // Shared axis X — parent to child
    // ============================================

    fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedXEnter(): EnterTransition =
        slideInHorizontally(tween(IN, delayMillis = HOLD, easing = FastOutSlowInEasing)) { it / 10 } +
            fadeIn(tween(IN, delayMillis = HOLD))

    fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedXExit(): ExitTransition =
        slideOutHorizontally(tween(OUT, easing = FastOutSlowInEasing)) { -it / 10 } +
            fadeOut(tween(OUT))

    fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedXPopEnter(): EnterTransition =
        slideInHorizontally(tween(IN, delayMillis = HOLD, easing = FastOutSlowInEasing)) { -it / 10 } +
            fadeIn(tween(IN, delayMillis = HOLD))

    fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedXPopExit(): ExitTransition =
        slideOutHorizontally(tween(OUT, easing = FastOutSlowInEasing)) { it / 10 } +
            fadeOut(tween(OUT))

    // ============================================
    // Shared axis Z — overlays above the hierarchy
    // ============================================

    val sharedZEnter: EnterTransition =
        scaleIn(initialScale = 0.86f, animationSpec = tween(IN, delayMillis = HOLD)) +
            fadeIn(tween(IN, delayMillis = HOLD))

    val sharedZExit: ExitTransition =
        scaleOut(targetScale = 1.08f, animationSpec = tween(OUT)) + fadeOut(tween(OUT))

    val sharedZPopEnter: EnterTransition =
        scaleIn(initialScale = 1.08f, animationSpec = tween(IN, delayMillis = HOLD)) +
            fadeIn(tween(IN, delayMillis = HOLD))

    val sharedZPopExit: ExitTransition =
        scaleOut(targetScale = 0.86f, animationSpec = tween(OUT)) + fadeOut(tween(OUT))
}
