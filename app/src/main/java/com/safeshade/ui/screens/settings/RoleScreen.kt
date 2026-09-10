package com.safeshade.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Footnote
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.SegmentedChoice
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the role screen draws. */
data class RoleUiState(
    val role: UserRole = UserRole.GUARDIAN,
    /** Used in the sample sentences, so the preview of the change is concrete. */
    val wearerName: String = "",
    /** The role tapped but not yet confirmed. Hoisted so the caller can clear it. */
    val confirming: UserRole? = null
)

/**
 * Who this phone belongs to.
 *
 * The single most load-bearing setting in the app. It does not change a feature
 * — both roles see the same screens and the same device — it changes who every
 * sentence is addressed to. Getting it wrong makes the whole product feel
 * written for somebody else, which is why the change is previewed here with
 * real sentences rather than described in the abstract.
 */
@Composable
fun RoleScreen(
    state: RoleUiState,
    onSelectRole: (UserRole) -> Unit,
    onConfirmRole: (UserRole) -> Unit,
    onCancelConfirm: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            ScreenHeader(
                title = "Your role",
                subtitle = "This decides how the app talks to you. It does not change " +
                    "what the device does.",
                onBack = onBack
            )
        }

        item("heading") { SectionPlate(title = "Who is using this phone") }

        item("options") {
            // A two-option behaviour (2.24): the choice and its consequence
            // are one control, not a bank of rows plus a paragraph.
            SegmentedChoice(
                options = UserRole.entries.map { it.label },
                selected = UserRole.entries.indexOf(state.role),
                onSelect = { onSelectRole(UserRole.entries[it]) },
                consequences = UserRole.entries.map { it.blurb }
            )
        }

        item("preview-heading") { SectionPlate(title = "How it reads") }

        // Two sentences the app actually uses, side by side. Abstract wording
        // like "changes the terminology" leaves the user to guess; this lets
        // them recognise which one sounds like their situation.
        item("preview") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                SamplePlate(
                    role = UserRole.GUARDIAN,
                    active = state.role == UserRole.GUARDIAN,
                    lines = listOf(
                        "${state.wearerName.ifBlank { "Your wearer" }} has not moved in 20 minutes.",
                        "Send a message to the device",
                        "A fall was detected. Calling your emergency contact in 30 seconds."
                    )
                )
                Hairline()
                SamplePlate(
                    role = UserRole.COMPANION,
                    active = state.role == UserRole.COMPANION,
                    lines = listOf(
                        "You have not moved in 20 minutes.",
                        "Reply to your guardian",
                        "A fall was detected. Calling your guardian in 30 seconds."
                    )
                )
            }
        }

        item("note") {
            Footnote("Switching roles keeps everything else: the paired device, the medical card, contacts and history are unchanged.")
        }
    }

    val confirming = state.confirming
    if (confirming != null) {
        RoleChangeDialog(
            from = state.role,
            to = confirming,
            onConfirm = { onConfirmRole(confirming) },
            onCancel = onCancelConfirm
        )
    }
}

/** Sample copy for one role. */
@Composable
private fun SamplePlate(
    role: UserRole,
    active: Boolean,
    lines: List<String>,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    Column(
        modifier = modifier.padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        Nameplate(
            text = if (active) "${role.label} – in use" else role.label,
            small = true,
            muted = !active
        )
        lines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                // The inactive sample is dimmed rather than hidden, so the two
                // can be compared. Faint ink, not a colour — this is emphasis,
                // and emphasis in this system is weight and tone.
                color = if (active) colors.ink else colors.inkFaint
            )
        }
    }
}

@Composable
private fun RoleChangeDialog(
    from: UserRole,
    to: UserRole,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.board
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.plate,
        titleContentColor = colors.ink,
        textContentColor = colors.inkMuted,
        title = {
            Text(
                text = "Switch to ${to.label}",
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "Every screen will stop addressing you as the ${from.label} " +
                        "and start addressing you as the ${to.label}. ${to.blurb}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
                Text(
                    text = "Nothing is deleted, and you can switch back here at any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        },
        confirmButton = {
            BoardButton(
                label = "Switch",
                onClick = onConfirm,
                weight = ButtonWeight.PRIMARY
            )
        },
        dismissButton = {
            BoardButton(
                label = "Cancel",
                onClick = onCancel,
                weight = ButtonWeight.QUIET
            )
        }
    )
}

// ============================================================================
// Previews
// ============================================================================

@Preview(name = "Role · light", showBackground = true)
@Composable
private fun RolePreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            RoleScreen(
                state = RoleUiState(role = UserRole.GUARDIAN, wearerName = "Baba"),
                onSelectRole = {},
                onConfirmRole = {},
                onCancelConfirm = {}
            )
        }
    }
}

@Preview(name = "Role · dark, companion", showBackground = true)
@Composable
private fun RolePreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            RoleScreen(
                state = RoleUiState(role = UserRole.COMPANION),
                onSelectRole = {},
                onConfirmRole = {},
                onCancelConfirm = {}
            )
        }
    }
}

@Preview(name = "Role · confirming", showBackground = true)
@Composable
private fun RolePreviewConfirming() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            RoleScreen(
                state = RoleUiState(
                    role = UserRole.GUARDIAN,
                    wearerName = "Baba",
                    confirming = UserRole.COMPANION
                ),
                onSelectRole = {},
                onConfirmRole = {},
                onCancelConfirm = {}
            )
        }
    }
}
