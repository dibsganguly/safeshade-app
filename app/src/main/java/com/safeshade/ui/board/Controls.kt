package com.safeshade.ui.board

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.ui.theme.BrandCharcoal
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/** How much visual weight a button carries. */
enum class ButtonWeight {
    PRIMARY,
    SECONDARY,
    QUIET,

    /**
     * Brand amber on charcoal ink: the one thing on this screen to do next.
     *
     * For the action a screen exists to offer when nothing else has been done
     * yet - connecting a wearable, pairing a second one. Charcoal on amber,
     * because amber is a light hue and bone-coloured ink on it measures under
     * 2:1; the charcoal pairing clears 8:1.
     */
    ATTENTION,

    /**
     * Brand teal on charcoal ink: this writes something down.
     *
     * Saving is the one action in this app that is neither routine navigation
     * nor an emergency, and it was previously indistinguishable from both. Teal
     * is the live hue, which is the right association - a saved setting is a
     * circuit that is now on.
     */
    COMMIT,

    DANGER
}

/**
 * A panel button.
 *
 * Wide, low, square-shouldered — a labelled plate you press, not a pill. The
 * press response is a small scale-down rather than a ripple, because a ripple
 * spreads light across the surface and this surface does not emit light.
 *
 * ## The three hued weights
 *
 * [ButtonWeight.DANGER] used to be the only variant carrying a hue, on the
 * argument that colour means circuit state and a button is not a circuit. That
 * held while every other button was charcoal, and stopped holding once a screen
 * carried four of them: "Connect to the device", "Save", "Pair another device"
 * and "Emergency numbers" all arrived at the same weight, so the one that
 * mattered on each screen had to be found by reading rather than seen.
 *
 * The three hues are the three lamp glasses and nothing else is added:
 * [ButtonWeight.ATTENTION] is amber (do this next), [ButtonWeight.COMMIT] is
 * teal (this writes something down), [ButtonWeight.DANGER] is trip red (this
 * places a real call or fires a real alert). All three carry charcoal ink,
 * because all three glasses are light hues - the pairing is not a style choice,
 * it is the only one that measures.
 *
 * This does mean colour on this panel now says one of two things rather than
 * exactly one. The saturation rule still separates them from the twelve
 * decorative accents, and every hued button still states its action in words.
 */
