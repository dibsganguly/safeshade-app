package com.safeshade.ui.screens.safety

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.EmergencyContact
import com.safeshade.data.FallSensitivity
import com.safeshade.data.MedicalId
import com.safeshade.data.PersonaMode
import com.safeshade.data.SafetySettings
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Chain
import com.safeshade.ui.board.ChainStop
import com.safeshade.ui.board.Footnote
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Tile
import com.safeshade.ui.board.TileGrid
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.Hub
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * Everything the Safety hub draws.
 *
 * Flat and presentational, like `BoardUiState`. A "way" on this screen may be
 * backed by a persisted setting, by a derived condition (is the medical card
 * worth showing a responder?), or by a count of something stored elsewhere —
 * and the hub does not care which.
 */
data class SafetyUiState(
    val role: UserRole = UserRole.GUARDIAN,
    val wearerName: String = "",
    val settings: SafetySettings = SafetySettings(),
    val medicalId: MedicalId = MedicalId(),
    val activeMode: PersonaMode = PersonaMode.AUTO,
    /** Whether the wearable is currently reachable. Settings still edit offline. */
    val linkLive: Boolean = false,
    /** False when a setting has been changed here but not yet acknowledged. */
    val settingsSynced: Boolean = true,
    val safeZoneCount: Int = 0,
    /** Null when there is no fix, or no zones to be inside or outside of. */
    val insideSafeZone: Boolean? = null,
    val unresolvedTripCount: Int = 0,
    val lastTripLabel: String? = null,
    val silentSosEnabled: Boolean = false,
    /** When the wearable was last on the link, while it is off it. Null when connected or never seen. */
    val wearableOfflineSince: Long? = null,
    /** Whether the out-of-reach notice has gone out for this outage. */
    val wearableOfflineAlerted: Boolean = false,
    /** Whether the ladder is climbing an open alert right now. */
    val escalationRunning: Boolean = false,
    /** The newest vitals reading as one line, e.g. "72 bpm · 98%", or null when none. */
    val vitalsLine: String? = null,
    /** Whether the newest reading breaches a threshold. */
    val vitalsFlagged: Boolean = false,
    /** Whether the microphone is armed for a fall or an SOS. */
    val evidenceArmed: Boolean = false,
    val evidenceClipCount: Int = 0
)

/**
 * The Safety hub.
 *
 * One question, answered as a bank of circuits: if something happens to this
 * person, what will actually fire? Every row is a real mechanism with a real
 * state, and every row that can be wrong says so — an empty contact list and a
 * blank medical card are the two failures that make everything else here
 * pointless, so neither is allowed to look like a neutral "not set up yet".
 *
 * The emergency directory sits above the bank rather than inside it. It is the
 * one thing on this screen someone might need while a person is on the floor
 * in front of them, and it must not require scrolling or a correct guess about
 * which row it is under.
 *
 * **Nothing here collapses, and that is a decision rather than an omission.**
 * The pushed screens in this bank hide their rarely-touched controls behind
 * `ExpandableSection`, and it is the right trade there: a control somebody
 * changes once a year costs a tap. This screen is not a set of controls. Every
 * row carries a lamp and a state word, and the row *is* the answer to the
 * question the screen exists to ask — so collapsing one does not hide a
 * setting, it hides a status. A guardian who cannot see at a glance that the
 * contact list is empty, or that the device is outside every safe zone, has
 * been given a shorter screen and a worse one. Length is the cost of a status
 * board being readable in one pass, and it is worth paying here.
 */
