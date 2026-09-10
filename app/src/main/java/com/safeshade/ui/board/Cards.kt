package com.safeshade.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * The card forms adopted from the v2.0 candidates in v2.8.0.
 *
 * Each is a way of giving a [BoardPlate] one more thing to say without a
 * second plate: a title machined into its top, a stamp in its corner, a glyph
 * under its text, an action as its foot, a header row that carries its own
 * button. None of them is a new kind of card; all of them are the same plate
 * with one feature, and a screen picks the one feature its plate needs.
 */

/**
 * A plate whose title is machined into a recess strip across its top (2.91).
 *
 * For a plate that is a bank with a name and would otherwise carry a
 * [SectionPlate] floating over it. The heading and the bank become one
 * object. The title is in the section-plate voice, so a screen that mixes
 * this with ruled section plates still reads as one system.
 */
@Composable
fun TitledPlate(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.recess)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.boardType.sectionPlate,
                color = colors.inkMuted,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            trailing?.invoke()
        }
        Hairline()
        content()
    }
}

/**
 * A bank's header as its first row (2.81): the title in the display voice, a
 * count in mono, and one action on a filled square at the end.
 *
 * Replaces a [SectionPlate] with a trailing icon button over a plate. The
 * action is an ink square with a plate-coloured glyph rather than an outlined
 * one, because a hairline box beside a hairline-bordered plate vanished into
 * it; a filled square is the one solid thing in the row and reads as the
 * button. The glyph is drawn from a vector whose stroke is baked in, so its
 * prominence comes from the fill and its 22dp size, not from a heavier line.
 *
 * Place it as the first child of a [BoardPlate]; it draws its own rule under.
 */
@Composable
fun BankHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionIcon: ImageVector? = null,
    /** Required with [actionIcon]: the word the button would have carried. */
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
    actionEnabled: Boolean = true
) {
    val colors = MaterialTheme.board
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            if (count != null) {
                Text(
                    text = "$count",
                    style = MaterialTheme.boardType.readout,
                    color = colors.inkFaint
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            if (actionIcon != null && onAction != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .plateClickable(enabled = actionEnabled, onClick = onAction)
                        .size(40.dp)
                        .clip(RoundedCornerShape(Radius.plate))
                        .background(if (actionEnabled) colors.ink else colors.recess)
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionDescription,
                        tint = if (actionEnabled) colors.plate else colors.inkFaint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(Stroke.rule).background(colors.hairline))
    }
}

/**
 * A plate with a stamp in its corner (2.90).
 *
 * The [QualifierChip]'s word set into the top-right corner with its top and
 * right edges flush to the plate's own, like a stamp on a form: SEALED, PLUS,
 * DEVICE-ONLY, YOUR PLAN. The content column keeps the whole width under it,
 * so the tag is placed outside the padded column rather than inside it, which
 * is what left the candidate's tag hovering near the top-middle.
 */
@Composable
fun TaggedPlate(
    tag: String,
    modifier: Modifier = Modifier,
    /** A state ink for a tag that reports something; null for a plain stamp. */
    tagColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            Column(content = content)
            CornerTag(tag, color = tagColor, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

/**
 * The stamp itself, for a caller that already has a Box. Clipped at the
 * bottom-left corner only, so the two outer edges sit flush with the plate's.
 */
@Composable
fun CornerTag(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val colors = MaterialTheme.board
    Text(
        text = text.uppercase(),
        style = MaterialTheme.boardType.sealPlate,
        color = color ?: colors.inkMuted,
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = Radius.card, topEnd = Radius.card))
            .background(colors.brass.copy(alpha = if (colors.isDark) 0.28f else 0.20f))
            .padding(horizontal = Spacing.sm, vertical = 3.dp)
    )
}

/**
 * A card with a watermark glyph (2.94): the card's own glyph at 96dp under
 * its text, bleeding off the bottom-right corner.
 *
 * The tint is the caller's choice and the rule is narrow. A plate about a
 * *thing* takes that thing's accent; a plate about a *circuit* takes the
 * circuit's lamp glass (a live zone's glyph is teal, a tripped alert's is
 * red), which is the same colour the row's bus tick would have carried. OFF
 * and UNKNOWN take the hairline, never an accent, or a dead circuit looks
 * decorated. One watermark per bank at most: two side by side read as
 * wallpaper.
 */
@Composable
fun WatermarkPlate(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glyphSize: Dp = 96.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    BoardPlate(
        modifier = modifier
            .then(if (onClick != null) Modifier.plateClickable(onClick = onClick) else Modifier)
            .fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint.copy(alpha = if (MaterialTheme.board.isDark) 0.14f else 0.10f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = glyphSize * 0.22f, y = glyphSize * 0.22f)
                    .size(glyphSize)
            )
            Column(content = content)
        }
    }
}

