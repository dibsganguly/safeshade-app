package com.safeshade.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.cloud.CloudResult
import com.safeshade.cloud.EmailPreferences
import com.safeshade.cloud.WeeklyReportSend
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
import kotlinx.coroutines.launch

/** Everything the emails page draws. */
data class EmailsUiState(
    val signedIn: Boolean = false,
    /** The account's address, for the line that says where mail goes. */
    val email: String? = null,
    /** Null until the server has answered; the rows show dashes until then. */
    val prefs: EmailPreferences? = null,
    /** Set while a change is on its way to the server. */
    val saving: Boolean = false
)

/**
 * Emails: which of SafeShade's messages this account receives.
 *
 * Every rocker writes to the server and only flips once the server has
 * accepted it, because the server is what honours the choice; a switch that
 * flipped locally and failed to land would be a promise the mail would
 * break. The sign-in code and password mails are not on the page: they are
 * how signing in works, not a notification, and there is nothing to turn off.
 *
 * The weekly report can be sent now, and the row under the button names who
 * it went to, who had it switched off, and who could not be reached, from
 * the server's answer and nothing else.
 */
@Composable
fun EmailsScreen(
    state: EmailsUiState,
    onChange: suspend (EmailPreferences) -> CloudResult<Unit>,
    onSendWeeklyNow: suspend () -> CloudResult<WeeklyReportSend>,
    onOpenSignIn: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var sendResult by remember { mutableStateOf<String?>(null) }
    val prefs = state.prefs

    fun change(next: EmailPreferences) {
        if (busy || prefs == null) return
        busy = true
        error = null
        scope.launch {
            when (val r = onChange(next)) {
                is CloudResult.Ok -> Unit
                is CloudResult.Failed -> error = r.reason
                CloudResult.Disabled -> error = "This build has no SafeShade Cloud project"
            }
            busy = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .padding(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        ScreenHeader(
            title = "Emails",
            subtitle = if (state.email != null) "What SafeShade sends to ${state.email}" else "What SafeShade sends to this account",
            onBack = onBack
        )

        if (!state.signedIn) {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Not signed in",
                    state = LampState.OFF,
                    stateLabel = "Off",
                    detail = "No emails are sent to a phone that is not signed in",
                    icon = SafeShadeIcons.NavbarCircle,
                    onClick = onOpenSignIn
                )
            }
            return@Column
        }

        SectionPlate(title = "Sent when something happens")
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            PrefWay(
                name = "Alerts",
                detail = "A fall or an SOS that stays unanswered. Sent to every guardian in the Circle.",
                on = prefs?.alerts,
                enabled = !busy,
                onToggle = { change(prefs!!.copy(alerts = it)) }
            )
            Hairline()
            PrefWay(
                name = "Circle changes",
                detail = "Somebody joins the Circle, an invitation is accepted or runs out.",
                on = prefs?.circle,
                enabled = !busy,
                onToggle = { change(prefs!!.copy(circle = it)) }
            )
            Hairline()
            PrefWay(
                name = "Account notices",
                detail = "The account is deleted, or something changes that you should know about.",
                on = prefs?.account,
                enabled = !busy,
                onToggle = { change(prefs!!.copy(account = it)) }
            )
        }

        SectionPlate(title = "Every week")
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            PrefWay(
                name = "Weekly report",
                detail = "Monday morning: the week's alerts, messages and coverage, for the whole Circle.",
                on = prefs?.weeklyReport,
                enabled = !busy,
                onToggle = { change(prefs!!.copy(weeklyReport = it)) }
            )
            Hairline()
            Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BoardButton(
                    label = if (sending) "Sending…" else "Send This Week's Report Now",
                    icon = SafeShadeIcons.FileExport,
                    supporting = "To everyone in the Circle who has it switched on",
                    onClick = {
                        if (sending) return@BoardButton
                        sending = true
                        sendResult = null
                        scope.launch {
                            sendResult = when (val r = onSendWeeklyNow()) {
                                is CloudResult.Ok -> describe(r.value)
                                is CloudResult.Failed -> r.reason
                                CloudResult.Disabled -> "This build has no SafeShade Cloud project"
                            }
                            sending = false
                        }
                    },
                    weight = ButtonWeight.SECONDARY,
                    enabled = prefs != null && !sending,
                    modifier = Modifier.fillMaxWidth()
                )
                val shown = sendResult
                if (shown != null) {
                    Text(text = shown, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                }
            }
        }

        val e = error
        if (e != null) {
            Text(text = e, style = MaterialTheme.typography.bodyMedium, color = colors.inkTrip)
        }

        Text(
            text = "Sign-in codes and password emails are not listed: they are how signing in works, and there is nothing to switch off. Every email says why it was sent.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkMuted
        )
    }
}

@Composable
private fun PrefWay(
    name: String,
    detail: String,
    on: Boolean?,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Way(
        name = name,
        state = when (on) {
            null -> LampState.UNKNOWN
            true -> LampState.LIVE
            false -> LampState.OFF
        },
        stateLabel = when (on) {
            null -> "—"
            true -> "On"
            false -> "Off"
        },
        detail = detail,
        icon = SafeShadeIcons.MessageInbox,
        checked = on == true,
        onCheckedChange = if (on != null && enabled) onToggle else null,
        onClick = if (on != null && enabled) { { onToggle(!on) } } else null
    )
}

/** The server's answer, as a sentence that names people. */
private fun describe(r: WeeklyReportSend): String = buildString {
    if (r.sent.isNotEmpty()) append("Sent to ${r.sent.joinToString(", ")}. ")
    if (r.skipped.isNotEmpty()) append("${r.skipped.joinToString(", ")} ${if (r.skipped.size == 1) "has" else "have"} it switched off. ")
    r.failed.forEach { (who, why) -> append("$who: $why. ") }
    if (isEmpty()) append("Nobody in the Circle has an address to send to.")
}.trim()

@Preview(name = "Emails", showBackground = true, heightDp = 1200)
@Composable
private fun EmailsPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            EmailsScreen(
                state = EmailsUiState(signedIn = true, email = "you@example.com", prefs = EmailPreferences(weeklyReport = false)),
                onChange = { CloudResult.Ok(Unit) },
                onSendWeeklyNow = { CloudResult.Ok(WeeklyReportSend(listOf("you@example.com"), emptyList(), emptyMap())) },
                onOpenSignIn = {}, onBack = {}
            )
        }
    }
}