@Composable
fun SafetyScreen(
    state: SafetyUiState,
    onOpenFallSettings: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenMedicalId: () -> Unit,
    onOpenEmergencyCard: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenTrips: () -> Unit,
    onOpenSilentSos: () -> Unit,
    onOpenZones: () -> Unit,
    onAutoCallChange: (Boolean) -> Unit,
    onSmsFallbackChange: (Boolean) -> Unit,
    onOpenEscalation: () -> Unit = {},
    onOpenWatch: () -> Unit = {},
    onOpenVitals: () -> Unit = {},
    onOpenEvidence: () -> Unit = {},
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
    val subject = state.wearerName.ifBlank {
        if (state.role == UserRole.GUARDIAN) "the wearer" else "you"
    }
    val contacts = state.settings.emergencyContacts
    val locked = state.activeMode.isGuardianLocked

    LazyColumn(
        state = listState,
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
        item("header") {
            // Direct call, not the bank's PanelHeader adapter: this list
            // already scrolls at Arrangement.spacedBy(Spacing.lg), so that
            // rhythm supplies the header's other 16dp the same way every
            // pattern-A screen gets it. Going through PanelHeader here would
            // add its own Spacer(Spacing.lg) on top and double the gap.
            // No subtitle: the bank below already shows what this screen is for.
            ScreenHeader(
                title = "Safety",
                tier = ScreenTier.ROOT
            )
        }

        // The head plate (2.53): the one tinted plate on this hub, answering
        // "is this person covered" before anything else. Trumps the old
        // per-condition "unresolved trip" banner — an open trip is this row's
        // TRIP state, not a second alert above it.
        item("head") {
            val fallOn = state.activeMode != PersonaMode.PET
            val headState = when {
                state.unresolvedTripCount > 0 -> LampState.TRIP
                !fallOn || contacts.isEmpty() -> LampState.ATTENTION
                else -> LampState.LIVE
            }
            val headLabel = when {
                state.unresolvedTripCount == 1 -> "1 open"
                state.unresolvedTripCount > 1 -> "${state.unresolvedTripCount} open"
                contacts.isEmpty() -> "No contacts"
                !fallOn -> "Detection off"
                else -> "Covered"
            }
            val headDetail = when {
                state.unresolvedTripCount > 0 -> state.lastTripLabel
                contacts.isEmpty() -> "Add a contact so a fall has somewhere to go."
                !fallOn -> "Off in ${state.activeMode.label} mode. Movement would trip it constantly."
                else -> "Fall detection and ${contacts.size} contact(s) are on for $subject."
            }
            BoardPlate(modifier = Modifier.fillMaxWidth(), hub = Hub.SAFETY) {
                Way(
                    name = "Protection",
                    state = headState,
                    stateLabel = headLabel,
                    detail = headDetail,
                    icon = SafeShadeIcons.ShieldWithKeyhole,
                    onClick = if (state.unresolvedTripCount > 0) onOpenTrips else null
                )
            }
        }

        // What happens, as a chain (2.27), in place of the paragraph this
        // screen used to open with. Only drawn while the path it describes is
        // actually live — a chain showing a call nobody will make is a chain
        // lying about the board.
        val fallOnForChain = state.activeMode != PersonaMode.PET
        if (fallOnForChain) {
            item("chain") {
                val ladder = state.settings.escalation
                Chain(
                    listOf(
                        ChainStop(SafeShadeIcons.FallDetection, "Fall", "detected"),
                        ChainStop(SafeShadeIcons.HourglassTimer, "${state.settings.fallCountdownSeconds} s", "to cancel"),
                        ChainStop(
                            SafeShadeIcons.CallAfterAFall,
                            state.settings.primaryContact?.name?.ifBlank { null } ?: "Nobody yet",
                            "is called"
                        ),
                        ChainStop(SafeShadeIcons.LadderStair, "Ladder", if (ladder.enabled) "if unanswered" else "off")
                    )
                )
            }
        }

        // Top of the screen, above everything, always - and in the SOS's red.
        //
        // This used to argue the opposite: that DANGER was reserved for actions
        // which place a real call, and that opening a list did not qualify. The
        // reasoning was sound and the result was wrong. Someone reaching for
        // emergency numbers is not reading a taxonomy of button weights; they
        // are looking for the one thing on this screen that is about an
        // emergency happening now, and it looked exactly like every other row.
        // Matching the SOS makes it findable at a glance, which is the only
        // property that matters here. Nothing dials on its own; the supporting
        // line still says so — kept short because BoardButton centres this
        // text inside Spacing.lg of horizontal padding with no maxLines
        // guard, and the original two-clause sentence wrapped to three lines
        // on a narrow phone at a raised font scale.
        item("services") {
            BoardButton(
                label = "Emergency Numbers",
                supporting = "112 and 7 more. Opens the dialer, never calls by itself.",
                icon = SafeShadeIcons.Cross,
                onClick = onOpenServices,
                weight = ButtonWeight.DANGER,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item("detection-heading") { SectionPlate(title = "Detection") }

        // The two rows that report a setting rather than opening one stay
        // Ways, in a plate of their own above the destinations (2.84): a
        // switch has nothing to navigate to.
        item("detection-switches") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Call after a fall",
                    state = if (state.settings.autoCallEmergency) LampState.LIVE else LampState.OFF,
                    stateLabel = if (state.settings.autoCallEmergency) "On" else "Off",
                    detail = if (contacts.isEmpty()) {
                        "Nobody to call yet. Add a contact first."
                    } else {
                        "Calls ${state.settings.primaryContact?.name.orEmpty().ifBlank { "your first contact" }} if the countdown runs out."
                    },
                    icon = SafeShadeIcons.CallAfterAFall,
                    checked = state.settings.autoCallEmergency,
                    onCheckedChange = onAutoCallChange
                )
                Hairline()
                Way(
                    name = "Text as well",
                    state = if (state.settings.smsFallbackEnabled) LampState.LIVE else LampState.OFF,
                    stateLabel = if (state.settings.smsFallbackEnabled) "On" else "Off",
                    detail = "Sends contacts a message with the last known location. Works without Bluetooth.",
                    icon = SafeShadeIcons.TextAsWell,
                    checked = state.settings.smsFallbackEnabled,
                    onCheckedChange = onSmsFallbackChange
                )
            }
        }

        if (!state.settingsSynced) {
            item("sync-note") {
                Footnote(
                    text = if (state.linkLive) {
                        "Sending these settings to the device."
                    } else {
                        "Saved on this phone. They will reach the device the next time it connects."
                    }
                )
            }
        }

        item("detection-tiles") {
            val fallOn = state.activeMode != PersonaMode.PET
            val ladder = state.settings.escalation
            TileGrid(
                listOf(
                    Tile(
                        title = "Fall detection",
                        icon = SafeShadeIcons.FallDetection,
                        state = if (fallOn) LampState.LIVE else LampState.OFF,
                        stateLabel = if (fallOn) state.settings.fallSensitivity.label else "Off",
                        onClick = onOpenFallSettings,
                        tag = if (locked) "Sealed" else null
                    ),
                    Tile(
                        title = "If nobody answers",
                        icon = SafeShadeIcons.LadderStair,
                        state = when {
                            state.escalationRunning -> LampState.ATTENTION
                            ladder.enabled -> LampState.LIVE
                            else -> LampState.OFF
                        },
                        stateLabel = when {
                            state.escalationRunning -> "Calling"
                            ladder.enabled -> "On"
                            else -> "Off"
                        },
                        onClick = onOpenEscalation,
                        tag = if (locked) "Sealed" else null
                    ),
                    Tile(
                        title = "Evidence",
                        icon = SafeShadeIcons.Microphone,
                        state = if (state.evidenceArmed) LampState.LIVE else LampState.OFF,
                        stateLabel = if (state.evidenceArmed) "Armed" else "Off",
                        onClick = onOpenEvidence
                    )
                )
            )
        }

        item("people-heading") { SectionPlate(title = "Who gets called") }

        item("people-tiles") {
            TileGrid(
                listOf(
                    Tile(
                        title = "Emergency contacts",
                        icon = SafeShadeIcons.EmergencyContacts,
                        // No contacts is not a neutral empty state — it means
                        // the whole detection chain above ends nowhere.
                        state = if (contacts.isEmpty()) LampState.ATTENTION else LampState.LIVE,
                        stateLabel = if (contacts.isEmpty()) "None" else "${contacts.size}",
                        onClick = onOpenContacts
                    ),
                    Tile(
                        title = "Medical ID",
                        icon = SafeShadeIcons.MedicalId,
                        state = medicalLamp(state.medicalId),
                        stateLabel = medicalLabel(state.medicalId),
                        onClick = onOpenMedicalId
                    ),
                    Tile(
                        title = "Emergency card",
                        icon = SafeShadeIcons.QrCode,
                        state = if (state.medicalId.isUsable) LampState.LIVE else LampState.OFF,
                        stateLabel = if (state.medicalId.isUsable) "Ready" else "Empty",
                        onClick = onOpenEmergencyCard
                    )
                )
            )
        }

        item("watch-heading") { SectionPlate(title = "Watching") }

        item("watch-tiles") {
            val watching = state.settings.offlineAlertMinutes > 0 || state.settings.lowBatteryPercent > 0
            val offline = state.wearableOfflineSince != null && !state.linkLive
            TileGrid(
                listOf(
                    Tile(
                        title = "Safe zones",
                        icon = SafeShadeIcons.SafeZone,
                        state = safeZoneLamp(state.safeZoneCount, state.insideSafeZone),
                        stateLabel = safeZoneLabel(state.safeZoneCount, state.insideSafeZone),
                        onClick = onOpenZones
                    ),
                    Tile(
                        title = "Silent SOS",
                        icon = SafeShadeIcons.SilentSos,
                        state = if (state.silentSosEnabled) LampState.LIVE else LampState.OFF,
                        stateLabel = if (state.silentSosEnabled) "Armed" else "Off",
                        onClick = onOpenSilentSos
                    ),
                    Tile(
                        title = "Out of reach",
                        icon = SafeShadeIcons.ConnectToTheDevice,
                        state = when {
                            offline && watching -> LampState.ATTENTION
                            watching -> LampState.LIVE
                            else -> LampState.OFF
                        },
                        stateLabel = when {
                            offline && watching -> "Out of reach"
                            watching -> "Watching"
                            else -> "Off"
                        },
                        onClick = onOpenWatch
                    ),
                    Tile(
                        title = "Vitals",
                        icon = SafeShadeIcons.HeartWithPulse,
                        state = when {
                            state.vitalsLine == null -> LampState.UNKNOWN
                            state.vitalsFlagged -> LampState.ATTENTION
                            else -> LampState.LIVE
                        },
                        stateLabel = when {
                            state.vitalsLine == null -> "—"
                            state.vitalsFlagged -> "Outside range"
                            else -> "In range"
                        },
                        onClick = onOpenVitals
                    ),
                    Tile(
                        title = "Trip log",
                        icon = SafeShadeIcons.History,
                        state = if (state.unresolvedTripCount > 0) LampState.TRIP else LampState.OFF,
                        stateLabel = if (state.unresolvedTripCount > 0) "Open" else "Clear",
                        onClick = onOpenTrips
                    )
                )
            )
        }
    }
}

