package com.safeshade.ui.screens.safety

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.FallSensitivity
import com.safeshade.data.PersonaMode
import com.safeshade.data.SafetySettings
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.DialControl
import com.safeshade.ui.board.ExpandableSection
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Seal
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.board.WhyDisclosure
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlin.math.abs
import kotlin.math.roundToInt

/** Everything the fall-settings screen draws. */
data class FallSettingsUiState(
    val settings: SafetySettings = SafetySettings(),
    val activeMode: PersonaMode = PersonaMode.AUTO,
    val role: UserRole = UserRole.GUARDIAN,
    val wearerName: String = "",
    val linkLive: Boolean = false,
    /** False while a change is still on its way to the device. */
    val settingsSynced: Boolean = true,
    /** Shown under the PIN field when the entered PIN is not usable yet. */
    val pinError: String? = null
)

/**
 * The siren levels offered.
 *
 * A slider was the obvious control and is the wrong one. Material's slider
 * thumb draws with elevation — a shadow, on a surface that has none — and a
 * continuous value invites fiddling with a setting that has three meaningful
 * positions. Three named steps also give a screen reader something to say
 * other than a percentage.
 */
private val VolumeChoices = listOf(
    Triple(0.4f, "Quiet", "Audible in a room. Will not carry down a corridor."),
    Triple(0.7f, "Normal", "Loud enough to bring someone from the next room."),
    Triple(1.0f, "Loud", "As loud as the device goes. Hard to ignore, hard to sleep through.")
)

/**
 * Fall detection, and everything that follows a fall.
 *
 * This screen is where the two clocks in this product have to be kept apart,
 * because conflating them is how people end up expecting the wrong thing:
 *
 *  - The **device** holds a fall for five seconds before it tells anyone. A
 *    double-click on the wearable inside that window cancels it entirely, and
 *    nothing leaves the device.
 *  - The **phone** then runs its own countdown before it places a call. That
 *    is the number set here.
 *
 * The sensitivity choices are the firmware's real impact thresholds, not
 * marketing tiers, which is why the copy can be specific about what changes.
 *
 * Fifteen controls do not all deserve the same weight. Sensitivity, whether a
 * contact is called, and how long the countdown runs are the three a guardian
 * comes back to — usually the morning after a false alarm — so they stay in
 * the open. The siren level, the text fallback and the parental PIN are set
 * during setup and then left alone for months, so they sit behind two
 * labelled, counted disclosures. Nothing was removed: the trade is one tap on
 * the rare settings against a screen where the common ones are visible without
 * scrolling.
 *
 * One thing deliberately did *not* go behind a disclosure. Elderly mode raises
 * a second alert of its own after three seconds of stillness, and nothing else
 * in the app says so — a behaviour a guardian will experience is not an
 * explanation they can choose to skip, so that line stays in the open while
 * the mechanism behind it does not.
 */
