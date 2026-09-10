package com.safeshade.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.safeshade.data.DeviceModel
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.BoardCondensed
import com.safeshade.ui.theme.BoardMono
import com.safeshade.ui.theme.BoardSans
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.accentFor
import com.safeshade.ui.theme.accents
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * The v2.0 candidates: kit members that do not exist yet, drawn so they can
 * be judged on a phone.
 *
 * Every screen in the app is built from the same eight or nine members, and
 * the user's verdict after three phases is that the pages have started to
 * look like one page repeated. This section is the answer to that verdict in
 * the only form that can be judged: forty-one numbered things on the
 * gallery, each either a new member or a refinement of one that exists, each
 * live where it has state, in both themes and at any font scale.
 *
 * Nothing here is used by a screen. A candidate becomes part of the kit only
 * when the user names its number, at which point it moves into the file its
 * family lives in, DESIGN.md gains its rule, and the rest of its group is
 * deleted. Until then this file is a sketchbook that compiles.
 *
 * The numbering is stable: a candidate keeps its number for as long as it
 * exists so a conversation about "2.14" means the same thing next week. The
 * gaps in the sequence are the adoptions of v2.8.0: thirty-three candidates
 * moved into the kit (see DESIGN.md, "Adopted from the v2.0 candidates") and
 * the type candidates went with their fonts. What is left is what was not
 * chosen, kept so a later pick can still be made by number.
 */
fun LazyListScope.kitCandidates() {
    item { CandidateIntro() }

    item { SectionPlate("2.0 · Colour", accent = Color.Unspecified) }
    item { C08HubGrounds() }
    item { C09WashedPlate() }
    item { C11WarmDusk() }
    item { C12IconDiscs() }

    item { SectionPlate("2.0 · Rows", accent = Color.Unspecified) }
    item { C13DiscWay() }
    item { C14OpensVersusReports() }
    item { C15CompactWay() }
    item { C16GroupedBank() }
    item { C17FigureWay() }
    item { C18ActionWay() }

    item { SectionPlate("2.0 · Plates and heroes", accent = Color.Unspecified) }
    item { C19MainsPlateV2() }
    item { C20HubMasthead() }
    item { C21FactWells() }

    item { SectionPlate("2.0 · Actions and navigation", accent = Color.Unspecified) }
    item { C32BarWithWords() }
    item { C33BarWithRule() }

    item { SectionPlate("2.0 · Instruments", accent = Color.Unspecified) }
    item { C35RangeGauge() }
    item { C36SignalBars() }
    item { C37StatusWell() }

    item { SectionPlate("2.0 · People and wearables", accent = Color.Unspecified) }
    item { C39PersonPlateV2() }
    item { C40WearerStrip() }
    item { C41WearableCard() }

    kitCandidates2()
}

// ============================================
// THE FRAME EVERY CANDIDATE SITS IN
// ============================================

@Composable
private fun CandidateIntro() {
    val colors = MaterialTheme.board
    Column {
        Text(
            "v2.0 candidates",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.ink
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "What was not chosen from the hundred and one. The thirty-three that were are in the kit above, on every screen. Each of these is still live where it has state; name a number to adopt it.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted
        )
    }
}

/**
 * The number, the name, what it refines, the thing, and one line on why.
 *
 * The number is set in the readout face inside a brass tag so it reads as a
 * catalogue number rather than as part of the title, and it is the first
 * thing on the line because it is the thing a person will say out loud.
 */
@Composable
internal fun Candidate(
    number: String,
    title: String,
    refines: String? = null,
    note: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.board
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = number,
                style = MaterialTheme.boardType.readout.copy(fontWeight = FontWeight.W700),
                color = colors.ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.tight))
                    .background(colors.brass.copy(alpha = if (colors.isDark) 0.32f else 0.24f))
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.ink)
        }
        if (refines != null) {
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = "Refines $refines",
                style = MaterialTheme.boardType.rowDetail,
                color = colors.inkFaint
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        content()
        if (note != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(text = note, style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
        }
    }
}

// ============================================
// 2.0 · TYPE
// ============================================

/**
 * One specimen shape for every type candidate, so the seven can be compared
 * line for line: a screen title, a mains headline with its status line, a way
 * with a detail line and a state word, and a readout strip.
 */

// ============================================
// 2.0 · COLOUR
// ============================================

private data class HubGround(val name: String, val light: Color, val dark: Color, val icon: ImageVector)

