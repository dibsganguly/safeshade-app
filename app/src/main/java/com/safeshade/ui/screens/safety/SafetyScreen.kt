package com.safeshade.ui.screens.safety

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Fence
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.PersonalInjury
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Seal
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
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
    val silentSosEnabled: Boolean = false
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
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val subject = state.wearerName.ifBlank {
        if (state.role == UserRole.GUARDIAN) "the wearer" else "you"
    }
    val contacts = state.settings.emergencyContacts
    val locked = state.activeMode.isGuardianLocked

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
        item("header") {
            PanelHeader(
                title = "Safety",
                subtitle = if (state.role == UserRole.GUARDIAN) {
                    "What happens if something happens to $subject."
                } else {
                    "What happens if something happens to you."
                }
            )
        }

        // Top of the screen, above everything, always. Not a DANGER button:
        // that weight is reserved for actions that place a real call, and this
        // one opens a list. Nothing here dials on its own.
        item("services") {
            BoardButton(
                label = "Emergency numbers",
                supporting = "112 and seven other Indian services. Opens the dialer; never calls by itself.",
                icon = Icons.Outlined.LocalHospital,
                onClick = onOpenServices,
                weight = ButtonWeight.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.unresolvedTripCount > 0) {
            item("unresolved") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Way(
                        name = if (state.unresolvedTripCount == 1) "1 trip needs a reply" else "${state.unresolvedTripCount} trips need a reply",
                        state = LampState.TRIP,
                        stateLabel = "Open",
                        detail = state.lastTripLabel,
                        icon = Icons.Outlined.History,
                        onClick = onOpenTrips
                    )
                }
            }
        }

        item("detection-heading") { SectionPlate(title = "Detection") }

        item("detection") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                // Pet mode runs with fall detection switched off in firmware —
                // an animal's normal movement trips every threshold — so the
                // row reports OFF rather than claiming cover that is not there.
                val fallOn = state.activeMode != PersonaMode.PET
                Way(
                    name = "Fall detection",
                    state = if (fallOn) LampState.LIVE else LampState.OFF,
                    stateLabel = if (fallOn) state.settings.fallSensitivity.label else "Off",
                    detail = if (fallOn) {
                        "${state.settings.fallSensitivity.blurb}. ${formatDuration(state.settings.fallCountdownSeconds)} to cancel before a call."
                    } else {
                        "Off in ${state.activeMode.label} mode. Movement would trip it constantly."
                    },
                    icon = Icons.Outlined.PersonalInjury,
                    sealed = locked,
                    onClick = onOpenFallSettings
                )
                Hairline()
                Way(
                    name = "Call after a fall",
                    state = if (state.settings.autoCallEmergency) LampState.LIVE else LampState.OFF,
                    stateLabel = if (state.settings.autoCallEmergency) "On" else "Off",
                    detail = if (contacts.isEmpty()) {
                        "Nobody to call yet. Add a contact first."
                    } else {
                        "Calls ${state.settings.primaryContact?.name.orEmpty().ifBlank { "your first contact" }} if the countdown runs out."
                    },
                    icon = Icons.Outlined.Phone,
                    checked = state.settings.autoCallEmergency,
                    onCheckedChange = onAutoCallChange
                )
                Hairline()
                Way(
                    name = "Text as well",
                    state = if (state.settings.smsFallbackEnabled) LampState.LIVE else LampState.OFF,
                    stateLabel = if (state.settings.smsFallbackEnabled) "On" else "Off",
                    detail = "Sends contacts a message with the last known location. Works without Bluetooth.",
                    icon = Icons.Outlined.Sms,
                    checked = state.settings.smsFallbackEnabled,
                    onCheckedChange = onSmsFallbackChange
                )
            }
        }

        if (!state.settingsSynced) {
            item("sync-note") {
                Note(
                    text = if (state.linkLive) {
                        "Sending these settings to the device."
                    } else {
                        "Saved on this phone. They will reach the device the next time it connects."
                    }
                )
            }
        }

        if (locked) {
            item("locked") { GuardianLockPlate(mode = state.activeMode, subject = subject) }
        }

        item("people-heading") { SectionPlate(title = "Who gets called") }

        item("people") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Emergency contacts",
                    // No contacts is not a neutral empty state — it means the
                    // whole detection chain above ends nowhere.
                    state = if (contacts.isEmpty()) LampState.ATTENTION else LampState.LIVE,
                    stateLabel = if (contacts.isEmpty()) "None" else "${contacts.size}",
                    detail = contactsDetail(contacts),
                    icon = Icons.Outlined.Contacts,
                    onClick = onOpenContacts
                )
                Hairline()
                Way(
                    name = "Medical ID",
                    state = medicalLamp(state.medicalId),
                    stateLabel = medicalLabel(state.medicalId),
                    detail = "${state.medicalId.filledFieldCount} of 11 details filled in. This is what a responder reads on the device.",
                    icon = Icons.Outlined.LocalHospital,
                    onClick = onOpenMedicalId
                )
                Hairline()
                Way(
                    name = "Emergency card",
                    state = if (state.medicalId.isUsable) LampState.LIVE else LampState.OFF,
                    stateLabel = if (state.medicalId.isUsable) "Ready" else "Empty",
                    detail = "A QR code any phone camera can read. No app, no internet.",
                    icon = Icons.Outlined.QrCode2,
                    onClick = onOpenEmergencyCard
                )
            }
        }

        item("watch-heading") { SectionPlate(title = "Watching") }

        item("watch") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Safe zones",
                    state = safeZoneLamp(state.safeZoneCount, state.insideSafeZone),
                    stateLabel = safeZoneLabel(state.safeZoneCount, state.insideSafeZone),
                    detail = safeZoneDetail(state.safeZoneCount, state.insideSafeZone, subject),
                    icon = Icons.Outlined.Fence,
                    onClick = onOpenZones
                )
                Hairline()
                Way(
                    name = "Silent SOS",
                    state = if (state.silentSosEnabled) LampState.LIVE else LampState.OFF,
                    stateLabel = if (state.silentSosEnabled) "Armed" else "Off",
                    detail = "Raise an alert without a sound, and stage a call to leave a situation.",
                    icon = Icons.Outlined.VisibilityOff,
                    onClick = onOpenSilentSos
                )
                Hairline()
                Way(
                    name = "Trip log",
                    state = if (state.unresolvedTripCount > 0) LampState.TRIP else LampState.OFF,
                    stateLabel = if (state.unresolvedTripCount > 0) "Open" else "Clear",
                    detail = state.lastTripLabel ?: "Nothing recorded yet.",
                    icon = Icons.Outlined.History,
                    onClick = onOpenTrips
                )
            }
        }

        item("footer") {
            Note(
                text = "Fall detection is a help, not a guarantee. It can miss a fall and it can " +
                    "report one that did not happen. Nothing here replaces calling for help."
            )
        }
    }
}

