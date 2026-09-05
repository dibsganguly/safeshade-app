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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.FallSensitivity
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceCapabilities
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.DialControl
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.ExpandableSection
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.RangeStrip
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.TimeStrip
import com.safeshade.ui.board.formatClock
import com.safeshade.ui.board.Way
// OptionWay lives in the safety package because that is where it was first
// needed. It is `internal`, so this is legal, and importing it is the point:
// this screen and FallSettingsScreen now use one control for one setting
// rather than two. It belongs in ui/board with the rest of the row kit; that
// move is worth doing and is not worth doing in the middle of this pass.
import com.safeshade.ui.screens.safety.OptionWay
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlin.math.roundToInt

/**
 * The settings this app can genuinely put on the wearable.
 *
 * Field-for-field the [DeviceCapabilities.synced] list, minus the three that
 * have screens of their own (mode, lights, message allowlist). Nothing from
 * [DeviceCapabilities.deviceOnly] appears here; those live on
 * a closed drawer at the foot of this screen as read-only rows.
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
    /** Start and end hour, 0..23. Replaces the picker that never existed. */
    onQuietWindowChange: (Int, Int) -> Unit,
    onMedicationChange: (Boolean) -> Unit,
    /** Hour 0..23 and minute 0..59. */
    onMedicationTimeChange: (Int, Int) -> Unit,
    onCheckInIntervalChange: (Int) -> Unit,
    onEditDeviceName: () -> Unit,
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
            ScreenHeader(title = "Device settings", onBack = onBack)
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
                // OptionWay rows, the same control the fall-settings screen
                // uses for this same value.
                //
                // There were two. This screen had a row of three plates with
                // the selected one filled, and FallSettingsScreen had these
                // rows - one setting, two widgets, and a user who changes it in
                // one place and then finds something that looks like a
                // different control in the other. Rows win because each option
                // states its own consequence: the plate row could only show the
                // blurb for whichever option was already chosen, which is the
                // one the user least needs explained.
                //
                // Deliberately NOT a slider. Three named settings on a track
                // reads as a continuum with two invisible stops, and this is a
                // choice between three named behaviours.
                Column {
                    FallSensitivity.entries.forEachIndexed { index, level ->
                        if (index > 0) Hairline()
                        OptionWay(
                            name = level.label,
                            detail = level.blurb,
                            selected = state.fallSensitivity == level,
                            onSelect = { onFallSensitivityChange(level) }
                        )
                    }
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
                // Stepped to ten percent and given advice that changes as it
                // moves. A bare slider let the user place the siren at 47%,
                // which is neither reproducible nor audibly different from 50%,
                // and said nothing about what any of it meant.
                //
                // `onCommit` still carries the write, for the original reason:
                // Android allows one outstanding GATT operation at a time, so a
                // write per frame spends the gesture draining a queue and drops
                // the last value.
                DialControl(
                    label = "Volume",
                    value = state.sosVolume,
                    valueRange = 0f..1f,
                    step = 0.1f,
                    onValueChange = onSosVolumeChange,
                    onCommit = onSosVolumeCommit,
                    format = { "${(it * 100).roundToInt()}%" },
                    spokenValue = { "${(it * 100).roundToInt()} percent" },
                    advice = ::sirenAdvice
                )
            }
        }

        item("safety-switches") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                SwitchWay(
                    name = "Auto-call on a fall",
                    description = "Places the call to your first emergency " +
                        "contact when nobody answers the countdown.",
                    icon = SafeShadeIcons.CallAfterAFall,
                    checked = state.autoCallEnabled,
                    ack = state.ackFor("autoCall"),
                    onCheckedChange = onAutoCallChange
                )
                Hairline()
                SwitchWay(
                    name = "Parental controls",
                    description = "Asks for the PIN before the wearable's own " +
                        "settings can be changed.",
                    icon = SafeShadeIcons.ParentalControl,
                    checked = state.parentalControlsEnabled,
                    ack = state.ackFor("parentalControls"),
                    onCheckedChange = onParentalControlsChange
                )
                Hairline()
                SwitchWay(
                    name = "SMS fallback alert",
                    description = "Sends the alert by text as well, in case the " +
                        "phone is out of Bluetooth range.",
                    icon = SafeShadeIcons.TextAsWell,
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
                    icon = SafeShadeIcons.QuietHours,
                    checked = state.quietHoursEnabled,
                    ack = state.ackFor("quietHours"),
                    onCheckedChange = onQuietHoursChange
                )
            }
            if (state.quietHoursEnabled) {
                Spacer(Modifier.height(Spacing.sm))
                // Shown only while quiet hours are on, which is what closes the
                // hole the old "Change" button had: that button called an empty
                // lambda, and a strip that wrote a window nobody was observing
                // would have looked just as broken - set it, watch nothing
                // happen. Off means there is no window to set.
                RangeStrip(
                    label = "Quiet from",
                    startMinutes = state.quietStartHour * 60,
                    endMinutes = state.quietEndHour * 60,
                    onChange = { start, end -> onQuietWindowChange(start / 60, end / 60) },
                    // Whole hours: the wire carries two hour fields, and
                    // offering minutes the protocol cannot hold would be a
                    // control that silently rounds what it was told.
                    snapMinutes = 60,
                    advice = { start, end ->
                        if (start == end) {
                            "Start and end are the same, so nothing is silenced."
                        } else {
                            "Silent from " + formatClock(start) + " to " + formatClock(end) +
                                ". An emergency still sounds."
                        }
                    }
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
                    icon = SafeShadeIcons.DailyReminder,
                    checked = state.medicationEnabled,
                    ack = state.ackFor("medicationTime"),
                    onCheckedChange = onMedicationChange
                )
            }
            if (state.medicationEnabled) {
                Spacer(Modifier.height(Spacing.sm))
                // This is the bug, not the polish. `onEditMedicationTime` was a
                // hardcoded empty lambda at both of its call sites, so the
                // "Change" button that used to sit here did nothing at all and
                // the reminder shipped stuck at whatever it defaulted to.
                //
                // Gated on the switch on purpose. The enable path writes
                // `setMedicationTime(null, null)` when it is off, so a strip
                // that stayed live while disabled would write a time and have
                // it nulled immediately - the user sets it, nothing sticks, and
                // it looks exactly like the bug being fixed here.
                TimeStrip(
                    label = "Every day at",
                    minutesOfDay = state.medicationHour * 60 + state.medicationMinute,
                    onChange = { minutes -> onMedicationTimeChange(minutes / 60, minutes % 60) },
                    advice = { "The wearable buzzes and shows the reminder until it is dismissed." }
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
                    icon = SafeShadeIcons.RepeatingCheckIn,
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

        // This was a button to a whole separate screen that had no interactive
        // controls on it at all — a reference list you could navigate to, read,
        // and navigate back out of. It is reference material, so it now sits
        // where reference material belongs: in a drawer at the bottom of the
        // settings it is a footnote to, one tap away and closed by default.
        item("device-only") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                ExpandableSection(
                    label = "Only on the device",
                    count = DeviceCapabilities.deviceOnly.size
                ) {
                    DeviceCapabilities.deviceOnly.forEach { setting ->
                        Hairline()
                        Way(
                            name = setting.label,
                            // UNKNOWN, not OFF. The app has never read these
                            // values and never will on this firmware, so
                            // claiming a state for them would be an invention.
                            state = LampState.UNKNOWN,
                            stateLabel = "On device",
                            detail = setting.deviceMenuPath,
                            // `deviceOnly` suppresses any switch and adds
                            // "change this on the device" to the spoken row, so
                            // the copy here does not repeat it.
                            deviceOnly = true
                        )
                    }
                    Hairline()
                    // Do Not Disturb is the one row where the plain statement
                    // above is not quite true, and the gap is exactly the kind
                    // that produces a bug report: someone changes the quiet
                    // window in the app, sees the device still chiming, and
                    // concludes the whole feature is broken.
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Nameplate("About Do Not Disturb", small = true, muted = true)
                        Text(
                            text = "Do Not Disturb is split across the two ends. Turning " +
                                "it on or off is device-only, but the start and end " +
                                "hours it uses do travel from this app — they are the " +
                                "quiet hours window above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted
                        )
                        Text(
                            text = "If a later firmware adds a Bluetooth path for one of " +
                                "these, it moves into the settings above and stops " +
                                "appearing here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkFaint
                        )
                    }
                }
            }
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
            // The value competes with a fixed-size trailing button for the
            // row's width — at "Device name" a long user-typed string had
            // nowhere to go and wrapped across several lines, pushing the
            // button off-centre. One line with an ellipsis keeps the row's
            // height predictable; the full value is still visible in the
            // rename dialog this button opens.
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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

/**
 * What a siren level means in practice.
 *
 * Percentages are meaningless for loudness - nobody knows what 60% of a siren
 * sounds like - so the number is paired with the thing it decides: whether
 * somebody in the next room hears it.
 */
private fun sirenAdvice(level: Float): String = when {
    level <= 0.2f -> "Barely audible. Useful only if the wearer is holding the device."
    level <= 0.5f -> "Heard in the same room. Quiet enough not to startle."
    level <= 0.8f -> "Heard through a closed door. This is the usual choice."
    else -> "As loud as the device goes. Carries outdoors and down a hallway."
}

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
            onQuietWindowChange = { _, _ -> },
            onMedicationChange = {},
            onMedicationTimeChange = { _, _ -> },
            onCheckInIntervalChange = {},
            onEditDeviceName = {}
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
