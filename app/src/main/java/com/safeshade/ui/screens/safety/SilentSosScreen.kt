package com.safeshade.ui.screens.safety

import android.content.res.Configuration
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.board.WhyDisclosure
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the silent SOS screen draws. */
data class SilentSosUiState(
    val silentSosEnabled: Boolean = false,
    /**
     * The delay of the call that is currently staged, in seconds, or null when
     * nothing is staged. Kept as the scheduled delay rather than a countdown so
     * this screen never has to own a ticking clock.
     */
    val stagedCallSeconds: Int? = null,
    /** The name the staged call shows. Something ordinary works best. */
    val callerName: String = "Home",
    val hasContacts: Boolean = false,
    /** The stranger-danger quiet word, as stored. Blank means none. */
    val quietWord: String = "",
    /** Why the typed word is not accepted, or null. */
    val quietWordError: String? = null,
    /**
     * Whether the wearable can raise an alert without making a sound. False on
     * current firmware, which always shows and sounds an SOS.
     */
    val deviceSupportsSilentAlert: Boolean = false
)

/** The staged-call delays offered, in seconds. */
private val CallDelayChoices = listOf(10, 30, 60, 300)

/**
 * Two ways out of a situation you cannot talk your way out of.
 *
 * They are different tools and the screen keeps them apart:
 *
 *  - **Silent SOS** tells your contacts. It makes no sound, shows nothing, and
 *    leaves no trace on the screen a person standing next to you can see.
 *  - **A staged call** tells nobody. It gives you a reason to leave a room, a
 *    car, or a conversation without explaining yourself.
 *
 * The copy is plain to the point of bluntness because the person reading it is
 * making a safety judgement about their own situation, and a euphemism here
 * costs them accuracy. There is no reassurance in the wording and no promise
 * that either of these makes anybody safe.
 *
 * What changed is the length, not the honesty. Each section now opens with one
 * sentence and keeps the fuller account behind a disclosure — with one
 * deliberate exception: the warning that current firmware is *not* quiet about
 * an SOS stays in the open, because a switch that does not do what its name
 * says is the one thing on this screen nobody may be allowed to miss.
 */
