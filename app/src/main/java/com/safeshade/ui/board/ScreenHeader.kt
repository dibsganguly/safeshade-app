package com.safeshade.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * The top of a screen. One treatment, everywhere.
 *
 * Before this existed, four peer screens opened four different ways —
 * `headlineSmall` in Safety, `titleMedium` in Circle, `displaySmall` in Device
 * and Settings — and the difference was visible the moment you moved between
 * tabs. None of the four was wrong on its own; having four was.
 *
 * The convergence goes **upward**. [ScreenTier.PUSHED] lands at
 * `headlineMedium`, above both incumbents rather than at the smaller of them:
 * these headings are the first thing read on a screen somebody may be opening
 * in a hurry, and this app's stated audience includes elderly guardians. Tab
 * roots keep a step above that again, so depth is still legible at a glance.
 *
 * The generous top margin is deliberate and is why [topMargin] is not a
 * caller's decision. A heading crowding the status bar was the specific
 * complaint; leaving it to call sites is how it comes back on one screen in
 * six months.
 *
 * ## Where the subtitle sits
 *
 * The subtitle is **not** inside the title's column. It sits below the whole
 * top row, starting at the gutter — level with the back control rather than
 * indented to the title. Indenting it made the two lines read as a single
 * block hanging off the arrow, which is wrong: the arrow belongs to the title
 * it sits beside, and the subtitle belongs to the screen. It also cost the
 * subtitle the width of a touch target on every pushed screen, which is where
 * the wrapping came from.
 *
 * ## The header-to-content gap
 *
 * The trailing [Spacing.xs] below the header is fixed and not a parameter, for
 * the same reason the top margin isn't: it used to be a call site's choice,
 * three different banks made three different choices, and sub-pages ended up
 * starting their content at three different distances from the header. The
 * fix is not "emit the whole gap here" — a header sitting as the first item
 * of a list already rhythmed with `Arrangement.spacedBy(Spacing.lg)` would
 * double up if it also emitted that much itself, and roughly nineteen screens
 * already depend on exactly that combination. Instead the gap is one number
 * decided in one place and assembled the same way everywhere: this trailing
 * [Spacing.xs], plus [Spacing.lg] of rhythm supplied by whatever holds the
 * header — a list's `spacedBy`, the Safety bank's header adapter, or a body
 * list's top `contentPadding` — for a fixed total of 20dp. The rule that makes
 * this hold is: **no call site ever places its own `Spacer` after a header.**
 * A screen that needs the 16dp and has no natural list rhythm to source it
 * from is a screen whose container is missing something, not a screen that
 * gets to invent its own number.
 *
 * It was 28dp until the total was measured across the app rather than assumed.
 * Three screens were reaching 44dp because they used the Safety bank's `Column`
 * adapter inside a list that already carried the rhythm, and two were reaching
 * 12dp because their container carried none. The number is smaller now and, for
 * the first time, actually the same everywhere.
 *
 * @param onBack null means no back control — correct for a tab root, which has
 *   nowhere to go. Every pushed screen passes one. It is nullable rather than
 *   defaulted-to-empty so a call site that forgets shows a visibly absent
 *   arrow instead of a drawn dead one.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    tier: ScreenTier = ScreenTier.PUSHED,
    /** Spoken name for the back control. "Back" alone is vague out of context. */
    backDescription: String = "Back",
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(if (tier == ScreenTier.ROOT) Spacing.lg else Spacing.sm))
        Row(
            // Centre, now that the subtitle has moved out of this row. The row
            // is one line of text tall (or one touch target, whichever wins),
            // so centring is what puts the chevron on the title's own optical
            // middle. It had to be Top while the row held both lines, because a
            // centred arrow then drifted down beside the subtitle.
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Spacing.touchTarget)
        ) {
            if (onBack != null) {
                // The slot is narrower than the control inside it. The chevron
                // keeps a full 48dp touch target — `requiredSize` is what lets
                // it ignore this Box's width — while the Box reserves only
                // [BackSlotWidth] of layout, so the title sits close beside the
                // glyph instead of a touch box away from it. Pulling the button
                // into the gutter with an offset alone could not do this: an
                // offset moves what is drawn and not what is measured, so the
                // title stayed put and the trailing slot lost the same width at
                // the other end.
                Box(
                    modifier = Modifier.width(BackSlotWidth),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .requiredSize(Spacing.touchTarget)
                            .offset(x = (-12).dp)
                    ) {
                        Icon(
                            imageVector = SafeShadeIcons.ArrowLeft01,
                            contentDescription = backDescription,
                            // The amber *ink*, not the amber lamp glass. Raw
                            // brand amber on the bone panel measures about
                            // 1.9:1, which is not a contrast a control glyph
                            // can be drawn at; `inkAttention` is the same hue
                            // family darkened for exactly this and clears 7:1
                            // in both themes.
                            tint = colors.inkAttention,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Text(
                text = title,
                style = when (tier) {
                    ScreenTier.ROOT -> MaterialTheme.typography.displaySmall
                    ScreenTier.PUSHED -> MaterialTheme.typography.headlineMedium
                },
                color = colors.ink,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            if (trailing != null) {
                Spacer(Modifier.width(Spacing.sm))
                // The mirror of the back control's correction. A trailing
                // IconButton centres its 24dp glyph inside a 48dp touch box, so
                // with no nudge it sits visibly inset from the gutter. The
                // Device screen passed one with no offset at all and it read as
                // misaligned with the word beside it.
                Box(modifier = Modifier.offset(x = 12.dp)) {
                    trailing()
                }
            }
        }
        if (subtitle != null) {
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                // At the gutter, not indented under the title. See the note
                // above: the arrow belongs to the title, the subtitle belongs
                // to the screen.
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted
            )
        }
        // The near half of the 20dp header gap described above — the rest comes
        // from whatever holds this header. Not a parameter: see that note.
        Spacer(Modifier.height(Spacing.xs))
    }
}

/** How deep a screen sits. Decides the heading's weight, nothing else. */
enum class ScreenTier {
    /** A bottom-bar destination. */
    ROOT,

    /** Anything pushed on top of one. */
    PUSHED
}

/**
 * A header with the back control but no separate title row, for screens whose
 * first content item already names them (a large illustrated hero, a live map).
 */
@Composable
fun BackOnlyHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backDescription: String = "Back",
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(
                imageVector = SafeShadeIcons.ArrowLeft01,
                contentDescription = backDescription,
                tint = colors.inkAttention,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/**
 * How much layout the back control reserves.
 *
 * Less than the 48dp it is: the touch target overhangs its slot on both sides,
 * 12dp into the gutter and the rest under the title's leading edge, so the
 * chevron reads as sitting next to the word rather than a thumb's width away
 * from it. Nothing overlaps — a headline's leading side bearing is wider than
 * the 4dp of glyph that reaches into it.
 */
private val BackSlotWidth = 28.dp
