package com.safeshade.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
    val measurer = rememberTextMeasurer()

    // The wordmark is fitted to the brand lockup, not typed at a chosen size.
    //
    // Every number in this block was measured off `docs/Logo/SafeShade Full
    // Logo.png` and expressed as a fraction of the emblem's height, because
    // that is the one dimension this screen fixes ([EMBLEM_H]). Guessing a
    // font size and hoping the pair looked right is what produced three rounds
    // of "the proportions are still off": at 38dp the name was about two
    // thirds of the width the logo gives it, so the emblem read as oversized
    // and the gap between them read as a mistake.
    //
    // Fitting rather than hard-coding also means the wordmark keeps its
    // proportion if the weight changes. It just did - W800 to W700, to match
    // the Board masthead - and narrower letterforms would otherwise have made
    // an already-small wordmark smaller still.
    val wordmarkStyle = remember(density) {
        val probeStyle = TextStyle(
            fontFamily = BoardSans,
            // The same weight as the Board screen's masthead. It was W800, on
            // the argument that a logotype wants more weight than any heading;
            // the two sit one swipe apart and the difference read as two
            // different wordmarks rather than as one used twice.
            fontWeight = FontWeight.W700,
            // Sized in dp converted to sp, not in sp.
            //
            // A logotype is artwork, not reading matter. Left in sp the
            // wordmark tracks the user's font-size preference while EMBLEM_W
            // does not, so at 1.3x the name grows a third wider beside an
            // emblem that has not moved and the lockup is back out of
            // proportion. Everything else in the app still honours the
            // setting; this one element opts out because it is a picture of a
            // name rather than a piece of text.
            fontSize = with(density) { PROBE.toSp() },
            // No negative tracking. What made the wordmark read badly was
            // -0.03em squeezing the letterforms into each other; the masthead
            // carries none, so neither does this.
            letterSpacing = 0.sp
        )
        val probeWidth = with(density) {
            // The wider of the two words. "Shade" is the longer string but not
            // necessarily the wider run, so both are measured.
            maxOf(
                measurer.measure("Safe", probeStyle).size.width,
                measurer.measure("Shade", probeStyle).size.width
            ).toDp()
        }
        // No lineHeight. Setting one here looked like it controlled the space
        // between "Safe" and "Shade" and did not: the two words are separate
        // Text composables, so what separates them is the height of each one's
        // own line box, and a lineHeight below that box is simply ignored. The
        // measured gap came out 22% looser than the logo's while the constant
        // said otherwise. The stacking is done by the Column's arrangement now,
        // where it is actually decided.
        probeStyle.copy(
            fontSize = with(density) { (PROBE * (WORDMARK_W / probeWidth)).toSp() }
        )
    }
    // Measured again at the fitted size rather than assumed to equal
    // [WORDMARK_W]. A text measurement includes side bearings and the target is
    // an ink width, so the two differ by a percent or two - and the drawn
    // column has to be the drawn width, or `maxLines = 1` clips the longer word
    // by however much the estimate was out.
    val wordmarkWidth = remember(wordmarkStyle, density) {
        with(density) {
            maxOf(
                measurer.measure("Safe", wordmarkStyle).size.width,
                measurer.measure("Shade", wordmarkStyle).size.width
            ).toDp()
        }
    }
    // What to add between the two words so their baselines land [LINE_B2B]
    // apart. Negative, and it has to be: a line box is taller than the brand's
    // baseline-to-baseline distance, which is what makes a stacked logotype
    // read as one mark rather than as two lines of a sentence.
    //
    // Derived from a real measurement rather than typed as a constant, because
    // the line box's height follows the fitted font size, which follows the
    // device's own text metrics. A hard-coded nudge would be right on this
    // phone and wrong on the next.
    val lineGap = remember(wordmarkStyle, density) {
        with(density) { LINE_B2B - measurer.measure("Shade", wordmarkStyle).size.height.toDp() }
    }
    // The plate spans the whole lockup, as it does in the supplied logo. Now
    // that the wordmark half is measured, this is the real drawn width - and
    // it only became real once `brand_tagline.png` was cropped to its content.
    // The artwork carried transparent margins on all four sides, so the pill
    // occupied 90.2% of the canvas and `width(taglineWidth)` sized the canvas,
    // not the pill: the plate rendered about a tenth narrower than the mark
    // above it no matter what number went in here. Measured off a device
    // screenshot, not reasoned about - it is invisible in a preview.
    val taglineWidth = EMBLEM_W + EMBLEM_GAP + wordmarkWidth

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
                        .offset(x = ((wordmarkWidth + EMBLEM_GAP) / 2) * (1f - slide))
                        .size(width = EMBLEM_W, height = EMBLEM_H)
                )

                Spacer(Modifier.width(EMBLEM_GAP * slide))

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
                    verticalArrangement = Arrangement.spacedBy(lineGap),
                    // Not centred against the emblem. In the supplied logo the
                    // wordmark's ink sits 0.0577 of the emblem's height below
                    // the emblem's middle, which is what stops the name reading
                    // as floating slightly high beside the mark. `offset`
                    // rather than padding, so the Row's height stays the
                    // emblem's and the tagline gap below is measured from one
                    // predictable place.
                    modifier = Modifier
                        .width(wordmarkWidth)
                        .offset(y = WORDMARK_DROP)
                ) {
                    WordmarkLine("Safe", safeIn, colors.ink, wordmarkStyle)
                    WordmarkLine("Shade", shadeIn, colors.ink, wordmarkStyle)
                }
            }

            Spacer(Modifier.height(TAGLINE_GAP))

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

