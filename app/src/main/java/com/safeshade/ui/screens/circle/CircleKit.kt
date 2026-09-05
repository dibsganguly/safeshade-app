package com.safeshade.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.safeshade.data.MessageChannel
import com.safeshade.data.UserRole
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board

/**
 * The three things the Circle screens need that the board kit does not have.
 *
 * Everything else — plates, ways, switches, buttons, lamps, gauges — comes from
 * `ui/board`. These are additions rather than alternatives: a text input, a
 * pick-one option plate, and the channel badge that tells a guardian a message
 * went by SMS. Each one is here because the kit genuinely has no equivalent,
 * not because the kit's version was inconvenient.
 *
 * They are `internal` and live beside the screens that use them so nobody
 * mistakes them for part of the shared system.
 */

/**
 * A field set into the panel.
 *
 * There is no input in the kit, and Material's `OutlinedTextField` brings a
 * floating label, a focus ring and a 20dp-ish rounded box — three pieces of a
 * different material entirely. The recess token exists precisely for this:
 * "a routed channel — inputs, wells, anything set *into* the panel". So the
 * field is a `BasicTextField` in a recess with a nameplate above it.
 *
 * The nameplate is a plain sibling text node rather than a content description
 * on the input — see the comment on the `BasicTextField` below for why that
 * distinction decides whether a screen-reader user can hear what they typed.
 */
@Composable
internal fun BoardField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supporting: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board
    val shape = RoundedCornerShape(Radius.plate)

    Column(modifier = modifier) {
        Nameplate(label, small = true, muted = true)
        Spacer(Modifier.height(Spacing.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.recess)
                .border(Stroke.hairline, colors.hairline, shape)
                // defaultMinSize rather than height: at a 1.3x font scale the
                // text has to be allowed to make the field taller.
                .defaultMinSize(minHeight = Spacing.touchTarget)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkFaint
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    keyboardOptions = keyboardOptions,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.ink),
                    // No contentDescription here, deliberately. On an editable
                    // node TalkBack announces the description *instead of* the
                    // text, so labelling the field would hide the digits the
                    // user just typed — precisely the fields (coordinates,
                    // phone numbers) where reading the value back matters
                    // most. The nameplate above is a sibling text node in
                    // traversal order and already names the field.
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(Spacing.sm))
                trailing()
            }
        }
        if (supporting != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
        }
    }
}

/**
 * One option in a pick-one set: an ETA, a check-in deadline.
 *
 * Deliberately the same construction as the role cards in onboarding — a
 * brass border and a lit pilot lamp — rather than a Material chip or a
 * segmented button. Selection is already a vocabulary this app has, and a lamp
 * carries "this one is chosen" to a screen reader through [stateDescription]
 * instead of through a fill colour that colour-blind users cannot separate.
 */
@Composable
internal fun ChoicePlate(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.board
    val shape = RoundedCornerShape(Radius.plate)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.plate else colors.ground)
            .border(
                width = if (selected) Stroke.brass else Stroke.hairline,
                color = if (selected) colors.brass else colors.hairline,
                shape = shape
            )
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .defaultMinSize(minHeight = Spacing.touchTarget)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
    ) {
        PilotLamp(
            state = if (selected) LampState.LIVE else LampState.OFF,
            size = 12.dp,
            description = if (selected) "Selected" else "Not selected"
        )
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Nameplate(label, muted = !selected)
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }
    }
}

/**
 * Lays a set of [ChoicePlate]s out two to a line.
 *
 * A fixed four-across row of ETA buttons collapses into unreadable slivers at
 * a raised font scale, and a single column of four is a lot of screen for a
 * minor choice. Two per line survives both.
 */
@Composable
internal fun <T> ChoiceGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    perRow: Int = 2,
    itemContent: @Composable (item: T, cellModifier: Modifier) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items.chunked(perRow).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { item -> itemContent(item, Modifier.weight(1f)) }
                // Keeps a trailing odd item at its normal width rather than
                // letting it stretch across the line, where it would read as a
                // different kind of control from its siblings.
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * How a message travelled.
 *
 * Achromatic on purpose. Channel is not circuit state, and the one rule of
 * this colour system is that a coloured element means a live circuit — an
 * amber "SMS" pill would tell a reader something untrue. It is also not a
 * [com.safeshade.ui.board.Seal]: that mark means "the guardian has locked
 * this", and borrowing it here would blunt a signal that carries real weight
 * elsewhere.
 */
@Composable
internal fun ChannelBadge(
    channel: MessageChannel,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.tight))
            .background(colors.recess)
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.tight))
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
    ) {
        Nameplate(
            text = when (channel) {
                MessageChannel.BLE -> "Bluetooth"
                MessageChannel.SMS -> "SMS"
            },
            small = true,
            muted = true
        )
    }
}

/**
 * The header every Circle detail route draws for itself.
 *
 * Not a Material `TopAppBar`: that brings its own surface, elevation and
 * insets, and the board has no floating bars — the Board screen already draws
 * its title as an ordinary first row, and these screens match it. The back
 * control is an icon button and therefore carries a spoken name; "Back" alone
 * is uselessly vague when a screen reader announces it out of context, so
 * callers pass what is being left.
 */
@Composable
internal fun CircleTopRow(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    backDescription: String = "Go back",
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = backDescription,
                tint = colors.inkMuted
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }
        trailing?.invoke()
    }
}

// ============================================
// Role-aware copy
// ============================================

/**
 * The name of the other person on the link, from this phone's point of view.
 *
 * Every screen in this package needs it and every screen would otherwise
 * compute it slightly differently. A guardian is looking at the person they
 * watch over; a wearer is looking at the guardian who watches over them.
 */
internal fun counterpartName(
    role: UserRole,
    wearerName: String,
    guardianName: String
): String = when (role) {
    UserRole.GUARDIAN -> wearerName.ifBlank { "the wearer" }
    UserRole.COMPANION -> guardianName.ifBlank { "your guardian" }
}

/**
 * The same person, for a slot styled as a name rather than a sentence.
 *
 * [counterpartName]'s fallbacks are sentence fragments — lowercase, and in the
 * companion's case word-for-word the page title. Set in headline type they read
 * as though somebody were actually called "your guardian". When nobody is set
 * yet, say so plainly instead.
 */
internal fun counterpartHeadline(
    role: UserRole,
    wearerName: String,
    guardianName: String
): String = when (role) {
    UserRole.GUARDIAN -> wearerName.ifBlank { "No wearer set yet" }
    UserRole.COMPANION -> guardianName.ifBlank { "No guardian set yet" }
}

/**
 * The wearer, named, for copy about the device itself.
 *
 * A guardian's screens talk about their father in the third person; a
 * companion's talk about themselves in the second. Guardian copy must never
 * say "you are wearing this", which is the single most common way this app
 * addresses the wrong person.
 */
internal fun wearerSubject(role: UserRole, wearerName: String): String = when (role) {
    UserRole.GUARDIAN -> wearerName.ifBlank { "the wearer" }
    UserRole.COMPANION -> "you"
}
