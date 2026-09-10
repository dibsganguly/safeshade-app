package com.safeshade.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.Hub
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.accentFor
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * The members adopted from the v2.0 candidates in v2.8.0, each live, shown in
 * the gallery under "The kit as shipped" so the shipped form (not the
 * candidate's mock) is what gets photographed in both themes and at 1.3x.
 */
fun LazyListScope.kitAdopted() {
    item { SectionPlate("Cards") }
    item { AdoptedCards() }
    item { SectionPlate("Actions") }
    item { AdoptedActions() }
    item { SectionPlate("Explainers") }
    item { AdoptedExplainers() }
    item { SectionPlate("Channels") }
    item { AdoptedChannels() }
    item { SectionPlate("Rows and tiles") }
    item { AdoptedRows() }
    item { SectionPlate("Instruments") }
    item { AdoptedInstruments() }
}

@Composable
private fun AdoptedCards() {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        BoardPlate(hub = Hub.SAFETY, modifier = Modifier.fillMaxWidth()) {
            BankHeader("Safe zones", count = 2, actionIcon = SafeShadeIcons.PlusAdd, actionDescription = "Add a zone", onAction = {})
            Way(name = "Home", state = LampState.LIVE, stateLabel = "Inside", detail = "200 m · told when leaving", icon = SafeShadeIcons.FavouritePlace, onClick = {})
            Hairline()
            Way(name = "Park", state = LampState.OFF, stateLabel = "Away", detail = "500 m · told when entering", icon = SafeShadeIcons.AddNewPlace, onClick = {})
        }
        TitledPlate("What the microphone heard") {
            Way(name = "Test recording", state = LampState.OFF, stateLabel = "On this phone", detail = "10 s · 43 KB", icon = SafeShadeIcons.Microphone, onClick = {})
            Hairline()
            Way(name = "Sound level", state = LampState.LIVE, stateLabel = "53 dB", icon = SafeShadeIcons.AudioWave)
        }
        TaggedPlate("Sealed") {
            Column(Modifier.padding(Spacing.lg)) {
                Text("Elderly mode", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                Text("Hides the wearable's own Safety menu.", style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            WatermarkPlate(SafeShadeIcons.WalkingPerson, tint = colors.accentFor("Journey"), modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(Spacing.md)) {
                    Text("Journey", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Text("To Home · 20 min", style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
                }
            }
            WatermarkPlate(SafeShadeIcons.FallDetection, tint = watermarkTint(LampState.TRIP), modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(Spacing.md)) {
                    Text("Fall · 04:31", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Text("Both were called", style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
                }
            }
        }
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Spacing.lg)) {
                Text("Fall · 04:31", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                Text("Countdown ran out · Meera and Arun were called", style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
            }
            FooterAction("Share as PDF", icon = SafeShadeIcons.Pdf, onClick = {})
        }
        CardStrip {
            items(4) { i ->
                val names = listOf("Home", "Park", "Clinic", "Temple")
                StripCard(names[i], if (i == 0) "Inside · 200 m" else "Away · 500 m", if (i == 0) LampState.LIVE else LampState.OFF, SafeShadeIcons.SafeZone, colors.accentFor(names[i]), onClick = {})
            }
        }
    }
}

@Composable
private fun AdoptedActions() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ActionPair("Save", {}, "Discard", {}, secondaryDestructive = true)
        ActionPair("I am OK", {}, "Call now", {}, primaryWeight = ButtonWeight.PRIMARY, secondaryWeight = ButtonWeight.DANGER)
        SplitButton("Call Meera", onClick = {}, onMore = {}, moreDescription = "Other people to call", weight = ButtonWeight.DANGER, icon = SafeShadeIcons.TelephoneCall)
        HoldToConfirm("Hold to Delete This Person", onConfirm = {}, icon = SafeShadeIcons.DeleteBin)
        BoardButton("Save and Send to the Device", onClick = {}, weight = ButtonWeight.COMMIT, figure = "3 changes")
        BoardButton("Add a Person", onClick = {}, icon = SafeShadeIcons.UserAdd, glyphOnDisc = true)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            BoardIconButton(SafeShadeIcons.SendMessage, "Message", onClick = {}, caption = "Message", modifier = Modifier.weight(1f))
            BoardIconButton(SafeShadeIcons.WhereNavigation, "Where", onClick = {}, caption = "Where", modifier = Modifier.weight(1f))
            BoardIconButton(SafeShadeIcons.TelephoneCall, "Call", onClick = {}, caption = "Call", enabled = false, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.xs))
        EditorFootBar(
            primaryLabel = "Save", onPrimary = {}, secondaryLabel = "Discard", onSecondary = {},
            changedLine = "3 fields changed", statusLine = "Not yet on the wearable", statusState = LampState.ATTENTION
        )
    }
}

@Composable
private fun AdoptedExplainers() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Callout("Never dials by itself.", "The dialer opens with the number ready; a person places the call.")
        Footnote("Sealed rows are hidden on the wearable in Elderly mode, so Baba cannot turn them down.")
        Chain(
            listOf(
                ChainStop(SafeShadeIcons.FallDetection, "Fall", "detected"),
                ChainStop(SafeShadeIcons.HourglassTimer, "30 s", "to cancel"),
                ChainStop(SafeShadeIcons.TelephoneCall, "Meera", "is called"),
                ChainStop(SafeShadeIcons.RepeatingCheckIn, "Ladder", "if unanswered")
            )
        )
        Steps(
            listOf(
                Step("Install Health Connect", "From the Play Store; the button below opens it.", done = true),
                Step("Allow SafeShade to read", "Heart rate, oxygen and temperature only.", current = true),
                Step("Read now", "The first reading appears on this page.")
            )
        )
        Ledger(
            listOf(
                LedgerRow("Blood group", "B+"),
                LedgerRow("Allergies", "Penicillin"),
                LedgerRow("Medication", "Amlodipine 5 mg"),
                LedgerRow("Last reading", "72 bpm", mono = true, state = LampState.LIVE),
                LedgerRow("Organ donor", "")
            )
        )
        Timeline(
            listOf(
                TimelineStop("04:31", "Fall detected", LampState.TRIP),
                TimelineStop("04:31", "Countdown 30 s · not cancelled", LampState.ATTENTION),
                TimelineStop("04:32", "Dialer opened with Meera", LampState.LIVE),
                TimelineStop("04:41", "Meera marked it handled", LampState.LIVE)
            )
        )
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Countdown", state = LampState.LIVE, stateLabel = "30 s", detail = "Before anyone is called", icon = SafeShadeIcons.HourglassTimer, onClick = {},
                help = "Thirty seconds is long enough to find the button after a stumble and short enough that a person who cannot move is called for before the fourth minute. The wearable buzzes through the whole count."
            )
            Hairline()
            Row(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                QualifierChip("Needs SIM"); QualifierChip("Device-only"); QualifierChip("Plus"); QualifierChip("Signed out")
            }
        }
    }
}

