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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.device.DeviceCapabilities
import com.safeshade.device.DeviceSetting
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * The settings that exist only on the wearable.
 *
 * The list comes from [DeviceCapabilities.deviceOnly] rather than being retyped
 * here, so that the day firmware grows an EXT tag for one of these, moving it
 * to the synced list makes the row disappear from this screen without anyone
 * having to remember that this screen existed.
 */
data class DeviceOnlyUiState(
    val settings: List<DeviceSetting> = DeviceCapabilities.deviceOnly,
    /** Shown so a guardian knows whether the device is even to hand. */
    val deviceName: String = "SafeShade S1"
)

/**
 * Settings on the device itself.
 *
 * Eight settings in the current firmware are reachable from the wearable's own
 * menu and persisted to its NVS, and have no characteristic or EXT tag behind
 * them. An app that drew a switch for one of those would look like it worked
 * and do nothing at all — there is no write to fail, so there is not even an
 * error to report.
 *
 * So these are rows with no control. Each one says exactly where it lives on
 * the wearable, because the useful thing this screen can do is shorten the walk
 * to the device rather than pretend the walk is unnecessary.
 */
@Composable
fun DeviceOnlyScreen(
    state: DeviceOnlyUiState,
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
            Column {
                Text(
                    text = "Set on the device",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.ink
                )
                Text(
                    // Stated as a limitation of this firmware, not as a feature
                    // in progress. "Coming soon" would be a guess; this is a
                    // fact about the build on the wearable.
                    text = "These eight settings live in the wearable's own menu " +
                        "and have no Bluetooth path in this firmware, so this app " +
                        "cannot change them. Each row says where to find it on " +
                        "the device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
            }
        }

        item("heading") { SectionPlate(title = state.deviceName) }

        item("rows") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                state.settings.forEachIndexed { index, setting ->
                    if (index > 0) Hairline()
                    Way(
                        name = setting.label,
                        // UNKNOWN, not OFF. The app has never read these values
                        // and never will on this firmware, so claiming a state
                        // for them would be an invention.
                        state = LampState.UNKNOWN,
                        stateLabel = "On device",
                        detail = setting.deviceMenuPath,
                        icon = iconFor(setting.key),
                        // `deviceOnly` suppresses any switch and adds "change
                        // this on the device" to the spoken row, so the copy
                        // here does not repeat it.
                        deviceOnly = true
                    )
                }
            }
        }

        // Do Not Disturb is the one row where the plain statement above is not
        // quite true, and the gap is exactly the kind that produces a bug
        // report: someone changes the quiet window in the app, sees the device
        // still chiming, and concludes the whole feature is broken.
        item("dnd-note") {
            BoardPlate(modifier = Modifier.fillMaxWidth(), recessed = true) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Nameplate("About Do Not Disturb", small = true, muted = true)
                    Text(
                        text = "Do Not Disturb is split across the two ends. Turning " +
                            "it on or off is device-only, but the start and end " +
                            "hours it uses do travel from this app — they are the " +
                            "quiet hours window in device settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted
                    )
                }
            }
        }

        item("footnote") {
            Text(
                text = "If a later firmware adds a Bluetooth path for one of these, " +
                    "it moves into device settings and stops appearing here.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
        }
    }
}

/**
 * An icon per device-only key.
 *
 * Keyed off the string rather than the position in the list, so reordering
 * `DeviceCapabilities.deviceOnly` cannot silently shuffle the icons onto the
 * wrong rows. An unrecognised key falls back to a neutral mark rather than
 * throwing — a new firmware setting should appear as a row, not as a crash.
 */
private fun iconFor(key: String): ImageVector = when (key) {
    "ledsEnabled" -> Icons.Outlined.Lightbulb
    "displayContrast" -> Icons.Outlined.Tune
    "flipDisplay" -> Icons.Outlined.ScreenRotation
    "homeTicker" -> Icons.Outlined.Info
    "masterVolume" -> Icons.Outlined.VolumeUp
    "bootChime" -> Icons.Outlined.PowerSettingsNew
    "notifChime" -> Icons.Outlined.Notifications
    "dndEnabled" -> Icons.Outlined.Bedtime
    else -> Icons.Outlined.Tune
}

// ============================================================================
// Previews
// ============================================================================

@Preview(name = "Device-only · light", showBackground = true)
@Composable
private fun DeviceOnlyPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            DeviceOnlyScreen(state = DeviceOnlyUiState(deviceName = "Baba's cane"))
        }
    }
}

@Preview(name = "Device-only · dark", showBackground = true)
@Composable
private fun DeviceOnlyPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            DeviceOnlyScreen(state = DeviceOnlyUiState(deviceName = "Baba's cane"))
        }
    }
}

@Preview(name = "Device-only · large text", showBackground = true, fontScale = 1.3f)
@Composable
private fun DeviceOnlyPreviewLargeText() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            DeviceOnlyScreen(state = DeviceOnlyUiState())
        }
    }
}
