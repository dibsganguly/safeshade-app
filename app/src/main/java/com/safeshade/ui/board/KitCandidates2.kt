package com.safeshade.ui.board

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.safeshade.data.DeviceModel
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.shady.Shady
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.theme.BoardCondensed
import com.safeshade.ui.theme.BoardMono
import com.safeshade.ui.theme.BoardSans
import com.safeshade.ui.theme.BrandCharcoal
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.accentFor
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * The second sixty candidates, 2.42 to 2.101, asked for by the user before
 * choosing from the first forty-one: more unique elements, card designs,
 * styles, fonts, buttons, readouts, gauges, pilot lamps, actions, mains
 * plates with Shady on them, bank and row styles, hub grounds, and the
 * spacing between a headline and what sits under it.
 *
 * The same rules as `KitCandidates.kt`: nothing here is used by a screen,
 * numbers are stable, a candidate is adopted by number and the rest of its
 * group deleted. Helpers shared with the first file are `internal` there.
 */
fun LazyListScope.kitCandidates2() {
    item { SectionPlate("2.0 · Spacing", accent = Color.Unspecified) }
    item { C46HeaderRhythm() }
    item { C47RowRhythm() }
    item { C48BankRhythm() }
    item { C49WideGutter() }

    item { SectionPlate("2.0 · Colour and grounds, second set", accent = Color.Unspecified) }
    item { C50StrongerHubGrounds() }
    item { C51HubBand() }
    item { C52HubRuleAtTop() }
    item { C54HubInkSections() }
    item { C55WarmNight() }
    item { C56PaperGrain() }

    item { SectionPlate("2.0 · Pilot lamps", accent = Color.Unspecified) }
    item { C58BezelLamp() }
    item { C59LampCluster() }
    item { C60LampWithCount() }
    item { C61StateChip() }

    item { SectionPlate("2.0 · Buttons, second set", accent = Color.Unspecified) }
    item { C62OutlinedHued() }
    item { C66ProgressButton() }

    item { SectionPlate("2.0 · Readouts and gauges, second set", accent = Color.Unspecified) }
    item { C68TrendReadout() }
    item { C69SparklineReadout() }
    item { C70HeroReadout() }
    item { C71ArcGauge() }
    item { C72CellBattery() }
    item { C73ValueAgainstTarget() }
    item { C74LampedReadoutRow() }

    item { SectionPlate("2.0 · Mains plates", accent = Color.Unspecified) }
    item { C75ShadyOverTheEdge() }
    item { C76ShadyBesideTheState() }
    item { C77StateBand() }
    item { C78MainsAsWell() }
    item { C79CentredMains() }
    item { C80MainsWithPlace() }

    item { SectionPlate("2.0 · Banks and rows, second set", accent = Color.Unspecified) }
    item { C82ZebraBank() }
    item { C85RowWithSwitchAndWord() }
    item { C86RowWithMeter() }
    item { C87RowWithLeadingLamp() }

    item { SectionPlate("2.0 · Cards", accent = Color.Unspecified) }
    item { C89AccentBandCard() }
    item { C93NestedCard() }

    item { SectionPlate("2.0 · Actions and explainers, second set", accent = Color.Unspecified) }
    item { C99StickyBankHeader() }
    item { C100EmptyBayWithDisc() }
    item { C101SnackbarWithLamp() }
}

// ============================================
// TYPE AND SPACING
// ============================================

@Composable
private fun HeaderSample(gap: Dp, subtitleStyle: TextStyle, label: String) {
    val colors = MaterialTheme.board
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkFaint)
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(SafeShadeIcons.ArrowLeft01, contentDescription = null, tint = colors.inkAttention, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text("Fall detection", style = MaterialTheme.typography.headlineMedium, color = colors.ink)
        }
        Spacer(Modifier.height(gap))
        Text("What the device watches for, and what happens next.", style = subtitleStyle, color = colors.inkMuted)
    }
}

@Composable
private fun C46HeaderRhythm() = Candidate(
    "2.46", "Title to subtitle: three gaps",
    refines = "ScreenHeader",
    note = "The same header at 2, 6 and 12dp between the title and its line, with the line one step smaller and in faint ink on the last. Today's is the first. The subtitle is the sentence a page is read by; where it sits decides whether it belongs to the title or to the page."
) {
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
            HeaderSample(2.dp, MaterialTheme.typography.bodyLarge, "2dp · body large (now)")
            HeaderSample(6.dp, MaterialTheme.typography.bodyMedium, "6dp · body medium")
            HeaderSample(12.dp, MaterialTheme.boardType.rowDetail.copy(fontSize = 14.sp, lineHeight = 20.sp), "12dp · detail at 14, faint")
        }
    }
}

@Composable
private fun RowSample(gap: Dp, detailStyle: TextStyle, label: String, vertical: Dp = Spacing.md) {
    val colors = MaterialTheme.board
    Column {
        Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkFaint, modifier = Modifier.padding(start = Spacing.lg, top = Spacing.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.md, top = vertical, bottom = vertical).height(IntrinsicSize.Min)
        ) {
            Icon(SafeShadeIcons.SafeZone, contentDescription = null, tint = colors.accentFor("Safe zones"), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Nameplate("Safe zones")
                Spacer(Modifier.height(gap))
                Text("Told when the wearable leaves one. One zone set.", style = detailStyle, color = colors.inkFaint)
            }
            Spacer(Modifier.width(Spacing.md))
            StateWord("1 zone", LampState.LIVE)
            Spacer(Modifier.width(Spacing.md))
            BusTick(LampState.LIVE, Modifier.fillMaxHeight())
        }
    }
}