/**
 * How complete the medical card is, as a lamp.
 *
 * `isUsable` is the honest threshold — blood type, a contact, or allergies is
 * the minimum a responder can act on. Anything less is ATTENTION rather than
 * OFF, because an empty card on a person wearing a medical device is a gap,
 * not a preference.
 */
private fun medicalLamp(medicalId: MedicalId): LampState =
    if (medicalId.filledFieldCount >= 6) LampState.LIVE else LampState.ATTENTION

private fun medicalLabel(medicalId: MedicalId): String = when {
    medicalId.filledFieldCount >= 6 -> "Filled in"
    medicalId.isUsable -> "Partial"
    else -> "Empty"
}

private fun safeZoneLamp(count: Int, inside: Boolean?): LampState = when {
    count == 0 -> LampState.OFF
    inside == null -> LampState.UNKNOWN
    inside -> LampState.LIVE
    else -> LampState.ATTENTION
}

private fun safeZoneLabel(count: Int, inside: Boolean?): String = when {
    count == 0 -> "None"
    inside == null -> "No fix"
    inside -> "Inside"
    else -> "Outside"
}

// ============================================
// PREVIEWS
// ============================================

private val previewState = SafetyUiState(
    role = UserRole.GUARDIAN,
    wearerName = "Baba",
    settings = SafetySettings(
        autoCallEmergency = true,
        fallSensitivity = FallSensitivity.HIGH,
        smsFallbackEnabled = true,
        fallCountdownSeconds = 30,
        emergencyContacts = listOf(
            EmergencyContact("Priya", "+91 98300 11223", isPrimary = true),
            EmergencyContact("Dr Sen", "+91 98300 44556")
        )
    ),
    medicalId = MedicalId(
        bloodType = "B+",
        emergencyContact = "+91 98300 11223",
        contactName = "Priya",
        allergies = "Penicillin",
        age = 74,
        conditions = "Type 2 diabetes",
        medications = "Metformin 500mg"
    ),
    activeMode = PersonaMode.ELDERLY,
    linkLive = true,
    settingsSynced = false,
    safeZoneCount = 2,
    insideSafeZone = true,
    unresolvedTripCount = 1,
    lastTripLabel = "Fall detected, yesterday at 6:40 pm"
)

@Preview(name = "Safety — light", showBackground = true, heightDp = 1400)
@Composable
private fun SafetyScreenLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        SafetyScreen(
            state = previewState,
            onOpenFallSettings = {}, onOpenContacts = {}, onOpenMedicalId = {},
            onOpenEmergencyCard = {}, onOpenServices = {}, onOpenTrips = {},
            onOpenSilentSos = {}, onOpenZones = {},
            onAutoCallChange = {}, onSmsFallbackChange = {}
        )
    }
}

@Preview(
    name = "Safety — dark, nothing set up",
    showBackground = true,
    heightDp = 1400,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SafetyScreenDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        SafetyScreen(
            state = SafetyUiState(
                role = UserRole.COMPANION,
                settings = SafetySettings(autoCallEmergency = false),
                activeMode = PersonaMode.AUTO,
                linkLive = false
            ),
            onOpenFallSettings = {}, onOpenContacts = {}, onOpenMedicalId = {},
            onOpenEmergencyCard = {}, onOpenServices = {}, onOpenTrips = {},
            onOpenSilentSos = {}, onOpenZones = {},
            onAutoCallChange = {}, onSmsFallbackChange = {}
        )
    }
}