@Composable
private fun AdoptedChannels() {
    var tab by remember { mutableIntStateOf(0) }
    var choice by remember { mutableIntStateOf(1) }
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        PageTabs(listOf("Recent", "All", "Falls"), tab, { tab = it })
        SegmentedChoice(
            listOf("Low", "Medium", "High"), choice, { choice = it },
            consequences = listOf(
                "Trips at 2.6 g. Fewer false calls; a soft fall may be missed.",
                "Trips at 1.9 g. Recommended for an elderly wearer.",
                "Trips at 1.4 g. Catches a slide off a chair; sitting down hard can trip it."
            ),
            sealed = true
        )
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            ExpandableSection(
                label = "More detection settings", open = open, onOpenChange = { open = it }, count = 3,
                preview = listOf("Sensitivity Medium" to LampState.LIVE, "Countdown 30 s" to LampState.LIVE, "Still 2 min" to LampState.OFF)
            ) {
                Hairline()
                Way(name = "Sensitivity", state = LampState.LIVE, stateLabel = "Medium", onClick = {})
                Hairline()
                Way(name = "Countdown", state = LampState.LIVE, stateLabel = "30 s", onClick = {})
                Hairline()
                Way(name = "Stillness", state = LampState.OFF, stateLabel = "2 min", onClick = {})
            }
        }
    }
}

@Composable
private fun AdoptedRows() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            NumberedRow(1, "Meera", LampState.LIVE, "Called", detail = "Waits 60 s for an answer", onClick = {})
            Hairline()
            NumberedRow(2, "Arun", LampState.LIVE, "Called", detail = "Waits 60 s", onClick = {})
            Hairline()
            NumberedRow(3, "112", LampState.ATTENTION, "Dialer", detail = "The dialer opens; nobody is called by itself", onClick = {})
        }
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Fall · 04:31", state = LampState.OFF, stateLabel = "Handled", detail = "Countdown ran out · both were called", onClick = {},
                trailing = { FaceStack(listOf(Face(AvatarSpec.PRESETS[5].encode(), "Meera"), Face(AvatarSpec.PRESETS[9].encode(), "Arun"))) }
            )
            Hairline()
            Way(
                name = "SOS · Tuesday", state = LampState.OFF, stateLabel = "Cancelled", detail = "By Baba after 8 s", onClick = {},
                trailing = { FaceStack(listOf(Face(AvatarSpec.PRESETS[2].encode(), "Baba"))) }
            )
        }
        TileGrid(
            listOf(
                Tile("Paired devices", SafeShadeIcons.PairedDevices, LampState.LIVE, "1 saved", {}),
                Tile("Find the device", SafeShadeIcons.FindTheDevice, LampState.OFF, "Ready", {}),
                Tile("Adaptive mode", SafeShadeIcons.AdaptiveMode, LampState.OFF, "Elderly", {}, tag = "Sealed"),
                Tile("Lights", SafeShadeIcons.Lights, LampState.ATTENTION, "Torch on", {}),
                Tile("Firmware", SafeShadeIcons.CpuChip, LampState.UNKNOWN, "—", {}),
                Tile("Ride log", SafeShadeIcons.Bicycle01, LampState.OFF, "None", {}, tag = "Plus")
            )
        )
    }
}

@Composable
private fun AdoptedInstruments() {
    val values = listOf(0.45f, 0.42f, 0.40f, 0.41f, 0.43f, 0.48f, 0.55f, 0.62f, 0.58f, 0.60f, 0.66f, 0.71f, 0.68f, 0.83f, 0.87f, 0.74f, 0.66f, 0.63f, null, null, 0.52f, 0.48f, 0.46f, 0.44f)
    HourBars(
        label = "Heart rate · today", value = "72", unit = "bpm", values = values, threshold = 0.8f,
        note = "2 above 110", noteState = LampState.ATTENTION,
        description = "Heart rate today, 72 beats per minute, two hours above 110, two hours without a reading"
    )
}