@Composable
private fun C47RowRhythm() = Candidate(
    "2.47", "Title to detail: three gaps",
    refines = "Way",
    note = "The way's detail at 4dp under the title (now), at 2dp with the detail a point smaller, and at 6dp with 16dp of row padding instead of 12. The second is denser; the third breathes. A screen is mostly this gap repeated forty times."
) {
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        RowSample(Spacing.xs, MaterialTheme.boardType.rowDetail, "4dp · detail 13 · padding 12 (now)")
        Hairline()
        RowSample(2.dp, MaterialTheme.boardType.rowDetail.copy(fontSize = 12.sp, lineHeight = 15.sp), "2dp · detail 12 · padding 12")
        Hairline()
        RowSample(6.dp, MaterialTheme.boardType.rowDetail.copy(lineHeight = 18.sp), "6dp · detail 13 · padding 16", vertical = Spacing.lg)
    }
}

@Composable
private fun C48BankRhythm() = Candidate(
    "2.48", "Section plate to bank: three gaps",
    refines = "the rhythm between a section plate and its plate",
    note = "The brass rule sits 16dp above the bank today. Shown at 8, 16 and 24, with the plate-to-plate gap after it at 12, 16 and 24 to match. Tighter binds the heading to its bank; looser separates banks from each other, which is the thing that actually needs separating."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xl)) {
        listOf(8.dp to 12.dp, 16.dp to 16.dp, 24.dp to 24.dp).forEach { (after, between) ->
            Column {
                Text("${after.value.toInt()}dp after the rule · ${between.value.toInt()}dp between plates", style = MaterialTheme.boardType.nameplateSmall, color = colors.inkFaint)
                Spacer(Modifier.height(Spacing.xs))
                SectionPlate("Detection")
                Spacer(Modifier.height(after))
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Way(name = "Fall detection", state = LampState.LIVE, stateLabel = "Medium", icon = SafeShadeIcons.FallDetection)
                }
                Spacer(Modifier.height(between))
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Way(name = "Call after a fall", state = LampState.LIVE, stateLabel = "Live", icon = SafeShadeIcons.CallAfterAFall)
                }
            }
        }
    }
}

@Composable
private fun C49WideGutter() = Candidate(
    "2.49", "A wider gutter, a tighter plate",
    refines = "Layout",
    note = "24dp screen gutter with 12dp inside the plate, against today's 20 and 16. The content column is the same width; the plate's edge sits further from the phone's, which reads as a card on a table rather than a panel filling the frame."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        listOf(Triple(20.dp, 16.dp, "20 · 16 (now)"), Triple(24.dp, 12.dp, "24 · 12")).forEach { (gutter, inner, label) ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.card)).background(colors.ground).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card)).padding(vertical = Spacing.md, horizontal = gutter - Spacing.gutter + Spacing.xs)) {
                Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkFaint)
                Spacer(Modifier.height(Spacing.xs))
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(inner)) {
                        Text("Baba is covered", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                        Text("SafeShade S1 · connected", style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
                    }
                }
            }
        }
    }
}

// ============================================
// COLOUR AND GROUNDS
// ============================================

/** The four hubs' real tab accents, so a hub mock wears the colour its tab does. */
@Composable
private fun hubAccent(name: String): Color {
    val colors = MaterialTheme.board
    return when (name) {
        "Board" -> colors.accentClay
        "Circle" -> colors.accentSky
        "Safety" -> colors.accentSage
        else -> colors.accentPlum
    }
}

private data class HubMock(val name: String, val icon: ImageVector, val lightGround: Color, val darkGround: Color)

private val HubsStrong = listOf(
    HubMock("Board", SafeShadeIcons.NavbarBoard, Color(0xFFEFEBE3), Color(0xFF14171A)),
    HubMock("Circle", SafeShadeIcons.NavbarCircle, Color(0xFFF3E6DA), Color(0xFF1E1713)),
    HubMock("Safety", SafeShadeIcons.NavbarSafety, Color(0xFFE4EDE1), Color(0xFF121D15)),
    HubMock("Device", SafeShadeIcons.NavbarDevice, Color(0xFFE3E7EF), Color(0xFF131722))
)

@Composable
private fun HubGroundRow(hubs: List<HubMock>, content: @Composable (HubMock, Color) -> Unit) {
    val colors = MaterialTheme.board
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        hubs.forEach { hub ->
            val ground = if (colors.isDark) hub.darkGround else hub.lightGround
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(Radius.card)).background(ground).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card)).padding(Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) { content(hub, ground) }
        }
    }
}

@Composable
private fun C50StrongerHubGrounds() = Candidate(
    "2.50", "A ground per hub, stronger",
    refines = "2.08",
    note = "The same idea at twice the cast, so the hub's colour is seen rather than sensed. The plate stays bone; the ink tokens still clear 4.5 on every ground (printed)."
) {
    val colors = MaterialTheme.board
    HubGroundRow(HubsStrong) { hub, ground ->
        Box(Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(Radius.plate)).background(colors.plate).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate)), contentAlignment = Alignment.Center) {
            Icon(hub.icon, contentDescription = null, tint = hubAccent(hub.name), modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(hub.name, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
        Text("%.1f:1".format(contrastRatio(colors.inkFaint, ground)), style = MaterialTheme.boardType.readout.copy(fontSize = 11.sp), color = colors.inkFaint)
    }
}

@Composable
private fun C51HubBand() = Candidate(
    "2.51", "A hub band at the top",
    refines = "the hub screens' head",
    note = "The ground stays bone; the top of each hub takes a band in the hub's accent at 0.12 alpha behind the title and the first plate, ending on a hairline. Colour where the eye lands first and nowhere else, so scrolling returns to the plain panel."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf("Circle" to SafeShadeIcons.NavbarCircle, "Safety" to SafeShadeIcons.NavbarSafety).forEach { (name, icon) ->
            val accent = hubAccent(name)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.card)).background(colors.ground).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))) {
                Column(Modifier.fillMaxWidth().background(accent.copy(alpha = 0.12f)).padding(Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text(name, style = MaterialTheme.typography.headlineMedium, color = colors.ink)
                    }
                    Spacer(Modifier.height(Spacing.md))
                    BoardPlate(modifier = Modifier.fillMaxWidth()) {
                        Text("Baba is covered", style = MaterialTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.padding(Spacing.md))
                    }
                }
                Hairline()
                Text("The rest of the page on plain ground", style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint, modifier = Modifier.padding(Spacing.lg))
            }
        }
    }
}

