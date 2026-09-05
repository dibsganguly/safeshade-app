package com.safeshade.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextAlign
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.shady.Shady
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.accentFor
import com.safeshade.ui.theme.board

/**
 * Every component in the kit on one screen.
 *
 * This exists to be looked at, on a real device, in both themes and at a
 * raised font scale, *before* any screen is built on top of the system. Six
 * screens written against an unverified kit means six screens to redo when the
 * kit turns out to be wrong; one gallery costs a few minutes.
 *
 * It is not shipped in the navigation graph — it is reachable only from the
 * developer screen, and it doubles as the reference a later contributor can
 * open to see what already exists before inventing a seventh kind of card.
 */
@Composable
fun KitGallery(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    var switchA by remember { mutableStateOf(true) }
    var switchB by remember { mutableStateOf(false) }
    // The gallery is the one place every control is live at once, which is how
    // a control that only looks right in a static preview gets caught.
    var chipValue by remember { mutableStateOf("Daughter") }
    var dialValue by remember { mutableFloatStateOf(0.6f) }
    var timeValue by remember { mutableIntStateOf(9 * 60) }
    var rangeStart by remember { mutableIntStateOf(22 * 60) }
    var rangeEnd by remember { mutableIntStateOf(7 * 60) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
        // Edge-to-edge means the list draws behind the status and navigation
        // bars, so the insets have to be added to the content padding rather
        // than clipped away - otherwise the first row sits under the clock.
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + Spacing.gutter,
            bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + Spacing.gutter
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item {
            Text(
                "Kit gallery",
                style = MaterialTheme.typography.displaySmall,
                color = colors.ink
            )
        }

        item { SectionPlate("Type") }
        item {
            BoardPlate {
                Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Display small", style = MaterialTheme.typography.displaySmall, color = colors.ink)
                    Text("Headline small", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                    Text("Title medium", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Text("Body medium — the quick brown fox jumps over the lazy dog.", style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                    Nameplate("Nameplate")
                    Nameplate("Nameplate small", small = true, muted = true)
                    Readout(label = "Readout", value = "-62 dBm · 84% · 1.02g")
                    Readout(value = "28", large = true)
                }
            }
        }

        item { SectionPlate("Pilot lamps") }
        item {
            BoardPlate {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LampState.entries.forEach { state ->
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            PilotLamp(state = state, size = 20.dp)
                            Spacer(Modifier.height(Spacing.sm))
                            Nameplate(state.name, small = true, muted = true)
                        }
                    }
                }
            }
        }

        item { SectionPlate("Ways") }
        item {
            BoardPlate {
                Way(
                    name = "Fall detection",
                    state = LampState.LIVE,
                    stateLabel = "Live",
                    detail = "High sensitivity",
                    icon = Icons.Outlined.Bolt,
                    checked = switchA,
                    onCheckedChange = { switchA = it }
                )
                Hairline()
                Way(
                    name = "SMS fallback",
                    state = LampState.OFF,
                    stateLabel = "Off",
                    icon = Icons.Outlined.Vibration,
                    checked = switchB,
                    onCheckedChange = { switchB = it }
                )
                Hairline()
                Way(
                    name = "Safe zone",
                    state = LampState.ATTENTION,
                    stateLabel = "Outside",
                    detail = "Left Home 8 minutes ago",
                    icon = Icons.Outlined.MyLocation,
                    onClick = {}
                )
                Hairline()
                Way(
                    name = "Adaptive mode",
                    state = LampState.LIVE,
                    stateLabel = "Elderly",
                    detail = "Managed by you — hidden on the device",
                    sealed = true,
                    onClick = {}
                )
                Hairline()
                Way(
                    name = "Screen contrast",
                    state = LampState.UNKNOWN,
                    stateLabel = "On device",
                    detail = "Change at Settings › Display",
                    deviceOnly = true
                )
            }
        }

        item { SectionPlate("Mains plate") }
        item {
            MainsPlate(
                state = LampState.LIVE,
                headline = "Baba is covered",
                subline = "SafeShade S1 · cane",
                batteryPercent = 84,
                signalDbm = -62
            )
        }

        item { SectionPlate("Gauges") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Gauge(label = "Temperature", value = "28", unit = "°C", caption = "Clear", modifier = Modifier.weight(1f))
                Gauge(label = "Heart rate", value = "72", unit = "bpm", caption = "Resting", stub = true, modifier = Modifier.weight(1f))
            }
        }

        item { SectionPlate("Buttons") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BoardButton("Test — ring device", onClick = {}, supporting = "Sounds the siren", modifier = Modifier.fillMaxWidth())
                BoardButton("Secondary", onClick = {}, weight = ButtonWeight.SECONDARY, modifier = Modifier.fillMaxWidth())
                BoardButton("Call now", onClick = {}, weight = ButtonWeight.DANGER, modifier = Modifier.fillMaxWidth())
                BoardButton("Disabled", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            }
        }

        item { SectionPlate("Shady") }
        item {
            BoardPlate {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShadyMood.entries.forEach { mood ->
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Shady(mood = mood, size = 44.dp)
                            // Full name, wrapped. Truncating to five characters
                            // produced "SEARC" / "CONCE", which reads as a
                            // clipping bug rather than a deliberate abbreviation.
                            Text(
                                text = mood.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkFaint,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        item { SectionPlate("Chips") }
        item {
            BoardPlate {
                Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Nameplate("Relationship", small = true, muted = true)
                    ChipRow(
                        options = RELATIONSHIP_SUGGESTIONS,
                        selected = chipValue,
                        onSelect = { chipValue = it }
                    )
                }
            }
        }

        item { SectionPlate("Controls") }
        item {
            DialControl(
                label = "Volume",
                value = dialValue,
                valueRange = 0f..1f,
                step = 0.1f,
                onValueChange = { dialValue = it },
                format = { "${(it * 100).toInt()}%" },
                advice = { "The line under a dial changes as it moves. That is the point of it." }
            )
        }
        item {
            TimeStrip(
                label = "Every day at",
                minutesOfDay = timeValue,
                onChange = { timeValue = it }
            )
        }
        item {
            RangeStrip(
                label = "Quiet from",
                startMinutes = rangeStart,
                endMinutes = rangeEnd,
                onChange = { start, end -> rangeStart = start; rangeEnd = end }
            )
        }

        item { SectionPlate("Icons") }
        item {
            // Every generated icon, at the size a row draws it. This is the
            // only place they can all be seen at once, and it is here because a
            // path that fails to parse renders as nothing at all rather than as
            // an error - so the check has to be visual.
            IconSheet()
        }

        item { SectionPlate("Empty bay") }
        item {
            EmptyBay(
                message = "No safe zones yet. Add one and you will be told when the device leaves it.",
                actionLabel = "Add a zone",
                onAction = {}
            )
        }
    }
}

/** The generated icon set, drawn at row size against the row tint. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconSheet() {
    val colors = MaterialTheme.board
    BoardPlate {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg)
        ) {
            allIcons.forEach { (name, vector) ->
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    modifier = Modifier.width(72.dp)
                ) {
                    Icon(
                        imageVector = vector,
                        contentDescription = null,
                        tint = colors.accentFor(name),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.inkFaint,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private val allIcons: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>> = listOf(
    "adaptive-mode" to SafeShadeIcons.AdaptiveMode,
    "call-after-a-fall" to SafeShadeIcons.CallAfterAFall,
    "check-in" to SafeShadeIcons.CheckIn,
    "connect-to-the-device" to SafeShadeIcons.ConnectToTheDevice,
    "cyber" to SafeShadeIcons.Cyber,
    "daily-reminder" to SafeShadeIcons.DailyReminder,
    "device-settings" to SafeShadeIcons.DeviceSettings,
    "emergency-contacts" to SafeShadeIcons.EmergencyContacts,
    "fall-detection" to SafeShadeIcons.FallDetection,
    "find-the-device" to SafeShadeIcons.FindTheDevice,
    "fire" to SafeShadeIcons.Fire,
    "gps" to SafeShadeIcons.Gps,
    "lights" to SafeShadeIcons.Lights,
    "medical-id" to SafeShadeIcons.MedicalId,
    "navbar-board" to SafeShadeIcons.NavbarBoard,
    "navbar-circle" to SafeShadeIcons.NavbarCircle,
    "navbar-device" to SafeShadeIcons.NavbarDevice,
    "ocean" to SafeShadeIcons.Ocean,
    "paired-devices" to SafeShadeIcons.PairedDevices,
    "parental-control" to SafeShadeIcons.ParentalControl,
    "police" to SafeShadeIcons.Police,
    "pulse" to SafeShadeIcons.Pulse,
    "quiet-hours" to SafeShadeIcons.QuietHours,
    "rainbow" to SafeShadeIcons.Rainbow,
    "reminders" to SafeShadeIcons.Reminders,
    "repeating-check-in" to SafeShadeIcons.RepeatingCheckIn,
    "ring-the-device" to SafeShadeIcons.RingTheDevice,
    "safe-zone" to SafeShadeIcons.SafeZone,
    "silent-sos" to SafeShadeIcons.SilentSos,
    "sim-and-sms" to SafeShadeIcons.SimAndSms,
    "sms-feedback-alert" to SafeShadeIcons.SmsFeedbackAlert,
    "telemetry" to SafeShadeIcons.Telemetry,
    "text-as-well" to SafeShadeIcons.TextAsWell,
    "top-right-settings" to SafeShadeIcons.TopRightSettings,
    "torch" to SafeShadeIcons.Torch
)

@Preview(name = "Kit — light", showBackground = true, heightDp = 1600)
@Composable
private fun KitGalleryLightPreview() {
    SafeShadeTheme(darkTheme = false) { KitGallery() }
}

@Preview(name = "Kit — dark", showBackground = true, heightDp = 1600)
@Composable
private fun KitGalleryDarkPreview() {
    SafeShadeTheme(darkTheme = true) { KitGallery() }
}
