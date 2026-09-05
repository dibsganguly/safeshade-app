package com.safeshade.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.PersonaMode
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.BusTick
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.Seal
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.icon
import com.safeshade.ui.board.plateClickable
import com.safeshade.ui.theme.BoardColors
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/** Everything the mode picker draws. */
data class ModePickerUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val activeMode: PersonaMode = PersonaMode.AUTO,
    /**
     * The mode written to `EXT MODE` and still waiting for `ACK:MODE:<name>`.
     *
     * Kept separate from [activeMode] so the picker can show the old mode as
     * still current while the new one shows as in flight. Collapsing them would
     * mean the card lights up before the wearable has agreed to anything.
     */
    val inFlightMode: PersonaMode? = null,
    val ack: AckState = AckState.IDLE,
    /**
     * A guardian-locked mode the user has tapped but not yet confirmed.
     *
     * Hoisted rather than held in a `remember` inside the composable: the
     * confirmation gates a change with real consequences on the wearable, and
     * the caller is entitled to be able to cancel it, restore it, or test it.
     */
    val confirmingMode: PersonaMode? = null
)

/**
 * Which profile the wearable is running.
 *
 * Three things shape this screen. The first is that AUTO is not one of eight
 * peers — it is the firmware's fresh-boot default and the answer for anyone who
 * does not want to think about this at all, so it gets a hero of its own above
 * the rule and the seven explicit modes are paged below it.
 *
 * The second is that four of those seven take something away from the person
 * wearing the device. In ELDERLY, KIDS, PET and HELMET the firmware hides its
 * own Mode and Safety menus, so the wearer cannot quietly turn their own fall
 * detection down. That is a defensible thing to do to a person and an
 * indefensible thing to do to them silently, which is why those modes carry a
 * seal on the card, name the seal in their spoken description, and take a
 * plainly worded confirmation on selection.
 *
 * The third is that a profile is a *choice about a life*, not a settings row.
 * A list of seven near-identical rows asks the reader to work out from four
 * words apiece which one describes the person they are worried about. One large
 * card at a time, colour-identified and carrying a scene of the character
 * actually doing the thing, answers that before the blurb is read. Paging is
 * what buys the room for it: swiping browses and costs nothing, and only a tap
 * selects — so the expensive action stays deliberate while looking is free.
 */
@Composable
fun ModePickerScreen(
    state: ModePickerUiState,
    onSelectMode: (PersonaMode) -> Unit,
    onConfirmMode: (PersonaMode) -> Unit,
    onCancelConfirm: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val explicit = remember { PersonaMode.entries.filter { it != PersonaMode.AUTO } }

    // Hoisted out of the carousel's list item on purpose. A pager state
    // remembered inside a `LazyColumn` item is destroyed the moment the item
    // scrolls out of the viewport, which silently resets the reader to the
    // first profile every time they look at the footnote and scroll back.
    val pagerState = rememberPagerState(
        initialPage = explicit.indexOf(state.activeMode).coerceAtLeast(0),
        pageCount = { explicit.size }
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Only the vertical inset belongs to the list. The horizontal gutter is
        // applied per item so the carousel can run edge to edge underneath it
        // and let the neighbouring cards show, which is the affordance that
        // says there are more profiles than the one on screen.
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        val gutter = Modifier.padding(horizontal = Spacing.gutter)

        item("title") {
            ScreenHeader(
                title = "Adaptive mode",
                subtitle = "The profile decides how hard the device listens for a " +
                    "fall and what it shows on its own screen.",
                onBack = onBack,
                modifier = gutter
            )
        }

        item("offline") {
            OfflineNotice(
                connection = state.connection,
                // The mode is persisted and re-pushed when the link reaches
                // Ready, so this promise is one the caller actually keeps.
                queuedNote = "A mode chosen now is stored and sent as soon as the " +
                    "device connects.",
                modifier = gutter
            )
        }

        // AUTO is separated from the seven by a rule and a whole different
        // shape, because choosing it is a different kind of decision: it is
        // declining to choose, and that deserves to be the easy path rather
        // than the eighth card in a carousel somebody has to swipe to reach.
        item("auto") {
            AutoHero(
                selected = state.activeMode == PersonaMode.AUTO,
                inFlight = state.inFlightMode == PersonaMode.AUTO,
                linkUsable = state.connection.isUsable,
                ack = state.ack,
                onSelect = { onSelectMode(PersonaMode.AUTO) },
                modifier = gutter
            )
        }

        item("explicit-heading") {
            SectionPlate(
                title = "Choose a profile",
                modifier = gutter,
                trailing = {
                    // A counter rather than only dots: seven dots is past the
                    // count a person can read at a glance, so the position is
                    // given in words as well as in shape.
                    Text(
                        text = "${pagerState.currentPage + 1} of ${explicit.size}",
                        style = MaterialTheme.boardType.stateLabel,
                        color = colors.inkFaint
                    )
                }
            )
        }

        item("explicit") {
            ModeCarousel(
                modes = explicit,
                pagerState = pagerState,
                state = state,
                onSelectMode = onSelectMode
            )
        }

        item("footnote") {
            Text(
                text = "Fall sensitivity shown here is the profile's starting " +
                    "point. You can change it afterwards in device settings.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint,
                modifier = gutter
            )
        }
    }

    if (state.confirmingMode != null) {
        GuardianLockDialog(
            mode = state.confirmingMode,
            onConfirm = { onConfirmMode(state.confirmingMode) },
            onCancel = onCancelConfirm
        )
    }
}

