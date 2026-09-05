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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the locate screen draws. */
data class LocateUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val deviceName: String = "SafeShade S1",
    /**
     * The app's *belief* that the siren is running.
     *
     * `CMD_FIND` is never acknowledged and the alarm clears only when somebody
     * physically taps the button on the wearable, so nothing on this link can
     * ever tell us it stopped. This flag is therefore sticky until a person
     * says otherwise, and the UI must never time it out.
     */
    val isRinging: Boolean = false,
    /**
     * First step of the two-step confirm.
     *
     * Hoisted rather than remembered locally so the caller can reset it — for
     * example when the screen is left, which should disarm rather than leave a
     * loaded button waiting for a stray tap on return.
     */
    val ringConfirmArmed: Boolean = false,
    /** Smoothed dBm. Null when there has been no reading this session. */
    val rssiDbm: Int? = null,
    /** "where your phone was", preformatted by the caller. */
    val lastKnownPlace: String? = null,
    val lastKnownCoordinates: String? = null,
    val lastKnownAgeLabel: String? = null
)

/**
 * Find the device.
 *
 * Three tools of decreasing bluntness. Ring it, because if it is under a
 * cushion that solves the problem in one second. Watch the signal get stronger,
 * because that is the only live proximity information this link actually
 * carries. And failing both, look at where the phone last saw it — which is a
 * weaker claim than it sounds and is labelled as such.
 *
 * The ring is behind two taps and a plain warning because of what the firmware
 * does with `CMD_FIND`: the wearable goes to its full EMERGENCY SOS screen,
 * with the siren and red LEDs running. Somebody in a waiting room would be
 * mortified. It is the right control to have and the wrong one to fire by
 * accident.
 */
