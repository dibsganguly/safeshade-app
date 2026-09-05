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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.FallSensitivity
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceCapabilities
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * The settings this app can genuinely put on the wearable.
 *
 * Field-for-field the [DeviceCapabilities.synced] list, minus the three that
 * have screens of their own (mode, lights, message allowlist). Nothing from
 * [DeviceCapabilities.deviceOnly] appears here; those live on
 * `DeviceOnlyScreen` as read-only rows.
 *
 * Ack state is keyed by the same string keys `DeviceCapabilities` uses, so a
 * setting moving between the synced and device-only lists is a one-line change
 * there and a row moving between two screens here, with no third naming scheme
 * in between.
 */
data class DeviceSettingsUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val deviceName: String = "SafeShade S1",
    val fallSensitivity: FallSensitivity = FallSensitivity.MEDIUM,
    /** 0f..1f. Field 2 of the SETTINGS payload. */
    val sosVolume: Float = 0.8f,
    val autoCallEnabled: Boolean = true,
    val parentalControlsEnabled: Boolean = false,
    val smsFallbackEnabled: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val medicationEnabled: Boolean = false,
    val medicationHour: Int = 9,
    val medicationMinute: Int = 0,
    /** 0 means no check-in schedule. */
    val checkInIntervalMinutes: Int = 0,
    /**
     * Ack per setting, keyed by `DeviceCapabilities` key.
     *
     * A map rather than a field per setting: the set of synced settings is
     * owned by `DeviceCapabilities`, and duplicating it as a fourteen-field
     * data class guarantees the two drift.
     */
    val acks: Map<String, AckState> = emptyMap()
)

private fun DeviceSettingsUiState.ackFor(key: String): AckState =
    acks[key] ?: AckState.IDLE

/**
 * Device settings.
 *
 * Every control here writes over BLE, and a BLE write is not a commit. So each
 * one states what happened to it — sending, confirmed, or no reply — instead of
 * flipping optimistically and leaving the user to find out later that the
 * wearable never heard.
 */