/**
 * The notice that the wearable has handed these settings over.
 *
 * Gated on `PersonaMode.isGuardianLocked` rather than a list of mode names, so
 * a mode added on either side of the link cannot quietly slip past it.
 */
@Composable
private fun GuardianLockPlate(mode: PersonaMode, subject: String) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Seal()
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "${mode.label} mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "In this mode the device hides its own Safety menu, so $subject cannot " +
                    "turn any of this down while wearing it. These settings live here and only here.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted
            )
        }
    }
}

private fun contactsDetail(contacts: List<EmergencyContact>): String {
    if (contacts.isEmpty()) return "Add at least one. Without a contact, an alert has nowhere to go."
    val first = contacts.firstOrNull { it.isPrimary } ?: contacts.first()
    val others = contacts.size - 1
    return when (others) {
        0 -> "${first.name} is called first."
        1 -> "${first.name} is called first, then 1 other."
        else -> "${first.name} is called first, then $others others."
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

private fun safeZoneDetail(count: Int, inside: Boolean?, subject: String): String = when {
    count == 0 -> "Mark a home or a school and you will be told when the device leaves it."
    inside == null -> "$count set. Waiting for a location fix before it can tell you where $subject is."
    inside -> if (count == 1) "1 zone set. Inside it now." else "$count zones set. Inside one now."
    else -> "$count set. Outside all of them right now."
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
