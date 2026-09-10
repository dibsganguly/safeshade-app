package com.safeshade.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.ActionPair
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Chain
import com.safeshade.ui.board.ChainStop
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** One of this phone's wearables, as the lost page prints it. */
data class LostDeviceRow(
    val address: String,
    val name: String,
    /** Who wears it, or what it is on. */
    val label: String,
    val lost: Boolean,
    val lostSinceLabel: String?,
    /** When this phone itself last heard it advertise. */
    val ownLastSeenLabel: String?,
    /** When another SafeShade phone last reported it, and roughly where. */
    val communityLastSeenLabel: String?,
    val communityLat: Double?,
    val communityLon: Double?
)

/** Everything the lost page draws. */
data class LostUiState(
    val devices: List<LostDeviceRow> = emptyList(),
    val sweeping: Boolean = false,
    val permissionsGranted: Boolean = true,
    val signedIn: Boolean = false,
    /** How many other SafeShades this phone has heard in the last day, for the community line. */
    val heardOthersToday: Int = 0,
    /** Whether this phone reports what it hears to the community. */
    val reporting: Boolean = false,
    val lastSweepLabel: String? = null,
    val sweepError: String? = null
)

/**
 * Lost.
 *
 * Two ways a wearable is found when it is not on the link: this phone's own
 * sweep, which hears it if it is in the next room, and the community, which
 * is every other SafeShade phone that walks past it and reports the sighting.
 * The page is the record of both, per device, with a dash where nothing has
 * been heard. Marking a device lost does two things and no more: it keeps
 * the sweep running whenever the app is open, and it flags the address so a
 * community report is fetched for it. Nothing is promised about being found.
 */