@Composable
fun DeviceSettingsScreen(
    state: DeviceSettingsUiState,
    onFallSensitivityChange: (FallSensitivity) -> Unit,
    onSosVolumeChange: (Float) -> Unit,
    onSosVolumeCommit: () -> Unit,
    onAutoCallChange: (Boolean) -> Unit,
    onParentalControlsChange: (Boolean) -> Unit,
    onSmsFallbackChange: (Boolean) -> Unit,
    onQuietHoursChange: (Boolean) -> Unit,
    onEditQuietHours: () -> Unit,
    onMedicationChange: (Boolean) -> Unit,
    onEditMedicationTime: () -> Unit,
    onCheckInIntervalChange: (Int) -> Unit,
    onEditDeviceName: () -> Unit,
    onOpenDeviceOnly: () -> Unit,
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
            Text(
                text = "Device settings",
                style = MaterialTheme.typography.displaySmall,
                color = colors.ink
            )
        }

        item("offline") {
            OfflineNotice(
                connection = state.connection,
                // The settings payload is stored and re-pushed when the link
                // reaches Ready, so this is a promise the caller keeps.
                queuedNote = "Changes made now are stored and sent as soon as the " +
                    "device connects."
            )
        }

        // ---------- Safety ----------
        item("safety-heading") { SectionPlate(title = "Safety") }

        item("sensitivity") {
            SettingBlock(
                label = "Fall sensitivity",
                ack = state.ackFor("fallSensitivity"),
                supporting = "How hard the accelerometer has to be hit before " +
                    "the device treats it as a fall."
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        FallSensitivity.entries.forEach { level ->
                            BoardButton(
                                label = level.label,
                                onClick = { onFallSensitivityChange(level) },
                                // The selected option is the filled plate.
                                // Weight rather than hue carries the selection,
                                // so this survives greyscale and stays inside
                                // the rule that colour only means circuit state.
                                weight = if (state.fallSensitivity == level) {
                                    ButtonWeight.PRIMARY
                                } else {
                                    ButtonWeight.SECONDARY
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    // The blurb sits under the row rather than inside each
                    // button: three of them at a third of the screen width wrap
                    // to four lines apiece at a raised font scale.
                    Text(
                        text = state.fallSensitivity.blurb,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted
                    )
                }
            }
        }

        item("sos-volume") {
            SettingBlock(
                label = "SOS siren volume",
                ack = state.ackFor("sosVolume"),
                supporting = "Only the emergency siren. The wearable's ordinary " +
                    "chime volume is set on the device itself."
            ) {
                Column {
                    Readout(value = "${(state.sosVolume * 100).toInt()}%")
                    Slider(
                        value = state.sosVolume,
                        onValueChange = onSosVolumeChange,
                        // The write happens on release, not on every pixel of
                        // the drag. A GATT queue fed one write per frame would
                        // spend the whole gesture draining and drop the last
                        // value, which is the only one that matters.
                        onValueChangeFinished = onSosVolumeCommit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Spacing.touchTarget)
                            .semantics {
                                contentDescription = "SOS siren volume, " +
                                    "${(state.sosVolume * 100).toInt()} percent"
                            }
                    )
                }
            }
        }

        item("safety-switches") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                SwitchWay(
                    name = "Auto-call on a fall",
                    description = "Places the call to your first emergency " +
                        "contact when nobody answers the countdown.",
                    icon = Icons.Outlined.Call,
                    checked = state.autoCallEnabled,
                    ack = state.ackFor("autoCall"),
                    onCheckedChange = onAutoCallChange
                )
                Hairline()
                SwitchWay(
                    name = "Parental controls",
                    description = "Asks for the PIN before the wearable's own " +
                        "settings can be changed.",
                    icon = Icons.Outlined.Lock,
                    checked = state.parentalControlsEnabled,
                    ack = state.ackFor("parentalControls"),
                    onCheckedChange = onParentalControlsChange
                )
                Hairline()
                SwitchWay(
                    name = "SMS fallback alert",
                    description = "Sends the alert by text as well, in case the " +
                        "phone is out of Bluetooth range.",
                    icon = Icons.Outlined.Sms,
                    checked = state.smsFallbackEnabled,
                    ack = state.ackFor("smsFallback"),
                    onCheckedChange = onSmsFallbackChange
                )
            }
        }

        // ---------- Schedule ----------
        item("schedule-heading") { SectionPlate(title = "Schedule") }

        item("quiet-hours") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                SwitchWay(
                    name = "Quiet hours",
                    description = "The device stays silent between these times. " +
                        "Emergencies still sound.",
                    icon = Icons.Outlined.Bedtime,
                    checked = state.quietHoursEnabled,
                    ack = state.ackFor("quietHours"),
                    onCheckedChange = onQuietHoursChange
                )
                Hairline()
                ValueRow(
                    label = "Window",
                    value = "${hourLabel(state.quietStartHour)} to ${hourLabel(state.quietEndHour)}",
                    actionLabel = "Change",
                    onAction = onEditQuietHours
                )
            }
        }

        // The switch and the time deliberately share one ack key: they are one
        // field on the wire (`EXT MED`), so a pending write covers both and
        // giving them separate keys would imply an independence the protocol
        // does not have.
        item("medication") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                SwitchWay(
                    name = "Medication reminder",
                    description = "The wearable buzzes and shows the reminder at " +
                        "this time each day.",
                    icon = Icons.Outlined.Alarm,
                    checked = state.medicationEnabled,
                    ack = state.ackFor("medicationTime"),
                    onCheckedChange = onMedicationChange
                )
                Hairline()
                ValueRow(
                    label = "Time",
                    value = timeLabel(state.medicationHour, state.medicationMinute),
                    actionLabel = "Change",
                    onAction = onEditMedicationTime
                )
            }
        }

        // The same setting is also editable on the reminders screen. Both write
        // `EXT CHECKIN` and both offer exactly these intervals — two screens
        // offering the same control with different options is how a user ends
        // up believing one of them is broken.
        item("check-in") {
            val ack = state.ackFor("checkInInterval")
            val enabled = state.checkInIntervalMinutes > 0
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                SwitchWay(
                    name = "Repeating check-in",
                    description = "The device asks the wearer to confirm they are " +
                        "all right. Missing one raises a trip.",
                    icon = Icons.Outlined.Timer,
                    checked = enabled,
                    ack = ack,
                    // Zero minutes *is* off on the wire, so the switch and the
                    // interval are one value and cannot disagree.
                    onCheckedChange = { on ->
                        onCheckInIntervalChange(if (on) DEFAULT_CHECK_IN_MINUTES else 0)
                    }
                )
                Hairline()
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Nameplate("Every", small = true, muted = true)
                    Spacer(Modifier.height(Spacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        CHECK_IN_CHOICES.forEach { (minutes, label) ->
                            BoardButton(
                                label = label,
                                onClick = { onCheckInIntervalChange(minutes) },
                                weight = if (state.checkInIntervalMinutes == minutes) {
                                    ButtonWeight.PRIMARY
                                } else {
                                    ButtonWeight.SECONDARY
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // ---------- Identity ----------
        item("identity-heading") { SectionPlate(title = "Identity") }

        item("device-name") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                ValueRow(
                    label = "Device name",
                    value = state.deviceName,
                    supporting = ackWord(state.ackFor("deviceName"))
                        ?: "Shown on the wearable's own screen and when pairing.",
                    ack = state.ackFor("deviceName"),
                    actionLabel = "Rename",
                    onAction = onEditDeviceName
                )
            }
        }

        item("device-only-link") {
            BoardButton(
                label = "Settings only on the device",
                supporting = "Eight more settings this app cannot reach over Bluetooth",
                onClick = onOpenDeviceOnly,
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// Blocks
//
// These are layouts inside the existing plate, not new card types. A setting
// that is a choice of three or a slider cannot live in a `Way` — `Way` renders
// either a switch or a state word, and neither is a slider — so it gets a
// nameplate, an acknowledgement line, and its own control beneath.
// ============================================================================

/** A setting whose control is not a switch: a choice, a slider, a value. */
@Composable
private fun SettingBlock(
    label: String,
    ack: AckState,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    content: @Composable () -> Unit
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            val acknowledgement = ackWord(ack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Nameplate(label, modifier = Modifier.weight(1f))
                // Only shown when there is something to acknowledge. An unlit
                // lamp beside every settled setting is noise on a screen where
                // a lamp is supposed to mean something, and it is always paired
                // with the word below it rather than left to carry it alone.
                if (acknowledgement != null) {
                    PilotLamp(
                        state = ackLamp(ack, LampState.OFF),
                        description = acknowledgement
                    )
                }
            }
            if (acknowledgement != null) {
                Text(
                    text = acknowledgement,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (ack) {
                        AckState.PENDING -> colors.inkAttention
                        AckState.CONFIRMED -> colors.inkLive
                        else -> colors.inkFaint
                    }
                )
            }
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
            Spacer(Modifier.height(Spacing.md))
            content()
        }
    }
}

/**
 * A switch row with its acknowledgement in the detail line.
 *
 * The detail line is the only place the ack can live on a switching row:
 * `Way` draws a switch *or* a state word and lamp, never both, so a row with a
 * switch has nothing but the bus tick's colour left — and colour alone is
 * exactly what this system forbids.
 */
@Composable
private fun SwitchWay(
    name: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    ack: AckState,
    onCheckedChange: (Boolean) -> Unit
) {
    Way(
        name = name,
        state = ackLamp(ack, if (checked) LampState.LIVE else LampState.OFF),
        stateLabel = if (checked) "On" else "Off",
        detail = ackWord(ack) ?: description,
        icon = icon,
        checked = checked,
        onCheckedChange = onCheckedChange
    )
}

/** A read-and-edit row: a value and the button that changes it. */
@Composable
private fun ValueRow(
    label: String,
    value: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    ack: AckState = AckState.IDLE
) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touchTarget)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Nameplate(label, small = true, muted = true)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }
        if (ack != AckState.IDLE) {
            PilotLamp(
                state = ackLamp(ack, LampState.OFF),
                description = ackWord(ack)
            )
            Spacer(Modifier.width(Spacing.sm))
        }
        BoardButton(
            label = actionLabel,
            onClick = onAction,
            weight = ButtonWeight.SECONDARY
        )
    }
}