// ============================================================================
// Identity
// ============================================================================

/**
 * The decorative accent a mode is known by.
 *
 * Six tokens across eight modes, so two hues appear twice. The pairs are chosen
 * so that no two modes *adjacent in the carousel* share one — a reader swiping
 * has to see the colour change to know the card changed, and a repeat two or
 * three pages apart is never seen side by side.
 *
 * Decorative, never state: this colour says which profile a card is, and says
 * nothing whatever about whether the wearable is running it. That job belongs
 * to the pilot lamp and the state word beside it.
 */
private fun accentFor(mode: PersonaMode, colors: BoardColors): Color = when (mode) {
    PersonaMode.AUTO -> colors.accentSage
    PersonaMode.ELDERLY -> colors.accentSky
    PersonaMode.KIDS -> colors.accentSand
    PersonaMode.BIKE -> colors.accentMoss
    PersonaMode.PET -> colors.accentClay
    PersonaMode.HELMET -> colors.accentSand
    PersonaMode.WRIST -> colors.accentLilac
    PersonaMode.BACKPACK -> colors.accentSky
}

/**
 * The lamp for a mode card.
 *
 * The middle branch is the one that matters. A teal lamp on a dead link would
 * claim we know what profile the wearable is running. We know what we last
 * stored, which is not the same thing and is exactly the Connected-versus-Ready
 * lie this codebase has already paid for once — so a selected mode with no
 * usable link is UNKNOWN, not LIVE.
 */
private fun modeLamp(
    selected: Boolean,
    inFlight: Boolean,
    linkUsable: Boolean,
    ack: AckState
): LampState = when {
    inFlight -> ackLamp(ack, LampState.LIVE)
    selected && linkUsable -> LampState.LIVE
    selected -> LampState.UNKNOWN
    else -> LampState.OFF
}

/** The same distinction in words, because colour alone never carries it. */
private fun modeStateWord(
    selected: Boolean,
    inFlight: Boolean,
    linkUsable: Boolean,
    unselectedWord: String = "Off"
): String = when {
    inFlight -> "Sending"
    selected && linkUsable -> "Active"
    selected -> "Last set"
    else -> unselectedWord
}

private fun stateInk(lamp: LampState, colors: BoardColors): Color = when (lamp) {
    LampState.LIVE -> colors.inkLive
    LampState.ATTENTION -> colors.inkAttention
    LampState.TRIP -> colors.inkTrip
    LampState.OFF, LampState.UNKNOWN -> colors.inkFaint
}

/**
 * Everything a card says, said once, for a reader who cannot see it.
 *
 * The seal clause is not optional. A sighted reader gets the seal badge on the
 * card and the dialog after the tap; without this sentence a screen-reader user
 * would meet the consequence for the first time in the confirmation, which is
 * the wrong place to learn what you are agreeing to.
 */
