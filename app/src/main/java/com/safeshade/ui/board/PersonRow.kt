package com.safeshade.ui.board

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * A person as a way: bus tick, their face, their name, one line, and on the
 * right either a state word or a chevron.
 *
 * The `Way` row carries an icon; a person carries a face, and a face is the
 * thing a guardian scans a list for. Everything else is the way's grammar,
 * so a bank of people reads like a bank of circuits: the tick and the word
 * tell you their state, the face and the name tell you who.
 *
 * Semantics are one node, "name, state, detail", like `Way`.
 */
@Composable
fun PersonRow(
    name: String,
    avatarId: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    state: LampState = LampState.OFF,
    /** The state word on the right. Null draws a chevron instead, for a row that only opens something. */
    stateLabel: String? = null,
    placeholder: String = "Unnamed",
    onClick: (() -> Unit)? = null
) {
    val colors = MaterialTheme.board
    val shown = name.ifBlank { placeholder }
    val spoken = buildString {
        append(shown)
        if (stateLabel != null) { append(", "); append(stateLabel) }
        if (detail != null) { append(", "); append(detail) }
        if (onClick != null) append(". Opens.")
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.rowClickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = Spacing.lg, end = Spacing.md, top = Spacing.md, bottom = Spacing.md)
            .height(IntrinsicSize.Min)
            .clearAndSetSemantics { contentDescription = spoken }
    ) {
        BusTick(state = state, modifier = Modifier.fillMaxHeight())
        Spacer(Modifier.width(Spacing.md))
        Avatar(avatarId = avatarId, name = name, size = 44.dp)
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shown,
                style = MaterialTheme.typography.titleMedium,
                color = if (name.isBlank()) colors.inkMuted else colors.ink
            )
            if (detail != null) {
                Text(text = detail, style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        if (stateLabel != null) {
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
        } else if (onClick != null) {
            Icon(
                imageVector = SafeShadeIcons.ArrowRight01,
                contentDescription = null,
                tint = colors.inkFaint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