// ============================================================================
// Formatting
//
// Wall-clock values, so formatting them in the composable is deterministic —
// unlike anything relative to "now", which the caller preformats.
// ============================================================================

/** Kept identical to `RemindersScreen`'s list. Both write `EXT CHECKIN`. */
private val CHECK_IN_CHOICES = listOf(
    30 to "30m",
    60 to "1h",
    120 to "2h",
    240 to "4h"
)

/** What the switch turns on to, when no interval has been chosen yet. */
private const val DEFAULT_CHECK_IN_MINUTES = 60

private fun hourLabel(hour: Int): String = "%02d:00".format(hour.coerceIn(0, 23))

private fun timeLabel(hour: Int, minute: Int): String =
    "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))

// ============================================================================
// Previews
// ============================================================================

private val previewSettings = DeviceSettingsUiState(
    connection = ConnectionState.Ready,
    deviceName = "Baba's cane",
    fallSensitivity = FallSensitivity.HIGH,
    sosVolume = 0.9f,
    autoCallEnabled = true,
    parentalControlsEnabled = true,
    smsFallbackEnabled = false,
    quietHoursEnabled = true,
    quietStartHour = 22,
    quietEndHour = 7,
    medicationEnabled = true,
    medicationHour = 8,
    medicationMinute = 30,
    checkInIntervalMinutes = 60,
    acks = mapOf(
        "fallSensitivity" to AckState.CONFIRMED,
        "sosVolume" to AckState.PENDING,
        "autoCall" to AckState.CONFIRMED,
        "smsFallback" to AckState.NO_RESPONSE,
        "deviceName" to AckState.CONFIRMED
    )
)

