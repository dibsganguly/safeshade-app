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
import com.safeshade.ui.theme.CandidateBricolage
import com.safeshade.ui.theme.CandidateFraunces
import com.safeshade.ui.theme.CandidateGeistMono
import com.safeshade.ui.theme.CandidateInstrument
import com.safeshade.ui.theme.CandidateInstrumentCondensed
import com.safeshade.ui.theme.CandidateJakarta
import com.safeshade.ui.theme.CandidateManrope
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
 * deleted. Until then this file is a sketchbook that compiles, and the
 * candidate type families in `CandidateType.kt` exist only for it.
 *
 * The numbering is stable: a candidate keeps its number for as long as it
 * exists so a conversation about "2.14" means the same thing next week.
 */
fun LazyListScope.kitCandidates() {
    item { CandidateIntro() }

    item { SectionPlate("2.0 · Type", accent = Color.Unspecified) }
    item { C01BricolageInstrument() }
    item { C02FrauncesHeadlines() }
    item { C03Manrope() }
    item { C04Jakarta() }
    item { C05Instrument() }
    item { C06GeistMono() }
    item { C07ReadabilityStep() }

    item { SectionPlate("2.0 · Colour", accent = Color.Unspecified) }
    item { C08HubGrounds() }
    item { C09WashedPlate() }
    item { C10DeeperNight() }
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
    item { C22LedgerPlate() }
    item { C23TimelinePlate() }
    item { C24SegmentedChoice() }

    item { SectionPlate("2.0 · Explainers", accent = Color.Unspecified) }
    item { C25Footnote() }
    item { C26Callout() }
    item { C27WhatHappensChain() }
    item { C28RowHelp() }
    item { C29QualifierChips() }

    item { SectionPlate("2.0 · Actions and navigation", accent = Color.Unspecified) }
    item { C30CaptionedStrip() }
    item { C31FigureButton() }
    item { C32BarWithWords() }
    item { C33BarWithRule() }
    item { C34FootCommitBar() }

    item { SectionPlate("2.0 · Instruments", accent = Color.Unspecified) }
    item { C35RangeGauge() }
    item { C36SignalBars() }
    item { C37StatusWell() }
    item { C38BarsPlate() }

    item { SectionPlate("2.0 · People and wearables", accent = Color.Unspecified) }
    item { C39PersonPlateV2() }
    item { C40WearerStrip() }
    item { C41WearableCard() }
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
            "Forty-one numbered kits, none of them on a screen yet. Each is live where it has state. Name a number to adopt it.",
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
private fun Candidate(
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
@Composable
private fun TypeSpecimen(
    display: FontFamily,
    body: FontFamily,
    condensed: FontFamily,
    mono: FontFamily,
    titleSize: Int = 28,
    rowTitleSize: Int = 15,
    detailSize: Int = 13,
    condensedTracking: Float = 0.12f
) {
    val colors = MaterialTheme.board
    BoardPlate {
        Column(Modifier.padding(Spacing.lg)) {
            Text(
                "Safety",
                style = TextStyle(fontFamily = display, fontWeight = FontWeight.W700, fontSize = titleSize.sp, lineHeight = (titleSize + 6).sp, letterSpacing = (-0.01).em),
                color = colors.ink
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                "Baba is covered",
                style = TextStyle(fontFamily = display, fontWeight = FontWeight.W600, fontSize = 20.sp, lineHeight = 26.sp),
                color = colors.ink
            )
            Text(
                "SafeShade S1 · connected · 84%",
                style = TextStyle(fontFamily = body, fontWeight = FontWeight.W600, fontSize = detailSize.sp, lineHeight = (detailSize + 4).sp),
                color = colors.inkMuted
            )
            Spacer(Modifier.height(Spacing.md))
            Hairline()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = Spacing.md).height(IntrinsicSize.Min)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Fall detection",
                        style = TextStyle(fontFamily = body, fontWeight = FontWeight.W600, fontSize = rowTitleSize.sp, lineHeight = (rowTitleSize + 5).sp),
                        color = colors.ink
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "30 seconds to cancel before a call.",
                        style = TextStyle(fontFamily = body, fontWeight = FontWeight.W400, fontSize = detailSize.sp, lineHeight = (detailSize + 4).sp),
                        color = colors.inkFaint
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Text(
                    "MEDIUM",
                    style = TextStyle(fontFamily = condensed, fontWeight = FontWeight.W700, fontSize = 13.sp, letterSpacing = condensedTracking.em),
                    color = colors.inkLive
                )
                Spacer(Modifier.width(Spacing.md))
                BusTick(LampState.LIVE, Modifier.fillMaxHeight())
            }
            Hairline()
            Spacer(Modifier.height(Spacing.md))
            Text(
                "SECTION PLATE",
                style = TextStyle(fontFamily = condensed, fontWeight = FontWeight.W700, fontSize = 13.sp, letterSpacing = 0.18.em),
                color = colors.inkMuted
            )
            Spacer(Modifier.height(Spacing.sm))
            Row {
                MonoSpecimen("84%", "Battery", mono, Modifier.weight(1f))
                MonoSpecimen("-62 dBm", "Signal", mono, Modifier.weight(1f))
                MonoSpecimen("20.29, 85.82", "Fix", mono, Modifier.weight(1.4f))
            }
        }
    }
}