@Composable
private fun C52HubRuleAtTop() = Candidate(
    "2.52", "A hub rule under the status bar",
    refines = "the hub screens' head",
    note = "A 3dp rule in the hub's accent across the very top of the content, the same weight as a bus tick, and nothing else. The cheapest possible hub identity: one line, no ground, no band. Shown for the four hubs."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf("Board", "Circle", "Safety", "Device").forEach { name ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.card)).background(colors.ground).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))) {
                Box(Modifier.fillMaxWidth().height(Stroke.heavy).background(hubAccent(name)))
                Text(name, style = MaterialTheme.typography.headlineSmall, color = colors.ink, modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))
            }
        }
    }
}

@Composable
private fun C54HubInkSections() = Candidate(
    "2.54", "Section plates in the hub's ink, rows in plain ink",
    refines = "SectionPlate",
    note = "Today every section plate hashes its own accent, so one screen wears five colours. Here every section plate on a hub takes the hub's one accent, and the row glyphs go to muted ink. Fewer colours per screen, one colour per hub; a screenful reads as one place."
) {
    val colors = MaterialTheme.board
    val accent = hubAccent("Safety")
    Column {
        SectionPlate("Detection", accent = accent)
        Spacer(Modifier.height(Spacing.lg))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(name = "Fall detection", state = LampState.LIVE, stateLabel = "Medium", icon = SafeShadeIcons.FallDetection, accent = Color.Unspecified)
            Hairline()
            Way(name = "Call after a fall", state = LampState.LIVE, stateLabel = "Live", icon = SafeShadeIcons.CallAfterAFall, accent = Color.Unspecified)
        }
        Spacer(Modifier.height(Spacing.lg))
        SectionPlate("Where", accent = accent)
        Spacer(Modifier.height(Spacing.lg))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(name = "Safe zones", state = LampState.OFF, stateLabel = "Not known", icon = SafeShadeIcons.SafeZone, accent = Color.Unspecified)
        }
    }
}

@Composable
private fun C55WarmNight() = Candidate(
    "2.55", "A warm night",
    refines = "the dark theme's three neutrals",
    note = "The night panel pulled towards brown rather than blue: a charcoal with the emblem's warmth in it, so the dark theme and the bone theme read as the same material with the lights off. Contrast printed live."
) {
    PaletteSwatches(
        ground = Color(0xFF17140F), plate = Color(0xFF26221C), recess = Color(0xFF100E0A), hairline = Color(0xFF3B352C),
        ink = Color(0xFFF1ECE3), inkMuted = Color(0xFFB8AF9F), inkFaint = Color(0xFF968D7C)
    )
}

@Composable
private fun C56PaperGrain() = Candidate(
    "2.56", "A ground with a grain",
    refines = "ground",
    note = "The bone ground carries a faint dot grain, drawn as flat 1dp dots on a 6dp grid at 0.05 alpha ink. Not a texture image and not a gradient; a machined surface rather than a flat fill. Judge at arm's length: it should be felt, not seen."
) {
    val colors = MaterialTheme.board
    Box(
        Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(Radius.card)).background(colors.ground).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))
            .drawBehind {
                val step = 6.dp.toPx(); val r = 0.6.dp.toPx()
                var y = step / 2
                while (y < size.height) {
                    var x = step / 2
                    while (x < size.width) { drawCircle(colors.ink.copy(alpha = 0.06f), r, Offset(x, y)); x += step }
                    y += step
                }
            }
    ) {
        BoardPlate(modifier = Modifier.padding(Spacing.lg).fillMaxWidth()) {
            Text("A plate on the grained ground", style = MaterialTheme.boardType.nameplate, color = colors.ink, modifier = Modifier.padding(Spacing.lg))
        }
    }
}

// ============================================
// PILOT LAMPS
// ============================================

@Composable
private fun LampGlass(state: LampState): Color {
    val colors = MaterialTheme.board
    return when (state) {
        LampState.LIVE -> colors.lampLive
        LampState.ATTENTION -> colors.lampAttention
        LampState.TRIP -> colors.lampTrip
        else -> colors.lampOff
    }
}

@Composable
private fun LampInk(state: LampState): Color {
    val colors = MaterialTheme.board
    return when (state) {
        LampState.LIVE -> colors.inkLive
        LampState.ATTENTION -> colors.inkAttention
        LampState.TRIP -> colors.inkTrip
        else -> colors.inkFaint
    }
}

@Composable
private fun LampRow(content: @Composable (LampState) -> Unit) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalArrangement = Arrangement.SpaceEvenly) {
            LampState.entries.forEach { state ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    content(state)
                    Spacer(Modifier.height(Spacing.sm))
                    Text(state.name, style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                }
            }
        }
    }
}

@Composable
private fun C58BezelLamp() = Candidate(
    "2.58", "A lamp in a bezel",
    refines = "PilotLamp",
    note = "A 2dp ink bezel ring around the glass with a hairline gap, the way a real panel lamp is seated. Reads as an object at 20dp and up; too heavy under 16. For the mains plate and status wells, not for rows."
) {
    val colors = MaterialTheme.board
    LampRow { state ->
        val glass = LampGlass(state)
        Box(Modifier.size(28.dp).clip(CircleShape).border(2.dp, colors.inkMuted, CircleShape).padding(4.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().fillMaxHeight().clip(CircleShape).background(glass))
        }
    }
}