@Composable
fun FallSettingsScreen(
    state: FallSettingsUiState,
    onBack: () -> Unit,
    onSensitivityChange: (FallSensitivity) -> Unit,
    onAutoCallChange: (Boolean) -> Unit,
    onCountdownChange: (Int) -> Unit,
    onSosVolumeChange: (Float) -> Unit,
    onSmsFallbackChange: (Boolean) -> Unit,
    onParentalControlsChange: (Boolean) -> Unit,
    onPinChange: (String) -> Unit,
    onOpenContacts: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val settings = state.settings
    val mode = state.activeMode
    val locked = mode.isGuardianLocked
    val subject = state.wearerName.ifBlank {
        if (state.role == UserRole.GUARDIAN) "the wearer" else "you"
    }
    val fallDetectionOff = mode == PersonaMode.PET

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
            title = "Fall detection",
            subtitle = "What the device watches for, and what happens next.",
            onBack = onBack
        )

        // PanelHeader already places the full header gap, so neither branch
        // below may open with a Spacer of its own — whichever one renders
        // first would otherwise double up. The Spacer stays only between
        // LockPlate and the sync note, and only when both are on screen.
        val syncNoteText = if (state.linkLive) {
            "Sending these settings to the device."
        } else {
            "Saved on this phone. They reach the device the next time it connects."
        }
        // Tracks whether LockPlate or the sync note rendered above, because
        // that decides what the xl Spacer below is doing: a separator after
        // real content, or a stray third gap stacked on the header's own
        // 20dp when neither one appears and Sensitivity is the first thing
        // on the screen.
        var contentAboveSensitivity = false
        if (locked) {
            LockPlate(mode = mode, subject = subject)
            contentAboveSensitivity = true
            if (!state.settingsSynced) {
                Spacer(Modifier.height(Spacing.lg))
                Note(text = syncNoteText)
            }
        } else if (!state.settingsSynced) {
            Note(text = syncNoteText)
            contentAboveSensitivity = true
        }

        if (contentAboveSensitivity) {
            Spacer(Modifier.height(Spacing.xl))
        }
        SectionPlate(title = "Sensitivity")
        Spacer(Modifier.height(Spacing.sm))

        if (fallDetectionOff) {
            // The second half of this line is not a detail. Somebody who reads
            // only "off in Pet mode" reasonably concludes the whole screen is
            // dead and leaves, when in fact everything below it still fires on
            // a held SOS button — so both facts stay in the open together.
            Note(
                text = "Off in ${mode.label} mode. Everything below still applies to the SOS button."
            )
            Spacer(Modifier.height(Spacing.xs))
            WhyDisclosure(
                label = "Why it is off in ${mode.label} mode",
                text = "An animal's normal movement crosses every impact threshold, so leaving " +
                    "fall detection on would mean an alert every few minutes. The countdown, " +
                    "who gets called and the siren all still run when the button on the device " +
                    "is held."
            )
            Spacer(Modifier.height(Spacing.md))
        }

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            FallSensitivity.entries.forEachIndexed { index, level ->
                if (index > 0) Hairline()
                OptionWay(
                    name = level.label,
                    detail = sensitivityDetail(level, mode),
                    selected = settings.fallSensitivity == level,
                    onSelect = { onSensitivityChange(level) }
                )
            }
        }

        // Elderly mode raises an alert nobody chose here and nothing else in
        // the app mentions. That is a behaviour, not an explanation of one, so
        // it stays in the open even though the mechanism behind it does not —
        // a guardian who is going to receive a second alert should not have to
        // open a disclosure to find out it exists.
        if (mode == PersonaMode.ELDERLY) {
            Spacer(Modifier.height(Spacing.sm))
            Note(text = "It also alerts a second time if nobody moves after an impact.")
        }

        // Each option already carries its own consequence as a `detail`, so
        // what is left here is the mechanism behind all three — real, worth
        // reading once, and not worth a paragraph above the choice every time.
        Spacer(Modifier.height(Spacing.sm))
        WhyDisclosure(
            label = "How the device decides",
            text = sensitivityFootnote(mode)
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "After a fall")
        Spacer(Modifier.height(Spacing.sm))

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Call a contact",
                state = if (settings.autoCallEmergency) LampState.LIVE else LampState.OFF,
                stateLabel = if (settings.autoCallEmergency) "On" else "Off",
                detail = if (settings.emergencyContacts.isEmpty()) {
                    "No contacts yet, so nothing would be called."
                } else {
                    "Calls ${settings.primaryContact?.name.orEmpty().ifBlank { "the first contact" }} when the countdown ends."
                },
                icon = SafeShadeIcons.CallAfterAFall,
                checked = settings.autoCallEmergency,
                onCheckedChange = onAutoCallChange
            )
            if (settings.emergencyContacts.isEmpty()) {
                Hairline()
                Way(
                    name = "Add a contact",
                    state = LampState.ATTENTION,
                    stateLabel = "None",
                    detail = "An alert with nobody to send it to stops at the device.",
                    onClick = onOpenContacts
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        SectionPlate(title = "Countdown")
        Spacer(Modifier.height(Spacing.sm))

        // A slider, not the four-row list this used to be. `onCountdownChange`
        // is the one callback there is for this value — there is no separate
        // cheap-vs-committed pair the way the siren volume gets — so dragging
        // is tracked in this local draft and only pushed out once, in
        // `onCommit`, on release. Resyncs from `settings.fallCountdownSeconds`
        // whenever that changes out from under the drag (a fresh device sync,
        // a different persona's settings loading in).
        var countdownDraft by remember(settings.fallCountdownSeconds) {
            mutableFloatStateOf(settings.fallCountdownSeconds.toFloat())
        }
        DialControl(
            label = "Countdown",
            value = countdownDraft,
            valueRange = 15f..60f,
            step = 15f,
            onValueChange = { countdownDraft = it },
            onCommit = { onCountdownChange(countdownDraft.roundToInt()) },
            unit = "seconds",
            advice = { countdownDetail(it.roundToInt()) }
        )

        Spacer(Modifier.height(Spacing.sm))
        Note(text = "The device waits five seconds of its own before this starts.")
        Spacer(Modifier.height(Spacing.xs))
        WhyDisclosure(
            label = "Why there are two countdowns",
            text = "Two waits, one after the other. The device holds a detected fall for five " +
                "seconds first – a double-click on the device in that time cancels it and " +
                "nobody is told. Only then does this countdown start on the phone. Conflating " +
                "the two is how people end up expecting a call sooner, or later, than it comes."
        )

        // Everything from here down is set once, during setup, and then not
        // looked at again for months. It is not less important — the siren is
        // what brings a neighbour, and the text fallback is the only path that
        // survives a dead Bluetooth link — but it is not what anybody came to
        // this screen to change, and at equal weight it buried the two things
        // that are. The section header's own count says how much is inside.
        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Set once")
        Spacer(Modifier.height(Spacing.sm))

        // One disclosure, not two. The siren level and the text fallback were
        // separate sections and could have stayed separate, but a header
        // reading "1" is a tap charged for nothing — a group has to be worth
        // opening before hiding it is a favour to anybody.
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            ExpandableSection(
                label = "How the alert is heard",
                count = VolumeChoices.size + 1
            ) {
                Hairline()
                VolumeChoices.forEachIndexed { index, (level, name, blurb) ->
                    if (index > 0) Hairline()
                    OptionWay(
                        name = name,
                        detail = blurb,
                        // Floats that have been through a percentage round-trip
                        // to the device rarely come back exactly equal.
                        selected = abs(settings.sosVolumeLevel - level) < 0.05f,
                        onSelect = { onSosVolumeChange(level) }
                    )
                }
                Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                    Note(
                        text = "The siren is what brings a person who is nearby. It sounds on " +
                            "the device during an SOS and after an unanswered fall."
                    )
                }
                Hairline()
                Way(
                    name = "Send a text as well",
                    state = if (settings.smsFallbackEnabled) LampState.LIVE else LampState.OFF,
                    stateLabel = if (settings.smsFallbackEnabled) "On" else "Off",
                    detail = "Texts every contact with the last known location.",
                    icon = SafeShadeIcons.TextAsWell,
                    checked = settings.smsFallbackEnabled,
                    onCheckedChange = onSmsFallbackChange
                )
                Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                    Note(
                        text = "This needs permission to send messages, and it may cost " +
                            "whatever your operator charges for an SMS. It is the only path " +
                            "that still works when the phone and the device are out of range " +
                            "of each other."
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // Opens itself when the PIN is already on, because at that point the
        // reason to come back here is to check or change the number, and a
        // guardian hunting for a PIN field behind a closed header is exactly
        // the cost this pattern is supposed to avoid.
        ExpandableSection(
            label = "Parental controls",
            count = if (settings.parentalControlsEnabled) 2 else 1,
            initiallyOpen = settings.parentalControlsEnabled
        ) {
            Spacer(Modifier.height(Spacing.xs))
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Require a PIN on the device",
                    state = if (settings.parentalControlsEnabled) LampState.LIVE else LampState.OFF,
                    stateLabel = if (settings.parentalControlsEnabled) "On" else "Off",
                    checked = settings.parentalControlsEnabled,
                    onCheckedChange = onParentalControlsChange
                )
            }

            if (settings.parentalControlsEnabled) {
                Spacer(Modifier.height(Spacing.lg))
                PlateField(
                    label = "PIN",
                    value = settings.parentalPin,
                    onValueChange = { onPinChange(it.filter(Char::isDigit)) },
                    placeholder = "4 digits",
                    error = state.pinError,
                    maxLength = 6,
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                )
                Spacer(Modifier.height(Spacing.sm))
                // Shown rather than masked deliberately: this is the guardian's
                // own phone, they are the person who needs to remember the
                // number, and a PIN they cannot check is a PIN they will lock
                // themselves out with. It is not a credential — it guards a
                // settings menu.
                Note(
                    text = "Keep this somewhere you will find it. There is no way to read it " +
                        "back off the device."
                )
            }
        }
    }
}

