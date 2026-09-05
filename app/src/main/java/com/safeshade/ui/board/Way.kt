package com.safeshade.ui.board

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.LocalBoardAccent
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * One circuit on the board — the atomic row this whole interface is built from.
 *
 * A way is a name, a state, and optionally a switch. That triple is what makes
 * the metaphor do real work rather than decorate: "is fall detection on?" and
 * "is the link up?" and "has something tripped?" are all the same shape of
 * question, so they get the same shape of answer, and a guardian learns to read
 * the whole screen once.
 *
 * @param sealed marks a way the wearer cannot change on the device itself.
 *   In the four guardian-locked modes the wearable hides its own Mode and
 *   Safety menus, so these settings live here and only here. The seal says
 *   that out loud instead of leaving it as invisible firmware behaviour.
 * @param deviceOnly marks the inverse: a setting this app genuinely cannot
 *   reach over BLE. Rendered as a readable value with no control, because a
 *   switch that silently does nothing is worse than no switch.
 */
@Composable
fun Way(
    name: String,
    state: LampState,
    stateLabel: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    icon: ImageVector? = null,
    sealed: Boolean = false,
    deviceOnly: Boolean = false,
    /**
     * A decorative accent for this row's icon — see `BoardColors`. Identity
     * only: which part of the app this row belongs to. The row's *state* is
     * carried by the bus tick and the state word, and neither is negotiable.
     */
    accent: Color? = LocalBoardAccent.current,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.board

    // The whole row is one semantic node. Announcing nameplate, state and
    // switch separately makes a screen reader read three fragments where a
    // sighted user takes in one line.
    val spoken = buildString {
        append(name)
        append(", ")
        append(stateLabel)
        if (detail != null) { append(", "); append(detail) }
        if (sealed) append(", managed by the guardian")
        if (deviceOnly) append(", change this on the device")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            // A tonal press state, not a ripple — see `rowClickable`.
            .then(
                if (onClick != null) Modifier.rowClickable(role = Role.Button, onClick = onClick)
                else Modifier
            )
            .padding(start = Spacing.lg, end = Spacing.md, top = Spacing.md, bottom = Spacing.md)
            // Intrinsic height so the bus tick can match whatever the text
            // actually occupies, rather than being a fixed stub that looks
            // right on one line and stunted on three.
            .height(IntrinsicSize.Min)
            .clearAndSetSemantics {
                contentDescription = spoken
                if (checked != null) {
                    toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                }
            }
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent?.takeIf { it.isSpecified } ?: colors.inkMuted,
                // Anchored to the top of the row rather than its middle. The
                // row is as tall as its title *plus* any detail line, so a
                // centred icon on a two-line row floats down beside the
                // subtitle and stops reading as a label for the title.
                // The 2dp matches where the title's own line box starts on a
                // single-line row, so both cases land level.
                modifier = Modifier
                    .align(Alignment.Top)
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Spacer(Modifier.width(Spacing.md))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                // 48dp touch target, kept by the column rather than the row so
                // the tick measures content and not padding.
                .defaultMinSize(minHeight = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Nameplate(name)
                if (sealed) {
                    Spacer(Modifier.width(Spacing.xs))
                    Seal()
                }
            }
            if (detail != null) {
                // A real gap under the title, and a tighter leading inside the
                // detail itself. Previously the two were flush while wrapped
                // detail lines sat far apart, which read as the subtitle
                // belonging to the row below.
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = detail,
                    style = MaterialTheme.boardType.rowDetail,
                    color = colors.inkFaint
                )
            }
        }

        Spacer(Modifier.width(Spacing.md))

        if (checked != null && onCheckedChange != null && !deviceOnly) {
            WaySwitch(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(Modifier.width(Spacing.sm))
        } else {
            Text(
                text = stateLabel.uppercase(),
                style = MaterialTheme.boardType.stateLabel,
                color = when (state) {
                    LampState.LIVE -> colors.inkLive
                    LampState.ATTENTION -> colors.inkAttention
                    LampState.TRIP -> colors.inkTrip
                    LampState.OFF, LampState.UNKNOWN -> colors.inkFaint
                }
            )
            Spacer(Modifier.width(Spacing.md))
        }

        // The tick moved from the left edge to the right, taking over from the
        // pilot lamp that used to sit here. The lamp and the state word said
        // the same thing twice; one coloured edge carries it, and dropping the
        // lamp buys the content real room on the left.
        BusTick(state = state, modifier = Modifier.fillMaxHeight())
    }
}