private val HubGrounds = listOf(
    HubGround("Board", Color(0xFFF2EFE9), Color(0xFF14171A), SafeShadeIcons.NavbarBoard),
    HubGround("Circle", Color(0xFFF4EDE6), Color(0xFF1A1614), SafeShadeIcons.NavbarCircle),
    HubGround("Safety", Color(0xFFEDF1EB), Color(0xFF141A16), SafeShadeIcons.NavbarSafety),
    HubGround("Device", Color(0xFFECEEF2), Color(0xFF14171E), SafeShadeIcons.NavbarDevice)
)

@Composable
private fun C08HubGrounds() = Candidate(
    "2.08", "A ground per hub",
    refines = "ground",
    note = "The panel behind everything takes a barely-there cast per hub: bone, warm, sage, slate. Plates stay the same plate colour, so the four hubs tell apart at the edge of the eye without a single plate changing. Sub-pages keep their hub's ground."
) {
    val colors = MaterialTheme.board
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        HubGrounds.forEach { hub ->
            val ground = if (colors.isDark) hub.dark else hub.light
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.card))
                    .background(ground)
                    .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))
                    .padding(Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(Radius.plate))
                        .background(colors.plate)
                        .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(hub.icon, contentDescription = null, tint = when (hub.name) { "Board" -> colors.accentClay; "Circle" -> colors.accentSky; "Safety" -> colors.accentSage; else -> colors.accentPlum }, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(hub.name, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
                Text(hexOf(ground), style = MaterialTheme.boardType.readout.copy(fontSize = 11.sp), color = colors.inkFaint)
            }
        }
    }
}

@Composable
private fun C09WashedPlate() = Candidate(
    "2.09", "A washed plate with an accent rule",
    refines = "BoardPlate",
    note = "The accent at 0.10 alpha behind the whole plate and at full strength as a 3dp rule down the leading edge. Two on a screen tell apart at a glance; it is the same ink on top. For the one plate on a page that is about a single thing: a person, a wearable, a zone."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf("Home" to "Baba is inside · since 07:40", "Park" to "Left 8 minutes ago").forEach { (name, line) ->
            val accent = colors.accentFor(name)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.card))
                    .background(accent.copy(alpha = 0.10f))
                    .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))
                    .height(IntrinsicSize.Min)
            ) {
                Box(Modifier.width(Stroke.heavy).fillMaxHeight().background(accent))
                Column(Modifier.padding(Spacing.lg)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Text(line, style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
                }
            }
        }
    }
}

@Composable
private fun C11WarmDusk() = Candidate(
    "2.11", "A warmer bone",
    refines = "the light theme's three neutrals",
    note = "Every neutral pulled two points warmer, towards the masthead beige the emails use, so the phone and the mail read as one material. Plates stay near white. Contrast is printed live and holds the 4.5 floor."
) {
    PaletteSwatches(
        ground = Color(0xFFF5EFE6), plate = Color(0xFFFDFAF4), recess = Color(0xFFEADFD0), hairline = Color(0xFFD9CDBB),
        ink = Color(0xFF22282E), inkMuted = Color(0xFF55514B), inkFaint = Color(0xFF69655F)
    )
}

@Composable
internal fun PaletteSwatches(
    ground: Color, plate: Color, recess: Color, hairline: Color,
    ink: Color, inkMuted: Color, inkFaint: Color
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(ground)
            .border(Stroke.hairline, hairline, RoundedCornerShape(Radius.card))
            .padding(Spacing.md)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.card))
                .background(plate)
                .border(Stroke.hairline, hairline, RoundedCornerShape(Radius.card))
                .padding(Spacing.lg)
        ) {
            Text("Baba is covered", style = MaterialTheme.typography.headlineSmall, color = ink)
            Text("SafeShade S1 · connected", style = MaterialTheme.boardType.rowDetail.copy(fontWeight = FontWeight.W600), color = inkMuted)
            Spacer(Modifier.height(Spacing.sm))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(Radius.plate))
                    .background(recess)
                    .border(Stroke.hairline, hairline, RoundedCornerShape(Radius.plate))
                    .padding(horizontal = Spacing.md),
                contentAlignment = Alignment.CenterStart
            ) { Text("A field set into the panel", style = MaterialTheme.typography.bodyMedium, color = inkFaint) }
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                ContrastReadout("ink", ink, plate, Modifier.weight(1f))
                ContrastReadout("muted", inkMuted, plate, Modifier.weight(1f))
                ContrastReadout("faint", inkFaint, plate, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf("ground" to ground, "plate" to plate, "recess" to recess, "hairline" to hairline).forEach { (n, c) ->
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(Radius.tight))
                            .background(c)
                            .border(Stroke.hairline, hairline, RoundedCornerShape(Radius.tight))
                    )
                    Text(n, style = MaterialTheme.boardType.nameplateSmall, color = inkMuted)
                    Text(hexOf(c), style = MaterialTheme.boardType.readout.copy(fontSize = 11.sp), color = inkFaint)
                }
            }
        }
    }
}