@Composable
fun LocateScreen(
    state: LocateUiState,
    onArmRing: () -> Unit,
    onCancelRing: () -> Unit,
    onRing: () -> Unit,
    onRingStopAcknowledged: () -> Unit,
    onOpenLastKnownLocation: () -> Unit,
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
            ScreenHeader(title = "Find the device", onBack = onBack)
        }

        // ---------- (a) Ring ----------
        item("ring-heading") { SectionPlate(title = "Ring") }

        if (state.isRinging) {
            item("ringing") {
                RingingBanner(
                    deviceName = state.deviceName,
                    onAcknowledged = onRingStopAcknowledged
                )
            }
        } else if (state.ringConfirmArmed) {
            item("ring-confirm") {
                RingConfirm(onConfirm = onRing, onCancel = onCancelRing)
            }
        } else {
            item("ring-arm") {
                Column {
                    BoardButton(
                        label = "Ring the device",
                        supporting = "Sounds the siren so you can hear where it is",
                        icon = Icons.Outlined.NotificationsActive,
                        onClick = onArmRing,
                        enabled = state.connection.isUsable,
                        weight = ButtonWeight.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        // Said before the tap as well as after it. Somebody who
                        // reads only this line still knows what they are about
                        // to do to whoever is carrying the device.
                        text = if (state.connection.isUsable) {
                            "The wearable goes to its full emergency screen with " +
                                "the siren and red lights on, and stays there until " +
                                "someone taps the button on the device."
                        } else {
                            "Ringing needs a live connection to the wearable."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }
        }

        // ---------- (b) Proximity ----------
        item("proximity-heading") { SectionPlate(title = "How close") }

        item("proximity") { ProximityMeter(rssiDbm = state.rssiDbm) }

        item("proximity-note") {
            Text(
                text = "Signal strength only tells you nearer or further, not which " +
                    "direction. Walk a few steps and watch which way the number moves.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
        }

        // ---------- (c) Last known location ----------
        item("last-heading") { SectionPlate(title = "Last known location") }

        item("last") {
            if (state.lastKnownPlace == null && state.lastKnownCoordinates == null) {
                EmptyBay(
                    message = "No position recorded yet. This fills in the first " +
                        "time your phone has both a location fix and the device " +
                        "in range."
                )
            } else {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Nameplate("Where your phone was", small = true, muted = true)
                        }
                        if (state.lastKnownPlace != null) {
                            Text(
                                text = state.lastKnownPlace,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.ink
                            )
                        }
                        if (state.lastKnownCoordinates != null) {
                            Readout(value = state.lastKnownCoordinates)
                        }
                        if (state.lastKnownAgeLabel != null) {
                            Text(
                                text = "Recorded ${state.lastKnownAgeLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkFaint
                            )
                        }
                    }
                    Hairline()
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        // The honest framing. The wearable has no GPS on this
                        // link, so this is a fact about the phone, not about the
                        // device — and if the two were separated after this fix
                        // was taken, it is worth nothing at all.
                        Text(
                            text = "This is where your phone was when it last saw " +
                                "the device, not where the device is now. The " +
                                "wearable has no GPS on this connection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted
                        )
                        Spacer(Modifier.height(Spacing.md))
                        BoardButton(
                            label = "Open in maps",
                            icon = Icons.Outlined.LocationOn,
                            onClick = onOpenLastKnownLocation,
                            weight = ButtonWeight.SECONDARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * The second tap.
 *
 * A separate plate rather than a dialog, because the consequence needs more
 * than one line and a dialog that has to be read is a dialog that gets
 * dismissed.
 */
@Composable
private fun RingConfirm(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Nameplate("Before you ring it")
            Text(
                text = "The wearable will show its full emergency screen, sound the " +
                    "siren and flash red. It will not stop on its own and this app " +
                    "cannot stop it — somebody has to tap the button on the device.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink
            )
            Text(
                text = "If the person carrying it is in company, warn them first.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted
            )
            Spacer(Modifier.height(Spacing.xs))
            BoardButton(
                label = "Ring it now",
                onClick = onConfirm,
                // DANGER is reserved for actions that fire something real in
                // the world. A siren on somebody's person qualifies.
                weight = ButtonWeight.DANGER,
                modifier = Modifier.fillMaxWidth()
            )
            BoardButton(
                label = "Cancel",
                onClick = onCancel,
                weight = ButtonWeight.QUIET,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * The ringing banner.
 *
 * Never dismisses itself. There is no signal on this link that the alarm
 * stopped, so a banner that faded after thirty seconds would be inventing one,
 * and the user would be left believing a siren had stopped that is still going.
 * It clears when a person confirms it, and only then.
 */
@Composable
private fun RingingBanner(
    deviceName: String,
    onAcknowledged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(Spacing.lg)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PilotLamp(state = LampState.ATTENTION, size = 18.dp, description = "Ringing")
                Spacer(Modifier.width(Spacing.sm))
                Nameplate("Ringing")
            }
            Text(
                text = "$deviceName is on its emergency screen with the siren running.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink
            )
            Text(
                text = "It stops only when someone presses the button on the device. " +
                    "This app is not told when that happens, so this notice stays " +
                    "until you clear it.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted
            )
            Spacer(Modifier.height(Spacing.xs))
            BoardButton(
                label = "It has stopped",
                onClick = onAcknowledged,
                weight = ButtonWeight.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * The proximity meter.
 *
 * Five bands of a smoothed RSSI, with the raw dBm underneath in the readout
 * face. The bands are what a person can act on; the number is what stops the
 * bands from overclaiming, because radio through a coat pocket is not a
 * distance measurement and the digit moving about makes that obvious.
 */
@Composable
private fun ProximityMeter(
    rssiDbm: Int?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val band = bandFor(rssiDbm)

    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Nameplate("Proximity", modifier = Modifier.weight(1f))
                Text(
                    text = band.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = when (band.lamp) {
                        LampState.LIVE -> colors.inkLive
                        LampState.ATTENTION -> colors.inkAttention
                        LampState.TRIP -> colors.inkTrip
                        LampState.OFF, LampState.UNKNOWN -> colors.inkFaint
                    }
                )
                Spacer(Modifier.width(Spacing.sm))
                PilotLamp(state = band.lamp, description = band.label)
            }

            Spacer(Modifier.height(Spacing.md))

            // The bar itself is achromatic. Which segments are filled is the
            // information; the lamp above already carries the state colour, and
            // a five-colour gradient here would be a second, competing signal.
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        contentDescription = band.spoken(rssiDbm)
                    }
            ) {
                repeat(BAND_COUNT) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(Radius.tight))
                            .background(
                                if (index < band.filledSegments) colors.ink else colors.recess
                            )
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            Readout(
                // The hairline number. Deliberately unrounded and unsmoothed in
                // presentation, so nobody mistakes the bands for metres.
                value = rssiDbm?.let { "$it dBm" } ?: "-- dBm",
                label = "Smoothed signal"
            )
        }
    }
}

private const val BAND_COUNT = 5

/** A proximity band: what to say, how to light it, how much bar to fill. */
private data class ProximityBand(
    val label: String,
    val lamp: LampState,
    val filledSegments: Int
) {
    fun spoken(rssiDbm: Int?): String = when (rssiDbm) {
        null -> "Proximity unknown, no signal reading"
        else -> "Proximity $label, $rssiDbm decibel milliwatts"
    }
}

/**
 * RSSI to band.
 *
 * The out-of-range boundary is -90 dBm, matching the threshold `MainsPlate`
 * already uses to flag a weak signal. Two screens disagreeing about when a link
 * has gone is worse than either threshold being slightly wrong.
 */
private fun bandFor(rssiDbm: Int?): ProximityBand = when {
    rssiDbm == null -> ProximityBand("Unknown", LampState.UNKNOWN, 0)
    rssiDbm >= -55 -> ProximityBand("Very close", LampState.LIVE, 5)
    rssiDbm >= -68 -> ProximityBand("Near", LampState.LIVE, 4)
    rssiDbm >= -80 -> ProximityBand("Same room", LampState.LIVE, 3)
    rssiDbm >= -90 -> ProximityBand("Far", LampState.ATTENTION, 2)
    else -> ProximityBand("Out of range", LampState.UNKNOWN, 1)
}

// ============================================================================
// Previews
// ============================================================================

private val previewLocate = LocateUiState(
    connection = ConnectionState.Ready,
    deviceName = "Baba's cane",
    rssiDbm = -63,
    lastKnownPlace = "Salt Lake Sector V, Kolkata",
    lastKnownCoordinates = "22.5726, 88.4337",
    lastKnownAgeLabel = "12 minutes ago"
)

@Composable
private fun LocatePreviewHost(state: LocateUiState) {
    Box(Modifier.background(MaterialTheme.board.ground)) {
        LocateScreen(
            state = state,
            onArmRing = {},
            onCancelRing = {},
            onRing = {},
            onRingStopAcknowledged = {},
            onOpenLastKnownLocation = {}
        )
    }
}

@Preview(name = "Locate · light", showBackground = true)
@Composable
private fun LocatePreviewLight() {
    SafeShadeTheme(darkTheme = false) { LocatePreviewHost(previewLocate) }
}

@Preview(name = "Locate · dark", showBackground = true)
@Composable
private fun LocatePreviewDark() {
    SafeShadeTheme(darkTheme = true) { LocatePreviewHost(previewLocate) }
}

@Preview(name = "Locate · confirming", showBackground = true)
@Composable
private fun LocatePreviewConfirming() {
    SafeShadeTheme(darkTheme = false) {
        LocatePreviewHost(previewLocate.copy(ringConfirmArmed = true))
    }
}

@Preview(name = "Locate · ringing", showBackground = true)
@Composable
private fun LocatePreviewRinging() {
    SafeShadeTheme(darkTheme = true) {
        LocatePreviewHost(previewLocate.copy(isRinging = true, rssiDbm = -94))
    }
}

@Preview(name = "Locate · never seen", showBackground = true)
@Composable
private fun LocatePreviewEmpty() {
    SafeShadeTheme(darkTheme = false) {
        LocatePreviewHost(
            LocateUiState(connection = ConnectionState.Disconnected, deviceName = "SafeShade S1")
        )
    }
}
