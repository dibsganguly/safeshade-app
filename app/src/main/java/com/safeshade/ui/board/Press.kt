package com.safeshade.ui.board

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.board

/**
 * How a plate on this board responds to being pressed.
 *
 * Material's default indication is a ripple: a circle of light expanding from
 * the touch point. It assumes a surface that emits light, and this one is sheet
 * metal — so it is the wrong material everywhere in this app, and it was
 * reimplemented-by-omission at seven call sites.
 *
 * At two of those it was also *unclipped*, which is what made it a visible bug
 * rather than a stylistic one. A ripple takes its bounds from a clip applied
 * earlier in the same chain; `WaySwitch` had no clip at all, and `AutoHero`'s
 * clip lives inside `BoardPlate`, i.e. after the modifier the caller passes in.
 * Both painted a hard grey **rectangle** over a rounded control. The other five
 * were correctly rounded ripples — still wrong for the material, but not the
 * grey flash.
 *
 * The replacement is a scale-down: the plate depresses very slightly, the way a
 * physical button set into a panel does. 1.5% is deliberately near the edge of
 * perceptible — you should feel that the control acknowledged you without being
 * able to say what moved.
 *
 * **Place this first in the modifier chain.** It scales this node and
 * everything drawn below it, so a `.clip().background().plateClickable()`
 * would depress the content while leaving the plate behind it still, which
 * looks like a bug. `.plateClickable().clip().background()` is correct.
 */
@Composable
fun Modifier.plateClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    /** Larger plates need less travel to read as moving. */
    pressScale: Float = 0.985f
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressScale else 1f,
        animationSpec = tween(Motion.fast),
        label = "plate-press"
    )
    return this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

/**
 * The same suppression, for a **full-width row** rather than a plate.
 *
 * A row cannot scale: it spans the screen, so shrinking it pulls both edges
 * away from the plate it sits in and the whole list appears to flinch. It steps
 * into the recess tone instead — the row itself depresses, which is the same
 * idea expressed in the only dimension a row has spare.
 *
 * Place it where the row's background belongs: after any width modifier, and
 * before the padding, so the tint covers the full row rather than only its
 * content box. Rows are rectangular and sit inside an already-clipped plate, so
 * unlike [plateClickable] there is nothing here that must come first.
 */
@Composable
fun Modifier.rowClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = Role.Button
): Modifier {
    val colors = MaterialTheme.board
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    return this
        .background(if (pressed && enabled) colors.recess else Color.Transparent)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}
