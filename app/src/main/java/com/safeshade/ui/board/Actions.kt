package com.safeshade.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.BrandCharcoal
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * The compound actions adopted from the v2.0 candidates in v2.8.0: a pair,
 * a split, a hold, and the foot bar that carries a pair at the end of every
 * editor. Each is built from the same plate and the same 56dp floor as
 * [BoardButton], so a page that mixes them reads as one set of controls.
 */

/** The near-white divider between joined cells. One value in both themes: it is the light between two plates, not a plate. */
private val CellDivider = Color(0xFFFBF9F5)

/** The fill and ink a [ButtonWeight] resolves to, shared by every compound action. */
@Composable
internal fun buttonColors(weight: ButtonWeight, enabled: Boolean): Pair<Color, Color> {
    val colors = MaterialTheme.board
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
    return container to content
}

/**
 * An action pair (2.65): one object with a primary cell two-thirds wide and
 * a quiet cell beside it, divided by a narrow near-white bar the full height
 * of the button. Save beside Discard; I am OK beside Call now. Two loose
 * buttons of different weights read as two decisions; a pair reads as one
 * decision with a default.
 *
 * The primary defaults to **amber**. A Save at the foot of an editor is the
 * one thing to do next on that page, which is exactly what the amber weight
 * means; teal stayed the colour of a standalone commit. The secondary cell is
 * quiet by default and takes trip ink when [secondaryDestructive] is set
 * (Discard, Remove), so the word is red without the cell being a call button.
 */