/**
 * The seal notice.
 *
 * Reads off `isGuardianLocked` rather than naming the four modes, so a mode
 * added later cannot slip past this by not being on a list.
 */
@Composable
private fun LockPlate(mode: PersonaMode, subject: String) {
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
                text = "The device hides its own Safety menu in this mode, so $subject cannot " +
                    "weaken any of this while wearing it. This screen is the only place these " +
                    "settings can be changed.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted
            )
        }
    }
}

/**
 * What each sensitivity actually does.
 *
 * The numbers are the firmware's `adjustedImpactThresh` values (70000 / 50000 /
 * 35000 on the summed accelerometer delta). They are not printed — a raw
 * threshold means nothing to a reader — but they are what the wording below is
 * describing, so it stays true if the firmware is retuned.
 */
private fun sensitivityDetail(level: FallSensitivity, mode: PersonaMode): String = when (level) {
    FallSensitivity.LOW ->
        "Only a hard impact. Fewest false alarms, and the most likely to miss a gentle fall."
    FallSensitivity.MEDIUM ->
        "A normal fall onto a floor. The setting most people should leave alone."
    FallSensitivity.HIGH -> if (mode == PersonaMode.BIKE || mode == PersonaMode.HELMET) {
        "The most sensitive setting, but ${mode.label} mode holds a higher floor regardless."
    } else {
        "Catches lighter falls. Expect the occasional alert from a dropped bag."
    }
}