@Composable
private fun C59LampCluster() = Candidate(
    "2.59", "A lamp cluster",
    refines = "the hub's summary",
    note = "Three or four lamps in a row inside one recess, each with a two-letter label under it: the circuits a hub is made of, at a glance. Safety's Fall, Call, Text, Zone. For the top of a hub or the row that opens it."
) {
    val colors = MaterialTheme.board
    Row(Modifier.clip(RoundedCornerShape(Radius.plate)).background(colors.recess).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate)).padding(horizontal = Spacing.md, vertical = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        listOf("Fall" to LampState.LIVE, "Call" to LampState.LIVE, "Text" to LampState.OFF, "Zone" to LampState.ATTENTION, "Mic" to LampState.OFF).forEach { (label, state) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PilotLamp(state, size = 14.dp)
                Spacer(Modifier.height(Spacing.xs))
                Text(label, style = MaterialTheme.boardType.nameplateSmall.copy(fontSize = 10.sp), color = colors.inkMuted)
            }
        }
    }
}

@Composable
private fun C60LampWithCount() = Candidate(
    "2.60", "A lamp with a count",
    refines = "PilotLamp, on a way that counts",
    note = "A lit lamp with a small mono figure sitting on its shoulder: 3 unread, 2 open alerts, 5 in the circle. The figure is ink on a plate disc so it stays legible on any glass. Replaces '· 3 new' in a title."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(Triple("Messages", 3, LampState.LIVE), Triple("Alerts", 2, LampState.TRIP), Triple("Invitations", 1, LampState.ATTENTION)).forEach { (label, n, state) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(34.dp)) {
                        PilotLamp(state, size = 22.dp, modifier = Modifier.align(Alignment.BottomStart))
                        Box(Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(colors.plate).border(Stroke.hairline, colors.hairline, CircleShape), contentAlignment = Alignment.Center) {
                            Text("$n", style = MaterialTheme.boardType.readout.copy(fontSize = 11.sp, lineHeight = 11.sp), color = colors.ink)
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                }
            }
        }
    }
}

@Composable
private fun C61StateChip() = Candidate(
    "2.61", "The state word on glass",
    refines = "the state word on a Way",
    note = "The caps word sits in a chip of its own glass at 0.16 alpha with a hairline of the glass at 0.5, instead of bare ink at the row's edge. Louder than the word alone and quieter than a lamp; the tick goes. Off and Unknown take the recess with faint ink."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        listOf(Triple("Fall detection", "Medium", LampState.LIVE), Triple("Safe zones", "Outside", LampState.ATTENTION), Triple("Wearable", "Fall", LampState.TRIP), Triple("Text as well", "Off", LampState.OFF)).forEachIndexed { i, (name, word, state) ->
            if (i > 0) Hairline()
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Nameplate(name, modifier = Modifier.weight(1f))
                val glass = LampGlass(state)
                val lit = state != LampState.OFF && state != LampState.UNKNOWN
                Text(
                    word.uppercase(),
                    style = MaterialTheme.boardType.stateLabel,
                    color = LampInk(state),
                    modifier = Modifier.clip(RoundedCornerShape(Radius.tight)).background(if (lit) glass.copy(alpha = 0.16f) else colors.recess).border(Stroke.hairline, if (lit) glass.copy(alpha = 0.5f) else colors.hairline, RoundedCornerShape(Radius.tight)).padding(horizontal = Spacing.sm, vertical = 3.dp)
                )
            }
        }
    }
}

// ============================================
// BUTTONS
// ============================================

@Composable
private fun C62OutlinedHued() = Candidate(
    "2.62", "An outlined hued button",
    refines = "ButtonWeight ATTENTION, COMMIT, DANGER",
    note = "The hue as a 1.5dp border and the label in the state's ink, on the plate. A hued action without a filled block of colour, for the second of two actions on a screen where the first already carries the fill. The filled forms stay for the first."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(Triple("Connect to the Device", colors.lampAttention, colors.inkAttention), Triple("Save and Send", colors.lampLive, colors.inkLive), Triple("Emergency Numbers", colors.lampTrip, colors.inkTrip)).forEach { (label, glass, ink) ->
            Box(Modifier.fillMaxWidth().plateClickable(onClick = {}).clip(RoundedCornerShape(Radius.plate)).background(colors.plate).border(Stroke.rule, glass, RoundedCornerShape(Radius.plate)).defaultMinSize(minHeight = 56.dp).padding(horizontal = Spacing.lg, vertical = Spacing.md), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.boardType.nameplate.copy(fontWeight = FontWeight.W700), color = ink)
            }
        }
    }
}

@Composable
private fun C66ProgressButton() = Candidate(
    "2.66", "A button that shows its progress",
    refines = "BoardButton while working",
    note = "The label becomes the verb in progress and the fill advances across the plate from the leading edge as a flat band: Sending… 2 of 5, Downloading… 64%. A button that says Sending… with nothing moving looks stuck after two seconds. Tap to advance the demonstration."
) {
    val colors = MaterialTheme.board
    var frac by remember { mutableFloatStateOf(0.4f) }
    val anim by animateFloatAsState(frac, tween(400), label = "progress")
    Box(Modifier.fillMaxWidth().plateClickable(onClick = { frac = if (frac >= 1f) 0.2f else (frac + 0.3f).coerceAtMost(1f) }).clip(RoundedCornerShape(Radius.plate)).background(colors.recess).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate)).defaultMinSize(minHeight = 56.dp)) {
        Box(Modifier.fillMaxWidth(anim).fillMaxHeight().background(colors.lampLive.copy(alpha = 0.35f)))
        Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).padding(horizontal = Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Text(if (anim >= 1f) "Sent" else "Sending…", style = MaterialTheme.boardType.nameplate.copy(fontWeight = FontWeight.W700), color = colors.ink, modifier = Modifier.weight(1f))
            Text("${(anim * 100).toInt()}%", style = MaterialTheme.boardType.readout, color = colors.inkMuted)
        }
    }
}

