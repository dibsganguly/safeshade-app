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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.R
import com.safeshade.ui.theme.BoardSans
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * The opening.
 *
 * One continuous move: the emblem arrives alone in the centre, holds long
 * enough to be seen as itself, then travels left to make room while "Safe" and
 * "Shade" rise into place beside it, stacked — the lockup from
 * docs/Logo/SafeShade Full Logo.png, assembling. The tagline plate settles
 * underneath last.
 *
 * The emblem is the *same element* throughout rather than a crossfade between
 * two arrangements. That is the whole idea: you watch one mark move and the
 * name appear in the space it vacates, which reads as a logo assembling itself
 * rather than as two slides.
 *
 * The brevity is a design constraint, not a compromise. This is a safety app;
 * somebody opening it may be opening it because something is wrong, and an
 * intro they have to sit through is exactly the wrong thing to put between them
 * and the board. So it runs on cold start only, it lasts about two seconds, and
 * every stage overlaps the next rather than queueing behind it.
 */
@Composable
fun IntroScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val isStatic = LocalInspectionMode.current

    // The wordmark is measured, not estimated.
    //
    // Its width used to be a 152.dp literal, and the tagline plate's width was
    // derived from it - so the plate was centred on a *layout* lockup that was
    // wider than the ink actually drawn inside it, and settled visibly right of
    // the mark above. That is the residual misalignment in the supplied logo
    // comparison, and no amount of adjusting the literal fixes it, because the
    // rendered width of "Shade" at 38sp W800 depends on the font file, the
    // user's font scale and the platform's shaping - none of which a constant
    // can know.
    //
    // Measuring with the *same* TextStyle the words are drawn with is what
    // makes this exact rather than merely closer. The style is built once here
    // and handed to both the measurer and the two lines; they cannot drift.
    val density = LocalDensity.current
    // Sized in dp converted to sp, not in sp.
    //
    // A logotype is artwork, not reading matter. Left as `38.sp` the wordmark
    // tracks the user's font-size preference while `EMBLEM_W` does not, so at
    // 1.3x the name grows a third wider beside an emblem that has not moved and
    // the lockup is back out of proportion - the exact complaint this screen was
    // rewritten to fix. It also made the derived tagline width unbounded, which
    // on a narrow phone at a large font scale could push the plate wider than
    // the screen; the constant it replaced could not do that.
    //
    // Everything else in the app still honours the setting. This one element
    // opts out because it is a picture of a name rather than a piece of text.
    val wordmarkStyle = MaterialTheme.typography.displayMedium.copy(
        fontFamily = BoardSans,
        // Archivo's variable weight axis runs to 900 and the family carries 800
        // and 900 for exactly this. A logotype wants more weight than any
        // heading in the app, or it reads as a large heading.
        fontWeight = FontWeight.W800,
        fontSize = with(density) { 38.dp.toSp() },
        lineHeight = with(density) { 40.dp.toSp() },
        // No negative tracking. The family and weight were already right - this
        // is the same Archivo as the Board masthead, one step heavier - and
        // what made the wordmark read badly was -0.03em squeezing 38sp
        // letterforms into each other. The masthead the user likes carries no
        // tracking at all, so neither does this.
        letterSpacing = 0.sp
    )
    val measurer = rememberTextMeasurer()
    val wordmarkWidth = remember(wordmarkStyle, density) {
        with(density) {
            // The wider of the two words. "Shade" is the longer string but not
            // necessarily the wider run, so both are measured rather than one
            // assumed.
            maxOf(
                measurer.measure("Safe", wordmarkStyle).size.width,
                measurer.measure("Shade", wordmarkStyle).size.width
            ).toDp()
        }
    }
    // The plate spans the whole lockup, as it does in the supplied logo. Now
    // that the wordmark half is measured, this is the real drawn width - and
    // it only became real once `brand_tagline.png` was cropped to its content.
    // The artwork carried transparent margins on all four sides, so the pill
    // occupied 90.2% of the canvas and `width(taglineWidth)` sized the canvas,
    // not the pill: the plate rendered about a tenth narrower than the mark
    // above it no matter what number went in here. Measured off a device
    // screenshot, not reasoned about - it is invisible in a preview.
    val taglineWidth = EMBLEM_W + Spacing.md + wordmarkWidth

    // A single 0..1 clock. Every element below is a window onto it, which is
    // what keeps the stages overlapping instead of running as a queue of
    // separate animations with visible seams between them.
    var t by remember { mutableFloatStateOf(if (isStatic) 0.85f else 0f) }

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
    //
    // There is no entrance window for the emblem any more. It is simply there
    // from the first frame, at full size and centred, and holds while the eye
    // lands - which is what the system splash used to spend its time doing,
    // twice over. The hold is the gap before `slide` opens.
    val slide = window(t, 0.26f, 0.56f, Glide)
    // The two words are staggered rather than simultaneous. "Safe" then "Shade"
    // is how the name is read, and letting the second follow the first by a
    // beat makes the lockup assemble in reading order.
    //
    // Both start only once the emblem is most of the way out of their space.
    // An earlier version overlapped them with the travel, and the mark passed
    // straight through the letters on its way past — the one thing a logo
    // animation must not do to its own logotype.
    val safeIn = window(t, 0.50f, 0.70f, Rise)
    val shadeIn = window(t, 0.56f, 0.76f, Rise)
    val tagline = window(t, 0.68f, 0.88f, FastOutSlowInEasing)
    val exit = window(t, 0.92f, 1.00f, LinearEasing)

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
                // left as the name appears. Half the wordmark's width plus the
                // gap is exactly the distance that leaves the assembled pair
                // centred at rest, so the mark never has to correct itself.
                Image(
                    painter = painterResource(R.drawable.splash_emblem),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = ((wordmarkWidth + Spacing.md) / 2) * (1f - slide))
                        .size(width = EMBLEM_W, height = EMBLEM_H)
                )

                Spacer(Modifier.width(Spacing.md * slide))

                // Stacked and left-aligned, matching the supplied full logo —
                // not one word on a line. Tight leading and a heavy weight are
                // what make two short words read as a single mark instead of as
                // two pieces of running text.
                //
                // Fixed width, not an animated one. Animating the width
                // clipped the words as they appeared, so "Shade" spent the
                // middle of the animation reading as "Shad". The words are
                // revealed by their own alpha and rise instead, and the layout
                // underneath never moves.
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.width(wordmarkWidth)
                ) {
                    WordmarkLine("Safe", safeIn, colors.ink, wordmarkStyle)
                    WordmarkLine("Shade", shadeIn, colors.ink, wordmarkStyle)
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            // The supplied tagline plate rather than app type. It is a fixed
            // piece of brand artwork with its own amber-and-teal colouring on
            // its own dark pill, and re-setting it in Archivo would produce a
            // near-miss of something the brand already defines exactly.
            Image(
                painter = painterResource(R.drawable.brand_tagline),
                contentDescription = "Your everything safety companion",
                modifier = Modifier
                    .width(taglineWidth)
                    .alpha(tagline)
                    // Rises the last few pixels into place rather than simply
                    // appearing, so it reads as settling under the lockup.
                    .offset(y = 10.dp * (1f - tagline))
            )
        }
    }
}