@Composable
fun ActionPair(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryWeight: ButtonWeight = ButtonWeight.ATTENTION,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true,
    secondaryDestructive: Boolean = false,
    /** A hued secondary cell, for the trip banner's Call now. Overrides the quiet cell. */
    secondaryWeight: ButtonWeight? = null
) {
    val colors = MaterialTheme.board
    val (primaryFill, primaryInk) = buttonColors(primaryWeight, primaryEnabled)
    val (secondaryFill, secondaryInk) = if (secondaryWeight != null) {
        buttonColors(secondaryWeight, secondaryEnabled)
    } else {
        (if (secondaryEnabled) colors.ground else colors.recess) to when {
            !secondaryEnabled -> colors.inkFaint
            secondaryDestructive -> colors.inkTrip
            else -> colors.inkMuted
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.plate))
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate))
            .height(IntrinsicSize.Min)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(2f)
                .fillMaxHeight()
                .plateClickable(enabled = primaryEnabled, onClick = onPrimary)
                .background(primaryFill)
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Text(
                text = primaryLabel,
                style = MaterialTheme.boardType.nameplate.copy(fontWeight = FontWeight.W700),
                color = primaryInk,
                textAlign = TextAlign.Center
            )
        }
        Box(Modifier.width(Stroke.brass).fillMaxHeight().background(CellDivider))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .plateClickable(enabled = secondaryEnabled, onClick = onSecondary)
                .background(secondaryFill)
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
        ) {
            Text(
                text = secondaryLabel,
                style = if (secondaryWeight != null) MaterialTheme.boardType.nameplate.copy(fontWeight = FontWeight.W700) else MaterialTheme.boardType.nameplate,
                color = secondaryInk,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A split button (2.64): the main action fills most of the width and a
 * narrow trailing cell with a chevron offers the variant, in the same fill,
 * divided by the same near-white bar the [ActionPair] uses. Call, and beside
 * it who else; Start the journey, and beside it Walk home. Two targets, one
 * object, no second row.
 */
@Composable
fun SplitButton(
    label: String,
    onClick: () -> Unit,
    onMore: () -> Unit,
    /** Spoken name of the trailing cell: "Other people to call", "Other routes". */
    moreDescription: String,
    modifier: Modifier = Modifier,
    weight: ButtonWeight = ButtonWeight.PRIMARY,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.board
    val (fill, ink) = buttonColors(weight, enabled)
    val bordered = weight == ButtonWeight.SECONDARY || weight == ButtonWeight.QUIET
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.plate))
            .background(fill)
            .border(Stroke.hairline, if (bordered) colors.hairline else fill, RoundedCornerShape(Radius.plate))
            .height(IntrinsicSize.Min)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .plateClickable(enabled = enabled, onClick = onClick)
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = label,
                style = MaterialTheme.boardType.nameplate.copy(fontWeight = FontWeight.W700),
                color = ink,
                textAlign = TextAlign.Center
            )
        }
        Box(Modifier.width(Stroke.brass).fillMaxHeight().background(if (bordered) colors.hairline else CellDivider))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .plateClickable(enabled = enabled, onClick = onMore)
                .defaultMinSize(minHeight = 56.dp)
        ) {
            Icon(
                imageVector = SafeShadeIcons.ArrowDown01,
                contentDescription = moreDescription,
                tint = ink,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Hold to confirm (2.67), for the one action on a page that cannot be undone:
 * deleting a person, a recording, an account.
 *
 * The finger stays down and a trip-red line traces the plate's perimeter,
 * starting at the top-left corner and running anticlockwise (down the left
 * edge, along the foot, up the right, back across the top); when it rejoins
 * at the top-left the action fires. Lifting early winds the trace back and
 * nothing happens. A slip on a red button does nothing, which is the whole
 * point of the form. The label says "Hold to" so the gesture is stated, not
 * discovered, and TalkBack gets the action as a plain click, because a
 * timed hold is a sighted-finger gesture and a screen reader's double-tap
 * already carries its own deliberateness.
 */
@Composable
fun HoldToConfirm(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    holdMillis: Int = Motion.holdToConfirm
) {
    val colors = MaterialTheme.board
    val haptics = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }
    var pressing by remember { mutableStateOf(false) }
    var fired by remember { mutableStateOf(false) }

    LaunchedEffect(pressing, enabled) {
        if (pressing && enabled) {
            fired = false
            progress.animateTo(1f, tween((holdMillis * (1f - progress.value)).toInt(), easing = LinearEasing))
            if (progress.value >= 1f && !fired) {
                fired = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onConfirm()
            }
        } else {
            progress.animateTo(0f, tween(Motion.normal))
        }
    }

    val trace = colors.lampTrip
    val labelInk = if (!enabled) colors.inkFaint else colors.inkTrip
    val strokePx = with(LocalDensity.current) { Stroke.heavy.toPx() }
    val cornerPx = with(LocalDensity.current) { Radius.plate.toPx() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = label
                onClick { if (enabled) { onConfirm(); true } else false }
            }
            .clip(RoundedCornerShape(Radius.plate))
            .background(if (enabled) colors.plate else colors.recess)
            .border(Stroke.hairline, if (enabled) colors.lampTrip.copy(alpha = 0.45f) else colors.hairline, RoundedCornerShape(Radius.plate))
            .drawWithContent {
                drawContent()
                val f = progress.value
                if (f > 0f) {
                    val inset = strokePx / 2f
                    val rect = Rect(inset, inset, size.width - inset, size.height - inset)
                    val path = Path().apply {
                        addRoundRect(RoundRect(rect, CornerRadius(cornerPx)))
                    }
                    val measure = PathMeasure().apply { setPath(path, false) }
                    val length = measure.length
                    // The path is laid down clockwise from the top-left, so the
                    // anticlockwise trace is its tail: from (1 - f) of the way
                    // round to the end, which runs down the left edge first.
                    val segment = Path()
                    measure.getSegment(length * (1f - f), length, segment, true)
                    drawPath(segment, trace, style = DrawStroke(width = strokePx, cap = StrokeCap.Butt))
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressing = true
                        tryAwaitRelease()
                        pressing = false
                    }
                )
            }
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = labelInk, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(
            text = label,
            style = MaterialTheme.boardType.nameplate.copy(fontWeight = FontWeight.W700),
            color = labelInk,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The foot of an editor (2.34): pinned to the bottom of the screen, above the
 * keyboard, with two short lines above the pair and the [ActionPair] under
 * them. The left line says what is unsaved ("3 fields changed"), the right
 * says where the record stands ("Not yet on the wearable"); either may be
 * null. A person three fields into the medical ID can always see the way
 * out, and the button says how much it is about to commit without a sentence
 * under it.
 *
 * Use it through [EditorScaffold] so the pin, the keyboard inset and the
 * list's bottom padding are decided in one place.
 */
@Composable
fun EditorFootBar(
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    changedLine: String? = null,
    statusLine: String? = null,
    /** A state for the status line: attention while unsaved, live once landed, trip on a refusal. */
    statusState: LampState? = null,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true,
    secondaryDestructive: Boolean = true
) {
    val colors = MaterialTheme.board
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.plate)
    ) {
        Hairline()
        Column(Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.md)) {
            if (changedLine != null || statusLine != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = changedLine ?: "",
                        style = MaterialTheme.boardType.nameplateSmall,
                        color = colors.ink,
                        modifier = Modifier.weight(1f)
                    )
                    if (statusLine != null) {
                        Spacer(Modifier.width(Spacing.md))
                        Text(
                            text = statusLine,
                            style = MaterialTheme.boardType.rowDetail,
                            color = when (statusState) {
                                LampState.LIVE -> colors.inkLive
                                LampState.ATTENTION -> colors.inkAttention
                                LampState.TRIP -> colors.inkTrip
                                else -> colors.inkFaint
                            },
                            textAlign = TextAlign.End
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
            }
            ActionPair(
                primaryLabel = primaryLabel,
                onPrimary = onPrimary,
                secondaryLabel = secondaryLabel,
                onSecondary = onSecondary,
                primaryEnabled = primaryEnabled,
                secondaryEnabled = secondaryEnabled,
                secondaryDestructive = secondaryDestructive
            )
        }
    }
}

/**
 * A screen with a pinned foot.
 *
 * The content gets a bottom padding equal to the foot's measured height so
 * its last field can scroll clear of the bar, and the foot sits above the
 * navigation bar and the keyboard. One implementation, so six editors do not
 * each invent a pin.
 */
@Composable
fun EditorScaffold(
    modifier: Modifier = Modifier,
    /**
     * The bottom of the screen's own content padding: what the host reserves
     * for the bottom bar. The foot sits on top of that reservation. The
     * navigation-bar inset is not added here because the host's bar already
     * consumes it, and adding it again floated the foot a thumb above the bar.
     */
    bottomPadding: Dp = 0.dp,
    foot: @Composable () -> Unit,
    content: @Composable (footPadding: PaddingValues) -> Unit
) {
    var footHeightPx by remember { mutableIntStateOf(0) }
    val footHeight = with(LocalDensity.current) { footHeightPx.toDp() }
    Box(modifier = modifier.fillMaxSize()) {
        content(PaddingValues(bottom = footHeight + bottomPadding))
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = bottomPadding)
                .imePadding()
                .onSizeChanged { footHeightPx = it.height }
        ) {
            foot()
        }
    }
}
