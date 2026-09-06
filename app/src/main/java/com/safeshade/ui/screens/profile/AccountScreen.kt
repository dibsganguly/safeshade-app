package com.safeshade.ui.screens.profile

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.cloud.CloudResult
import com.safeshade.cloud.CloudSession
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.vm.SyncSummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The account page: who is signed in, whether the cloud copy is current, and
 * the two things a person can do to the account itself.
 *
 * The sync plate reports facts off the outbox rather than a mood: how many
 * rows are still on this phone only, when the last one landed, and the last
 * error in the client's own words. "Synced" with a time is a claim the
 * outbox can back; a green tick with no time would not be.
 *
 * Deleting the account is the one destructive control in the app that is
 * not a call, so it is a red button behind a dialog that says what goes.
 */
@Composable
fun AccountScreen(
    session: CloudSession,
    sync: SyncSummary,
    onSyncNow: () -> Unit,
    onSignOut: suspend () -> CloudResult<Unit>,
    onDeleteAccount: suspend () -> CloudResult<Unit>,
    onOpenSignIn: () -> Unit,
    onOpenPlan: () -> Unit = {},
    planLabel: String = "Free",
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }

    fun act(block: suspend () -> CloudResult<Unit>) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            when (val r = block()) {
                is CloudResult.Ok -> Unit
                is CloudResult.Failed -> error = r.reason
                CloudResult.Disabled -> error = "This build has no SafeShade Cloud project"
            }
            busy = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            ScreenHeader(title = "Account", subtitle = "SafeShade Cloud", onBack = onBack)
        }

        item("session") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                when (session) {
                    is CloudSession.SignedIn -> Way(
                        name = "Signed in",
                        state = LampState.LIVE,
                        stateLabel = "Live",
                        detail = session.email ?: session.userId,
                        icon = SafeShadeIcons.NavbarCircle
                    )
                    CloudSession.Guest -> Way(
                        name = "Not signed in",
                        state = LampState.OFF,
                        stateLabel = "Off",
                        detail = "Saved on this phone only",
                        icon = SafeShadeIcons.NavbarCircle,
                        onClick = onOpenSignIn
                    )
                    CloudSession.Loading -> Way(
                        name = "Checking",
                        state = LampState.UNKNOWN,
                        stateLabel = "…",
                        detail = "Reading the saved session",
                        icon = SafeShadeIcons.NavbarCircle
                    )
                    CloudSession.Disabled -> Way(
                        name = "Not available",
                        state = LampState.OFF,
                        stateLabel = "Off",
                        detail = "This build has no SafeShade Cloud project",
                        icon = SafeShadeIcons.NavbarCircle
                    )
                }
            }
        }

        if (session is CloudSession.SignedIn) {
            item("sync-heading") { SectionPlate(title = "Sync") }
            item("sync") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    val (state, label, detail) = syncWay(sync)
                    Way(
                        name = "Cloud copy",
                        state = state,
                        stateLabel = label,
                        detail = detail,
                        icon = SafeShadeIcons.Info
                    )
                    if (sync.lastError != null) {
                        Hairline()
                        Way(
                            name = "Last error",
                            state = LampState.ATTENTION,
                            stateLabel = "Reported",
                            detail = sync.lastError,
                            icon = SafeShadeIcons.Alert02
                        )
                    }
                    Hairline()
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        BoardButton(
                            label = "Sync Now",
                            onClick = onSyncNow,
                            weight = ButtonWeight.SECONDARY,
                            enabled = !sync.syncing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item("plan-heading") { SectionPlate(title = "Plan") }
            item("plan") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Way(
                        name = "Your plan",
                        state = LampState.LIVE,
                        stateLabel = planLabel,
                        detail = "What the cloud adds around the board, and what each plan costs",
                        icon = SafeShadeIcons.Info,
                        onClick = onOpenPlan
                    )
                }
            }

            item("privacy-heading") { SectionPlate(title = "What leaves this phone") }
            item("privacy") {
                // Facts, not reassurance: the exact records the cloud copy
                // holds and the exact ones it never sees. A person deciding
                // whether to sign in is entitled to the list, and a list is
                // checkable against the code in a way "we respect your
                // privacy" is not.
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Way(
                        name = "Copied to SafeShade Cloud",
                        state = LampState.LIVE,
                        stateLabel = "Synced",
                        detail = "Wearers, medical IDs, emergency contacts, paired wearables, alerts, messages, safe zones, your name",
                        icon = SafeShadeIcons.NavbarCircle
                    )
                    Hairline()
                    Way(
                        name = "Never leaves this phone",
                        state = LampState.OFF,
                        stateLabel = "Local",
                        detail = "The parental PIN, the SMS allowlist, the wearable's SIM number, and photos used as faces",
                        icon = SafeShadeIcons.Info
                    )
                    Hairline()
                    Way(
                        name = "Who can read it",
                        state = LampState.LIVE,
                        stateLabel = "Circle",
                        detail = "Only members of your Circle. Every table is row-locked to the Circle on the server; a viewer can read and not write.",
                        icon = SafeShadeIcons.SafeZone
                    )
                    Hairline()
                    Way(
                        name = "Where it lives",
                        state = LampState.LIVE,
                        stateLabel = "Singapore",
                        detail = "Supabase, region ap-southeast-1. Deleting the account removes every row.",
                        icon = SafeShadeIcons.Info
                    )
                }
            }

            item("account-heading") { SectionPlate(title = "This account") }
            item("account-actions") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        BoardButton(
                            label = if (busy) "Working…" else "Sign Out",
                            supporting = "Everything stays on this phone",
                            onClick = { act(onSignOut) },
                            weight = ButtonWeight.SECONDARY,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        BoardButton(
                            label = "Delete Account",
                            supporting = "Removes the cloud copy for good",
                            onClick = { confirmingDelete = true },
                            weight = ButtonWeight.DANGER,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val shown = error
                        if (shown != null) {
                            Text(
                                text = shown,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inkTrip
                            )
                        }
                    }
                }
            }
        } else if (session is CloudSession.Guest) {
            item("sign-in") {
                BoardButton(
                    label = "Sign In",
                    onClick = onOpenSignIn,
                    weight = ButtonWeight.COMMIT,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (confirmingDelete) {
        DeleteAccountDialog(
            onConfirm = { confirmingDelete = false; act(onDeleteAccount) },
            onCancel = { confirmingDelete = false }
        )
    }
}

/** The sync way's lamp, word and line, from the summary's facts. */
private fun syncWay(s: SyncSummary): Triple<LampState, String, String> = when {
    s.syncing -> Triple(LampState.ATTENTION, "Syncing", pendingLine(s.pending))
    s.lastError != null && s.pending > 0 -> Triple(LampState.ATTENTION, "Waiting", pendingLine(s.pending))
    s.pending > 0 -> Triple(LampState.ATTENTION, "Pending", pendingLine(s.pending))
    s.lastSyncedAt != null -> Triple(LampState.LIVE, "Current", "Last change reached the cloud at ${timeOf(s.lastSyncedAt)}")
    else -> Triple(LampState.OFF, "Nothing yet", "No change has needed sending from this phone")
}

private fun pendingLine(n: Int) =
    if (n == 1) "1 change is still on this phone only" else "$n changes are still on this phone only"

private fun timeOf(millis: Long): String =
    SimpleDateFormat("HH:mm, d MMM", Locale.getDefault()).format(Date(millis))

@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val colors = MaterialTheme.board
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.plate,
        titleContentColor = colors.ink,
        textContentColor = colors.inkMuted,
        title = {
            Text(text = "Delete this account", style = MaterialTheme.typography.titleMedium, color = colors.ink)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "The cloud copy of every wearer, alert, zone and message on this account is " +
                        "removed, and the other phones in the Circle lose it too.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
                Text(
                    text = "What is saved on this phone stays on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        },
        confirmButton = {
            BoardButton(label = "Delete", onClick = onConfirm, weight = ButtonWeight.DANGER)
        },
        dismissButton = {
            BoardButton(label = "Keep", onClick = onCancel, weight = ButtonWeight.QUIET)
        }
    )
}

@Preview(name = "Account · signed in", showBackground = true)
@Composable
private fun AccountPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            AccountScreen(
                session = CloudSession.SignedIn("u1", "dibs@example.com"),
                sync = SyncSummary(pending = 2, lastSyncedAt = System.currentTimeMillis(), lastError = "No network"),
                onSyncNow = {}, onSignOut = { CloudResult.Ok(Unit) }, onDeleteAccount = { CloudResult.Ok(Unit) },
                onOpenSignIn = {}, onBack = {}
            )
        }
    }
}
