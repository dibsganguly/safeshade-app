package com.safeshade.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.safeshade.R
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * The opening.
 *
 * A single continuous move: the emblem arrives alone in the centre, settles,
 * then slides left to make room while the wordmark wipes in beside it and the
 * tagline rises underneath. One idea, performed once, in about a second and
 * three quarters.
 *
 * The brevity is the design constraint, not a compromise. This is a safety app;
 * somebody opening it may be opening it because something is wrong, and an
 * intro they have to sit through is exactly the wrong thing to put between them
 * and the board. So it runs on cold start only, it is short, and every stage
 * overlaps the next rather than waiting for it.
 *
 * The wordmark arrives as a left-to-right wipe rather than a fade because the
 * emblem is *moving* left to right out of its way — the reveal reads as the
 * logo uncovering the name, which is one gesture instead of two unrelated ones.
 */
@Composable
fun IntroScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val isStatic = LocalInspectionMode.current

    // A single 0..1 clock. Every element below is a window on it, which is what
    // keeps the stages overlapping instead of running as a queue of separate
    // animations with visible seams between them.
    var t by remember { mutableFloatStateOf(if (isStatic) 0.75f else 0f) }

    LaunchedEffect(Unit) {
        if (isStatic) return@LaunchedEffect
        var start = -1L
        var progress = 0f
        while (progress < 1f) {
            withFrameMillis { now ->
                if (start < 0) start = now
                progress = ((now - start).toFloat() / TOTAL_MS).coerceIn(0f, 1f)
                t = progress
            }
        }
        onFinished()
    }

    // Stage windows, in fractions of the whole.
    val emblemIn = window(t, 0.00f, 0.28f, Settle)
    val slide = window(t, 0.30f, 0.62f, Glide)
    val wipe = window(t, 0.36f, 0.68f, Glide)
    val tagline = window(t, 0.58f, 0.80f, FastOutSlowInEasing)
    val exit = window(t, 0.90f, 1.00f, LinearEasing)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .alpha(1f - exit),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The emblem starts centred under the whole lockup and travels
                // left as the name appears; half the wordmark's width is
                // exactly the distance that leaves the pair centred at rest.
                Image(
                    painter = painterResource(R.drawable.splash_emblem),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = (WORDMARK_W / 2) * (1f - slide))
                        .size(72.dp)
                        .scale(0.82f + 0.18f * emblemIn)
                        .alpha(emblemIn)
                )

                Spacer(Modifier.width(Spacing.md * slide))

                Box(
                    modifier = Modifier
                        .width(WORDMARK_W * slide)
                        // A wipe, not a fade: the name is uncovered by the
                        // emblem's own movement.
                        .drawWithContent {
                            clipRect(right = size.width * wipe) { this@drawWithContent.drawContent() }
                        }
                ) {
                    Text(
                        text = "SafeShade",
                        style = MaterialTheme.typography.displayMedium,
                        color = colors.ink,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            Text(
                text = "Your everything safety companion",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                modifier = Modifier
                    .alpha(tagline)
                    // Rises the last few pixels into place rather than simply
                    // appearing, so it reads as settling under the lockup.
                    .offset(y = (8.dp) * (1f - tagline))
            )
        }
    }
}

private const val TOTAL_MS = 1850

/** Arrives with a little weight and stops dead, rather than easing to nothing. */
private val Settle: Easing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)

/** The travel and the wipe share one curve so they read as one movement. */
private val Glide: Easing = CubicBezierEasing(0.65f, 0f, 0.20f, 1f)

private val WORDMARK_W = 196.dp

/** Maps the global clock onto one stage's own 0..1, eased. */
private fun window(t: Float, from: Float, to: Float, easing: Easing): Float {
    if (t <= from) return 0f
    if (t >= to) return 1f
    return easing.transform((t - from) / (to - from))
}