@Composable
internal fun ContrastReadout(name: String, fg: Color, bg: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(name, style = MaterialTheme.boardType.nameplateSmall, color = fg)
        Text("%.2f:1".format(contrastRatio(fg, bg)), style = MaterialTheme.boardType.readout, color = fg)
    }
}

internal fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

internal fun hexOf(c: Color): String {
    val r = (c.red * 255).toInt(); val g = (c.green * 255).toInt(); val b = (c.blue * 255).toInt()
    return "#%02X%02X%02X".format(r, g, b)
}

@Composable
private fun C12IconDiscs() = Candidate(
    "2.12", "Glyphs on discs",
    refines = "the icon on a Way",
    note = "The way's 20dp glyph on a 36dp disc of its own accent at 0.14 alpha, the same treatment the selected tab already gets. Twelve accents become twelve visible discs, and a bank of rows reads as a bank of labelled things rather than a list with small drawings in the margin."
) {
    val colors = MaterialTheme.board
    BoardPlate {
        Row(
            Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                SafeShadeIcons.FallDetection to "Fall",
                SafeShadeIcons.SafeZone to "Zones",
                SafeShadeIcons.MedicalId to "Medical",
                SafeShadeIcons.Reminders to "Reminders",
                SafeShadeIcons.Lights to "Lights",
                SafeShadeIcons.WalkieTalkie to "Talk"
            ).forEach { (icon, key) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconDisc(icon, colors.accentFor(key))
                    Spacer(Modifier.height(Spacing.xs))
                    Text(key, style = MaterialTheme.typography.labelSmall, color = colors.inkFaint)
                }
            }
        }
    }
}

// ============================================
// 2.0 · ROWS
// ============================================

@Composable
private fun C13DiscWay() = Candidate(
    "2.13", "A way with its glyph on a disc",
    refines = "Way",
    note = "2.12 applied to the row. The disc takes 36dp where the glyph took 20, so the title moves 16dp right; everything else is the way as it is. Compare the plain way under it."
) {
    val colors = MaterialTheme.board
    var on by remember { mutableStateOf(true) }
    BoardPlate {
        DiscWay("Fall detection", "30 seconds to cancel before a call.", SafeShadeIcons.FallDetection, LampState.LIVE, "Medium", colors.accentFor("Fall detection"))
        Hairline()
        DiscWay("Safe zones", "1 zone · told when leaving", SafeShadeIcons.SafeZone, LampState.OFF, "Not known", colors.accentFor("Safe zones"))
        Hairline()
        Way(name = "Call after a fall", state = LampState.LIVE, stateLabel = "Live", detail = "Calls Meera if the countdown runs out.", icon = SafeShadeIcons.CallAfterAFall, checked = on, onCheckedChange = { on = it })
    }
}

@Composable
internal fun DiscWay(name: String, detail: String?, icon: ImageVector, state: LampState, stateLabel: String, accent: Color, trailingChevron: Boolean = false) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .rowClickable(role = Role.Button, onClick = {})
            .padding(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = Spacing.sm)
            .height(IntrinsicSize.Min)
    ) {
        IconDisc(icon, accent)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f).defaultMinSize(minHeight = 36.dp), verticalArrangement = Arrangement.Center) {
            Nameplate(name)
            if (detail != null) {
                Spacer(Modifier.height(Spacing.xxs))
                Text(detail, style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
            }
        }
        Spacer(Modifier.width(Spacing.md))
        StateWord(stateLabel, state)
        Spacer(Modifier.width(Spacing.md))
        if (trailingChevron) {
            Icon(SafeShadeIcons.ArrowRight01, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(18.dp))
        } else {
            BusTick(state, Modifier.fillMaxHeight())
        }
    }
}