/** The watermark tint a [LampState] earns. Lit states take their glass; unlit take the hairline. */
@Composable
fun watermarkTint(state: LampState): Color {
    val colors = MaterialTheme.board
    return when (state) {
        LampState.LIVE -> colors.lampLive
        LampState.ATTENTION -> colors.lampAttention
        LampState.TRIP -> colors.lampTrip
        LampState.OFF, LampState.UNKNOWN -> colors.hairline
    }
}

/**
 * A card's one action as its last row (2.92): a hairline, then the label in
 * the nameplate voice with a leading glyph and a trailing chevron, full width.
 * Not a button inside a card; the card's foot is the button.
 *
 * Place it as the last child of a [BoardPlate]. It draws its own hairline.
 */
@Composable
fun FooterAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    /** A state ink for a foot that reports as well as acts. */
    color: Color? = null
) {
    val colors = MaterialTheme.board
    Column(modifier = modifier.fillMaxWidth()) {
        Hairline()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .rowClickable(role = Role.Button, enabled = enabled, onClick = onClick)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) (color ?: colors.inkMuted) else colors.inkFaint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = label,
                style = MaterialTheme.boardType.nameplate,
                color = if (enabled) (color ?: colors.ink) else colors.inkFaint,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = SafeShadeIcons.ArrowRight01,
                contentDescription = null,
                tint = colors.inkFaint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * A strip of cards (2.95): fixed-width plates in a row that scrolls under
 * the gutter. Where a bank of four rows takes 200dp of height, a strip takes
 * 120 and shows two and a half at once, which is the right trade for a set
 * of peers a person scans rather than reads: the Circle's zones, the
 * Device's wearables, the people looked after.
 *
 * The strip bleeds to the screen edges and pads its content by the gutter,
 * so the first card lines up with the plates above it and the last card
 * peeks in from the right to say there is more. Callers inside a gutter
 * should pass [bleed] as the gutter so the strip can reach past it.
 */
@Composable
fun CardStrip(
    modifier: Modifier = Modifier,
    bleed: Dp = Spacing.gutter,
    content: LazyListScope.() -> Unit
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                // Widen the row by the bleed on both sides, then shift it
                // back, so it draws edge to edge while its parent still
                // measures it at the gutter width.
                val extra = (bleed * 2).roundToPx()
                val placeable = measurable.measure(
                    constraints.copy(
                        maxWidth = constraints.maxWidth + extra,
                        minWidth = (constraints.minWidth + extra).coerceAtMost(constraints.maxWidth + extra)
                    )
                )
                layout(placeable.width - extra, placeable.height) {
                    placeable.placeRelative(-bleed.roundToPx(), 0)
                }
            },
        contentPadding = PaddingValues(horizontal = bleed),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        content = content
    )
}

/**
 * One card in a [CardStrip]: a lamp and a title, one line under, and the
 * thing's glyph on a recess strip at the foot in its accent.
 */
@Composable
fun StripCard(
    title: String,
    line: String,
    state: LampState,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
    onClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.board
    BoardPlate(
        modifier = modifier
            .then(if (onClick != null) Modifier.plateClickable(onClick = onClick) else Modifier)
            .width(width)
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PilotLamp(state, size = 10.dp)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = line,
                style = MaterialTheme.boardType.rowDetail,
                color = colors.inkMuted,
                maxLines = 2
            )
            Spacer(Modifier.height(Spacing.md))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(Radius.plate))
                    .background(colors.recess)
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}
