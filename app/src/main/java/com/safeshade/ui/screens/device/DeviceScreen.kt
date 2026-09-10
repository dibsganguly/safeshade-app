package com.safeshade.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.DeviceIconType
import com.safeshade.data.LedPattern
import com.safeshade.data.PersonaMode
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.Avatar
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.plateClickable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.MainsPlate
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.nav.Routes
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

// ============================================================================
// Shared across com.safeshade.ui.screens.device
//
// These declarations are `internal` and live here rather than in `ui/board/`,
// because they are screen-level vocabulary rather than kit components — the
// sibling screens in this package reuse them by name. Duplicate top-level
// declarations in one package do not compile and nothing catches that until a
// build, so anything shared belongs here and only here.
// ============================================================================

/**
 * How a write to the wearable is getting on.
 *
 * Every synced setting needs this, because a BLE write is not a commit: it can
 * be dropped by the GATT queue, arrive at a device busy on its alarm screen, or
 * be acknowledged. Showing the new value the instant the user taps would be a
 * lie roughly as often as the link is imperfect, which for a wearable in a
 * pocket is often.
 *
 * [NO_RESPONSE] is deliberately not a trip. `LampState.TRIP` means "something
 * happened and it needs a person"; an unacknowledged settings write means "we
 * do not know", which is `LampState.UNKNOWN`.
 */
enum class AckState {
    /** Nothing in flight. The stored value is the last thing both ends agreed on. */
    IDLE,

    /** Written, waiting for `ACK:<tag>`. */
    PENDING,

    /** The device answered. This value is genuinely on the wearable. */
    CONFIRMED,

    /** The ack window elapsed. The write may or may not have landed. */
    NO_RESPONSE
}

/**
 * The lamp for a row whose value reads as [settled] when nothing is in flight.
 *
 * A function rather than a property on [AckState], because the settled
 * appearance belongs to the setting and not to the ack: an "off" switch that
 * has been confirmed should still read OFF, not LIVE.
 */
internal fun ackLamp(ack: AckState, settled: LampState): LampState = when (ack) {
    AckState.PENDING -> LampState.ATTENTION
    AckState.NO_RESPONSE -> LampState.UNKNOWN
    AckState.IDLE, AckState.CONFIRMED -> settled
}

/**
 * The ack as words, for a row's `detail` line.
 *
 * This goes in `detail` rather than in `stateLabel` on purpose. `Way` renders
 * either a switch **or** a state word plus lamp — never both — so on a row that
 * carries a switch the state word is invisible and only the bus tick's colour
 * is left to say anything. Colour alone is exactly what this system forbids, so
 * the acknowledgement has to be readable text somewhere the switch cannot
 * displace it.
 */
internal fun ackWord(ack: AckState): String? = when (ack) {
    AckState.IDLE -> null
    AckState.PENDING -> "Sending to the device"
    AckState.CONFIRMED -> "Confirmed by the device"
    AckState.NO_RESPONSE -> "No reply from the device"
}

/**
 * Link state as a lamp.
 *
 * `Connected` is amber rather than teal, matching the Board. The GATT link is
 * up but service discovery has not finished, so every write in that window is
 * dropped against a null characteristic with nothing but a log line. A teal
 * lamp there would be the app claiming it can reach a device it cannot.
 */
internal fun ConnectionState.toLampState(): LampState = when (this) {
    is ConnectionState.Ready -> LampState.LIVE
    is ConnectionState.Connected,
    is ConnectionState.Scanning,
    is ConnectionState.Connecting,
    is ConnectionState.Found -> LampState.ATTENTION
    is ConnectionState.ScanFailed, is ConnectionState.BluetoothUnavailable -> LampState.TRIP
    is ConnectionState.Disconnected -> LampState.OFF
}

/** The link in one short phrase, for a state label or a subline. */
internal fun ConnectionState.word(): String = when (this) {
    is ConnectionState.Ready -> "Connected"
    is ConnectionState.Connected -> "Starting up"
    is ConnectionState.Connecting -> "Connecting"
    is ConnectionState.Scanning -> "Searching"
    is ConnectionState.Found -> "Found"
    is ConnectionState.BluetoothUnavailable -> "Bluetooth off"
    is ConnectionState.ScanFailed -> "Scan failed"
    is ConnectionState.Disconnected -> "Not connected"
}