@Composable
private fun C14OpensVersusReports() = Candidate(
    "2.14", "Rows that open look different from rows that report",
    refines = "Way",
    note = "Today a row that opens a page, a row that reports a state and a row with a switch all end in the same tick. Here a row that opens ends in a chevron and keeps its state word; a row that only reports keeps the tick; a switch is a switch. A guardian learns which rows go somewhere without tapping to find out."
) {
    var on by remember { mutableStateOf(false) }
    BoardPlate {
        NavWay("Trip log", "5 logged", LampState.LIVE, SafeShadeIcons.History)
        Hairline()
        NavWay("Medical ID", "6 of 11 fields", LampState.LIVE, SafeShadeIcons.MedicalId, detail = null)
        Hairline()
        Way(name = "Link", state = LampState.OFF, stateLabel = "Not connected", detail = "Reports only; nothing to open.", icon = SafeShadeIcons.Bluetooth)
        Hairline()
        Way(name = "Text as well", state = if (on) LampState.LIVE else LampState.OFF, stateLabel = if (on) "Live" else "Off", icon = SafeShadeIcons.TextAsWell, checked = on, onCheckedChange = { on = it })
    }
}

/** A way that opens a page: state word, then a chevron, and no bus tick. */
@Composable
internal fun NavWay(name: String, stateLabel: String, state: LampState, icon: ImageVector, detail: String? = null) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .rowClickable(role = Role.Button, onClick = {})
            .padding(start = Spacing.lg, end = Spacing.md, top = Spacing.md, bottom = Spacing.md)
            .defaultMinSize(minHeight = 24.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.accentFor(name), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Nameplate(name)
            if (detail != null) Text(detail, style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
        }
        Spacer(Modifier.width(Spacing.md))
        StateWord(stateLabel, state)
        Spacer(Modifier.width(Spacing.sm))
        Icon(SafeShadeIcons.ArrowRight01, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun C15CompactWay() = Candidate(
    "2.15", "A compact way",
    refines = "Way, for dense banks",
    note = "One line, no glyph, 40dp, the state word in the readout face. For a bank of eight facts read off the firmware, a list of paired wearables, a ladder of rungs: places where the full way's 48dp and detail line make a page twice as tall as its content."
) {
    val colors = MaterialTheme.board
    BoardPlate {
        listOf(
            Triple("Trips at", "1.9 g", LampState.OFF),
            Triple("Rotary cycles", "Time · Weather · Steps", LampState.OFF),
            Triple("Asks for a fix", "every 5 min", LampState.OFF),
            Triple("Screen lock", "hidden", LampState.LIVE)
        ).forEachIndexed { i, (k, v, s) ->
            if (i > 0) Hairline()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            ) {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(Spacing.md))
                // The value yields before the key does: at 1.3x a long mono
                // value was folding "Rotary cycles" onto two lines.
                Text(v, style = MaterialTheme.boardType.readout, color = if (s == LampState.LIVE) colors.inkLive else colors.inkMuted, textAlign = TextAlign.End, modifier = Modifier.weight(1.4f))
            }
        }
    }
}

@Composable
private fun C16GroupedBank() = Candidate(
    "2.16", "A bank with named groups inside it",
    refines = "the Device page's eleven ways",
    note = "One plate, three groups, each with a small nameplate above its rows and a hairline between groups drawn heavier. Eleven rows become three questions: the link, the person, the records. A group's nameplate is not a section plate; there is no brass rule and no caps."
) {
    BoardPlate {
        GroupTitle("Link")
        NavWay("Paired devices", "1 saved", LampState.LIVE, SafeShadeIcons.PairedDevices)
        Hairline()
        NavWay("Nearby", "—", LampState.UNKNOWN, SafeShadeIcons.BluetoothNearby)
        Hairline()
        NavWay("Find the device", "Ready", LampState.OFF, SafeShadeIcons.FindTheDevice)
        GroupRule()
        GroupTitle("The wearable")
        NavWay("Adaptive mode", "Elderly", LampState.OFF, SafeShadeIcons.AdaptiveMode)
        Hairline()
        NavWay("Lights", "Torch", LampState.OFF, SafeShadeIcons.Lights)
        Hairline()
        NavWay("Firmware", "—", LampState.UNKNOWN, SafeShadeIcons.CpuChip)
        GroupRule()
        GroupTitle("Records")
        NavWay("Telemetry", "Live", LampState.LIVE, SafeShadeIcons.Telemetry)
        Hairline()
        NavWay("Ride log", "None", LampState.OFF, SafeShadeIcons.Bicycle01)
    }
}

@Composable
internal fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.boardType.nameplateSmall,
        color = MaterialTheme.board.inkMuted,
        modifier = Modifier.padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.xxs)
    )
}

