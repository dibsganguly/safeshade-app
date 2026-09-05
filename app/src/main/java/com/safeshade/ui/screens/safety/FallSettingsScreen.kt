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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Seal
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlin.math.abs

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

/** The countdown lengths offered. Long enough to react, short enough to matter. */
private val CountdownChoices = listOf(15, 30, 45, 60)

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

        if (locked) {
            Spacer(Modifier.height(Spacing.lg))
            LockPlate(mode = mode, subject = subject)
        }

        if (!state.settingsSynced) {
            Spacer(Modifier.height(Spacing.lg))
            Note(
                text = if (state.linkLive) {
                    "Sending these settings to the device."
                } else {
                    "Saved on this phone. They reach the device the next time it connects."
                }
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Sensitivity")
        Spacer(Modifier.height(Spacing.sm))

        if (fallDetectionOff) {
            Note(
                text = "Fall detection is off in ${mode.label} mode. An animal's normal " +
                    "movement crosses every impact threshold, so leaving it on would mean an " +
                    "alert every few minutes. The settings below still apply to the SOS button."
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

        Spacer(Modifier.height(Spacing.md))
        Note(text = sensitivityFootnote(mode))

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
                icon = Icons.Outlined.Phone,
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

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            CountdownChoices.forEachIndexed { index, seconds ->
                if (index > 0) Hairline()
                OptionWay(
                    name = formatDuration(seconds),
                    detail = countdownDetail(seconds),
                    selected = settings.fallCountdownSeconds == seconds,
                    onSelect = { onCountdownChange(seconds) }
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Note(
            text = "Two waits, one after the other. The device holds a detected fall for five " +
                "seconds first — a double-click on the device in that time cancels it and nobody " +
                "is told. Only then does this countdown start on the phone."
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Siren")
        Spacer(Modifier.height(Spacing.sm))

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            VolumeChoices.forEachIndexed { index, (level, name, blurb) ->
                if (index > 0) Hairline()
                OptionWay(
                    name = name,
                    detail = blurb,
                    // Floats that have been through a percentage round-trip to
                    // the device rarely come back exactly equal.
                    selected = abs(settings.sosVolumeLevel - level) < 0.05f,
                    onSelect = { onSosVolumeChange(level) }
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Note(
            text = "The siren is what brings a person who is nearby. It sounds on the device " +
                "during an SOS and after an unanswered fall."
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "If Bluetooth is down")
        Spacer(Modifier.height(Spacing.sm))

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Send a text as well",
                state = if (settings.smsFallbackEnabled) LampState.LIVE else LampState.OFF,
                stateLabel = if (settings.smsFallbackEnabled) "On" else "Off",
                detail = "Texts every contact with the last known location.",
                icon = Icons.Outlined.Sms,
                checked = settings.smsFallbackEnabled,
                onCheckedChange = onSmsFallbackChange
            )
        }

        Spacer(Modifier.height(Spacing.md))
        Note(
            text = "This needs permission to send messages, and it may cost whatever your " +
                "operator charges for an SMS. It is the only path that still works when the " +
                "phone and the device are out of range of each other."
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Parental controls")
        Spacer(Modifier.height(Spacing.sm))

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Require a PIN on the device",
                state = if (settings.parentalControlsEnabled) LampState.LIVE else LampState.OFF,
                stateLabel = if (settings.parentalControlsEnabled) "On" else "Off",
                detail = "Stops these settings being changed on the device itself.",
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
                helper = "Digits only. Four to six of them.",
                error = state.pinError,
                maxLength = 6,
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            )
            Spacer(Modifier.height(Spacing.sm))
            // Shown rather than masked deliberately: this is the guardian's own
            // phone, they are the person who needs to remember the number, and
            // a PIN they cannot check is a PIN they will lock themselves out
            // with. It is not a credential — it guards a settings menu.
            Note(
                text = "Keep this somewhere you will find it. There is no way to read it back " +
                    "off the device."
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        Note(
            text = "Fall detection uses movement, so it can miss a slow slide to the floor and " +
                "it can call a dropped bag a fall. Treat it as a second pair of eyes, not a " +
                "guarantee."
        )
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
