package com.safeshade.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.PersonaMode
import com.safeshade.data.Reminder
import com.safeshade.data.ReminderKind
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.TimeStrip
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the reminders screen draws. */
data class RemindersUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val mode: PersonaMode = PersonaMode.AUTO,
    /** The daily medication reminder. Null when one has never been set. */
    val medication: Reminder? = null,
    /** The recurring worker check-in. Null when one has never been set. */
    val checkIn: Reminder? = null,
    /**
     * Whether the OS will let alarms fire at the minute.
     *
     * `SCHEDULE_EXACT_ALARM` is a user-revocable special access on API 31+, and
     * the app deliberately does not claim the `USE_EXACT_ALARM` carve-out —
     * that is for alarm-clock and calendar apps, and a wearable companion is
     * neither. So this is genuinely often false, and the drift it causes has to
     * be visible rather than discovered.
     */
    val exactAlarmsAllowed: Boolean = true,
    /** False below API 31, where exact alarms need no grant at all. */
    val exactAlarmsGrantable: Boolean = false,
    val acks: Map<String, AckState> = emptyMap()
)

private fun RemindersUiState.ackFor(key: String): AckState = acks[key] ?: AckState.IDLE

/**
 * Reminders.
 *
 * Two schedules, each belonging to a profile: the daily medication prompt that
 * Elderly mode is built around, and the recurring check-in that Helmet mode
 * uses to notice a lone worker who has stopped answering.
 *
 * Both are shown whatever the active mode is. Hiding a schedule because the
 * device is not currently in the matching profile would mean a guardian who
 * switches modes loses sight of a reminder that is still armed.
 */
@Composable
fun RemindersScreen(
    state: RemindersUiState,
    onMedicationEnabledChange: (Boolean) -> Unit,
    /** Hour 0..23 and minute 0..59. Replaces a callback nobody ever passed. */
    onMedicationTimeChange: (Int, Int) -> Unit,
    onCheckInEnabledChange: (Boolean) -> Unit,
    onCheckInIntervalChange: (Int) -> Unit,
    onGrantExactAlarms: () -> Unit,
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
            ScreenHeader(title = "Reminders", onBack = onBack)
        }

        // The timing caveat comes first, because it changes how every schedule
        // below actually behaves. Putting it at the bottom would mean the user
        // sets a time, trusts it, and finds out afterwards.
        if (!state.exactAlarmsAllowed) {
            item("drift") {
                InexactAlarmNotice(
                    grantable = state.exactAlarmsGrantable,
                    onGrant = onGrantExactAlarms
                )
            }
        }

        item("offline") { OfflineNotice(connection = state.connection) }

        // ---------- Medication ----------
        item("medication-heading") { SectionPlate(title = "Medication") }

        item("medication") {
            // Hoisted out of the plate: the strip below is a sibling of the
            // plate, not a child of it, and both need to know whether the
            // reminder is on.
            val enabled = state.medication?.enabled == true
            val ack = state.ackFor("medicationTime")
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Daily reminder",
                    state = ackLamp(ack, if (enabled) LampState.LIVE else LampState.OFF),
                    stateLabel = if (enabled) "On" else "Off",
                    detail = ackWord(ack)
                        ?: "The wearable buzzes and shows the reminder at this time.",
                    icon = SafeShadeIcons.DailyReminder,
                    checked = enabled,
                    onCheckedChange = onMedicationEnabledChange
                )
            }
            if (enabled) {
                Spacer(Modifier.height(Spacing.sm))
                // The other half of the medication bug. This screen and the
                // device-settings screen both offered a "Change" button for
                // this time, and both were wired to the same empty lambda -
                // so the reminder could be switched on and off but never
                // moved. Both are strips now, and both write the same value.
                //
                // Only while the reminder is on: the enable path writes a null
                // time when it is off, so a live strip would set a time and
                // watch it be nulled a frame later.
                TimeStrip(
                    label = "Every day at",
                    minutesOfDay = state.medication
                        ?.let { it.hour * 60 + it.minute }
                        ?: DEFAULT_REMINDER_MINUTES,
                    onChange = { minutes -> onMedicationTimeChange(minutes / 60, minutes % 60) },
                    advice = { timingNote(state.exactAlarmsAllowed) }
                )
            }
        }

        item("medication-mode-note") {
            ProfileNote(
                text = "Medication reminders are what the Elderly profile is built " +
                    "around. They still work in any mode.",
                relevant = state.mode == PersonaMode.ELDERLY
            )
        }

        // ---------- Check-in ----------
        item("checkin-heading") { SectionPlate(title = "Check-in") }

        item("checkin") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                val enabled = state.checkIn?.enabled == true
                val ack = state.ackFor("checkInInterval")
                Way(
                    name = "Repeating check-in",
                    state = ackLamp(ack, if (enabled) LampState.LIVE else LampState.OFF),
                    stateLabel = if (enabled) "On" else "Off",
                    detail = ackWord(ack)
                        ?: "The device asks the wearer to confirm they are all " +
                        "right. A missed check-in raises a trip.",
                    icon = SafeShadeIcons.RepeatingCheckIn,
                    checked = enabled,
                    onCheckedChange = onCheckInEnabledChange
                )
                Hairline()
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Nameplate("Every", small = true, muted = true)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        INTERVAL_CHOICES.forEach { (minutes, label) ->
                            BoardButton(
                                label = label,
                                onClick = { onCheckInIntervalChange(minutes) },
                                weight = if (state.checkIn?.intervalMinutes == minutes) {
                                    ButtonWeight.PRIMARY
                                } else {
                                    ButtonWeight.SECONDARY
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = timingNote(state.exactAlarmsAllowed),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }
        }

        item("checkin-mode-note") {
            ProfileNote(
                text = "Check-ins are the Helmet profile's lone-worker feature. " +
                    "They still work in any mode.",
                relevant = state.mode == PersonaMode.HELMET
            )
        }
    }
}