@Composable
internal fun GroupRule() {
    Box(Modifier.fillMaxWidth().height(Stroke.rule).background(MaterialTheme.board.hairline))
}

@Composable
private fun C17FigureWay() = Candidate(
    "2.17", "A way whose state is a figure",
    refines = "Way, for logs",
    note = "The right side is a mono readout with its unit set small, not a caps state word. For a ride, a trip, a recording: rows whose answer is a quantity. A caps word says a condition; a figure says a measurement, and the two should not wear the same clothes."
) {
    val colors = MaterialTheme.board
    BoardPlate {
        listOf(
            Triple("Tuesday ride", "moving 42 min · top 31 km/h · 118 fixes", "12.4" to "km"),
            Triple("Walk home", "moving 18 min · 41 fixes", "1.6" to "km"),
            Triple("Test recording", "On this phone · 10 s", "43" to "KB")
        ).forEachIndexed { i, (name, detail, figure) ->
            if (i > 0) Hairline()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().rowClickable(role = Role.Button, onClick = {}).padding(start = Spacing.lg, end = Spacing.md, top = Spacing.md, bottom = Spacing.md).height(IntrinsicSize.Min)
            ) {
                Column(Modifier.weight(1f)) {
                    Nameplate(name)
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(detail, style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
                }
                Spacer(Modifier.width(Spacing.md))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(figure.first, style = MaterialTheme.boardType.readout.copy(fontSize = 20.sp, lineHeight = 24.sp), color = colors.ink)
                    Spacer(Modifier.width(Spacing.xxs))
                    Text(figure.second, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted, modifier = Modifier.padding(bottom = 2.dp))
                }
                Spacer(Modifier.width(Spacing.md))
                BusTick(LampState.OFF, Modifier.fillMaxHeight())
            }
        }
    }
}

@Composable
private fun C18ActionWay() = Candidate(
    "2.18", "An action as a way",
    refines = "the quiet buttons at the foot of a bank",
    note = "Add a person, pair another wearable, add a zone: today each is a 56dp button under its plate, which makes every bank end in a button. Here the action is the last row of the bank, its glyph on a disc in ink, the title in the nameplate voice, and a plus at the right. The bank ends where it ends."
) {
    val colors = MaterialTheme.board
    BoardPlate {
        PersonRow(name = "Baba", avatarId = AvatarSpec.PRESETS[0].encode(), detail = "SafeShade S1", state = LampState.LIVE, stateLabel = "Live")
        Hairline()
        PersonRow(name = "Mia", avatarId = "", detail = "No wearable", state = LampState.OFF, stateLabel = "No wearable")
        Hairline()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().rowClickable(role = Role.Button, onClick = {}).padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(colors.recess).border(Stroke.hairline, colors.hairline, CircleShape), contentAlignment = Alignment.Center) {
                Icon(SafeShadeIcons.UserAdd, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Spacing.md))
            Nameplate("Add a person", modifier = Modifier.weight(1f))
            Icon(SafeShadeIcons.PlusAdd, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
        }
    }
}

// ============================================
// 2.0 · PLATES AND HEROES
// ============================================

@Composable
private fun C19MainsPlateV2() = Candidate(
    "2.19", "Mains plate with the person on it",
    refines = "MainsPlate",
    note = "The face at 56dp is the headline: this is who is covered. The state is one large word beside a lamp, the wearable's name under it, and the instruments are three wells set into the plate rather than a strip under a hairline. Shady moves off the plate; it has the stage below."
) {
    val colors = MaterialTheme.board
    BoardPlate {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Avatar(avatarId = AvatarSpec.PRESETS[3].encode(), name = "Baba", size = 56.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Baba", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PilotLamp(LampState.LIVE, size = 12.dp)
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Covered", style = MaterialTheme.boardType.nameplate, color = colors.inkLive)
                    Text(" · SafeShade S1", style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
                }
            }
        }
        Row(Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Well("Battery", "84", "%", Modifier.weight(1f))
            Well("Signal", "-62", "dBm", Modifier.weight(1f))
            Well("Seen", "just now", null, Modifier.weight(1f), compact = true)
        }
    }
}