@Composable
private fun MonoSpecimen(value: String, label: String, mono: FontFamily, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    Column(modifier) {
        Text(label, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
        Text(
            value,
            style = TextStyle(fontFamily = mono, fontWeight = FontWeight.W500, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em),
            color = colors.ink
        )
    }
}

@Composable
private fun C01BricolageInstrument() = Candidate(
    "2.01", "Bricolage Grotesque titles, Instrument Sans everything else",
    refines = "the whole scale",
    note = "Bricolage has a lowercase with real character at title size and stays plain small; Instrument is quieter than Archivo in the rows. The condensed voice is Instrument at width 75, so it is still one file for body and plates."
) {
    TypeSpecimen(CandidateBricolage, CandidateInstrument, CandidateInstrumentCondensed, BoardMono)
}

@Composable
private fun C02FrauncesHeadlines() = Candidate(
    "2.02", "Fraunces headlines over Archivo",
    refines = "display and headline only",
    note = "A soft serif for the three lines that greet a worried person, and nothing else. Body, rows and plates stay exactly as they are. The warmest of the seven; also the biggest departure from a panel."
) {
    TypeSpecimen(CandidateFraunces, BoardSans, BoardCondensed, BoardMono)
}

@Composable
private fun C03Manrope() = Candidate(
    "2.03", "Manrope throughout",
    refines = "the whole scale",
    note = "Rounded terminals and open apertures. Reads a step friendlier than Archivo at the same size and slightly wider, so long row titles wrap a little sooner. No width axis: the condensed voice would be Archivo's, kept."
) {
    TypeSpecimen(CandidateManrope, CandidateManrope, BoardCondensed, BoardMono)
}

@Composable
private fun C04Jakarta() = Candidate(
    "2.04", "Plus Jakarta Sans throughout",
    refines = "the whole scale",
    note = "Geometric and more contemporary than Manrope, with a taller x-height that helps at 13sp detail size. The heaviest weights are very black, which suits a masthead and would need restraint elsewhere."
) {
    TypeSpecimen(CandidateJakarta, CandidateJakarta, BoardCondensed, BoardMono)
}

@Composable
private fun C05Instrument() = Candidate(
    "2.05", "Instrument Sans throughout, its own condensed",
    refines = "the whole scale",
    note = "The closest to the current voice: a plain grotesk with a width axis, so the one-family-two-widths rule still holds. Slightly narrower and lighter than Archivo; the difference shows most in the rows."
) {
    TypeSpecimen(CandidateInstrument, CandidateInstrument, CandidateInstrumentCondensed, BoardMono)
}

@Composable
private fun C06GeistMono() = Candidate(
    "2.06", "Geist Mono for readouts",
    refines = "Readout, Gauge, countdown",
    note = "Both faces at the sizes the app draws them. Geist has a dotted zero and squarer figures; Azeret is more condensed so more fits across a strip. Judge on the coordinates and the large number."
) {
    BoardPlate {
        Row(Modifier.padding(Spacing.lg)) {
            MonoColumn("Azeret (now)", BoardMono, Modifier.weight(1f))
            Spacer(Modifier.width(Spacing.lg))
            MonoColumn("Geist Mono", CandidateGeistMono, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MonoColumn(title: String, mono: FontFamily, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    Column(modifier) {
        Text(title, style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
        Spacer(Modifier.height(Spacing.sm))
        Text("28", style = TextStyle(fontFamily = mono, fontWeight = FontWeight.W500, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.03).em), color = colors.ink)
        Text("84% · -62", style = TextStyle(fontFamily = mono, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 20.sp), color = colors.ink)
        Text("20.2961", style = TextStyle(fontFamily = mono, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 20.sp), color = colors.ink)
        Text("09:12 · 45 min", style = TextStyle(fontFamily = mono, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 20.sp), color = colors.ink)
        Text("0 1 l I O", style = TextStyle(fontFamily = mono, fontWeight = FontWeight.W500, fontSize = 14.sp, lineHeight = 20.sp), color = colors.inkMuted)
    }
}

@Composable
private fun C07ReadabilityStep() = Candidate(
    "2.07", "A readability step: rows at 17 and 14",
    refines = "nameplate and rowDetail",
    note = "Same faces, one size up on the two styles that make up most of every screen; 2.02 above shows the current 15 / 13 pair in the same specimen. A guardian who is also elderly reads rows, not headlines."
) {
    TypeSpecimen(BoardSans, BoardSans, BoardCondensed, BoardMono, rowTitleSize = 17, detailSize = 14)
}

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
                    Icon(hub.icon, contentDescription = null, tint = colors.accentFor(hub.name), modifier = Modifier.size(20.dp))
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
private fun C10DeeperNight() = Candidate(
    "2.10", "A deeper night",
    refines = "the dark theme's three neutrals",
    note = "Ground near black, the plate two steps up, the recess below the ground. On an OLED the ground stops being a colour and the plates read as the only things there. Contrast of the three inks on the new plate is printed live."
) {
    PaletteSwatches(
        ground = Color(0xFF0B0D0F), plate = Color(0xFF181C20), recess = Color(0xFF050607), hairline = Color(0xFF2A3036),
        ink = Color(0xFFECEFF1), inkMuted = Color(0xFFB4AEA4), inkFaint = Color(0xFF948F86)
    )
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
private fun PaletteSwatches(
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
private fun ContrastReadout(name: String, fg: Color, bg: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(name, style = MaterialTheme.boardType.nameplateSmall, color = fg)
        Text("%.2f:1".format(contrastRatio(fg, bg)), style = MaterialTheme.boardType.readout, color = fg)
    }
}

private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

private fun hexOf(c: Color): String {
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

@Composable
private fun IconDisc(icon: ImageVector, accent: Color, size: Dp = 36.dp, glyph: Dp = 20.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(glyph))
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
private fun DiscWay(name: String, detail: String?, icon: ImageVector, state: LampState, stateLabel: String, accent: Color, trailingChevron: Boolean = false) {
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
private fun StateWord(label: String, state: LampState) {
    val colors = MaterialTheme.board
    Text(
        label.uppercase(),
        style = MaterialTheme.boardType.stateLabel,
        color = when (state) {
            LampState.LIVE -> colors.inkLive
            LampState.ATTENTION -> colors.inkAttention
            LampState.TRIP -> colors.inkTrip
            LampState.OFF, LampState.UNKNOWN -> colors.inkFaint
        }
    )
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
private fun NavWay(name: String, stateLabel: String, state: LampState, icon: ImageVector, detail: String? = null) {
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
private fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.boardType.nameplateSmall,
        color = MaterialTheme.board.inkMuted,
        modifier = Modifier.padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.xxs)
    )
}

@Composable
private fun GroupRule() {
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
private fun Well(label: String, value: String, unit: String?, modifier: Modifier = Modifier, state: LampState? = null, compact: Boolean = false) {
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
                IconDisc(icon, colors.accentFor(title), size = 44.dp, glyph = 24.dp)
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

@Composable
private fun C22LedgerPlate() = Candidate(
    "2.22", "A ledger",
    refines = "paragraphs that list facts",
    note = "Key on the left, value on the right, a dotted leader between, one line each. The medical ID's eleven fields, a wearable's facts, the smart-home hook's last firing: each is a ledger and none is prose. Values that are quantities set in mono; words in body."
) {
    val colors = MaterialTheme.board
    BoardPlate {
        Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
            listOf(
                "Blood group" to "B+", "Allergies" to "Penicillin", "Medication" to "Amlodipine 5 mg",
                "Conditions" to "Hypertension", "Doctor" to "Dr Rao · 98765 43210", "Organ donor" to "—"
            ).forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(k, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                    Spacer(Modifier.width(Spacing.sm))
                    Box(Modifier.weight(1f).height(1.dp)) { DottedLeader() }
                    Spacer(Modifier.width(Spacing.sm))
                    Text(v, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600), color = colors.ink, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun DottedLeader() {
    val colors = MaterialTheme.board
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(60) {
            Box(Modifier.size(1.5.dp).clip(CircleShape).background(colors.hairline))
        }
    }
}

@Composable
private fun C23TimelinePlate() = Candidate(
    "2.23", "A timeline on the bus",
    refines = "the trip page's record and the ladder plate",
    note = "One vertical bus down the left, a lamp at each stop, the time in mono and what happened beside it. The trip's record and the ladder's rungs are both sequences, and a sequence reads as a line with stops, not as a bank of ways that happen to be in order."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg)) {
            listOf(
                Triple("04:31", "Fall detected", LampState.TRIP),
                Triple("04:31", "Countdown 30 s · not cancelled", LampState.ATTENTION),
                Triple("04:32", "Dialer opened with Meera", LampState.LIVE),
                Triple("04:35", "Dialer opened with Arun", LampState.LIVE),
                Triple("04:41", "Meera marked it handled", LampState.LIVE)
            ).forEachIndexed { i, (time, what, state) ->
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp).fillMaxHeight()) {
                        if (i > 0) Box(Modifier.width(2.dp).height(8.dp).background(colors.hairline))
                        PilotLamp(state, size = 12.dp)
                        if (i < 4) Box(Modifier.width(2.dp).weight(1f).background(colors.hairline))
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.padding(top = if (i > 0) 8.dp else 0.dp, bottom = if (i < 4) Spacing.md else 0.dp)) {
                        Text(time, style = MaterialTheme.boardType.readout, color = colors.inkMuted)
                        Text(what, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun C24SegmentedChoice() = Candidate(
    "2.24", "A segmented choice",
    refines = "OptionWay for two to four options",
    note = "Three named behaviours as three plates in one channel, the chosen one raised to plate colour with an ink border and the rest recessed. The chosen option's consequence is the one line under the control. Fall sensitivity's three rows with three sentences become one control and one sentence."
) {
    val colors = MaterialTheme.board
    var chosen by remember { mutableIntStateOf(1) }
    val options = listOf("Low", "Medium", "High")
    val lines = listOf(
        "Trips at 2.6 g. Fewer false calls; a soft fall may be missed.",
        "Trips at 1.9 g. Recommended for an elderly wearer.",
        "Trips at 1.4 g. Catches a slide off a chair; sitting down hard can trip it."
    )
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.plate))
                .background(colors.recess)
                .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            options.forEachIndexed { i, name ->
                val on = i == chosen
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(Radius.tight))
                        .background(if (on) colors.plate else Color.Transparent)
                        .border(Stroke.hairline, if (on) colors.ink else Color.Transparent, RoundedCornerShape(Radius.tight))
                        .rowClickable(role = Role.RadioButton, onClick = { chosen = i }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name, style = MaterialTheme.boardType.nameplate, color = if (on) colors.ink else colors.inkMuted)
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(lines[chosen], style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
    }
}

// ============================================
// 2.0 · EXPLAINERS
// ============================================

@Composable
private fun C25Footnote() = Candidate(
    "2.25", "A footnote under the bank",
    refines = "the explanatory plate above a bank",
    note = "The one sentence a bank needs, in faint ink with an info glyph, under the plate instead of a paragraph plate above it. It is read after the rows, which is when a person wants it, and it takes 20dp instead of 90."
) {
    Column {
        BoardPlate {
            Way(name = "Fall detection", state = LampState.LIVE, stateLabel = "Medium", icon = SafeShadeIcons.FallDetection, sealed = true, onClick = {})
            Hairline()
            Way(name = "Call after a fall", state = LampState.LIVE, stateLabel = "Live", detail = "Calls Meera if the countdown runs out.", icon = SafeShadeIcons.CallAfterAFall, onClick = {})
        }
        Spacer(Modifier.height(Spacing.sm))
        Footnote("Sealed rows are hidden on the wearable in Elderly mode, so Baba cannot turn them down.")
    }
}

@Composable
private fun Footnote(text: String) {
    val colors = MaterialTheme.board
    Row(Modifier.padding(horizontal = Spacing.xs)) {
        Icon(SafeShadeIcons.Info, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.padding(top = 1.dp).size(14.dp))
        Spacer(Modifier.width(Spacing.sm))
        Text(text, style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
    }
}

@Composable
private fun C26Callout() = Candidate(
    "2.26", "A callout",
    refines = "the paragraph that states a consequence",
    note = "A lead of three or four words in the nameplate voice, one sentence after it, an accent rule down the leading edge. For the one thing on a page a person must not miss: that the emergency number is never dialled by itself, that a recording stays on the phone. Prose that is not this important is a footnote or is cut."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        CalloutPlate("Never dials by itself.", "The dialer opens with the number ready; a person places the call.", colors.accentFor("Never"))
        CalloutPlate("Stays on this phone.", "A recording leaves only if the rocker below says so, and one made while it is off stays here for good.", colors.accentFor("Stays"))
    }
}

@Composable
private fun CalloutPlate(lead: String, sentence: String, accent: Color) {
    val colors = MaterialTheme.board
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.plate))
            .background(colors.plate)
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate))
            .height(IntrinsicSize.Min)
    ) {
        Box(Modifier.width(Stroke.heavy).fillMaxHeight().background(accent))
        Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Text(lead, style = MaterialTheme.boardType.nameplate, color = colors.ink)
            Text(sentence, style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
        }
    }
}

@Composable
private fun C27WhatHappensChain() = Candidate(
    "2.27", "What happens, as a chain",
    refines = "the sentence that describes a sequence",
    note = "Three or four stops across one line, each a glyph on a disc with a word under it and an arrow between. 'A fall is detected, thirty seconds pass, Meera is called, then the ladder' is read in one glance. For the top of Safety and of the ladder page, in place of the paragraph."
) {
    val colors = MaterialTheme.board
    BoardPlate {
        Row(
            Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalAlignment = Alignment.Top
        ) {
            listOf(
                Triple(SafeShadeIcons.FallDetection, "Fall", "detected"),
                Triple(SafeShadeIcons.HourglassTimer, "30 s", "to cancel"),
                Triple(SafeShadeIcons.TelephoneCall, "Meera", "is called"),
                Triple(SafeShadeIcons.RepeatingCheckIn, "Ladder", "if unanswered")
            ).forEachIndexed { i, (icon, word, sub) ->
                if (i > 0) {
                    Icon(SafeShadeIcons.ArrowRight01, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.padding(top = 11.dp).size(14.dp))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconDisc(icon, colors.accentFor(word))
                    Spacer(Modifier.height(Spacing.xs))
                    Text(word, style = MaterialTheme.boardType.nameplateSmall, color = colors.ink, textAlign = TextAlign.Center)
                    Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.W400), color = colors.inkFaint, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun C28RowHelp() = Candidate(
    "2.28", "Help on the row that needs it",
    refines = "WhyDisclosure",
    note = "The long explanation lives on the row it is about, behind a small help glyph at the end of the title, and opens under that row. The page keeps no paragraph at all. A person who wants to know why the countdown is thirty seconds asks the countdown, not the page."
) {
    val colors = MaterialTheme.board
    var open by remember { mutableStateOf(false) }
    BoardPlate {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.md, top = Spacing.md, bottom = Spacing.md).height(IntrinsicSize.Min)
            ) {
                Icon(SafeShadeIcons.HourglassTimer, contentDescription = null, tint = colors.accentFor("Countdown"), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Nameplate("Countdown")
                        Spacer(Modifier.width(Spacing.xs))
                        Icon(
                            SafeShadeIcons.HelpCircle, contentDescription = "Why",
                            tint = if (open) colors.ink else colors.inkFaint,
                            modifier = Modifier.size(16.dp).rowClickable(role = Role.Button, onClick = { open = !open })
                        )
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    Text("30 seconds", style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
                }
                Spacer(Modifier.width(Spacing.md))
                StateWord("30 s", LampState.LIVE)
                Spacer(Modifier.width(Spacing.md))
                BusTick(LampState.LIVE, Modifier.fillMaxHeight())
            }
            if (open) {
                Text(
                    "Thirty seconds is long enough to find the button after a stumble and short enough that a person who cannot move is called for before the fourth minute, which is when a fall on a hard floor starts to cost. The wearable buzzes through the whole count.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    modifier = Modifier.background(colors.recess).fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md)
                )
            }
        }
    }
}

@Composable
private fun C29QualifierChips() = Candidate(
    "2.29", "Qualifier chips",
    refines = "the seal, generalised",
    note = "The seal is the one chip the kit has, and it works. Four more of its kind, in the same micro-caps: NEEDS SIM, DEVICE-ONLY, PLUS, SIGNED OUT. Each replaces a sentence under a row with a stamp beside its title, and each is a fact the row cannot change."
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            listOf(
                Triple("Call", "Needs SIM", SafeShadeIcons.SimAndSms),
                Triple("Brake light", "Device-only", SafeShadeIcons.Lights),
                Triple("Community map", "Plus", SafeShadeIcons.RadarBroadcast),
                Triple("Reports", "Signed out", SafeShadeIcons.CloudOffUnavailable)
            ).forEach { (name, chip, icon) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = colors.accentFor(name), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Spacing.md))
                    Nameplate(name)
                    Spacer(Modifier.width(Spacing.xs))
                    QualifierChip(chip)
                }
            }
        }
    }
}

@Composable
private fun QualifierChip(text: String) {
    val colors = MaterialTheme.board
    Text(
        text.uppercase(),
        style = MaterialTheme.boardType.sealPlate,
        color = colors.inkMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.tight))
            .background(colors.brass.copy(alpha = if (colors.isDark) 0.28f else 0.20f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

// ============================================
// 2.0 · ACTIONS AND NAVIGATION
// ============================================

@Composable
private fun C30CaptionedStrip() = Candidate(
    "2.30", "Glyph buttons with a caption",
    refines = "BoardIconButton",
    note = "The three glyph buttons the dashboard now has, each with its word under the glyph in the small nameplate voice. Taller by 16dp; the word never wraps because it sits on its own line. The alternative to a glyph alone."
) {
    val colors = MaterialTheme.board
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(
            Triple(SafeShadeIcons.SendMessage, "Message", true),
            Triple(SafeShadeIcons.WhereNavigation, "Where", true),
            Triple(SafeShadeIcons.TelephoneCall, "Call", false)
        ).forEach { (icon, word, enabled) ->
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.plate))
                    .background(if (enabled) colors.ground else colors.recess)
                    .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate))
                    .plateClickable(enabled = enabled, onClick = {})
                    .padding(vertical = Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = null, tint = if (enabled) colors.ink else colors.inkFaint, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(Spacing.xs))
                Text(word, style = MaterialTheme.boardType.nameplateSmall, color = if (enabled) colors.ink else colors.inkFaint)
            }
        }
    }
}

