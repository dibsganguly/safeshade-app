package com.safeshade.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * A raised plate on the panel.
 *
 * This replaces the frosted-glass card the app used everywhere. Glass wants
 * depth and blur; a distribution board is flat sheet metal with a milled edge,
 * so separation comes from a hairline and a tonal step rather than from a
 * shadow. Nothing here casts light.
 */
@Composable
fun BoardPlate(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.card),
    recessed: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.board
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (recessed) colors.recess else colors.plate)
            .border(Stroke.hairline, colors.hairline, shape),
        content = content
    )
}

/**
 * An engraved circuit label.
 *
 * Uppercased here rather than at every call site, because the label's identity
 * *is* its engraved form — a caller passing mixed case should still get a
 * nameplate. Tracking comes from the type style.
 */
@Composable
fun Nameplate(
    text: String,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    muted: Boolean = false
) {
    val colors = MaterialTheme.board
    Text(
        text = text.uppercase(),
        style = if (small) MaterialTheme.boardType.nameplateSmall else MaterialTheme.boardType.nameplate,
        color = if (muted) colors.inkMuted else colors.ink,
        modifier = modifier
    )
}

/**
 * A section heading sitting above a brass rule.
 *
 * The rule is the one piece of ornament the system allows, and it earns its
 * place by doing structural work: it is how a long settings screen reads as a
 * set of labelled banks rather than an undifferentiated list.
 */
@Composable
fun SectionPlate(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.boardType.sectionPlate,
                color = colors.inkMuted,
                modifier = Modifier.semantics { heading() }
            )
            trailing?.invoke()
        }
        Box(
            modifier = Modifier
                .padding(top = Spacing.xs)
                .fillMaxWidth()
                .height(Stroke.brass)
                .background(colors.brass)
        )
    }
}

/** A plain structural separator between ways. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Stroke.hairline)
            .background(MaterialTheme.board.hairline)
    )
}

/**
 * A short vertical tick, used to mark the left edge of a way.
 *
 * Reads as the busbar the circuit taps off. Its colour carries state, which is
 * why it takes a [LampState] rather than a colour.
 */
@Composable
fun BusTick(
    state: LampState,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val tint = when (state) {
        LampState.LIVE -> colors.lampLive
        LampState.ATTENTION -> colors.lampAttention
        LampState.TRIP -> colors.lampTrip
        LampState.OFF, LampState.UNKNOWN -> colors.hairline
    }
    Box(
        modifier = modifier
            .width(Stroke.heavy)
            .height(28.dp)
            .clip(RoundedCornerShape(Radius.tight))
            .background(tint)
    )
}
