package com.safeshade.ui.nav

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.BoardColors
import com.safeshade.ui.icons.SafeShadeIcons
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.safeshade.data.UserRole
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * The four primary destinations.
 *
 * A Material `NavigationBar` rather than something hand-rolled — this is
 * exactly the component Android users expect at the bottom of a phone app, and
 * replacing it with a custom pill is the most common way an app announces that
 * it was designed for iOS. It is themed to the board (flat plate, hairline top
 * rule) without departing from the component's behaviour.
 *
 * ## Why there is no `saveState`/`restoreState` here
 *
 * There was, and it produced three separate bugs from one cause. The graph's
 * start destination **is** `board`, so `popUpTo(startDestination) { saveState =
 * true }` filed the popped stack under `backStackMap[board.id]` — and the
 * `restoreState = true` on the very same `navigate` call then read that key
 * straight back. The tab tapped from a settings route popped and restored itself to
 * a net no-op; the Board tab could restore an orphaned Settings stack it had
 * never owned; and any tab round-trip re-opened whatever sub-page you had left.
 *
 * The fix is not a better key. With neither flag set, `backStackMap` is never
 * written and never read on these navigations, so the aliasing cannot happen by
 * construction. The cost is that a tab's non-root state does not survive
 * leaving it — which is the correct behaviour anyway (you asked for the tab,
 * not for where you were in it three minutes ago) — and that scroll position
 * would go with it, which is why the four root list states
 * are hoisted above the `NavHost` instead.
 *
 * @param onReselect tapping the tab you are already at the root of. Scrolls
 *   that root to the top.
 */