@Composable
private fun PreviewHost(state: DeviceSettingsUiState) {
    Box(Modifier.background(MaterialTheme.board.ground)) {
        DeviceSettingsScreen(
            state = state,
            onFallSensitivityChange = {},
            onSosVolumeChange = {},
            onSosVolumeCommit = {},
            onAutoCallChange = {},
            onParentalControlsChange = {},
            onSmsFallbackChange = {},
            onQuietHoursChange = {},
            onEditQuietHours = {},
            onMedicationChange = {},
            onEditMedicationTime = {},
            onCheckInIntervalChange = {},
            onEditDeviceName = {},
            onOpenDeviceOnly = {}
        )
    }
}

@Preview(name = "Device settings · light", showBackground = true)
@Composable
private fun DeviceSettingsPreviewLight() {
    SafeShadeTheme(darkTheme = false) { PreviewHost(previewSettings) }
}

@Preview(name = "Device settings · dark", showBackground = true)
@Composable
private fun DeviceSettingsPreviewDark() {
    SafeShadeTheme(darkTheme = true) { PreviewHost(previewSettings) }
}

@Preview(name = "Device settings · offline", showBackground = true)
@Composable
private fun DeviceSettingsPreviewOffline() {
    SafeShadeTheme(darkTheme = false) {
        PreviewHost(previewSettings.copy(connection = ConnectionState.Disconnected))
    }
}

@Preview(name = "Device settings · large text", showBackground = true, fontScale = 1.3f)
@Composable
private fun DeviceSettingsPreviewLargeText() {
    SafeShadeTheme(darkTheme = false) { PreviewHost(previewSettings) }
}