@Composable
internal fun Well(label: String, value: String, unit: String?, modifier: Modifier = Modifier, state: LampState? = null, compact: Boolean = false) {
    val colors = MaterialTheme.board
    Column(
        modifier
            .clip(RoundedCornerShape(Radius.plate))
            .background(colors.recess)
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                // A phrase in the readout face at 20sp wraps inside a third of
                // a plate; a phrase is set a step smaller than a figure.
                style = if (compact) MaterialTheme.boardType.readout.copy(fontSize = 15.sp, lineHeight = 24.sp)
                else MaterialTheme.boardType.readout.copy(fontSize = 20.sp, lineHeight = 24.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = when (state) {
                    LampState.ATTENTION -> colors.inkAttention
                    LampState.TRIP -> colors.inkTrip
                    else -> colors.ink
                }
            )
            if (unit != null) {
                Spacer(Modifier.width(Spacing.xxs))
                Text(unit, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun C20HubMasthead() = Candidate(
    "2.20", "A masthead per hub",
    refines = "the hub screens' bare title",
    note = "Circle, Safety and Device open on a word alone. Here each opens on its glyph on a disc in the hub's accent, the title, and one line that answers the hub's question: who is covered, what is armed, whether the link is up. Paired with 2.08 the four hubs stop being the same page with a different word at the top."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        listOf(
            Triple("Circle", "2 people · Baba is not connected", SafeShadeIcons.NavbarCircle),
            Triple("Safety", "Fall detection on · calls Meera · 112 and 7 more", SafeShadeIcons.NavbarSafety),
            Triple("Device", "SafeShade S1 · not connected · seen 7 Sept", SafeShadeIcons.NavbarDevice)
        ).forEach { (title, line, icon) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconDisc(icon, when (title) { "Circle" -> colors.accentSky; "Safety" -> colors.accentSage; else -> colors.accentPlum }, size = 44.dp, glyph = 24.dp)
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = colors.ink)
                    Text(line, style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
                }
            }
        }
    }
}

@Composable
private fun C21FactWells() = Candidate(
    "2.21", "Fact wells",
    refines = "the readout strips on Vitals, Ride log and Lost",
    note = "Readouts as wells in a grid: label small, figure large in mono, unit small, a dash where nothing was measured. Three across on a phone, wrapping to two rows when there are more. The Vitals page's three readouts and the Lost page's two dashes both become this."
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Well("Heart rate", "72", "bpm", Modifier.weight(1f))
            Well("Blood oxygen", "97", "%", Modifier.weight(1f))
            Well("Temperature", "38.2", "°C", Modifier.weight(1f), state = LampState.ATTENTION)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Well("This phone heard", "—", null, Modifier.weight(1f))
            Well("Community heard", "2 d ago", null, Modifier.weight(1f), compact = true)
        }
    }
}

// ============================================
// 2.0 · EXPLAINERS
// ============================================

// ============================================
// 2.0 · ACTIONS AND NAVIGATION
// ============================================

@Composable
private fun C32BarWithWords() = Candidate(
    "2.32", "The bar with words",
    refines = "the bottom bar",
    note = "Each tab's word under its glyph in the small nameplate voice, the selected one in its accent and filled. A guardian who has never used a phone app learns the four places by name. The SOS disc keeps its place and gains no word: its glyph is the word."
) {
    MockBar(words = true, rule = false)
}

@Composable
private fun C33BarWithRule() = Candidate(
    "2.33", "The bar with a rule",
    refines = "the bottom bar",
    note = "No disc behind the selected tab; a 3dp rule in the tab's accent along the top edge of the bar, over that tab, the way a bus tick marks a way. The bar reads as part of the panel rather than as a row of buttons, and the selection is still carried by weight as well as colour."
) {
    MockBar(words = false, rule = true)
}

