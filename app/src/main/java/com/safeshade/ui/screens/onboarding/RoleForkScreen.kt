package com.safeshade.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.plateClickable
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board

/**
 * The fork: who is this device for.
 *
 * The single most load-bearing question in the app — every screen after it
 * is worded from the answer — so it gets its own scene: two Shadys, one with a
 * cane beside it and one with a phone, and whichever card is chosen comes
 * forward while the other waits. The cards themselves stay plates with a
 * lamp, because that is how this app says "this one".
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
    ) {
        Column {
            OnboardingScene(
                OnboardingSceneKind.FORK,
                focusLeft = when (selected) {
                    UserRole.GUARDIAN -> true
                    UserRole.COMPANION -> false
                    null -> null
                }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
        ) {
            Spacer(Modifier.height(Spacing.lg))
            StepProgress(step = 1, total = ONBOARDING_STEPS)
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "Who is this for?",
                style = MaterialTheme.typography.headlineLarge,
                color = colors.ink
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "This changes how the rest of the app talks to you. It can be switched later on your Profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted
            )

            Spacer(Modifier.height(Spacing.xl))

            RoleCard(
                icon = SafeShadeIcons.NavbarSafety,
                title = "Someone I look after",
                body = "A parent, a child or a pet wears it. Their alerts, their location and their safe zones arrive on this phone.",
                examples = "Elderly parent · Child · Pet",
                selected = selected == UserRole.GUARDIAN,
                onClick = { onSelect(UserRole.GUARDIAN) }
            )

            Spacer(Modifier.height(Spacing.md))

            RoleCard(
                icon = SafeShadeIcons.User,
                title = "Me",
                body = "You wear it and carry this phone. Alerts go to the contacts you choose, and you can reply from the wearable.",
                examples = "Personal safety · Commuting · Cycling",
                selected = selected == UserRole.COMPANION,
                onClick = { onSelect(UserRole.COMPANION) }
            )
            Spacer(Modifier.height(Spacing.lg))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.sm, bottom = Spacing.lg)
        ) {
            BoardButton(
                label = "Continue",
                onClick = onContinue,
                enabled = selected != null,
                weight = ButtonWeight.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
            .plateClickable(role = SemanticsRole.RadioButton, onClick = onClick)
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.plate else colors.ground)
            .border(
                width = if (selected) Stroke.brass else Stroke.hairline,
                color = if (selected) colors.brass else colors.hairline,
                shape = shape
            )
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

@Preview(name = "Role fork", showBackground = true)
@Composable
private fun RolePreview() = SafeShadeTheme {
    RoleForkScreen(selected = UserRole.GUARDIAN, onSelect = {}, onContinue = {})
}

@Preview(name = "Role fork · dark", showBackground = true)
@Composable
private fun RoleDarkPreview() = SafeShadeTheme(darkTheme = true) {
    RoleForkScreen(selected = null, onSelect = {}, onContinue = {})
}