/**
 * The drift notice.
 *
 * Names the actual number. "Reminders may be delayed" is the sort of sentence
 * that gets skimmed; "up to 15 minutes late" is a thing a person can decide
 * about, and 15 minutes is what Doze's inexact window costs.
 */
@Composable
private fun InexactAlarmNotice(
    grantable: Boolean,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The lamp is paired with the words beside it, never left to
                // carry the meaning by itself.
                PilotLamp(
                    state = LampState.ATTENTION,
                    description = "Reminders may run late"
                )
                Spacer(Modifier.width(Spacing.sm))
                Nameplate(text = "Reminders may run late", modifier = Modifier.weight(1f))
            }
            Text(
                text = "Android is not letting this app set alarms to the minute, so " +
                    "reminders fire in a window rather than at a time. They can be " +
                    "up to 15 minutes late.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink
            )
            if (grantable) {
                BoardButton(
                    label = "Allow exact alarms",
                    supporting = "Opens the Android setting for this app",
                    onClick = onGrant,
                    weight = ButtonWeight.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "This device does not offer a setting to change it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }
    }
}


/**
 * Which profile a schedule belongs to.
 *
 * Phrased the same way whether or not the profile is active, and never used to
 * disable anything. A guardian who set a check-in in Helmet mode and then moved
 * the device to Backpack should see the check-in still running, not discover
 * later that switching profiles quietly turned it off.
 */
@Composable
private fun ProfileNote(
    text: String,
    relevant: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (relevant) colors.inkMuted else colors.inkFaint,
        modifier = modifier
    )
}

/** Kept identical to `DeviceSettingsScreen`'s list. Both write `EXT CHECKIN`. */
private val INTERVAL_CHOICES = listOf(
    30 to "30m",
    60 to "1h",
    120 to "2h",
    240 to "4h"
)

private fun timingNote(exact: Boolean): String = if (exact) {
    "Fires at this time."
} else {
    "Fires within about 15 minutes of this time."
}

// ============================================================================
// Previews
// ============================================================================

private val previewReminders = RemindersUiState(
    connection = ConnectionState.Ready,
    mode = PersonaMode.ELDERLY,
    medication = Reminder(
        kind = ReminderKind.MEDICATION,
        hour = 8,
        minute = 30,
        enabled = true,
        label = "Morning tablets"
    ),
    checkIn = Reminder(
        kind = ReminderKind.CHECK_IN,
        intervalMinutes = 60,
        enabled = false
    ),
    exactAlarmsAllowed = true,
    exactAlarmsGrantable = true,
    acks = mapOf("medicationTime" to AckState.CONFIRMED)
)

@Composable
private fun RemindersPreviewHost(state: RemindersUiState) {
    Box(Modifier.background(MaterialTheme.board.ground)) {
        RemindersScreen(
            state = state,
            onMedicationEnabledChange = {},
            onMedicationTimeChange = { _, _ -> },
            onCheckInEnabledChange = {},
            onCheckInIntervalChange = {},
            onGrantExactAlarms = {}
        )
    }
}

@Preview(name = "Reminders · light", showBackground = true)
@Composable
private fun RemindersPreviewLight() {
    SafeShadeTheme(darkTheme = false) { RemindersPreviewHost(previewReminders) }
}

@Preview(name = "Reminders · dark, inexact", showBackground = true)
@Composable
private fun RemindersPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        RemindersPreviewHost(previewReminders.copy(exactAlarmsAllowed = false))
    }
}

@Preview(name = "Reminders · nothing set", showBackground = true)
@Composable
private fun RemindersPreviewEmpty() {
    SafeShadeTheme(darkTheme = false) {
        RemindersPreviewHost(
            RemindersUiState(
                connection = ConnectionState.Disconnected,
                mode = PersonaMode.HELMET,
                exactAlarmsAllowed = false,
                exactAlarmsGrantable = true
            )
        )
    }
}

/**
 * Where the strip starts when the reminder has never been given a time.
 *
 * 09:00, matching the default the device-settings screen has always carried.
 * Not midnight, which is what a bare zero would have shown and would have read
 * as a broken control rather than an unset one.
 */
private const val DEFAULT_REMINDER_MINUTES = 9 * 60