@Composable
fun LostModeScreen(
    state: LostUiState,
    onMarkLost: (address: String) -> Unit,
    onMarkFound: (address: String) -> Unit,
    onSweep: () -> Unit,
    onOpenMap: (lat: Double, lon: Double) -> Unit,
    onReporting: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSignIn: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board

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
            )
    ) {
        ScreenHeader(title = "Lost", subtitle = "Finding a wearable that is off the link", onBack = onBack)

        // What lost mode does, as a chain rather than a paragraph: marking a
        // device lost starts this phone listening for it, and asks the
        // community to report a sighting too.
        Chain(
            listOf(
                ChainStop(SafeShadeIcons.Search01, "Marked lost", "Starts listening"),
                ChainStop(SafeShadeIcons.Bluetooth, "Nearby sweep", "Twenty seconds, in range"),
                ChainStop(SafeShadeIcons.LocationCheck, "The community", "Reports a sighting")
            )
        )
        Spacer(Modifier.height(Spacing.lg))

        SectionPlate(title = "This phone's wearables")
        Spacer(Modifier.height(Spacing.sm))
        if (state.devices.isEmpty()) {
            Note("No wearable is paired with this phone yet.")
        } else {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                state.devices.forEachIndexed { i, d ->
                    if (i > 0) Hairline()
                    Way(
                        name = d.label.ifBlank { d.name },
                        state = if (d.lost) LampState.ATTENTION else LampState.OFF,
                        stateLabel = if (d.lost) "Lost" else "Not lost",
                        detail = buildString {
                            append(if (d.lost) "Since ${d.lostSinceLabel ?: "—"}. " else "")
                            append("This phone heard it ${d.ownLastSeenLabel ?: "—"}. ")
                            append("Community: ${d.communityLastSeenLabel ?: "—"}.")
                        },
                        icon = SafeShadeIcons.Search01
                    )
                    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                        // Two decisions with a default (2.65) in place of the
                        // switch: which state to set this wearable to, with the
                        // one it is already in shown quiet and disabled.
                        ActionPair(
                            primaryLabel = "Mark Lost",
                            onPrimary = { onMarkLost(d.address) },
                            primaryWeight = ButtonWeight.ATTENTION,
                            primaryEnabled = !d.lost,
                            secondaryLabel = "Mark Found",
                            onSecondary = { onMarkFound(d.address) },
                            secondaryEnabled = d.lost
                        )
                    }
                    if (d.lost && d.communityLat != null && d.communityLon != null) {
                        Hairline()
                        Way(
                            name = "Where the community last heard it",
                            state = LampState.LIVE,
                            stateLabel = "On a map",
                            detail = d.communityLastSeenLabel,
                            icon = SafeShadeIcons.PinLocation,
                            onClick = { onOpenMap(d.communityLat, d.communityLon) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Sweep from here")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Listening",
                state = when {
                    !state.permissionsGranted -> LampState.ATTENTION
                    state.sweeping -> LampState.LIVE
                    else -> LampState.OFF
                },
                stateLabel = when {
                    !state.permissionsGranted -> "Not allowed"
                    state.sweeping -> "Listening"
                    else -> "Idle"
                },
                detail = when {
                    !state.permissionsGranted -> "Bluetooth scanning is not allowed yet."
                    state.sweeping -> "Twenty seconds of low-power listening for any SafeShade advertising nearby."
                    state.lastSweepLabel != null -> "Last swept ${state.lastSweepLabel}. Hears a wearable within a few rooms."
                    else -> "Hears a wearable within a few rooms."
                },
                icon = SafeShadeIcons.Bluetooth
            )
        }
        Spacer(Modifier.height(Spacing.md))
        if (!state.permissionsGranted) {
            BoardButton(label = "Allow Bluetooth", onClick = onRequestPermissions, weight = ButtonWeight.ATTENTION, icon = SafeShadeIcons.Bluetooth, modifier = Modifier.fillMaxWidth())
        } else {
            BoardButton(
                label = if (state.sweeping) "Listening…" else "Sweep Now",
                icon = SafeShadeIcons.Search01,
                onClick = onSweep,
                enabled = !state.sweeping,
                weight = ButtonWeight.SECONDARY,
                // The commit's own figure (2.31): what it is about to do,
                // in the numbers the detail line already states.
                figure = if (state.sweeping) null else "20 s",
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (state.sweepError != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(state.sweepError, style = MaterialTheme.typography.bodyMedium, color = colors.inkTrip)
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "The community")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Report what this phone hears",
                state = when {
                    state.reporting && state.signedIn -> LampState.LIVE
                    state.reporting -> LampState.ATTENTION
                    else -> LampState.OFF
                },
                stateLabel = when {
                    state.reporting && state.signedIn -> "On"
                    state.reporting -> "Signed out"
                    else -> "Off"
                },
                detail = when {
                    state.reporting && state.signedIn -> "Another SafeShade heard nearby is reported once every ten minutes with this phone's position. ${state.heardOthersToday} heard today."
                    state.reporting -> "Reports go out once this phone is signed in."
                    else -> "Other phones can only find yours if theirs report too. Nothing about you is sent, only the wearable's address, the time and the place."
                },
                icon = SafeShadeIcons.LocationCheck,
                checked = state.reporting,
                onCheckedChange = onReporting
            )
        }
        if (state.reporting && !state.signedIn) {
            Spacer(Modifier.height(Spacing.md))
            BoardButton(label = "Sign In", onClick = onOpenSignIn, weight = ButtonWeight.SECONDARY, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.board.inkMuted, modifier = Modifier.fillMaxWidth())
}

@Preview(name = "Lost", showBackground = true, heightDp = 1200)
@Composable
private fun LostPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            LostModeScreen(
                state = LostUiState(
                    devices = listOf(LostDeviceRow("AA", "SafeShade S1", "Biscuit's collar", true, "yesterday 18:40", "yesterday 18:12", "2 hours ago near Park Street", 22.55, 88.35)),
                    reporting = true, signedIn = true, heardOthersToday = 3
                ),
                onMarkLost = {}, onMarkFound = {}, onSweep = {}, onOpenMap = { _, _ -> }, onReporting = {},
                onRequestPermissions = {}, onOpenSignIn = {}, onBack = {}
            )
        }
    }
}