@Composable
fun BoardBottomBar(
    navController: NavController,
    role: UserRole,
    sos: SosBarState,
    onReselect: (BottomDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Read as a lambda, never as a State in composition: this is sampled inside
    // draw scopes so a five-second charge repaints without recomposing.
    val sosHold: () -> Float = { sos.progress.value }

    Column(modifier = modifier.fillMaxWidth()) {
        // The bar is separated by a rule, not a shadow. Nothing on this panel
        // floats above anything else.
        //
        // The same rule doubles as the SOS's charging indicator — it is already
        // full-width, already present, and costs no layout space, so the hold
        // reads across the whole screen instead of inside a 24dp ring nobody's
        // thumb is not covering. Drawn rather than composed so a five-second
        // animation does not recompose the bar sixty times a second.
        Box(
            Modifier
                .fillMaxWidth()
                .height(Stroke.rule)
                .drawBehind {
                    drawRect(
                        color = colors.hairline,
                        size = size.copy(height = Stroke.hairline.toPx())
                    )
                    val charge = sosHold()
                    if (charge > 0f) {
                        drawRect(
                            color = colors.lampTrip,
                            size = size.copy(width = size.width * charge)
                        )
                    }
                }
        )
        NavigationBar(
            containerColor = colors.plate,
            tonalElevation = 0.dp
        ) {
            val currentTab = tabForRoute(currentRoute)

            BottomDestination.entries.forEachIndexed { index, destination ->
                // The SOS is a peer in this Row, not an overlay on top of it.
                // NavigationBar's content lambda is a RowScope and every item
                // weights itself, so a weighted Box is a legal fifth child —
                // which keeps the component's window-inset handling, its 80dp
                // token height, its per-item ripple bounds and its
                // selectableGroup semantics rather than reimplementing all four.
                if (index == 2) {
                    SosSlot(
                        state = sos,
                        hold = sosHold,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Sub-screens keep their parent tab lit, so a user three levels
                // into Safety still knows where they are.
                val selected = currentTab == destination

                // A plain selectable Box, not NavigationBarItem.
                //
                // The item component draws Material's press state layer and
                // exposes no `indication` or `interactionSource` parameter to
                // turn it off, so the grey flash on every tab press could not
                // be removed while using it. The *container* is still
                // NavigationBar, which is where the window insets, the 80dp
                // height and `selectableGroup()` come from - so replacing the
                // item costs none of those, and TalkBack still announces
                // "<label>, tab, N of 4".
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        // A fixed height, never fillMaxHeight. Scaffold measures
                        // its bottomBar with the whole screen height as the max
                        // constraint, so a child that fills it drags the whole
                        // NavigationBar to full height and the bar ends up
                        // floating in the middle of a blank screen. This has
                        // caught this file once before.
                        .height(Spacing.touchTarget)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                when {
                                    // Already at this tab's root. Android's
                                    // convention is "take me to the top of the
                                    // list", not "do nothing".
                                    currentRoute == destination.route -> onReselect(destination)

                                    // Somewhere beneath this tab. A real pop, so
                                    // the graph plays its *pop* transitions and
                                    // the motion reads as coming back up rather
                                    // than going sideways. Falls through to the
                                    // branch below when it returns false - the
                                    // case where the tab root was never pushed,
                                    // i.e. every one of the Board's deep links.
                                    currentTab == destination &&
                                        navController.popBackStack(destination.route, inclusive = false) -> Unit

                                    else -> navController.navigate(destination.route) {
                                        // Anchored to the graph's real start
                                        // destination rather than a hardcoded
                                        // route: the previous version pinned
                                        // this to "home", which breaks the
                                        // moment onboarding is the start.
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                        .semantics { contentDescription = destination.labelFor(role) }
                ) {
                    // Selection is a colour *and* a shape, still. The brass rule
                    // is gone, but a disc of the tab's own accent stays behind
                    // the glyph: a tint change on its own is invisible to a
                    // colourblind reader, and this system has said so from the
                    // start. The disc is the accent at low alpha, so it reads as
                    // the icon being filled in rather than as a lamp.
                    val accent = destination.accent(colors)
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.16f))
                        )
                    }
                    Icon(
                        imageVector = destination.icon,
                        // Mandatory now the labels are gone. The label used to
                        // carry the name for a screen reader; with it removed a
                        // null description leaves four anonymous buttons. The
                        // description is set on the selectable above so it is
                        // announced once, not twice.
                        contentDescription = null,
                        tint = if (selected) accent else colors.inkFaint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * The emergency control, held for five seconds.
 *
 * Flush inside the bar rather than a raised FAB. Three reasons and they all
 * point the same way: `Elevation`'s own doc says elevation is never decoration,
 * this bar's doc says nothing on the panel floats above anything else, and the
 * Board's list deliberately carries no bottom padding so Shady stands on the
 * bar's top rule — a protruding button would sit on its head.
 *
 * Five seconds is a long time on purpose. This sends real SMS to real people,
 * and the cost of an accidental send is somebody's parent being frightened.
 */
@Composable
private fun SosSlot(
    state: SosBarState,
    hold: () -> Float,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(pressed) {
        if (!pressed) {
            // A partial charge means the finger came off early. Exactly 1f means
            // it fired and the finger is only now lifting, which must not buzz
            // "rejected" at somebody who just called for help.
            if (state.progress.value > 0f && state.progress.value < 1f) {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
            }
            state.progress.animateTo(0f, tween(160, easing = LinearEasing))
            return@LaunchedEffect
        }
        if (!state.canFire) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            state.onArmRejected()
            return@LaunchedEffect
        }
        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        val ticks = launch {
            repeat(SOS_HOLD_SECONDS - 1) {
                delay(1_000)
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
        try {
            state.progress.animateTo(1f, tween(SOS_HOLD_SECONDS * 1000, easing = LinearEasing))
            sosFiredBuzz(context)
            state.onFire()
        } finally {
            ticks.cancel()
        }
    }

    Box(
        modifier = modifier
            // Deliberately NOT fillMaxHeight. Scaffold measures its bottomBar
            // with the full screen height as the maximum, so a child filling it
            // makes the NavigationBar's Row claim the entire display — the bar
            // ends up vertically centred over a blank screen. A fixed height
            // inside a weighted slot is the whole of what this needs.
            .height(Spacing.touchTarget)
            // Plain clickable with a hoisted interaction source rather than
            // detectTapGestures(onLongPress): that has a fixed threshold, yields
            // no progress to draw, and would need its own cancellation
            // bookkeeping. Here, releasing early and dragging off the control
            // are the *same* path — `pressed` goes false, the effect is
            // cancelled, and the charge unwinds.
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = state.onTap
            )
            .semantics {
                role = Role.Button
                contentDescription =
                    "Emergency SOS. Hold for five seconds to alert your emergency contact."
                // A screen-reader user physically cannot perform a five-second
                // hold — TalkBack activates on a double tap — so without this
                // the single most important control in the app is unreachable
                // to them.
                customActions = listOf(
                    CustomAccessibilityAction("Send SOS alert now") {
                        state.onFire()
                        true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // A solid disc, not a tinted glyph. This is the one control on the
            // panel that has to be findable without reading anything, by
            // somebody who may be in trouble — an outline among four other
            // outlines is exactly what it must not look like. It stays inside
            // the bar's own height, so it reads as the most important key on
            // the board rather than as a button floating over it.
            Canvas(
                modifier = Modifier
                    .size(44.dp)
                    .scale(if (pressed) 0.94f else 1f)
            ) {
                val charge = hold()
                val r = size.minDimension / 2f - Stroke.brass.toPx()
                if (state.canFire) {
                    drawCircle(color = colors.lampTrip, radius = r)
                } else {
                    // Not armed — no contact yet, or no permission to text one.
                    // Drawn as an empty ring rather than a faded disc: a washed
                    // out fill reads as a rendering fault, while a ring reads as
                    // a control that is present and waiting for something. It is
                    // still pressable, and pressing it says what is missing.
                    drawCircle(
                        color = colors.lampTrip,
                        radius = r,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = Stroke.brass.toPx()
                        )
                    )
                }
                if (charge > 0f) {
                    // The local echo of the full-width rule above the bar. Same
                    // value, two readings: one under the thumb, one across the
                    // screen where the thumb is not covering it.
                    drawArc(
                        color = colors.ink,
                        startAngle = -90f,
                        sweepAngle = 360f * charge,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = Stroke.brass.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
            Icon(
                imageVector = EmergencyAsterisk,
                contentDescription = null,
                // Knocked out of the disc when armed; drawn in the ring's own
                // colour when it is only an outline.
                tint = when {
                    !state.canFire -> colors.lampTrip
                    colors.isDark -> colors.ground
                    else -> colors.plate
                },
                modifier = Modifier.size(22.dp).scale(if (pressed) 0.94f else 1f)
            )
        }
    }
}

private const val SOS_HOLD_SECONDS = 5

/**
 * The buzz that says it has gone.
 *
 * Deliberately not `HapticFeedbackType.Confirm`, which maps to
 * `HapticFeedbackConstants.CONFIRM` — an API 30 constant. This app's minSdk is
 * 26, so on API 26–29 that call returns false and the user gets *nothing* at
 * the one moment they most need to know the alert was sent. A direct
 * `VibrationEffect` works everywhere; VIBRATE is already declared.
 */
private fun sosFiredBuzz(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(VibratorManager::class.java))?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    } ?: return
    runCatching {
        vibrator.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 60, 80, 200), -1)
        )
    }
}

/**
 * Everything the bar needs to know about the SOS, and nothing about how it
 * sends. The work lives in the view model over `SafetyRepository`.
 *
 * [progress] is an `Animatable` held by the caller so the charging rule above
 * the bar and the ring around the glyph read the *same* value, and so both can
 * read it inside a draw scope rather than in composition — a 0..1 float
 * animating for five seconds would otherwise recompose the whole bar on every
 * frame.
 */
@Stable
class SosBarState(
    val canFire: Boolean,
    val progress: Animatable<Float, AnimationVector1D>,
    val onArmRejected: () -> Unit,
    val onTap: () -> Unit,
    val onFire: () -> Unit
)

/**
 * Which tab a route belongs to, or null if it belongs to none.
 *
 * The one definition of that question. It was previously answered twice — by a
 * prefix test in this file and by a set membership in the nav graph's motion
 * logic — and when two answers disagree the bar lights one tab while the
 * screen animates as another.
 *
 * The settings routes are their own branch (see [Routes.SETTINGS]), but they are
 * entered from the Device screen and belong to the Device tab, so the bar stays
 * lit inside them and the Device tab is the way back out.
 *
 * The comparison is `== route || startsWith("$route/")` rather than a bare
 * `startsWith`, which would one day match a route named `boardroom` against
 * `board`. Argument-carrying routes (`safety/trips/detail/{tripId}`) match
 * because they still begin with their tab's segment and a slash, and
 * `device/settings` correctly does *not* match the settings branch.
 */
internal fun tabForRoute(route: String?): BottomDestination? {
    if (route == null) return null
    if (route == Routes.SETTINGS || route.startsWith("${Routes.SETTINGS}/")) {
        return BottomDestination.DEVICE
    }
    return BottomDestination.entries.firstOrNull { d ->
        route == d.route || route.startsWith("${d.route}/")
    }
}

internal fun BottomDestination.labelFor(role: UserRole): String =
    when (role) {
        UserRole.GUARDIAN -> guardianLabel
        UserRole.COMPANION -> companionLabel
    }

/**
 * The glyph for a tab.
 *
 * All four are the app's own icons now, which are stroke drawings with no
 * heavier twin - so the selected/unselected difference they carry is colour
 * plus the accent disc behind them, not weight.
 *
 * Safety was the last holdout, on Material only because the set had no shield.
 * It has one now, and a single Material glyph sitting between three custom ones
 * on the most-looked-at surface in the app was the most visible seam left. The
 * weight change it used to carry goes with it; the other three never had one
 * and the bar reads consistently without it.
 */
private val BottomDestination.icon: ImageVector
    get() = when (this) {
        BottomDestination.BOARD -> SafeShadeIcons.NavbarBoard
        BottomDestination.CIRCLE -> SafeShadeIcons.NavbarCircle
        BottomDestination.SAFETY -> SafeShadeIcons.NavbarSafety
        BottomDestination.DEVICE -> SafeShadeIcons.NavbarDevice
    }

/**
 * The colour a tab wears when it is the one you are on.
 *
 * Four fixed picks rather than the usual hash, and spread around the wheel on
 * purpose: these four are the only accents ever seen *side by side and at the
 * same moment*, so two neighbours landing on close hues would read as a
 * mistake. Everywhere else in the app a colour only has to differ from the row
 * above it.
 */
private fun BottomDestination.accent(colors: BoardColors): Color = when (this) {
    BottomDestination.BOARD -> colors.accentClay
    BottomDestination.CIRCLE -> colors.accentSky
    BottomDestination.SAFETY -> colors.accentSage
    BottomDestination.DEVICE -> colors.accentPlum
}

/**
 * The six-armed asterisk the SOS disc has always carried.
 *
 * Material's "emergency" glyph, kept verbatim (Apache 2.0) after the icon
 * set replaced Material everywhere else: the set's first-aid case did not
 * read as SOS knocked out of the red disc, and the user asked for exactly
 * this one back. It is the only glyph in the app that is not generated
 * from docs/Icons, and this is the only place it is drawn.
 */
private val EmergencyAsterisk: ImageVector by lazy {
    ImageVector.Builder(
        name = "EmergencyAsterisk",
        defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M20.79 9.23l-2-3.46L14 8.54V3h-4v5.54L5.21 5.77l-2 3.46L8 12l-4.79 2.77 2 3.46L10 15.46V21h4v-5.54l4.79 2.77 2-3.46L16 12z"
            ),
            fill = SolidColor(Color.Black)
        )
    }.build()
}
