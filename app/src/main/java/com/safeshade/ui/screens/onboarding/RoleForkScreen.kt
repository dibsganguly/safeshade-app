package com.safeshade.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.unit.dp
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.LampState
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board

/**
 * The fork the entire app hangs off.
 *
 * SafeShade serves two mental models that happen to share one binary, and they
 * are not variations of each other:
 *
 *  - A working adult or someone carrying it for personal safety **is** the
 *    wearer. The phone and the device belong to the same person.
 *  - For a child, an elderly parent, or a pet, the wearer never opens this app
 *    at all. Every screen is being read by somebody else, about somebody else.
 *
 * Getting this wrong is not a cosmetic problem. Copy written for the first
 * model — "your device", "you fell", "your medical ID" — is actively confusing
 * and slightly upsetting when a daughter is reading it about her father. So it
 * is asked first, before pairing, before permissions, before anything, and the
 * answer drives wording everywhere afterwards.
 *
 * It is not framed as a preference or buried in settings, because a user who
 * skips it lands in the wrong voice for the whole app.
 */
@Composable
fun RoleForkScreen(
    selected: UserRole?,
    onSelect: (UserRole) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.gutter),
        verticalArrangement = Arrangement.Center
    ) {
        StepProgress(step = 1, total = 5)
        Spacer(Modifier.height(Spacing.lg))

        Text(
            text = "Who is this device for?",
            style = MaterialTheme.typography.displaySmall,
            color = colors.ink
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "This changes how the rest of the app talks to you. You can switch later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted
        )

        Spacer(Modifier.height(Spacing.xl))

        RoleCard(
            icon = Icons.Outlined.Shield,
            title = "Someone I look after",
            body = "A parent, a child, or a pet wears it. You get their alerts, their location and their safe zones on this phone.",
            examples = "Elderly parent · Child · Pet",
            selected = selected == UserRole.GUARDIAN,
            onClick = { onSelect(UserRole.GUARDIAN) }
        )

        Spacer(Modifier.height(Spacing.md))

        RoleCard(
            icon = Icons.Outlined.Person,
            title = "Me",
            body = "You wear it and carry this phone. Alerts go to the emergency contacts you choose, and you can reply to them from the device.",
            examples = "Personal safety · Commuting · Cycling",
            selected = selected == UserRole.COMPANION,
            onClick = { onSelect(UserRole.COMPANION) }
        )

        Spacer(Modifier.height(Spacing.xl))

        BoardButton(
            label = "Continue",
            onClick = onContinue,
            enabled = selected != null,
            weight = ButtonWeight.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    body: String,
    examples: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.board
    val shape = RoundedCornerShape(Radius.card)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.plate else colors.ground)
            .border(
                width = if (selected) Stroke.brass else Stroke.hairline,
                color = if (selected) colors.brass else colors.hairline,
                shape = shape
            )
            .clickable(role = SemanticsRole.RadioButton, onClick = onClick)
            .padding(Spacing.lg)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) colors.ink else colors.inkFaint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted
            )
            Spacer(Modifier.height(Spacing.sm))
            Nameplate(examples, small = true, muted = true)
        }
        Spacer(Modifier.width(Spacing.sm))
        // Selection reads as a lit lamp rather than a radio dot — it is the
        // same vocabulary the rest of the app uses for "this one is active".
        PilotLamp(
            state = if (selected) LampState.LIVE else LampState.OFF,
            description = if (selected) "Selected" else "Not selected"
        )
    }
}
