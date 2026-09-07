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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the privacy page draws. */
data class PrivacyUiState(
    val signedIn: Boolean = false,
    /** Whether alert places are pushed with the alert. Null until the cloud state has loaded. */
    val sharePlaces: Boolean? = null,
    /** Whether the wearable's SIM number is stored on this phone. */
    val simStored: Boolean = false,
    val zoneCount: Int = 0,
    val voiceNoteCount: Int = 0
)

/**
 * Privacy: what this phone knows, where each thing goes, and the one
 * switch that changes it.
 *
 * Facts, not reassurance. Every row names a record and says where it
 * lives, in a form that can be checked against the code. The one control
 * is the community heat map: whether an alert's place goes up with the
 * alert. Off withholds the place and the alert still syncs, so the Circle's
 * trip logs agree; places already sent stay sent, and the row says so.
 */
@Composable
fun PrivacyScreen(
    state: PrivacyUiState,
    onSharePlacesChange: (Boolean) -> Unit,
    onOpenAccount: () -> Unit,
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
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        ScreenHeader(
            title = "Privacy",
            subtitle = "What this phone knows, and where each thing goes",
            onBack = onBack
        )

        SectionPlate(title = "The community map")
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            val share = state.sharePlaces
            Way(
                name = "Share where alerts happen",
                state = when {
                    !state.signedIn -> LampState.OFF
                    share == null -> LampState.UNKNOWN
                    share -> LampState.LIVE
                    else -> LampState.OFF
                },
                stateLabel = when {
                    !state.signedIn -> "Off"
                    share == null -> "…"
                    share -> "On"
                    else -> "Off"
                },
                detail = when {
                    !state.signedIn -> "Nothing leaves this phone while you are signed out"
                    share == true -> "An alert's place goes up with it, rounded to about a kilometre and only drawn once five alerts share a cell"
                    else -> "Alerts still sync, without their place. Places sent before this was turned off stay sent."
                },
                icon = SafeShadeIcons.RadarBroadcast,
                checked = share == true,
                onCheckedChange = if (state.signedIn && share != null) onSharePlacesChange else null,
                // The whole row is the target, as on every other rocker row.
                onClick = if (state.signedIn && share != null) { { onSharePlacesChange(!share) } } else null
            )
        }
        Text(
            text = "The map never shows a single alert, a name, or a time. It shows cells where enough alerts happened to be worth knowing about.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted
        )

        SectionPlate(title = "On this phone only")
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "The parental PIN",
                state = LampState.OFF,
                stateLabel = "Local",
                detail = "Stored here, checked here, never copied",
                icon = SafeShadeIcons.ParentalControl
            )
            Hairline()
            Way(
                name = "The wearable's SIM number",
                state = LampState.OFF,
                stateLabel = if (state.simStored) "Stored" else "None",
                detail = "Used for SMS from this phone. Not copied to the cloud.",
                icon = SafeShadeIcons.SimAndSms
            )
            Hairline()
            Way(
                name = "The SMS allowlist",
                state = LampState.OFF,
                stateLabel = "Local",
                detail = "Which numbers may text the wearable through this phone",
                icon = SafeShadeIcons.TextAsWell
            )
            Hairline()
            Way(
                name = "Photos used as faces",
                state = LampState.OFF,
                stateLabel = "Local",
                detail = "Copied into the app's own folder on this phone and nowhere else",
                icon = SafeShadeIcons.ImageAdd
            )
        }

        SectionPlate(title = "Copied to the Circle when signed in")
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "People, medical IDs, contacts",
                state = if (state.signedIn) LampState.LIVE else LampState.OFF,
                stateLabel = if (state.signedIn) "Synced" else "Held",
                detail = "So every guardian's phone shows the same board",
                icon = SafeShadeIcons.MedicalId
            )
            Hairline()
            Way(
                name = "Alerts and messages",
                state = if (state.signedIn) LampState.LIVE else LampState.OFF,
                stateLabel = if (state.signedIn) "Synced" else "Held",
                detail = "The trip log and the thread, row-locked to the Circle on the server",
                icon = SafeShadeIcons.Alert02
            )
            Hairline()
            Way(
                name = "Safe zones",
                state = if (state.signedIn) LampState.LIVE else LampState.OFF,
                stateLabel = if (state.zoneCount > 0) "${state.zoneCount}" else "None",
                detail = "Their centres and radii, so the Circle shares one set",
                icon = SafeShadeIcons.SafeZone
            )
            Hairline()
            Way(
                name = "Voice notes",
                state = if (state.signedIn) LampState.LIVE else LampState.OFF,
                stateLabel = if (state.voiceNoteCount > 0) "${state.voiceNoteCount}" else "None",
                detail = "The recording goes to a private bucket only the Circle can open",
                icon = SafeShadeIcons.Microphone
            )
        }

        SectionPlate(title = "Location")
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "This phone's location",
                state = LampState.OFF,
                stateLabel = "Local",
                detail = "Used for the weather sent to the wearable, safe zones and nearby services. Sent up only as an alert's place, and only with the switch above on.",
                icon = SafeShadeIcons.Gps
            )
            Hairline()
            Way(
                name = "The wearable's location",
                state = LampState.OFF,
                stateLabel = "Local",
                detail = "Comes over Bluetooth to this phone. It reaches the Circle only inside an alert.",
                icon = SafeShadeIcons.PinLocation
            )
        }

        SectionPlate(title = "The account")
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Where it lives, and how to delete it",
                state = LampState.OFF,
                stateLabel = "Account",
                detail = "Supabase, Singapore. Deleting the account removes every row and every recording.",
                icon = SafeShadeIcons.UserCircle,
                onClick = onOpenAccount
            )
        }
    }
}

@Preview(name = "Privacy", showBackground = true, heightDp = 1800)
@Composable
private fun PrivacyPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            PrivacyScreen(
                state = PrivacyUiState(signedIn = true, sharePlaces = true, simStored = true, zoneCount = 1, voiceNoteCount = 2),
                onSharePlacesChange = {}, onOpenAccount = {}, onBack = {}
            )
        }
    }
}