@Composable
internal fun MockBar(words: Boolean, rule: Boolean) {
    val colors = MaterialTheme.board
    var selected by remember { mutableIntStateOf(1) }
    val tabs = listOf(
        "Board" to SafeShadeIcons.NavbarBoard, "Circle" to SafeShadeIcons.NavbarCircle,
        "SOS" to SafeShadeIcons.Cross, "Safety" to SafeShadeIcons.NavbarSafety, "Device" to SafeShadeIcons.NavbarDevice
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(colors.plate)
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))
    ) {
        Row(Modifier.fillMaxWidth().height(if (words) 72.dp else 64.dp)) {
            tabs.forEachIndexed { i, (name, icon) ->
                val on = i == selected
                val accent = colors.accentFor(name)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .rowClickable(role = Role.Tab, onClick = { if (i != 2) selected = i }),
                    contentAlignment = Alignment.Center
                ) {
                    if (rule && on) {
                        Box(Modifier.align(Alignment.TopCenter).padding(top = Stroke.hairline).width(40.dp).height(Stroke.heavy).clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp)).background(accent))
                    }
                    if (i == 2) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(colors.lampTrip), contentAlignment = Alignment.Center) {
                            // The six-armed asterisk the real bar keeps, as a glyph.
                            Text("✱", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.W700), color = colors.plate)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (!rule && on) {
                                IconDisc(icon, accent, size = 36.dp, glyph = 22.dp)
                            } else {
                                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                    Icon(icon, contentDescription = null, tint = if (on) accent else colors.inkFaint, modifier = Modifier.size(22.dp))
                                }
                            }
                            if (words) {
                                Text(name, style = MaterialTheme.boardType.nameplateSmall.copy(fontSize = 11.sp), color = if (on) accent else colors.inkFaint)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// 2.0 · INSTRUMENTS
// ============================================

@Composable
private fun C35RangeGauge() = Candidate(
    "2.35", "A gauge with its range",
    refines = "Gauge",
    note = "The same labelled plate with a 6dp channel under the figure showing where the value sits in its range, and ticks where the thresholds are. A battery at 84 percent, a heart rate at 72 in a 50 to 120 band. The bar is flat and the ticks are hairlines; nothing is a dial."
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        RangeGauge("Battery", "84", "%", 0.84f, listOf(0.15f, 0.30f), Modifier.weight(1f))
        RangeGauge("Heart rate", "72", "bpm", 0.44f, listOf(0.20f, 0.80f), Modifier.weight(1f), LampState.LIVE)
    }
}

@Composable
internal fun RangeGauge(label: String, value: String, unit: String, fraction: Float, ticks: List<Float>, modifier: Modifier = Modifier, state: LampState? = null) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.boardType.readoutLarge, color = colors.ink)
                Spacer(Modifier.width(Spacing.xs))
                Text(unit, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(Spacing.sm))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(Radius.tight))
                    .background(colors.recess)
                    .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.tight))
            ) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(if (state == LampState.LIVE) colors.lampLive else colors.ink))
                ticks.forEach { t ->
                    Box(Modifier.fillMaxWidth(t).fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
                        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.inkFaint))
                    }
                }
            }
        }
    }
}

@Composable
private fun C36SignalBars() = Candidate(
    "2.36", "Signal as bars",
    refines = "the Signal readout on the mains plate",
    note = "Five machined bars rising to the right, lit up to the strength, with the dBm figure beside them for the person who reads figures. A readout of -62 asks the reader to know that -62 is good; four lit bars does not."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            listOf(-55 to 5, -62 to 4, -75 to 3, -88 to 2, -95 to 1).forEach { (dbm, lit) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) { i ->
                            Box(
                                Modifier
                                    .width(5.dp)
                                    .height((8 + i * 4).dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(if (i < lit) (if (lit <= 2) colors.lampAttention else colors.lampLive) else colors.lampOff)
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text("$dbm", style = MaterialTheme.boardType.readout.copy(fontSize = 12.sp), color = colors.inkMuted)
                }
            }
        }
    }
}

@Composable
private fun C37StatusWell() = Candidate(
    "2.37", "A status well",
    refines = "the hub-level state",
    note = "One large lamp in a recessed well with the state word beside it in the nameplate voice and the reason under it. For the top of a sub-page whose whole point is one condition: Out of reach, Vitals, Lost, Evidence. It says the answer before the controls that change it."
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatusWell(LampState.LIVE, "Within reach", "Connected · 84% · seen just now")
        StatusWell(LampState.ATTENTION, "Out of reach", "Since 09:12 · 45 min · you were told")
        StatusWell(LampState.UNKNOWN, "No reading yet", "Nothing has measured a heart rate")
    }
}

@Composable
internal fun StatusWell(state: LampState, word: String, reason: String) {
    val colors = MaterialTheme.board
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(colors.recess)
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PilotLamp(state, size = 28.dp, description = word)
        Spacer(Modifier.width(Spacing.lg))
        Column {
            Text(
                word,
                style = MaterialTheme.typography.titleMedium,
                color = when (state) {
                    LampState.LIVE -> colors.inkLive
                    LampState.ATTENTION -> colors.inkAttention
                    LampState.TRIP -> colors.inkTrip
                    else -> colors.ink
                }
            )
            Text(reason, style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
        }
    }
}

// ============================================
// 2.0 · PEOPLE AND WEARABLES
// ============================================

