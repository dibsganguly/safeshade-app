package com.safeshade.ui.board

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * A wrapping row of one-tap suggestions for a field that is still free text.
 *
 * The problem it solves is narrow and comes up repeatedly: a field whose real
 * answer set is open, but whose *common* answers are five or six words the user
 * should never have to type. "Daughter". "O+". Making such a field a dropdown
 * is wrong — it turns "Upstairs neighbour" into an impossible answer — and
 * leaving it as a bare text box is what the app does today, which asks an
 * elderly guardian to spell a word on a phone keyboard to record something the
 * app could have offered in one tap.
 *
 * So this is deliberately **not** a picker. It sets the value of a text field
 * that remains fully editable beside it. A chip lights when the field happens
 * to hold its word; typing something else simply lights none of them, which is
 * a correct and untroubled state rather than an error.
 *
 * Tapping the lit chip clears the field. That matters more than it looks: with
 * no way to undo, a mis-tap on a seven-chip row can only be fixed by opening
 * the keyboard and deleting a word, which is precisely the work the row exists
 * to avoid.
 *
 * Not a Material `FilterChip`. Those are pills with a tonal fill and a leading
 * check that animates in, and this surface is built from square-shouldered
 * plates and hairlines — a pill row would read as borrowed from another app.
 * The selected state is carried by border weight, fill and ink together, never
 * by colour alone, for the same reason every other control here does that.
 *
 * @param options the words offered. Order is meaning: put the common answers
 *   first, because a wrapping row buries whatever lands on the second line.
 * @param selected the field's current value, not an index. Matching is
 *   case-insensitive and trims, so a contact saved as "daughter" before this
 *   row existed still lights its chip.
 * @param accent unspecified means the board's brass. Pass a section's accent
 *   where the surrounding block already carries one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color.Unspecified
) {
    val colors = MaterialTheme.board
    val tint = accent.takeOrElse { colors.brass }

    FlowRow(
        // So TalkBack announces "2 of 9" across the row. The bottom bar gets
        // this free from NavigationBar; a hand-built row has to say it.
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        for (option in options) {
            val isOn = option.equals(selected.trim(), ignoreCase = true)
            // A chip has no ripple, so keyboard and switch-access focus had
            // nothing to show: the edge steps up to ink while focused, the
            // same weight the lit chip's border carries.
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()

            // Animated so a tap reads as the same chip changing state rather
            // than as two chips swapping places, which is what an instant
            // switch looks like at this size.
            val fill by animateColorAsState(
                targetValue = if (isOn) tint.copy(alpha = 0.16f) else colors.recess,
                animationSpec = tween(Motion.fast),
                label = "chipFill"
            )
            val edge by animateColorAsState(
                targetValue = when {
                    focused -> colors.ink
                    isOn -> tint
                    else -> colors.hairline
                },
                animationSpec = tween(Motion.fast),
                label = "chipEdge"
            )

            Text(
                text = option,
                style = if (isOn) MaterialTheme.boardType.nameplateSmall else MaterialTheme.boardType.rowDetail,
                color = if (isOn) tint else colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.card))
                    .background(fill)
                    .border(
                        width = if (isOn || focused) Stroke.rule else Stroke.hairline,
                        color = edge,
                        shape = RoundedCornerShape(Radius.card)
                    )
                    .selectable(
                        selected = isOn,
                        role = Role.RadioButton,
                        interactionSource = interaction,
                        indication = null,
                        // Tapping the lit chip clears the field. See the note
                        // above: without this a mis-tap costs a keyboard.
                        onClick = { onSelect(if (isOn) "" else option) }
                    )
                    // Not `Spacing.touchTarget` tall. A chip row is a set of
                    // small adjacent targets and 48dp each would make seven of
                    // them taller than the field they fill; 36dp with 8dp of
                    // gap on every side is the compromise the rest of the kit
                    // makes for secondary controls.
                    .defaultMinSize(minHeight = 36.dp)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
        }
    }
}

/**
 * The relationships offered on a contact.
 *
 * Ordered by how often an emergency contact is actually one of them, not
 * alphabetically: the first line of chips should cover most people.
 */
val RELATIONSHIP_SUGGESTIONS = listOf(
    "Daughter", "Son", "Spouse", "Parent", "Sibling", "Friend", "Neighbour", "Doctor", "Carer"
)

/** Blood groups, for the Medical ID field that is a plain text box today. */
val BLOOD_GROUP_SUGGESTIONS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