/**
 * The banner every screen in this package uses to say why its controls are
 * inert.
 *
 * The base sentence is the only thing this composable can honestly assert: the
 * link is down, so nothing reaches the wearable. Whether a change made now is
 * *queued* for the next connection is a fact about the caller's repository, not
 * about this screen — settings and mode are persisted and re-pushed on `Ready`,
 * an LED pattern is a direct characteristic write and is not. So the promise
 * arrives as [queuedNote] from the screen that can keep it, and a screen that
 * cannot keep it simply does not pass one.
 */
@Composable
internal fun OfflineNotice(
    connection: ConnectionState,
    modifier: Modifier = Modifier,
    queuedNote: String? = null
) {
    if (connection.isUsable) return
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth(), recessed = true) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Nameplate("Not connected", small = true, muted = true)
            Text(
                text = "Nothing reaches the wearable until it connects.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted
            )
            if (queuedNote != null) {
                Text(
                    text = queuedNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }
    }
}

// ============================================================================
// The screen
// ============================================================================

/** Everything the Device hub draws. */
data class DeviceUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val deviceName: String = "SafeShade S1",
    /** Whose device it is, when a guardian has named the wearer. */
    val wearerName: String = "",
    /** The person holding the phone, for the header's profile control. */
    val ownerName: String = "",
    val ownerAvatarId: String = "",
    val wearerAvatarId: String = "",
    val iconType: DeviceIconType = DeviceIconType.BACKPACK,
    val batteryPercent: Int? = null,
    val signalDbm: Int? = null,
    val mode: PersonaMode = PersonaMode.AUTO,
    val ledPattern: LedPattern = LedPattern.TORCH,
    /**
     * Preformatted by the caller — "just now", "2 hours ago".
     *
     * Anything relative to the clock arrives as a string. Working it out inside
     * a composable makes it stale the moment recomposition stops, and makes
     * previews depend on when they were rendered.
     */
    val lastSeenLabel: String? = null,
    val syncSummary: String? = null,
    val activeReminderCount: Int = 0,
    val pairedDeviceCount: Int = 0,
    val hasTelemetry: Boolean = false,
    val isRinging: Boolean = false,
    /** What the wearable reported as its firmware, or null when it never has. */
    val firmwareVersion: String? = null,
    /** How many of this phone's wearables are marked lost. */
    val lostCount: Int = 0,
    val rideCount: Int = 0,
    /** The leash as a word and a line, or null while the link is down. */
    val leashWord: String? = null,
    val leashLine: String? = null,
    val leashLamp: LampState = LampState.UNKNOWN
)

/**
 * The wearable, as a panel.
 *
 * A guardian arrives here to do maintenance — change the mode, find the thing,
 * check whether it still has charge — so the mains plate answers "is it there
 * and is it well" first, and everything under it is one way through to one job.
 *
 * Every row leaves through a single [onOpenWay] taking a route string, the same
 * shape the Board uses. That keeps the navigation graph the caller's business:
 * this file names destinations, it does not know how they are reached.
 */
