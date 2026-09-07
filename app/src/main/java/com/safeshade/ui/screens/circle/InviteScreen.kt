package com.safeshade.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.cloud.CircleInvite
import com.safeshade.cloud.CircleMember
import com.safeshade.cloud.CircleRole
import com.safeshade.cloud.CloudResult
import com.safeshade.cloud.InviteStatus
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.screens.safety.PlateField
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Everything the guardians page draws. */
data class GuardiansUiState(
    val signedIn: Boolean = false,
    val hasCircle: Boolean = false,
    val members: List<CircleMember> = emptyList(),
    val invites: List<CircleInvite> = emptyList(),
    /** The account's own user id, so the list can say "you". */
    val selfUserId: String? = null
)

/**
 * The guardians in the Circle, and inviting another.
 *
 * Members are what the server says they are. An invitation is a row with
 * its own state word: Sent, Accepted, Expired, or Failed with Resend's
 * reason - and Failed is the ordinary case for any address but the account
 * owner's until SafeShade has a sending domain, so it is drawn as a plain
 * state, not an error the person did something to cause. The send button
 * holds until the function answers; the row appears from the server's
 * response, never from the tap.
 */
@Composable
fun GuardiansScreen(
    state: GuardiansUiState,
    onInvite: suspend (email: String, role: CircleRole) -> CloudResult<CircleInvite>,
    onOpenSignIn: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(CircleRole.GUARDIAN) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastSent by remember { mutableStateOf<CircleInvite?>(null) }
    val emailOk = email.contains('@') && email.substringAfter('@').contains('.')

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        ScreenHeader(
            title = "Guardians",
            subtitle = "Everyone who sees this Circle's board",
            onBack = onBack
        )

        if (!state.signedIn) {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Only this phone",
                    state = LampState.OFF,
                    stateLabel = "Off",
                    detail = "Sign in to share the Circle with another phone",
                    icon = SafeShadeIcons.LogSignIn,
                    onClick = onOpenSignIn
                )
            }
            return@Column
        }

        SectionPlate(title = "In the Circle")
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            if (state.members.isEmpty()) {
                Text(
                    text = if (state.hasCircle) "Reading the Circle…" else "This account has no Circle yet. Sync once and it appears.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(Spacing.lg)
                )
            }
            state.members.forEachIndexed { index, member ->
                if (index > 0) Hairline()
                val self = member.userId == state.selfUserId
                Way(
                    name = when {
                        self -> "You"
                        !member.name.isNullOrBlank() -> member.name!!
                        !member.email.isNullOrBlank() -> member.email!!
                        else -> "A guardian"
                    },
                    state = LampState.LIVE,
                    stateLabel = member.role.label,
                    detail = member.email?.takeIf { !self && !member.name.isNullOrBlank() } ?: member.role.blurb,
                    icon = SafeShadeIcons.User
                )
            }
        }

        if (state.invites.isNotEmpty()) {
            SectionPlate(title = "Invited")
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                state.invites.sortedByDescending { it.sentAt }.forEachIndexed { index, invite ->
                    if (index > 0) Hairline()
                    val (lamp, word, line) = inviteWay(invite)
                    Way(
                        name = invite.email,
                        state = lamp,
                        stateLabel = word,
                        detail = line,
                        icon = SafeShadeIcons.MailSendInvite
                    )
                }
            }
        }

        SectionPlate(title = "Invite by email")
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                PlateField(
                    label = "Their email",
                    value = email,
                    onValueChange = { email = it.trim() },
                    placeholder = "priya@example.com",
                    maxLength = 120,
                    enabled = !busy,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                )
                Column {
                    listOf(CircleRole.GUARDIAN, CircleRole.VIEWER).forEachIndexed { i, option ->
                        if (i > 0) Hairline()
                        Way(
                            name = option.label,
                            state = if (role == option) LampState.LIVE else LampState.OFF,
                            stateLabel = if (role == option) "In use" else "Not chosen",
                            detail = option.blurb,
                            icon = SafeShadeIcons.UserGroup,
                            onClick = { role = option }
                        )
                    }
                }
                BoardButton(
                    label = if (busy) "Sending…" else "Send the Invitation",
                    supporting = "An email with a link that joins this Circle. Good for seven days.",
                    onClick = {
                        if (busy) return@BoardButton
                        busy = true
                        error = null
                        scope.launch {
                            when (val r = onInvite(email, role)) {
                                is CloudResult.Ok -> { lastSent = r.value; email = "" }
                                is CloudResult.Failed -> error = r.reason
                                CloudResult.Disabled -> error = "This build has no SafeShade Cloud project"
                            }
                            busy = false
                        }
                    },
                    weight = ButtonWeight.COMMIT,
                    enabled = emailOk && !busy && state.hasCircle,
                    modifier = Modifier.fillMaxWidth()
                )
                val sent = lastSent
                if (sent != null && state.invites.none { it.id == sent.id }) {
                    val (_, word, line) = inviteWay(sent)
                    Text(
                        text = "${sent.email}: $word. $line",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (sent.status is InviteStatus.Failed) colors.inkAttention else colors.inkMuted
                    )
                }
                val shown = error
                if (shown != null) {
                    Text(text = shown, style = MaterialTheme.typography.bodyMedium, color = colors.inkTrip)
                }
            }
        }
    }
}

/** The lamp, the word and the line for one invitation. */
private fun inviteWay(invite: CircleInvite): Triple<LampState, String, String> {
    val sent = if (invite.sentAt > 0L) "Sent ${SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(invite.sentAt))}" else "Sent"
    return when (val s = invite.status) {
        InviteStatus.Pending -> Triple(LampState.ATTENTION, "Sent", "$sent · ${invite.role.label} · not yet accepted")
        InviteStatus.Accepted -> Triple(LampState.LIVE, "Accepted", "${invite.role.label} · in the Circle")
        InviteStatus.Expired -> Triple(LampState.OFF, "Expired", "$sent · the link is past its seven days")
        is InviteStatus.Failed -> Triple(LampState.TRIP, "Not sent", s.reason)
    }
}

internal val CircleRole.label: String
    get() = when (this) {
        CircleRole.OWNER -> "Owner"
        CircleRole.GUARDIAN -> "Guardian"
        CircleRole.VIEWER -> "Viewer"
    }

internal val CircleRole.blurb: String
    get() = when (this) {
        CircleRole.OWNER -> "Runs the Circle and can remove anyone"
        CircleRole.GUARDIAN -> "Sees everything and can change settings, zones and contacts"
        CircleRole.VIEWER -> "Sees the board and the alerts, changes nothing"
    }

@Preview(name = "Guardians", showBackground = true)
@Composable
private fun GuardiansPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            GuardiansScreen(
                state = GuardiansUiState(
                    signedIn = true, hasCircle = true, selfUserId = "u1",
                    members = listOf(CircleMember("u1", "Dibyendu", "d@x.com", CircleRole.OWNER)),
                    invites = listOf(CircleInvite("i1", "priya@example.com", CircleRole.GUARDIAN, InviteStatus.Failed("You can only send testing emails to your own email address"), System.currentTimeMillis()))
                ),
                onInvite = { _, _ -> CloudResult.Ok(CircleInvite("i2", "a@b.co", CircleRole.GUARDIAN, InviteStatus.Pending)) },
                onOpenSignIn = {}, onBack = {}
            )
        }
    }
}