@Composable
fun BoardButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    weight: ButtonWeight = ButtonWeight.PRIMARY,
    icon: ImageVector? = null,
    supporting: String? = null,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.board
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(Motion.fast),
        label = "button-press"
    )

    val container = when {
        !enabled -> colors.recess
        weight == ButtonWeight.PRIMARY -> colors.ink
        weight == ButtonWeight.ATTENTION -> colors.lampAttention
        weight == ButtonWeight.COMMIT -> colors.lampLive
        weight == ButtonWeight.DANGER -> colors.lampTrip
        weight == ButtonWeight.SECONDARY -> colors.plate
        else -> colors.ground
    }
    val content = when {
        !enabled -> colors.inkFaint
        weight == ButtonWeight.PRIMARY -> colors.plate
        // Charcoal on both glasses in both themes. The lamp hues do not darken
        // for the night panel - they are the same glass - so the ink that works
        // on them does not change either.
        weight == ButtonWeight.ATTENTION || weight == ButtonWeight.COMMIT -> BrandCharcoal
        weight == ButtonWeight.DANGER -> if (colors.isDark) colors.ground else colors.plate
        else -> colors.ink
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(Radius.plate))
            .background(container)
            .border(
                Stroke.hairline,
                if (weight == ButtonWeight.SECONDARY || weight == ButtonWeight.QUIET) colors.hairline
                else container,
                RoundedCornerShape(Radius.plate)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        // The icon sits with the *label*, not with the button, which is what
        // keeps it level with the first line of type when a supporting line is
        // present. Top-aligned within that row so a label that wraps to two
        // lines does not drag the glyph down to the middle of the block it
        // labels.
        Row(verticalAlignment = Alignment.Top) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = content,
                    // Optical centring against the first line's *cap height*,
                    // not its line box. An 18dp glyph is taller than the cap of
                    // a 15sp nameplate, so top-aligning it flush leaves the
                    // glyph hanging below the baseline and reading as dropped -
                    // visible on the emergency-numbers button, where the badge
                    // sat a clear two points under the E beside it. Lifting it
                    // puts the two optical centres together.
                    modifier = Modifier.offset(y = (-2).dp).size(18.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = label,
                // A step heavier on the three hued weights.
                //
                // Charcoal on amber, teal or red is a dark ink on a light but
                // *saturated* ground, and a W600 that reads as solid on bone
                // reads slightly thin there - the colour under the letters
                // competes with them in a way a flat panel does not. The
                // achromatic weights keep W600, so this is a correction to the
                // coloured buttons rather than a general heaviness.
                style = if (weight == ButtonWeight.ATTENTION ||
                    weight == ButtonWeight.COMMIT ||
                    weight == ButtonWeight.DANGER
                ) {
                    MaterialTheme.boardType.nameplate.copy(fontWeight = FontWeight.W700)
                } else {
                    MaterialTheme.boardType.nameplate
                },
                color = content,
                textAlign = TextAlign.Center
            )
        }
        if (supporting != null) {
            // A real gap, and centred under the label. Flush against the
            // nameplate the two read as one wrapped line rather than a label
            // and its explanation.
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = supporting,
                style = MaterialTheme.boardType.rowDetail,
                color = content.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A numeric readout.
 *
 * Monospaced so digits do not jitter as they change — a battery percentage
 * that shifts the label left and right every second reads as instability in
 * the device rather than in the typography.
 */
@Composable
fun Readout(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    large: Boolean = false,
    /** Set when the value is a phrase rather than a figure. See the style below. */
    compact: Boolean = false,
    state: LampState? = null,
    /** Lets a row of readouts distribute rather than all left-packing. */
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    val colors = MaterialTheme.board
    val tint = when (state) {
        LampState.LIVE -> colors.inkLive
        LampState.ATTENTION -> colors.inkAttention
        LampState.TRIP -> colors.inkTrip
        else -> colors.ink
    }
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        if (label != null) Nameplate(label, small = true, muted = true)
        Text(
            text = value,
            style = when {
                large -> MaterialTheme.boardType.readoutLarge
                // Smaller, and set much tighter.
                //
                // A readout is normally a value - a percentage, a count, a
                // dash - and 14sp mono on a 20sp line is right for that. One
                // of them holds a sentence instead ("Location taken 26 minutes
                // ago"), which wraps, and mono prose on 20sp leading opens a
                // gap between the two lines wide enough that they stop reading
                // as one phrase. This is not a third size in the scale; it is
                // the same readout compressed for the one case where the value
                // is words.
                compact -> MaterialTheme.boardType.readout.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 15.sp
                )
                else -> MaterialTheme.boardType.readout
            },
            color = tint
        )
    }
}

/**
 * A gauge tile — one instrument reading in its own bay.
 *
 * Used for weather, UV, battery and signal. Kept as a labelled plate rather
 * than a dial: a dial looks handsome and is slower to read, and everything on
 * this screen is meant to be taken in at a glance.
 */
@Composable
fun Gauge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    caption: String? = null,
    state: LampState? = null,
    stub: Boolean = false
) {
    val colors = MaterialTheme.board
    Box(modifier = modifier) {
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Nameplate(label, small = true, muted = true)
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        style = MaterialTheme.boardType.readoutLarge,
                        color = when (state) {
                            LampState.ATTENTION -> colors.inkAttention
                            LampState.TRIP -> colors.inkTrip
                            else -> colors.ink
                        }
                    )
                    if (unit != null) {
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkFaint,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }
        }
        if (stub) StubMark(modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.sm))
    }
}

/**
 * Marks a card whose data is representative rather than live.
 *
 * Used only where a feature is genuinely blocked by missing hardware or
 * infrastructure — a vitals sensor that is not on this board revision, a cloud
 * tier that does not exist. The card behaves and reads exactly like the real
 * feature; this small dotted square in the corner is the only tell.
 *
 * It carries a content description so the distinction is available to a screen
 * reader too. Showing representative data with no marker at all would be a
 * different thing entirely, and not an honest one.
 */
@Composable
fun StubMark(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(RoundedCornerShape(Radius.tight))
            .background(colors.inkFaint.copy(alpha = 0.22f))
            .border(Stroke.hairline, colors.inkFaint.copy(alpha = 0.5f), RoundedCornerShape(Radius.tight))
            .semantics { contentDescription = "Representative data, not live" }
    )
}