@Composable
fun DeviceScreen(
    state: DeviceUiState,
    onOpenSettings: () -> Unit,
    onOpenWay: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /**
     * Hoisted above the NavHost by SafeShadeApp so scroll position
     * survives a tab switch. Defaulted so the previews still compile
     * without one.
     */
    listState: LazyListState = rememberLazyListState()
) {
    val colors = MaterialTheme.board
    val lamp = state.connection.toLampState()

    LazyColumn(
        state = listState,
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
                title = "Device",
                // No subtitle - it named the rows underneath it.
                tier = ScreenTier.ROOT,
                trailing = {
                    // The person, not a gear. What used to be "settings" —
                    // role, appearance, whether alerts reach you — are facts
                    // about the person holding the phone, and the Profile
                    // page opens on their face.
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .plateClickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onOpenSettings)
                            .clearAndSetSemantics { contentDescription = "Profile" },
                        contentAlignment = Alignment.Center
                    ) {
                        Avatar(
                            avatarId = state.ownerAvatarId.ifBlank { state.wearerAvatarId },
                            name = state.ownerName.ifBlank { state.wearerName },
                            size = 34.dp
                        )
                    }
                }
            )
        }

        item("mains") {
            MainsPlate(
                state = lamp,
                headline = state.deviceName,
                subline = deviceSubline(state),
                // The Protecting bay was never fed here, so it read "Not set"
                // under a subline that named the wearer.
                protectedName = state.wearerName,
                // Battery and signal are omitted entirely while there is no
                // link, rather than shown as dashes. The lamp has already said
                // why, and "--" in a readout reads as a broken instrument.
                batteryPercent = state.batteryPercent,
                signalDbm = state.signalDbm
            )
        }

        item("ways-heading") { SectionPlate(title = "Ways") }

        item("ways") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Adaptive mode",
                    state = if (state.connection.isUsable) LampState.LIVE else LampState.UNKNOWN,
                    stateLabel = state.mode.label,
                    detail = state.mode.blurb,
                    // The row's own glyph, not the selected persona's.
                    //
                    // This was `state.mode.icon`, which made it the one row in
                    // the bank whose icon changed shape with its value: a row
                    // called "Adaptive mode" showed a backpack, or a helmet, or
                    // a cane. An icon in this column is an identity - it says
                    // which way you are looking at, the way "Lights" and
                    // "Reminders" do - and what the row is set to is already
                    // stated twice to the right of it, in the state word and in
                    // the blurb underneath.
                    icon = SafeShadeIcons.AdaptiveMode,
                    // The seal is the honest signal that this mode has taken
                    // the wearable's own Mode and Safety menus away from the
                    // person wearing it.
                    sealed = state.mode.isGuardianLocked,
                    onClick = { onOpenWay(Routes.DEVICE_MODE) }
                )
                Hairline()
                Way(
                    name = "Device settings",
                    state = if (state.connection.isUsable) LampState.LIVE else LampState.UNKNOWN,
                    stateLabel = if (state.connection.isUsable) "Open" else "Offline",
                    detail = state.syncSummary,
                    icon = SafeShadeIcons.DeviceSettings,
                    onClick = { onOpenWay(Routes.DEVICE_SETTINGS) }
                )
                Hairline()
                Way(
                    name = "Lights",
                    state = if (state.connection.isUsable) LampState.LIVE else LampState.UNKNOWN,
                    stateLabel = state.ledPattern.label,
                    icon = SafeShadeIcons.Lights,
                    onClick = { onOpenWay(Routes.DEVICE_LIGHTS) }
                )
                Hairline()
                Way(
                    name = "Find the device",
                    // Ringing is a lit, attention-demanding state and stays lit
                    // until somebody taps the wearable — the app is never told
                    // that it stopped, so this row must not settle on its own.
                    state = if (state.isRinging) LampState.ATTENTION else LampState.OFF,
                    stateLabel = if (state.isRinging) "Ringing" else "Ready",
                    detail = state.lastSeenLabel?.let { "Last seen $it" },
                    icon = SafeShadeIcons.FindTheDevice,
                    onClick = { onOpenWay(Routes.DEVICE_LOCATE) }
                )
                Hairline()
                Way(
                    name = "Telemetry",
                    state = if (state.hasTelemetry) LampState.LIVE else LampState.UNKNOWN,
                    stateLabel = if (state.hasTelemetry) "Live" else "No data",
                    icon = SafeShadeIcons.Telemetry,
                    onClick = { onOpenWay(Routes.DEVICE_TELEMETRY) }
                )
                Hairline()
                Way(
                    name = "Reminders",
                    state = if (state.activeReminderCount > 0) LampState.LIVE else LampState.OFF,
                    stateLabel = if (state.activeReminderCount > 0) {
                        "${state.activeReminderCount} on"
                    } else {
                        "None"
                    },
                    icon = SafeShadeIcons.Reminders,
                    onClick = { onOpenWay(Routes.DEVICE_REMINDERS) }
                )
                Hairline()
                Way(
                    name = "Paired devices",
                    state = if (state.pairedDeviceCount > 0) LampState.LIVE else LampState.OFF,
                    stateLabel = if (state.pairedDeviceCount > 0) {
                        "${state.pairedDeviceCount} saved"
                    } else {
                        "None"
                    },
                    icon = SafeShadeIcons.PairedDevices,
                    onClick = { onOpenWay(Routes.DEVICE_PAIRED) }
                )
                Hairline()
                Way(
                    name = "Nearby",
                    state = state.leashLamp,
                    stateLabel = state.leashWord ?: "—",
                    detail = state.leashLine ?: "How far the wearable is from this phone, read off the link's strength.",
                    // Its own glyph, not the Bluetooth mark Paired devices
                    // already wears two rows up: two rows with one glyph read
                    // as one thing twice.
                    icon = SafeShadeIcons.BluetoothNearby
                )
                Hairline()
                Way(
                    name = "Firmware",
                    state = if (state.firmwareVersion != null) LampState.LIVE else LampState.UNKNOWN,
                    stateLabel = state.firmwareVersion ?: "—",
                    icon = SafeShadeIcons.CpuChip,
                    onClick = { onOpenWay(Routes.DEVICE_FIRMWARE) }
                )
                Hairline()
                Way(
                    name = "Lost",
                    state = if (state.lostCount > 0) LampState.ATTENTION else LampState.OFF,
                    stateLabel = if (state.lostCount > 0) "${state.lostCount} lost" else "None",
                    icon = SafeShadeIcons.Search01,
                    onClick = { onOpenWay(Routes.DEVICE_LOST) }
                )
                Hairline()
                Way(
                    name = "Ride log",
                    state = if (state.rideCount > 0) LampState.LIVE else LampState.OFF,
                    stateLabel = if (state.rideCount > 0) "${state.rideCount}" else "None",
                    icon = SafeShadeIcons.Bicycle01,
                    onClick = { onOpenWay(Routes.DEVICE_RIDES) }
                )
            }
        }
    }
}