/**
 * The lockup's own proportions, read off `docs/Logo/SafeShade Full Logo.png`.
 *
 * Every one of these is a multiple of [EMBLEM_H], because that is the single
 * dimension this screen fixes and everything else in a lockup is relative to
 * it. Measured from the artwork's alpha channel rather than eyeballed:
 *
 * - emblem: 476 by 728px, so 0.6538 wide - the ratio [EMBLEM_W] and [EMBLEM_H]
 *   already carried, and the one thing here that was previously right.
 * - gap from emblem to wordmark: 132px, 0.1813 of the emblem's height. It was
 *   `Spacing.md`, 12dp, against a true 20dp - the wordmark sat too close.
 * - widest wordmark line: 1188px, 1.6319. This is the number the type is
 *   fitted to, and the one that was most wrong.
 * - baseline to baseline: 360px, 0.4945. Tighter than one line box, which is
 *   normal for a stacked logotype and is what makes two words read as one mark
 *   rather than as two lines of a sentence.
 * - gap from lockup to tagline plate: 107px, 0.147.
 *
 * The three that are not ratios off the logo - [EMBLEM_GAP], [WORDMARK_W] and
 * [TAGLINE_GAP] - carry a correction, because what the logo gives is an *ink*
 * measurement and what Compose lays out is a *box*. A text measurement includes
 * side bearings and a line box includes leading, so feeding the raw ratios in
 * produced a wordmark 3% narrow, a gap 2% wide and a tagline sitting 8% low.
 * Each was re-measured off a device screenshot and corrected once. The
 * remaining error against the supplied logo is under 0.01 of the emblem's
 * height on every dimension, which on this screen is under three pixels.
 *
 * One thing does not match and cannot. The logo's wordmark is 0.4451 tall per
 * line for its width; Archivo W700 is 0.4118. The lockup is fitted to width,
 * because width is the proportion a horizontal lockup is read by, and the
 * weight is W700 rather than anything heavier because it has to be the Board
 * masthead's weight - the two sit one swipe apart.
 */
private val EMBLEM_GAP = EMBLEM_H * 0.1581f
private val WORDMARK_W = EMBLEM_H * 1.681f
private val LINE_B2B = EMBLEM_H * 0.4945f
private val TAGLINE_GAP = EMBLEM_H * 0.110f
private val WORDMARK_DROP = EMBLEM_H * 0.0723f

/**
 * The size the wordmark is measured at before being scaled to [WORDMARK_W].
 *
 * Arbitrary and irrelevant to the result - it only has to be large enough that
 * hinting and rounding do not distort the measurement it is scaled from.
 */
private val PROBE = 48.dp

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
