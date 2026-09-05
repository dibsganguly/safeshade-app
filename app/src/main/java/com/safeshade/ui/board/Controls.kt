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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/** How much visual weight a button carries. */
enum class ButtonWeight { PRIMARY, SECONDARY, QUIET, DANGER }

/**
 * A panel button.
 *
 * Wide, low, square-shouldered — a labelled plate you press, not a pill. The
 * press response is a small scale-down rather than a ripple, because a ripple
 * spreads light across the surface and this surface does not emit light.
 *
 * [ButtonWeight.DANGER] is the only variant that carries a hue, and it is
 * reserved for actions that place a real call or fire a real alert.
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
        weight == ButtonWeight.DANGER -> colors.lampTrip
        weight == ButtonWeight.SECONDARY -> colors.plate
        else -> colors.ground
    }
    val content = when {
        !enabled -> colors.inkFaint
        weight == ButtonWeight.PRIMARY -> colors.plate
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
                style = MaterialTheme.boardType.nameplate,
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
            style = if (large) MaterialTheme.boardType.readoutLarge else MaterialTheme.boardType.readout,
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