// ============================================
// READOUTS AND GAUGES
// ============================================

@Composable
private fun C68TrendReadout() = Candidate(
    "2.68", "A readout with its trend",
    refines = "Readout",
    note = "The figure, and beside it a small arrow and the change since the last reading in the state's ink: 84% ▾3 since 09:00, 72 bpm ▴6. A number alone asks whether it is going the right way; the delta answers."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            listOf(Triple("Battery", "84%", "▾ 3 · since 09:00" to colors.inkAttention), Triple("Heart rate", "72", "▴ 6 · resting" to colors.inkMuted), Triple("Signal", "-62", "▴ 8 · nearer" to colors.inkLive)).forEach { (label, value, delta) ->
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
                    Text(value, style = MaterialTheme.boardType.readout.copy(fontSize = 20.sp, lineHeight = 24.sp), color = colors.ink)
                    Text(delta.first, style = MaterialTheme.boardType.rowDetail.copy(fontSize = 11.sp), color = delta.second)
                }
            }
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.drawBehind {
        val w = size.width; val h = size.height
        val stepX = w / (values.size - 1)
        var prev = Offset(0f, h - values[0] * h)
        values.drop(1).forEachIndexed { i, v ->
            val p = Offset((i + 1) * stepX, h - v * h)
            drawLine(color, prev, p, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            prev = p
        }
        drawCircle(color, 3.dp.toPx(), prev)
    })
}

@Composable
private fun C69SparklineReadout() = Candidate(
    "2.69", "A readout with a sparkline",
    refines = "Readout",
    note = "A flat 2dp line of the last twelve readings under the figure, ending in a dot at the current one. No axes, no fill. The shape of the last hour, in the space a caption takes."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            listOf(
                Triple("Battery", "84%", listOf(0.95f, 0.93f, 0.92f, 0.9f, 0.89f, 0.88f, 0.87f, 0.86f, 0.86f, 0.85f, 0.85f, 0.84f)),
                Triple("Signal", "-62", listOf(0.3f, 0.35f, 0.5f, 0.45f, 0.6f, 0.55f, 0.7f, 0.65f, 0.72f, 0.8f, 0.78f, 0.82f))
            ).forEach { (label, value, series) ->
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
                    Text(value, style = MaterialTheme.boardType.readout.copy(fontSize = 20.sp, lineHeight = 24.sp), color = colors.ink)
                    Spacer(Modifier.height(Spacing.xs))
                    Sparkline(series, colors.inkMuted, Modifier.fillMaxWidth().height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun C70HeroReadout() = Candidate(
    "2.70", "One number, very large",
    refines = "Readout large, Gauge",
    note = "The figure at 56sp in the mono face, the unit and the label under it, a lamp beside the label. For a page about one quantity: minutes out of reach, seconds of countdown, decibels. It is the whole answer and it is sized like one."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            listOf(Triple("45", "min out of reach", LampState.ATTENTION), Triple("53", "dB · uncalibrated", LampState.LIVE)).forEach { (value, label, state) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = TextStyle(fontFamily = BoardMono, fontWeight = FontWeight.W500, fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-0.04).em), color = colors.ink)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PilotLamp(state, size = 10.dp)
                        Spacer(Modifier.width(Spacing.xs))
                        Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun C71ArcGauge() = Candidate(
    "2.71", "An arc gauge",
    refines = "Gauge",
    note = "DESIGN.md says a dial is handsome and slower to read, and it is; this is the flattest possible one so the claim can be tested against the eye. A 270° track in the recess, the value as a flat arc in ink or in the state's glass, the figure inside. No needle, no ticks, no gradient."
) {
    val colors = MaterialTheme.board
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        listOf(Triple("Battery", 0.84f, "84%"), Triple("UV index", 0.55f, "6"), Triple("Steps", 0.32f, "3.2k")).forEach { (label, frac, text) ->
            BoardPlate(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).drawBehind {
                        val stroke = 7.dp.toPx()
                        val inset = stroke / 2
                        val rect = Size(size.width - stroke, size.height - stroke)
                        drawArc(colors.recess, 135f, 270f, false, Offset(inset, inset), rect, style = DrawStroke(stroke, cap = StrokeCap.Butt))
                        drawArc(if (frac < 0.2f) colors.lampAttention else colors.ink, 135f, 270f * frac, false, Offset(inset, inset), rect, style = DrawStroke(stroke, cap = StrokeCap.Butt))
                    }, contentAlignment = Alignment.Center) {
                        Text(text, style = MaterialTheme.boardType.readout.copy(fontSize = 16.sp), color = colors.ink)
                    }
                    Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
                }
            }
        }
    }
}