@Composable
private fun C31FigureButton() = Candidate(
    "2.31", "A button that carries a figure",
    refines = "BoardButton",
    note = "The label at the left, a mono figure at the right edge: Save · 3 changes, Send · 2 contacts, Sweep · 20 s. A commit button that says how much it is about to commit needs no sentence under it."
) {
    val colors = MaterialTheme.board
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FigureButton("Save and Send to the Device", "3 changes", colors.lampLive, com.safeshade.ui.theme.BrandCharcoal)
        FigureButton("Send the Invitation", "2 people", colors.ink, colors.plate)
        FigureButton("Sweep for Wearables", "20 s", colors.plate, colors.ink, bordered = true)
    }
}

@Composable
private fun FigureButton(label: String, figure: String, container: Color, content: Color, bordered: Boolean = false) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .plateClickable(onClick = {})
            .clip(RoundedCornerShape(Radius.plate))
            .background(container)
            .border(Stroke.hairline, if (bordered) colors.hairline else container, RoundedCornerShape(Radius.plate))
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Text(label, style = MaterialTheme.boardType.nameplate.copy(fontWeight = FontWeight.W700), color = content, modifier = Modifier.weight(1f))
        Text(figure, style = MaterialTheme.boardType.readout, color = content.copy(alpha = 0.8f))
    }
}

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
private fun MockBar(words: Boolean, rule: Boolean) {
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

@Composable
private fun C34FootCommitBar() = Candidate(
    "2.34", "A foot bar for editors",
    refines = "the COMMIT button at the end of a long editor",
    note = "On a page that ends in one commit, the button sits in a plate pinned to the foot with a hairline above it and a one-line status beside it: what is unsaved, or the repository's answer. It is never off screen, so a person three fields into the medical ID can always see the way out."
) {
    val colors = MaterialTheme.board
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(colors.plate)
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))
    ) {
        Box(Modifier.fillMaxWidth().height(72.dp).background(colors.ground), contentAlignment = Alignment.Center) {
            Text("(the editor scrolls under it)", style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
        }
        Hairline()
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("3 fields changed", style = MaterialTheme.boardType.nameplateSmall, color = colors.ink)
                Text("Not yet on the wearable", style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
            }
            Spacer(Modifier.width(Spacing.md))
            BoardButton("Save", onClick = {}, weight = ButtonWeight.COMMIT, icon = SafeShadeIcons.Tick02)
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
private fun RangeGauge(label: String, value: String, unit: String, fraction: Float, ticks: List<Float>, modifier: Modifier = Modifier, state: LampState? = null) {
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
private fun StatusWell(state: LampState, word: String, reason: String) {
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

@Composable
private fun C38BarsPlate() = Candidate(
    "2.38", "Twenty-four bars",
    refines = "the history rows on Vitals and the ride log",
    note = "A day as twenty-four flat bars in one plate, each the hour's reading, the threshold as a hairline across, breaches in attention ink. A list of readings is a ledger; a day of readings is a shape, and a shape is read in a glance."
) {
    val colors = MaterialTheme.board
    val values = listOf(0.45f, 0.42f, 0.40f, 0.41f, 0.43f, 0.48f, 0.55f, 0.62f, 0.58f, 0.60f, 0.66f, 0.71f, 0.68f, 0.83f, 0.87f, 0.74f, 0.66f, 0.63f, 0.61f, 0.58f, 0.52f, 0.48f, 0.46f, 0.44f)
    BoardPlate {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Heart rate · today", style = MaterialTheme.boardType.nameplateSmall, color = colors.inkMuted)
                    Text("72", style = MaterialTheme.boardType.readoutLarge, color = colors.ink)
                }
                Text("2 above 110", style = MaterialTheme.boardType.rowDetail, color = colors.inkAttention)
            }
            Spacer(Modifier.height(Spacing.sm))
            Box(Modifier.fillMaxWidth().height(64.dp)) {
                Row(Modifier.fillMaxWidth().fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    values.forEach { v ->
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(v)
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (v > 0.8f) colors.lampAttention else colors.inkFaint)
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().padding(top = (64 * 0.2f).dp).height(1.dp).background(colors.hairline))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("00", "06", "12", "18", "24").forEach { Text(it, style = MaterialTheme.boardType.readout.copy(fontSize = 11.sp), color = colors.inkFaint) }
            }
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