@Composable
fun SilentSosScreen(
    state: SilentSosUiState,
    onBack: () -> Unit,
    onSilentSosChange: (Boolean) -> Unit,
    onStageCall: (Int) -> Unit,
    onQuietWordChange: (String) -> Unit = {},
    onCancelStagedCall: () -> Unit,
    onOpenContacts: () -> Unit,
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
        PanelHeader(
            title = "Silent SOS",
            subtitle = "For when being seen asking for help is the problem.",
            onBack = onBack
        )

        SectionPlate(title = "Alert without a sound")
        Spacer(Modifier.height(Spacing.sm))

        Note(text = "Hold the button on the device. Your contacts get your location.")
        Spacer(Modifier.height(Spacing.xs))
        WhyDisclosure(
            label = "What happens when you hold it",
            text = "Holding the button on the device sends every emergency contact your last " +
                "known location. There is no call to place and nothing to say out loud. " +
                "The device stays dark and quiet while it does it, so somebody standing " +
                "next to you sees you put a hand in your pocket and nothing more."
        )

        Spacer(Modifier.height(Spacing.md))

        // Whether the wearable's firmware honours the quiet path is recorded in
        // DeviceCapabilities.awaitingFirmware and the handoff, not on the
        // screen: the page reads as it will at launch.
        Box {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Silent SOS",
                    state = if (state.silentSosEnabled) LampState.LIVE else LampState.OFF,
                    stateLabel = if (state.silentSosEnabled) "Armed" else "Off",
                    detail = if (state.hasContacts) {
                        "Sends every emergency contact a message. No call, no siren."
                    } else {
                        "There are no contacts yet, so this would reach nobody."
                    },
                    checked = state.silentSosEnabled,
                    onCheckedChange = onSilentSosChange
                )
                if (!state.hasContacts) {
                    Hairline()
                    Way(
                        name = "Add a contact",
                        state = LampState.ATTENTION,
                        stateLabel = "None",
                        detail = "A silent alert with nobody to send it to does nothing at all.",
                        onClick = onOpenContacts
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Quiet word")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "A word that means trouble",
                state = if (state.quietWord.isNotBlank() && state.quietWordError == null) LampState.LIVE else LampState.OFF,
                stateLabel = if (state.quietWord.isNotBlank() && state.quietWordError == null) "Armed" else "Off",
                detail = if (state.quietWord.isNotBlank()) {
                    "A message from the wearable containing it raises a Quiet word alert here, without a sound on the wearable."
                } else {
                    "Something ordinary to say or type that tells you something is wrong. Nobody else knows it."
                },
                icon = SafeShadeIcons.ChatLock
            )
        }
        Spacer(Modifier.height(Spacing.md))
        PlateField(
            label = "The word",
            value = state.quietWord,
            onValueChange = onQuietWordChange,
            placeholder = "blue kettle",
            helper = "Three letters or more. Not a word that comes up on its own, like help or ok.",
            error = state.quietWordError,
            maxLength = 30
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Stage a call")
        Spacer(Modifier.height(Spacing.sm))

        Note(text = "Your phone rings after the delay you pick. Nobody is calling.")
        Spacer(Modifier.height(Spacing.xs))
        WhyDisclosure(
            label = "What a staged call is and is not",
            text = "Your phone rings after the delay you pick, as though somebody were " +
                "calling. Nobody is: there is no call and no connection, and it costs " +
                "nothing. It is a reason to stand up and leave a room, a car or a " +
                "conversation without explaining yourself – nothing more."
        )

        Spacer(Modifier.height(Spacing.md))

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            CallDelayChoices.forEachIndexed { index, seconds ->
                if (index > 0) Hairline()
                OptionWay(
                    name = formatDuration(seconds),
                    detail = delayDetail(seconds),
                    selected = state.stagedCallSeconds == seconds,
                    onSelect = { onStageCall(seconds) },
                    selectedLabel = "Staged",
                    unselectedLabel = "Not staged"
                )
            }
        }

        val staged = state.stagedCallSeconds
        if (staged != null) {
            Spacer(Modifier.height(Spacing.lg))
            BoardButton(
                label = "Cancel the staged call",
                supporting = "A call is set for ${formatDuration(staged)} from when you chose it.",
                icon = SafeShadeIcons.SilentSos,
                onClick = onCancelStagedCall,
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                DetailLine("Shows as", state.callerName)
                Spacer(Modifier.height(Spacing.xs))
                WhyDisclosure(
                    label = "Choosing a name",
                    text = "An ordinary name works better than an obvious excuse. The call " +
                        "rings with your normal ringtone, so leave your phone unmuted if you " +
                        "want it heard."
                )
            }
        }
    }
}

private fun delayDetail(seconds: Int): String = when (seconds) {
    10 -> "Almost immediate. For getting out of a conversation."
    30 -> "Enough time to put the phone away first."
    60 -> "A minute. Long enough that the timing does not look staged."
    else -> "Five minutes. For a meeting or a journey you want an exit from."
}

// ============================================
// PREVIEWS
// ============================================

@Preview(name = "Silent SOS — light", showBackground = true, heightDp = 1400)
@Composable
private fun SilentSosLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        SilentSosScreen(
            state = SilentSosUiState(
                silentSosEnabled = true,
                stagedCallSeconds = 60,
                callerName = "Home",
                hasContacts = true
            ),
            onBack = {}, onSilentSosChange = {}, onStageCall = {},
            onCancelStagedCall = {}, onOpenContacts = {}
        )
    }
}

@Preview(
    name = "Silent SOS — dark, no contacts",
    showBackground = true,
    heightDp = 1400,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SilentSosDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        SilentSosScreen(
            state = SilentSosUiState(),
            onBack = {}, onSilentSosChange = {}, onStageCall = {},
            onCancelStagedCall = {}, onOpenContacts = {}
        )
    }
}