@Composable
private fun C72CellBattery() = Candidate(
    "2.72", "A battery in cells",
    refines = "the Battery readout",
    note = "Ten machined cells in a channel, lit from the left to the tenth the charge has reached, in live glass above 30, attention to 15, trip below. Two cells lit is a number a person feels; 17% is a number they compute."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            listOf(84 to "84%", 27 to "27%", 12 to "12%").forEach { (pct, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(Radius.tight)).background(colors.recess).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.tight)).padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        val lit = (pct + 9) / 10
                        val glass = when { pct <= 15 -> colors.lampTrip; pct <= 30 -> colors.lampAttention; else -> colors.lampLive }
                        repeat(10) { i -> Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(1.dp)).background(if (i < lit) glass else Color.Transparent)) }
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Text(label, style = MaterialTheme.boardType.readout, color = colors.ink, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun C73ValueAgainstTarget() = Candidate(
    "2.73", "A value against its target",
    refines = "Readout, for thresholds",
    note = "The reading large, then a slash and the threshold small in muted ink: 38.2 / 38.0 °C, 72 / 120 bpm. The relationship a threshold page is about, in one readout, without a second row explaining it."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            listOf(Triple("Temperature", "38.2" to "38.0 °C", LampState.ATTENTION), Triple("Heart rate", "72" to "120 bpm", null), Triple("Oxygen", "97" to "90 %", null)).forEach { (label, pair, state) ->
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
                    // The figure on one line, the target on the next: side by
                    // side they folded into three lines in a third of a plate.
                    Text(pair.first, style = MaterialTheme.boardType.readout.copy(fontSize = 22.sp, lineHeight = 26.sp), color = if (state == LampState.ATTENTION) colors.inkAttention else colors.ink)
                    Text("/ ${pair.second}", style = MaterialTheme.boardType.readout.copy(fontSize = 12.sp, lineHeight = 16.sp), color = colors.inkFaint, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun C74LampedReadoutRow() = Candidate(
    "2.74", "Readouts with lamps",
    refines = "the instrument strip",
    note = "The mains plate's strip with a 10dp lamp at each readout's label, lit by that instrument's own state. The strip then says which instrument is the problem, not only that one is. Unlit lamps stay drawn so the strip keeps its shape when nothing is wrong."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Spacing.lg)) {
            listOf(Triple("Protecting", "Baba", LampState.LIVE), Triple("Battery", "12%", LampState.TRIP), Triple("Signal", "-62", LampState.LIVE)).forEach { (label, value, state) ->
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PilotLamp(state, size = 10.dp)
                        Spacer(Modifier.width(Spacing.xs))
                        Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
                    }
                    Text(value, style = MaterialTheme.boardType.readout.copy(fontSize = 18.sp, lineHeight = 24.sp), color = if (state == LampState.TRIP) colors.inkTrip else colors.ink)
                }
            }
        }
    }
}

// ============================================
// MAINS PLATES
// ============================================

@Composable
private fun MainsBody(headline: String, subline: String, state: LampState) {
    val colors = MaterialTheme.board
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PilotLamp(state, size = 14.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(headline, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(subline, style = MaterialTheme.boardType.rowDetail.copy(fontWeight = FontWeight.W600), color = colors.inkMuted)
    }
}

@Composable
private fun C75ShadyOverTheEdge() = Candidate(
    "2.75", "Shady over the edge of the mains plate",
    refines = "MainsPlate and the stage",
    note = "The character stands behind the mains plate with its head and antenna over the top edge, looking down at the headline. It is still outside the plate's grammar and still absent in an emergency, but it is now at the top of the Board where the eye goes first, not at the foot where it competes with nothing."
) {
    val colors = MaterialTheme.board
    Box(Modifier.fillMaxWidth().padding(top = 34.dp)) {
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Spacing.lg)) {
                MainsBody("Baba is covered", "SafeShade S1 · connected", LampState.LIVE)
            }
            Hairline()
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                Readout(label = "Protecting", value = "Baba", modifier = Modifier.weight(1.2f))
                Readout(label = "Battery", value = "84%", horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f))
                Readout(label = "Signal", value = "-62", horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f))
            }
        }
        Box(Modifier.align(Alignment.TopEnd).offset(x = (-20).dp, y = (-38).dp)) {
            Shady(mood = ShadyMood.WATCHING, size = 64.dp)
        }
    }
}

@Composable
private fun C76ShadyBesideTheState() = Candidate(
    "2.76", "Shady beside the state",
    refines = "MainsPlate",
    note = "The character inside the plate at the leading edge, at 56dp, with the headline and its line beside it. Its mood is the link state's, so the picture and the words agree; the lamp goes because the face is the lamp here. The one place in the system a character sits next to a state, kept off every other plate."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(Triple("Baba is covered", "SafeShade S1 · connected · 84%", ShadyMood.CALM), Triple("Baba is not connected", "Last heard 7 Sept · 18:44", ShadyMood.SEARCHING)).forEach { (h, s, mood) ->
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                    Shady(mood = mood, size = 56.dp)
                    Spacer(Modifier.width(Spacing.lg))
                    Column {
                        Text(h, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                        Text(s, style = MaterialTheme.boardType.rowDetail.copy(fontWeight = FontWeight.W600), color = colors.inkMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun C77StateBand() = Candidate(
    "2.77", "A state band across the top",
    refines = "MainsPlate",
    note = "The plate opens on a 6dp band of the link's glass across its full width, then the headline, then the instruments. In greyscale the band is a hairline; in colour it is the whole state at a glance from across a room. Off draws it in unlit stone, so the band never vanishes."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(Triple(LampState.LIVE, "Baba is covered", "SafeShade S1 · connected"), Triple(LampState.TRIP, "Baba may have fallen", "04:31 · countdown running on the wearable"), Triple(LampState.OFF, "Baba is not connected", "Last heard 7 Sept")).forEach { (state, h, s) ->
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(6.dp).background(LampGlass(state)))
                Column(Modifier.padding(Spacing.lg)) {
                    Text(h, style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                    Text(s, style = MaterialTheme.boardType.rowDetail.copy(fontWeight = FontWeight.W600), color = colors.inkMuted)
                }
            }
        }
    }
}

@Composable
private fun C78MainsAsWell() = Candidate(
    "2.78", "The mains as a status well with wells",
    refines = "MainsPlate",
    note = "2.37's status well as the head of the mains, the instruments as 2.21's wells beneath, all set into one plate. Everything on the mains is then recessed into the panel rather than printed on it, which is what the mains of a real board looks like."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.plate)).background(colors.recess).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate)).padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                PilotLamp(LampState.LIVE, size = 24.dp)
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Text("Baba is covered", style = MaterialTheme.typography.titleMedium, color = colors.inkLive)
                    Text("SafeShade S1 · connected", style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Well("Battery", "84", "%", Modifier.weight(1f))
                Well("Signal", "-62", "dBm", Modifier.weight(1f))
                Well("Seen", "just now", null, Modifier.weight(1f), compact = true)
            }
        }
    }
}

