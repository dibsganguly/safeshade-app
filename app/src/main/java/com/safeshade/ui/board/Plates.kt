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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.accentFor
import com.safeshade.ui.theme.Hub
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.hubAccent
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
    /**
     * Tints the plate in a hub's accent (candidate 2.53): the fill takes the
     * accent at 0.06 over the plate tone and the border takes it at 0.35.
     * For the head plate of a hub only, the one plate on the screen that
     * says which hub this is. Every plate tinted is no plate tinted.
     */
    hub: Hub? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.board
    val accent = hub?.let { colors.hubAccent(it) }
    val fill = when {
        recessed -> colors.recess
        accent != null -> accent.copy(alpha = if (colors.isDark) 0.10f else 0.06f).over(colors.plate)
        else -> colors.plate
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(Stroke.hairline, accent?.copy(alpha = 0.35f)?.over(colors.plate) ?: colors.hairline, shape),
        content = content
    )
}

/** Flat compositing, so a tinted plate is one opaque colour rather than a translucent layer. */
private fun Color.over(base: Color): Color = Color(
    red = red * alpha + base.red * (1 - alpha),
    green = green * alpha + base.green * (1 - alpha),
    blue = blue * alpha + base.blue * (1 - alpha)
)

/**
 * A circuit label — the title of a way, the caption on a gauge.
 *
 * It no longer uppercases. Doing it here rather than at the call sites was the
 * right *mechanism*, and it is why relaxing the rule was a two-line change
 * instead of a sweep through 250 call sites; but caps on every row in the app
 * made the whole thing read as shouting. Caps now belong to [SectionPlate] and
 * to state words, where being rare is what makes them mean something.
 *
 * Callers pass the string they want shown, in the case they want it shown in.
 */
@Composable
fun Nameplate(
    text: String,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    muted: Boolean = false,
    color: Color? = null
) {
    val colors = MaterialTheme.board
    Text(
        text = text,
        style = if (small) MaterialTheme.boardType.nameplateSmall else MaterialTheme.boardType.nameplate,
        color = color ?: if (muted) colors.inkMuted else colors.ink,
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
    /**
     * A decorative accent for this section's identity — see `BoardColors`.
     * Tints the label and the rule together so the pair reads as one plate.
     * Never a state colour: a section heading describes what a bank of ways is
     * *about*, not whether anything in it is live.
     *
     * Left null, the heading derives one from its own [title], so every
     * section on a screen is a different colour without a screen having to say
     * so. Pass `Color.Unspecified` to opt a single section back out to plain
     * ink — the right call on a safety path.
     */
    accent: Color? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board
    val tint = when {
        accent == null -> colors.accentFor(title)
        accent.isSpecified -> accent
        else -> null
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            // A fixed header height, so a section with a trailing icon button
            // and one without land their rule at the same place. Previously the
            // button's 48dp touch target inflated only those headers, and the
            // rule sat noticeably further from the label on Conditions and
            // Where than everywhere else.
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.boardType.sectionPlate,
                color = tint ?: colors.inkMuted,
                modifier = Modifier.semantics { heading() }
            )
            trailing?.invoke()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Stroke.brass)
                // The accent is muted well below its full strength here. At
                // full strength a 2dp rule spanning the screen becomes the
                // loudest thing on it, which is the opposite of what a
                // separator is for.
                .background(tint?.copy(alpha = 0.55f) ?: colors.brass)
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
        // No fixed height. Callers pass fillMaxHeight() inside an
        // IntrinsicSize.Min row so the tick grows with the text beside it; a
        // hardcoded stub looked right on one line and stunted on three.
        modifier = modifier
            .width(Stroke.heavy)
            .heightIn(min = 20.dp)
            .clip(RoundedCornerShape(Radius.tight))
            .background(tint)
    )
}