@Composable
private fun C39PersonPlateV2() = Candidate(
    "2.39", "The person plate, again",
    refines = "the family dashboard's plate",
    note = "Face at 56, name, the mode as a qualifier chip, a lamp with its word; three wells; the captioned glyph strip from 2.30. The same facts as today's plate, in less height, with nothing that wraps."
) {
    val colors = MaterialTheme.board
    BoardPlate {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Avatar(avatarId = AvatarSpec.PRESETS[3].encode(), name = "Baba", size = 56.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Baba", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                    Spacer(Modifier.width(Spacing.sm))
                    QualifierChip("Elderly")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PilotLamp(LampState.OFF, size = 10.dp)
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Not connected", style = MaterialTheme.boardType.rowDetail.copy(fontWeight = FontWeight.W600), color = colors.inkMuted)
                }
            }
        }
        Row(Modifier.padding(horizontal = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Well("Battery", "—", null, Modifier.weight(1f))
            Well("Seen", "2 d ago", null, Modifier.weight(1f), compact = true)
            Well("Alert", "SOS · 3 d", null, Modifier.weight(1.3f), compact = true)
        }
        Spacer(Modifier.height(Spacing.md))
        Row(Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(SafeShadeIcons.SendMessage to "Message", SafeShadeIcons.WhereNavigation to "Where", SafeShadeIcons.TelephoneCall to "Call").forEach { (icon, word) ->
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Radius.plate))
                        .background(colors.ground)
                        .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate))
                        .plateClickable(onClick = {})
                        .padding(vertical = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, contentDescription = null, tint = colors.ink, modifier = Modifier.size(24.dp))
                    Text(word, style = MaterialTheme.boardType.nameplateSmall.copy(fontSize = 11.sp), color = colors.inkMuted)
                }
            }
        }
    }
}

@Composable
private fun C40WearerStrip() = Candidate(
    "2.40", "A wearer strip",
    refines = "the person switch at the top of Safety and Device",
    note = "The people this phone looks after as a row of faces, the chosen one ringed in brass with the name under it, a lamp dot at each face's corner for the link. A guardian of two switches whose settings they are looking at in one tap, without a page."
) {
    val colors = MaterialTheme.board
    var chosen by remember { mutableIntStateOf(0) }
    val people = listOf("Baba" to AvatarSpec.PRESETS[3].encode(), "Mia" to "", "Dadu" to AvatarSpec.PRESETS[7].encode())
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        people.forEachIndexed { i, (name, id) ->
            val on = i == chosen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.plateClickable(onClick = { chosen = i }).padding(Spacing.xs)
            ) {
                Box {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(if (on) 2.dp else Stroke.hairline, if (on) colors.brass else colors.hairline, CircleShape)
                            .padding(3.dp)
                    ) {
                        Avatar(avatarId = id, name = name, size = 50.dp, ringed = false)
                    }
                    Box(Modifier.align(Alignment.BottomEnd)) {
                        PilotLamp(if (i == 0) LampState.OFF else if (i == 2) LampState.LIVE else LampState.UNKNOWN, size = 12.dp)
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(name, style = MaterialTheme.boardType.nameplateSmall, color = if (on) colors.ink else colors.inkMuted)
            }
        }
    }
}

@Composable
private fun C41WearableCard() = Candidate(
    "2.41", "The wearable's card",
    refines = "the Device page's head plate",
    note = "The product silhouette at 72 beside the name, the model and who wears it, a lamp and its word; under a rule, the battery as a channel with the figure and the last-seen time. The Device page opens on the object it is about, drawn, not on two lines of text."
) {
    val colors = MaterialTheme.board
    BoardPlate {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            ProductSilhouette(DeviceModel.S1, size = 72.dp, accent = colors.accentFor("S1"))
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text("SafeShade S1", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                Text("Worn by Baba · Elderly", style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PilotLamp(LampState.OFF, size = 10.dp)
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Not connected", style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
                }
            }
        }
        Hairline()
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Battery", style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
                Spacer(Modifier.height(Spacing.xs))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(Radius.tight))
                        .background(colors.recess)
                        .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.tight))
                ) {
                    Box(Modifier.fillMaxWidth(0.84f).fillMaxHeight().background(colors.lampLive))
                }
            }
            Spacer(Modifier.width(Spacing.lg))
            Column(horizontalAlignment = Alignment.End) {
                Text("84%", style = MaterialTheme.boardType.readout.copy(fontSize = 20.sp, lineHeight = 24.sp), color = colors.ink)
                Text("seen 7 Sept", style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