@Composable
private fun C79CentredMains() = Candidate(
    "2.79", "A centred mains",
    refines = "MainsPlate",
    note = "The face at 64dp centred, the name under it, the state word with its lamp under that, the instruments as a centred strip. A portrait rather than a row: the Board opens on a person's face the way a contact card does. Taller by about 40dp than the plate it replaces."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(avatarId = AvatarSpec.PRESETS[3].encode(), name = "Baba", size = 64.dp)
            Spacer(Modifier.height(Spacing.sm))
            Text("Baba", style = MaterialTheme.typography.headlineMedium, color = colors.ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                PilotLamp(LampState.LIVE, size = 12.dp)
                Spacer(Modifier.width(Spacing.xs))
                Text("Covered · SafeShade S1", style = MaterialTheme.boardType.nameplateSmall, color = colors.inkLive)
            }
        }
        Hairline()
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md), horizontalArrangement = Arrangement.SpaceEvenly) {
            Readout(label = "Battery", value = "84%", horizontalAlignment = Alignment.CenterHorizontally)
            Readout(label = "Signal", value = "-62", horizontalAlignment = Alignment.CenterHorizontally)
            Readout(label = "Seen", value = "now", horizontalAlignment = Alignment.CenterHorizontally)
        }
    }
}

@Composable
private fun C80MainsWithPlace() = Candidate(
    "2.80", "The mains with the place on it",
    refines = "MainsPlate",
    note = "Under the instruments, one more row: where the wearer was last seen and how long ago, with a pin glyph, and a small Where button at the end. The Board answers who, whether, how much, and now where, before anyone scrolls. A dash when there is no fix."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg)) { MainsBody("Baba is covered", "SafeShade S1 · connected", LampState.LIVE) }
        Hairline()
        Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
            Readout(label = "Battery", value = "84%", modifier = Modifier.weight(1f))
            Readout(label = "Signal", value = "-62", horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f))
        }
        Hairline()
        Row(Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Icon(SafeShadeIcons.PinLocation, contentDescription = null, tint = colors.accentFor("Where"), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text("Near Home", style = MaterialTheme.boardType.nameplate, color = colors.ink)
                Text("12 min ago · ±25 m", style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
            }
            BoardIconButton(icon = SafeShadeIcons.WhereNavigation, contentDescription = "Where", onClick = {})
        }
    }
}

// ============================================
// BANKS AND ROWS
// ============================================

@Composable
private fun C82ZebraBank() = Candidate(
    "2.82", "A bank with alternating rows",
    refines = "Hairline between ways",
    note = "Every second row on the recess tone instead of a hairline between rows. Long banks (contacts, the trip log, paired devices) read row by row without a line to count; short banks look odd with it, so it is for lists, not settings."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        listOf(Triple("Meera", "Daughter · 98765 43210", LampState.LIVE), Triple("Arun", "Son · 91234 56780", LampState.LIVE), Triple("Dr Rao", "Doctor · 98700 11223", LampState.OFF), Triple("Neighbour", "No number", LampState.OFF)).forEachIndexed { i, (n, d, s) ->
            Box(Modifier.background(if (i % 2 == 1) colors.recess.copy(alpha = 0.6f) else Color.Transparent)) {
                Way(name = n, state = s, stateLabel = if (s == LampState.LIVE) "Called" else "Not called", detail = d, onClick = {})
            }
        }
    }
}

@Composable
private fun C85RowWithSwitchAndWord() = Candidate(
    "2.85", "A switch row that also says its state",
    refines = "Way with a rocker",
    note = "Today a rocker row shows the rocker and nothing else at the right; the state is the chip's position. Here the word sits above the rocker in the state's ink, so 'Live' or 'Off' is read as well as seen, and a row whose rocker is on but whose circuit is degraded can say Waiting."
) {
    val colors = MaterialTheme.board
    var a by remember { mutableStateOf(true) }
    var b by remember { mutableStateOf(true) }
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        listOf(Triple("Call after a fall", "Live", a), Triple("Text as well", "Waiting for a SIM", b)).forEachIndexed { i, (n, w, on) ->
            if (i > 0) Hairline()
            Row(Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.md, top = Spacing.sm, bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Nameplate(n, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(w.uppercase(), style = MaterialTheme.boardType.stateLabel.copy(fontSize = 10.sp), color = if (w == "Live") colors.inkLive else if (on) colors.inkAttention else colors.inkFaint)
                    WaySwitch(checked = on, onCheckedChange = { if (i == 0) a = it else b = it })
                }
            }
        }
    }
}

@Composable
private fun C86RowWithMeter() = Candidate(
    "2.86", "A row with a meter in it",
    refines = "Way, for quantities",
    note = "Under the title, a 4dp channel showing the quantity the row is about: a paired device's battery, a zone's radius against the largest, a recording's length against the cap. The state word stays. The row says how much, not only what."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        listOf(Triple("SafeShade S1 · Baba", 0.84f, "84%" to LampState.LIVE), Triple("SafeShade S1 · Dadu", 0.12f, "12%" to LampState.TRIP)).forEachIndexed { i, (n, f, st) ->
            if (i > 0) Hairline()
            Row(Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.md, top = Spacing.md, bottom = Spacing.md).height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Nameplate(n)
                    Spacer(Modifier.height(Spacing.sm))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(1.dp)).background(colors.recess)) {
                        Box(Modifier.fillMaxWidth(f).fillMaxHeight().background(LampGlass(st.second)))
                    }
                }
                Spacer(Modifier.width(Spacing.lg))
                Text(st.first, style = MaterialTheme.boardType.readout, color = LampInk(st.second))
                Spacer(Modifier.width(Spacing.md))
                BusTick(st.second, Modifier.fillMaxHeight())
            }
        }
    }
}