private fun modeDescription(
    mode: PersonaMode,
    stateWord: String,
    inFlight: Boolean,
    ack: AckState
): String = buildString {
    append(mode.label)
    append(", ")
    append(stateWord)
    append(". ")
    if (mode.isGuardianLocked) {
        append("Guardian-sealed: the wearer cannot change this from the device. ")
    }
    append(mode.blurb)
    append(" Starting fall sensitivity ")
    append(mode.defaultFallSensitivity.label)
    append(".")
    if (inFlight) ackWord(ack)?.let { append(" "); append(it); append(".") }
}

// ============================================================================
// The carousel
// ============================================================================

/**
 * The seven explicit profiles, one at a time.
 *
 * Swiping only browses. Selection is always a tap on the card, so a thumb
 * flicking through seven profiles cannot reconfigure somebody's fall detection
 * on the way past.
 */
@Composable
private fun ModeCarousel(
    modes: List<PersonaMode>,
    pagerState: PagerState,
    state: ModePickerUiState,
    onSelectMode: (PersonaMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board

    // Follow the active mode when it changes underneath us — an ack landing, a
    // guardian confirming, another phone changing the profile. Keyed on the
    // mode rather than driven from the pager, so a reader browsing is never
    // dragged back to the card they happen to have selected.
    LaunchedEffect(state.activeMode) {
        val target = modes.indexOf(state.activeMode)
        if (target >= 0 && target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            // One height for every page. A pager is a lazy layout, so letting
            // pages size themselves makes the whole list jump as a taller card
            // scrolls in mid-swipe. Text scale is the only thing that changes
            // the content height materially, so it is the only thing the height
            // is allowed to depend on.
            modifier = Modifier.height(carouselHeight()),
            contentPadding = PaddingValues(horizontal = Spacing.gutter),
            pageSpacing = Spacing.xs
        ) { page ->
            val mode = modes[page]
            ModeCard(
                mode = mode,
                accent = accentFor(mode, colors),
                selected = state.activeMode == mode,
                inFlight = state.inFlightMode == mode,
                linkUsable = state.connection.isUsable,
                ack = state.ack,
                onSelect = { onSelectMode(mode) },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(Spacing.md))
        CarouselDots(
            count = modes.size,
            current = pagerState.currentPage,
            accent = accentFor(modes[pagerState.currentPage], colors),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * A page height that survives large text.
 *
 * The 300dp base is the card at default scale with a three-line blurb and the
 * acknowledgement line present. The rest is headroom bought at the same rate
 * the text grows, so the sensitivity row at the foot of the card stays inside
 * it at a 1.3 font scale — which is not a hypothetical on a product whose
 * audience includes elderly guardians.
 */
@Composable
private fun carouselHeight() =
    300.dp + ((LocalDensity.current.fontScale - 1f).coerceAtLeast(0f) * 150f).dp

/** Position in the set. Decoration — the counter beside the section heading says it in words. */
@Composable
private fun CarouselDots(
    count: Int,
    current: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    Row(
        modifier = modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val here = index == current
            Box(
                modifier = Modifier
                    .height(Stroke.brass)
                    .width(if (here) 20.dp else 8.dp)
                    .background(
                        color = if (here) accent else colors.hairline,
                        shape = RoundedCornerShape(Radius.tight)
                    )
            )
        }
    }
}

/**
 * One profile, at the size the decision deserves.
 *
 * The scene band carries the mode's identity twice over — a wash of its accent
 * behind the animation, and the accent at full strength as the rule under it —
 * while the plate, the ink and the lamp underneath are the same materials as
 * every other surface in the app. Identity above the rule, state below it.
 */
@Composable
private fun ModeCard(
    mode: PersonaMode,
    accent: Color,
    selected: Boolean,
    inFlight: Boolean,
    linkUsable: Boolean,
    ack: AckState,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val lamp = modeLamp(selected, inFlight, linkUsable, ack)
    val stateWord = modeStateWord(selected, inFlight, linkUsable, unselectedWord = "Not selected")

    BoardPlate(
        modifier = modifier
            .plateClickable(role = Role.RadioButton, onClick = onSelect)
            .clearAndSetSemantics {
                this.selected = selected
                contentDescription = modeDescription(mode, stateWord, inFlight, ack)
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // The busbar the card taps off. It runs the full height of the
            // plate, which is what makes a selected card legible from across
            // the carousel without spending a decorative colour on state.
            BusTick(state = lamp, modifier = Modifier.fillMaxHeight())

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(accent.copy(alpha = if (colors.isDark) 0.16f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    ModeScene(mode = mode, accent = accent, size = 124.dp)

                    // The seal appears whether or not the mode is active, so it
                    // reads as a property of the choice rather than as a
                    // consequence discovered after making it.
                    if (mode.isGuardianLocked) {
                        Seal(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(Spacing.sm)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Stroke.brass)
                        .background(accent)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(Spacing.lg)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        // The label is weighted so the state word (a short,
                        // fixed vocabulary: SENDING/LAST SET/RUNNING) is
                        // measured first and always stays whole — but a
                        // weighted Text still wraps by default when squeezed,
                        // which broke a long mode label into two lines and
                        // threw off the row's vertical centering against the
                        // icon and lamp. One line with an ellipsis instead.
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stateWord.uppercase(),
                            style = MaterialTheme.boardType.stateLabel,
                            color = stateInk(lamp, colors)
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        PilotLamp(state = lamp, size = 14.dp)
                    }

                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        // Straight from the enum. The firmware, the app and
                        // this sentence should never be able to drift apart.
                        text = mode.blurb,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.weight(1f))

                    // The acknowledgement replaces nothing here — it sits under
                    // the sensitivity, because on a card this size there is room
                    // to say both what the profile is and whether the device has
                    // agreed to it yet.
                    val ackLine = if (inFlight) ackWord(ack) else null
                    if (ackLine != null) {
                        Text(
                            text = ackLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkFaint
                        )
                        Spacer(Modifier.height(Spacing.xs))
                    }
                    Hairline()
                    Spacer(Modifier.height(Spacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // The label takes the weight so the *value* is measured
                        // first and stays whole. Without it a large text scale
                        // leaves "Medium" too little room and breaks it across
                        // two lines mid-word.
                        Nameplate(
                            "Starting fall sensitivity",
                            small = true,
                            muted = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            text = mode.defaultFallSensitivity.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * The adaptive hero.
 *
 * Built from a plate, a bus tick and a lamp rather than a new card type: it is
 * wider and shaped differently from a carousel card, but it is the same
 * material and obeys the same rule that only the lamp carries state colour.
 *
 * It sits outside the pager deliberately. Anything inside a carousel is one of
 * a set of peers to be compared; AUTO is the option for a person who does not
 * want to compare anything, and putting it behind a swipe would hide the easy
 * answer behind the hard question.
 */
@Composable
private fun AutoHero(
    selected: Boolean,
    inFlight: Boolean,
    linkUsable: Boolean,
    ack: AckState,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val accent = accentFor(PersonaMode.AUTO, colors)
    val lamp = modeLamp(selected, inFlight, linkUsable, ack)
    val stateWord = modeStateWord(selected, inFlight, linkUsable, unselectedWord = "Not selected")

    BoardPlate(
        modifier = modifier
            .plateClickable(role = Role.RadioButton, onClick = onSelect)
            .fillMaxWidth()
            .clearAndSetSemantics {
                this.selected = selected
                contentDescription =
                    modeDescription(PersonaMode.AUTO, stateWord, inFlight, ack)
            }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            BusTick(state = lamp, modifier = Modifier.fillMaxHeight())

            Column(modifier = Modifier.weight(1f).padding(Spacing.lg)) {
                // The state word gets a line of its own above the title rather
                // than sharing one with it. Sharing cost the heading four
                // characters of width, which is exactly enough to break
                // "Adaptive" across two lines on a 393dp phone.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stateWord.uppercase(),
                        style = MaterialTheme.boardType.stateLabel,
                        color = stateInk(lamp, colors)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    PilotLamp(state = lamp, size = 18.dp)
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = PersonaMode.AUTO.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink
                )
                Text(
                    // Identity type at full accent strength — the one place the
                    // hero states which colour it is.
                    text = "The device's own default",
                    style = MaterialTheme.typography.bodySmall,
                    color = accent
                )

                Spacer(Modifier.height(Spacing.md))
                Text(
                    // Straight from the enum. The firmware, the app and this
                    // sentence should never be able to drift apart.
                    text = PersonaMode.AUTO.blurb,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.ink
                )

                Spacer(Modifier.height(Spacing.md))
                Hairline()
                Spacer(Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Nameplate(
                        "Starting fall sensitivity",
                        small = true,
                        muted = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = PersonaMode.AUTO.defaultFallSensitivity.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted
                    )
                }
                val ackLine = if (inFlight) ackWord(ack) else null
                if (ackLine != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = ackLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(108.dp)
                    .fillMaxHeight()
                    .background(accent.copy(alpha = if (colors.isDark) 0.16f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                ModeScene(
                    mode = PersonaMode.AUTO,
                    accent = accent,
                    size = 96.dp,
                    modifier = Modifier.padding(Spacing.sm)
                )
            }
        }
    }
}

/**
 * The consequence of a guardian-locked mode, said once and plainly.
 *
 * Deliberately not a "are you sure?" — that asks the user to guess what they
 * are agreeing to. It names the two things the wearer loses and the one way
 * back, and it uses the wearable's own menu names so the sentence can be
 * checked against the device in front of them.
 */
@Composable
private fun GuardianLockDialog(
    mode: PersonaMode,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.board
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.plate,
        titleContentColor = colors.ink,
        textContentColor = colors.inkMuted,
        icon = {
            Icon(
                imageVector = mode.icon,
                contentDescription = null,
                tint = colors.inkMuted
            )
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Seal the device to ${mode.label}",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink
                )
                Spacer(Modifier.width(Spacing.sm))
                Seal()
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "While ${mode.label} is active the wearable hides mode " +
                        "switching and its whole Safety menu. The person wearing " +
                        "it cannot change the profile or turn fall detection " +
                        "down from the device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
                Text(
                    text = "Only this app can change it back.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink
                )
                Text(
                    text = "Starting fall sensitivity: " +
                        mode.defaultFallSensitivity.label +
                        " — " + mode.defaultFallSensitivity.blurb.lowercase() + ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        },
        confirmButton = {
            BoardButton(
                label = "Seal to ${mode.label}",
                onClick = onConfirm,
                weight = ButtonWeight.PRIMARY
            )
        },
        dismissButton = {
            BoardButton(
                label = "Cancel",
                onClick = onCancel,
                weight = ButtonWeight.QUIET
            )
        }
    )
}

// ============================================================================
// Previews
// ============================================================================

@Preview(name = "Modes · light", showBackground = true)
@Composable
private fun ModePickerPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModePickerScreen(
                state = ModePickerUiState(
                    connection = ConnectionState.Ready,
                    activeMode = PersonaMode.AUTO
                ),
                onSelectMode = {},
                onConfirmMode = {},
                onCancelConfirm = {}
            )
        }
    }
}

@Preview(name = "Modes · dark, sealed active", showBackground = true)
@Composable
private fun ModePickerPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModePickerScreen(
                state = ModePickerUiState(
                    connection = ConnectionState.Ready,
                    activeMode = PersonaMode.ELDERLY,
                    ack = AckState.CONFIRMED
                ),
                onSelectMode = {},
                onConfirmMode = {},
                onCancelConfirm = {}
            )
        }
    }
}

@Preview(name = "Modes · offline, last set", showBackground = true)
@Composable
private fun ModePickerPreviewOffline() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModePickerScreen(
                state = ModePickerUiState(
                    connection = ConnectionState.Disconnected,
                    activeMode = PersonaMode.PET
                ),
                onSelectMode = {},
                onConfirmMode = {},
                onCancelConfirm = {}
            )
        }
    }
}

@Preview(name = "Modes · confirming a seal", showBackground = true)
@Composable
private fun ModePickerPreviewConfirming() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModePickerScreen(
                state = ModePickerUiState(
                    connection = ConnectionState.Ready,
                    activeMode = PersonaMode.BACKPACK,
                    confirmingMode = PersonaMode.KIDS
                ),
                onSelectMode = {},
                onConfirmMode = {},
                onCancelConfirm = {}
            )
        }
    }
}

@Preview(name = "Modes · large text", showBackground = true, fontScale = 1.3f)
@Composable
private fun ModePickerPreviewLargeText() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModePickerScreen(
                state = ModePickerUiState(
                    connection = ConnectionState.Ready,
                    activeMode = PersonaMode.BACKPACK,
                    inFlightMode = PersonaMode.BIKE,
                    ack = AckState.PENDING
                ),
                onSelectMode = {},
                onConfirmMode = {},
                onCancelConfirm = {}
            )
        }
    }
}