/**
 * The one line under the device name.
 *
 * Prefers the wearer's name over the bare link state when a guardian has set
 * one: with two devices paired, "Baba" is what tells them which panel they are
 * looking at, and the lamp beside it has already said whether it is connected.
 */
private fun deviceSubline(state: DeviceUiState): String = when {
    state.isRinging -> "Ringing – tap the button on the device to stop it"
    state.wearerName.isNotBlank() -> "${state.wearerName} · ${state.connection.word()}"
    else -> state.connection.word()
}

// ============================================================================
// Previews
// ============================================================================

private val previewDevice = DeviceUiState(
    connection = ConnectionState.Ready,
    deviceName = "SafeShade S1",
    wearerName = "Baba",
    iconType = DeviceIconType.CANE,
    batteryPercent = 74,
    signalDbm = -63,
    mode = PersonaMode.ELDERLY,
    ledPattern = LedPattern.PULSE,
    lastSeenLabel = "just now",
    syncSummary = "All settings confirmed by the device",
    activeReminderCount = 2,
    pairedDeviceCount = 1,
    hasTelemetry = true
)

@Preview(name = "Device · light", showBackground = true)
@Composable
private fun DeviceScreenPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            DeviceScreen(state = previewDevice, onOpenWay = {}, onOpenSettings = {})
        }
    }
}

@Preview(name = "Device · dark", showBackground = true)
@Composable
private fun DeviceScreenPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            DeviceScreen(state = previewDevice, onOpenWay = {}, onOpenSettings = {})
        }
    }
}

@Preview(name = "Device · disconnected", showBackground = true)
@Composable
private fun DeviceScreenPreviewDisconnected() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            DeviceScreen(
                state = DeviceUiState(
                    connection = ConnectionState.Disconnected,
                    wearerName = "Baba",
                    lastSeenLabel = "2 hours ago"
                ),
                onOpenWay = {},
                onOpenSettings = {}
            )
        }
    }
}