@Composable
private fun C87RowWithLeadingLamp() = Candidate(
    "2.87", "A row with a leading lamp",
    refines = "Way",
    note = "The pilot lamp at the leading edge where the glyph is, the bus tick gone, the state word kept. The row reads left to right as lamp, name, word: the order a real panel is read in. Glyphs then go to the section, not the row."
) {
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        listOf(Triple("Fall detection", "Medium", LampState.LIVE), Triple("Safe zones", "Outside", LampState.ATTENTION), Triple("Text as well", "Off", LampState.OFF)).forEachIndexed { i, (n, w, s) ->
            if (i > 0) Hairline()
            Row(Modifier.fillMaxWidth().rowClickable(role = Role.Button, onClick = {}).padding(horizontal = Spacing.lg, vertical = Spacing.md).defaultMinSize(minHeight = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                PilotLamp(s, size = 14.dp)
                Spacer(Modifier.width(Spacing.md))
                Nameplate(n, modifier = Modifier.weight(1f))
                StateWord(w, s)
            }
        }
    }
}

// ============================================
// CARDS
// ============================================

@Composable
private fun C89AccentBandCard() = Candidate(
    "2.89", "A card with a top band",
    refines = "BoardPlate",
    note = "A 4dp band of the card's accent along the top edge, inside the clip. The washed plate (2.09) colours the whole card; this colours one edge. For a card that is about one identity: a zone, a person, a wearable."
) {
    val colors = MaterialTheme.board
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf("Home" to "Baba is inside", "Park" to "Left 8 min ago").forEach { (n, l) ->
            BoardPlate(modifier = Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(colors.accentFor(n)))
                Column(Modifier.padding(Spacing.md)) {
                    Text(n, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Text(l, style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
                }
            }
        }
    }
}

@Composable
private fun C93NestedCard() = Candidate(
    "2.93", "A recess inside a plate",
    refines = "BoardPlate recessed",
    note = "A plate whose body is a recessed panel with 8dp of plate showing around it: the plate is the frame, the recess is the content. For a plate that presents one object, a zone's map, a wearable's silhouette, a recording's waveform, so the object is set into the panel."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.sm)) {
            Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(Radius.plate)).background(colors.recess).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate)), contentAlignment = Alignment.Center) {
                ProductSilhouette(DeviceModel.S1, size = 64.dp, accent = colors.accentFor("S1"))
            }
            Row(Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Nameplate("SafeShade S1")
                    Text("Worn by Baba", style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
                }
                StateWord("Not connected", LampState.OFF)
            }
        }
    }
}

// ============================================
// ACTIONS AND EXPLAINERS
// ============================================

@Composable
private fun C99StickyBankHeader() = Candidate(
    "2.99", "A section plate that stays",
    refines = "SectionPlate on a long page",
    note = "The section plate drawn on a strip of ground with a hairline under it, so it can pin to the top while its bank scrolls under. A long page then always says which bank you are in. Shown pinned over a scrolled bank."
) {
    val colors = MaterialTheme.board
    Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(Radius.card)).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))) {
        Column(Modifier.padding(top = 44.dp)) {
            BoardPlate(modifier = Modifier.fillMaxWidth().offset(y = (-20).dp)) {
                Way(name = "Meera", state = LampState.LIVE, stateLabel = "Called", detail = "Daughter")
                Hairline()
                Way(name = "Arun", state = LampState.LIVE, stateLabel = "Called", detail = "Son")
                Hairline()
                Way(name = "Dr Rao", state = LampState.OFF, stateLabel = "Not called", detail = "Doctor")
            }
        }
        Column(Modifier.fillMaxWidth().background(colors.ground)) {
            Box(Modifier.padding(horizontal = Spacing.lg)) { SectionPlate("Contacts") }
            Hairline()
        }
    }
}

@Composable
private fun C100EmptyBayWithDisc() = Candidate(
    "2.100", "An empty bay with a glyph disc",
    refines = "EmptyBay",
    note = "Without Shady: the bank's own glyph on a 56dp disc of its accent, the sentence, and the action. For the empty states where DESIGN.md says the emptiness is itself the bad news and a character would be wrong: no contacts, an empty emergency card."
) {
    val colors = MaterialTheme.board
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.card)).background(colors.recess).border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card)).padding(Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        IconDisc(SafeShadeIcons.EmergencyContacts, colors.accentFor("Contacts"), size = 56.dp, glyph = 28.dp)
        Spacer(Modifier.height(Spacing.md))
        Text("Nobody is called yet. Add the first person and a fall reaches them.", style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.md))
        Text("Add a contact", style = MaterialTheme.boardType.nameplate, color = colors.ink, modifier = Modifier.plateClickable(onClick = {}).clip(RoundedCornerShape(Radius.tight)).padding(horizontal = Spacing.md, vertical = Spacing.sm))
    }
}

@Composable
private fun C101SnackbarWithLamp() = Candidate(
    "2.101", "A snackbar with a lamp",
    refines = "Transient messages",
    note = "The night plate snackbar with a 12dp lamp at its leading edge lit by the outcome: teal for landed, amber for queued, trip for refused with the reason. The action stays in amber ink. A message that says 'Not sent' and a message that says 'Sent' look the same at a glance today; the lamp tells them apart."
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(Triple(LampState.LIVE, "Sent to the wearable", null), Triple(LampState.ATTENTION, "Queued until the link is back", "Undo"), Triple(LampState.TRIP, "Not sent. The wearable did not answer.", "Retry")).forEach { (s, msg, action) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.plate)).background(Color(0xFF22282E)).border(Stroke.hairline, Color(0xFF353C43), RoundedCornerShape(Radius.plate)).padding(horizontal = Spacing.lg, vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                PilotLamp(s, size = 12.dp)
                Spacer(Modifier.width(Spacing.md))
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFBF9F5), modifier = Modifier.weight(1f))
                if (action != null) {
                    Spacer(Modifier.width(Spacing.md))
                    Text(action, style = MaterialTheme.boardType.nameplate, color = Color(0xFFFFC46B))
                }
            }
        }
    }
}