/**
 * One word of the wordmark.
 *
 * Each rises into place behind a clip of its own height, so it reads as being
 * revealed by the lockup assembling rather than fading in from nowhere.
 */
@Composable
private fun WordmarkLine(
    text: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
    /** The style the caller measured with. Passing it is what keeps the drawn
     *  width and the measured width the same number. */
    style: TextStyle
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .alpha(progress)
            .offset(y = 14.dp * (1f - progress))
    )
}

private const val TOTAL_MS = 2100

/** Arrives with a little weight and stops dead, rather than easing to nothing. */
private val Settle: Easing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)

/** The emblem's travel. Decisive out, soft landing. */
private val Glide: Easing = CubicBezierEasing(0.65f, 0f, 0.20f, 1f)

/** How each word comes up into its slot. */
private val Rise: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * The emblem, sized to the mark itself rather than to a square box.
 *
 * `splash_emblem` used to be a 432px canvas carrying a 169x259 glyph, so
 * `size(112.dp)` drew a 44dp mark surrounded by 68dp of nothing - which is why
 * the emblem looked small and the gap to the wordmark looked enormous, and why
 * the lockup could never match the supplied logo. The drawable is cropped to
 * its content now, so these numbers mean what they say. The 0.6536 ratio is
 * the artwork's own.
 */
private val EMBLEM_H = 112.dp
private val EMBLEM_W = 73.dp

// The wordmark's width and the tagline plate's width are no longer constants.
// Both were literals - 152dp and 260dp - typed independently of the type they
// were supposed to enclose, which is why the plate never sat square under the
// mark. They are measured in IntroScreen now; see the note there.

/** Maps the global clock onto one stage's own 0..1, eased. */
private fun window(t: Float, from: Float, to: Float, easing: Easing): Float {
    if (t <= from) return 0f
    if (t >= to) return 1f
    return easing.transform((t - from) / (to - from))
}