/**
 * A rocker switch.
 *
 * Not a Material `Switch`. The stock control glides and rounds; a rocker
 * throws, lands, and settles — that overshoot is most of why a toggle feels
 * like a physical thing rather than an animation. The chip also crosses to the
 * *leading* edge when live, so a bank of switches reads as a row of thrown
 * levers at a glance without relying on colour.
 */
@Composable
fun WaySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.board
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val chipWidth = 22.dp
    val inset = 3.dp

    val spec = tween<Dp>(Motion.switchThrow, easing = Motion.ThrowEasing)
    val chipOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - chipWidth - inset else inset,
        animationSpec = spec,
        label = "switch-throw"
    )
    // The chip squashes along its direction of travel and springs back as it
    // lands — the same reason the throw overshoots. A lever that changes shape
    // under load reads as a physical object; one that translates rigidly reads
    // as a rectangle being animated. Held narrow while pressed, so the control
    // responds to the finger before it responds to the state change.
    val chipW by animateDpAsState(
        targetValue = if (pressed) chipWidth - 3.dp else chipWidth,
        animationSpec = tween(Motion.fast),
        label = "switch-squash"
    )
    // Colour used to snap between lamp and stone the instant `checked` flipped,
    // so the chip arrived at its destination already recoloured and the throw
    // carried no information. It now crosses with the travel.
    val chipTint by animateColorAsState(
        targetValue = when {
            !enabled -> colors.recess
            checked -> colors.lampLive
            else -> colors.lampOff
        },
        animationSpec = tween(Motion.switchThrow),
        label = "switch-tint"
    )
    val trackTint by animateColorAsState(
        targetValue = if (checked && enabled) colors.lampLive.copy(alpha = 0.14f) else colors.recess,
        animationSpec = tween(Motion.switchThrow),
        label = "switch-track"
    )

    Box(
        modifier = modifier
            // The visual track is 52x30 but the touch target is the full 48dp.
            .size(width = trackWidth, height = Spacing.touchTarget)
            // No indication. Material's ripple is unbounded on this node, so it
            // painted a hard 52x48 rectangle straight over a 52x30 rounded
            // track on every tap — the grey flash. The press is expressed by
            // the chip instead, which is the part of the control a finger is
            // actually on.
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            )
            .semantics {
                stateDescription = if (checked) "On" else "Off"
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .clip(RoundedCornerShape(Radius.tight))
                .background(trackTint)
                .border(
                    Stroke.hairline,
                    if (checked && enabled) colors.lampLive.copy(alpha = 0.5f) else colors.hairline,
                    RoundedCornerShape(Radius.tight)
                )
        ) {
            Box(
                modifier = Modifier
                    .offset(x = chipOffset)
                    .align(Alignment.CenterStart)
                    .size(width = chipW, height = trackHeight - inset * 2)
                    .clip(RoundedCornerShape(Radius.tight))
                    // The same on/off vocabulary as a pilot lamp: lit teal when
                    // thrown, unlit stone when not. Position alone would be
                    // ambiguous at a glance across a bank of switches.
                    .background(chipTint)
                    .border(
                        Stroke.hairline,
                        if (checked) colors.lampLive else colors.hairline,
                        RoundedCornerShape(Radius.tight)
                    )
            )
        }
    }
}

/**
 * The seal on a guardian-managed way.
 *
 * A sealed MCB is a real object — a physical clip that stops a circuit being
 * switched by whoever happens to be standing at the board. It is exactly the
 * right vocabulary for a setting the wearer is deliberately prevented from
 * weakening on the device itself.
 */
@Composable
fun Seal(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(Radius.tight))
            .background(colors.brass.copy(alpha = if (colors.isDark) 0.28f else 0.20f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = colors.inkMuted,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = "SEALED",
            style = MaterialTheme.boardType.sealPlate,
            color = colors.inkMuted
        )
    }
}