private fun sensitivityFootnote(mode: PersonaMode): String = when (mode) {
    // Bike and Helmet apply a minimum impact threshold underneath the user's
    // choice — road vibration and worksite knocks would otherwise trip High
    // constantly — so High genuinely does not mean what it means elsewhere.
    PersonaMode.BIKE, PersonaMode.HELMET ->
        "${mode.label} mode ignores anything below a crash-sized impact even on High, and looks " +
            "for the rotation of a real crash alongside it. That floor is deliberate: road " +
            "vibration would otherwise set it off all day."
    PersonaMode.ELDERLY ->
        "Elderly mode also watches for stillness for three seconds after an impact. If there is " +
            "no movement at all in that time, it sends a second alert on its own."
    else ->
        "The device checks roughly fifty times a second and compares the jolt against this " +
            "threshold."
}

private fun countdownDetail(seconds: Int): String = when (seconds) {
    15 -> "Quickest. Little room to cancel a false alarm."
    30 -> "The default. Enough time to get up and press the button."
    45 -> "Room to recover from a knock without a call going out."
    else -> "Longest. Best where false alarms are common."
}

// ============================================
// PREVIEWS
// ============================================

private val previewSettings = SafetySettings(
    parentalControlsEnabled = true,
    parentalPin = "4821",
    autoCallEmergency = true,
    fallSensitivity = FallSensitivity.HIGH,
    sosVolumeLevel = 0.7f,
    smsFallbackEnabled = true,
    fallCountdownSeconds = 30
)

@Preview(name = "Fall settings — light, elderly", showBackground = true, heightDp = 2000)
@Composable
private fun FallSettingsLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        FallSettingsScreen(
            state = FallSettingsUiState(
                settings = previewSettings,
                activeMode = PersonaMode.ELDERLY,
                wearerName = "Baba",
                linkLive = true,
                settingsSynced = false
            ),
            onBack = {}, onSensitivityChange = {}, onAutoCallChange = {},
            onCountdownChange = {}, onSosVolumeChange = {}, onSmsFallbackChange = {},
            onParentalControlsChange = {}, onPinChange = {}, onOpenContacts = {}
        )
    }
}

@Preview(
    name = "Fall settings — dark, bike",
    showBackground = true,
    heightDp = 2000,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun FallSettingsDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        FallSettingsScreen(
            state = FallSettingsUiState(
                settings = SafetySettings(
                    autoCallEmergency = false,
                    fallSensitivity = FallSensitivity.HIGH,
                    sosVolumeLevel = 1.0f,
                    fallCountdownSeconds = 15
                ),
                activeMode = PersonaMode.BIKE,
                role = UserRole.COMPANION
            ),
            onBack = {}, onSensitivityChange = {}, onAutoCallChange = {},
            onCountdownChange = {}, onSosVolumeChange = {}, onSmsFallbackChange = {},
            onParentalControlsChange = {}, onPinChange = {}, onOpenContacts = {}
        )
    }
}
