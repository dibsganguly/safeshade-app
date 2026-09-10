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
    enabled: Boolean = true,
    /**
     * A mono figure at the trailing edge (candidate 2.31): Save · 3 changes,
     * Send · 2 people, Sweep · 20 s. A commit button that says how much it is
     * about to commit needs no sentence under it. The label moves to the
     * leading edge to make room.
     */
    figure: String? = null,
    /**
     * The glyph on a 32dp disc of the content colour at the leading edge
     * (candidate 2.63), the label left-aligned after it. Gives a full-width
     * button a place for the eye to land. For the one or two prominent
     * actions on a hub; not for every button on a page.
     */
    glyphOnDisc: Boolean = false
) {
    val colors = MaterialTheme.board
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(Motion.fast),
        label = "button-press"
    )
    val leading = figure != null || glyphOnDisc

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
        horizontalAlignment = if (leading) Alignment.Start else Alignment.CenterHorizontally,
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
            .padding(horizontal = if (glyphOnDisc) Spacing.md else Spacing.lg, vertical = if (glyphOnDisc) Spacing.sm else Spacing.md)
    ) {
        // The icon sits with the *label*, not with the button, which is what
        // keeps it level with the first line of type when a supporting line is
        // present. Top-aligned within that row so a label that wraps to two
        // lines does not drag the glyph down to the middle of the block it
        // labels.
        Row(
            verticalAlignment = if (glyphOnDisc) Alignment.CenterVertically else Alignment.Top,
            modifier = if (leading) Modifier.fillMaxWidth() else Modifier
        ) {
            if (icon != null && glyphOnDisc) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(content.copy(alpha = 0.14f))
                ) {
                    Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(Spacing.md))
            } else if (icon != null) {
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
                textAlign = if (leading) TextAlign.Start else TextAlign.Center,
                modifier = if (leading) Modifier.weight(1f) else Modifier
            )
            if (figure != null) {
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = figure,
                    style = MaterialTheme.boardType.readout,
                    color = content.copy(alpha = 0.8f)
                )
            }
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
 * A button that is only its glyph.
 *
 * For a strip of two or three actions on a plate that is already narrow -
 * the family dashboard's Message, Where and Call - where a word beside the
 * glyph wrapped to "Mess / age" and "Wher / e" at the phone's own width, which
 * read as a fault rather than as a label. The word moves into
 * [contentDescription], where TalkBack still says it, and the glyph grows to
 * 28dp and sits centred so it is the whole target rather than a prefix to one.
 *
 * Same plate, same weights, same 56dp floor and press as [BoardButton], so a
 * row of these reads as the same kind of object; only the text is gone. The
 * description is required rather than optional because a button that says
 * nothing to a screen reader is a button that does not exist for that reader.
 */
@Composable
fun BoardIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    weight: ButtonWeight = ButtonWeight.QUIET,
    enabled: Boolean = true,
    /**
     * The word under the glyph (candidate 2.30), in the small nameplate
     * voice on its own line, so it never wraps mid-word. Taller by 16dp. For
     * a strip whose glyphs are not self-evident; a strip of three universal
     * glyphs (message, map, phone) may leave it off.
     */
    caption: String? = null
) {
    val colors = MaterialTheme.board
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(Motion.fast),
        label = "icon-button-press"
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
        weight == ButtonWeight.ATTENTION || weight == ButtonWeight.COMMIT -> BrandCharcoal
        weight == ButtonWeight.DANGER -> if (colors.isDark) colors.ground else colors.plate
        else -> colors.ink
    }
    Box(
        contentAlignment = Alignment.Center,
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
            .padding(horizontal = Spacing.sm, vertical = Spacing.md)
    ) {
        if (caption == null) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = content,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    icon,
                    contentDescription = contentDescription,
                    tint = content,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = caption,
                    style = MaterialTheme.boardType.nameplateSmall,
                    color = content,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
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
    state: LampState? = null
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
    }
}
