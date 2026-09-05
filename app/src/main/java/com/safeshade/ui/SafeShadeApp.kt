package com.safeshade.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.safeshade.MainActivity
import com.safeshade.data.SosBlocker
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import com.safeshade.repo.AppState
import com.safeshade.ui.board.TripBanner
import com.safeshade.ui.nav.BoardBottomBar
import com.safeshade.ui.nav.BottomDestination
import com.safeshade.ui.nav.MainNavGraph
import com.safeshade.ui.nav.OnboardingNavGraph
import com.safeshade.ui.nav.Routes
import com.safeshade.ui.nav.SosBarState
import com.safeshade.ui.nav.tabForRoute
import com.safeshade.ui.screens.IntroScreen
import com.safeshade.ui.theme.BoardColors
import com.safeshade.ui.theme.LocalBoardAccent
import com.safeshade.ui.theme.board
import com.safeshade.ui.vm.SafeShadeViewModel
import kotlinx.coroutines.launch

/**
 * The root of the app.
 *
 * Three states, resolved in this order, and the order matters:
 *
 *  1. **Loading** — persisted state has not arrived yet. Renders a plain ground
 *     rather than default-constructed content. The previous version seeded
 *     `activeMode` with a default and corrected it a frame later, which showed
 *     the wrong mode on every cold start; there is no version of that bug that
 *     is worth the millisecond it saves.
 *  2. **A live trip** — a fall or SOS takes the entire frame, above navigation,
 *     regardless of where the user was. An emergency is not a screen you have
 *     to navigate to.
 *  3. **Onboarding, or the app.**
 */
@Composable
fun SafeShadeApp(viewModel: SafeShadeViewModel) {
    val state by viewModel.appState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.board
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Requested from the SOS guard rather than batched into the cold-start
    // permission dialog. SEND_SMS is a restricted permission, and asking for it
    // alongside Bluetooth and location — before the user has seen anything that
    // would send a text — gives them no reason to say yes. Asked here, the
    // prompt arrives one tap after "SafeShade cannot send texts yet", which is
    // the moment it makes sense.
    val requestSms: () -> Unit = { (context as? MainActivity)?.requestSmsPermission() }

    // Cold start only: rememberSaveable so a rotation does not replay it, and
    // so it never sits between a worried person and the board more than once.
    var introDone by rememberSaveable { mutableStateOf(false) }

    val ready = state as? AppState.Ready
    if (ready == null) {
        // Deliberately empty. The splash screen is still up at this point, and
        // a spinner here would only ever be seen as a flicker.
        Box(Modifier.fillMaxSize().background(colors.ground))
        return
    }

    if (!introDone) {
        IntroScreen(onFinished = { introDone = true })
        return
    }

    Box(Modifier.fillMaxSize().background(colors.ground)) {
        if (!ready.onboardingSeen) {
            OnboardingNavGraph(viewModel = viewModel, state = ready)
        } else {
            val navController = rememberNavController()

            // Hoisted above the NavHost, and this is not an optimisation.
            // The bar no longer saves and restores tab stacks (see
            // BoardBottomBar's own note on why), so switching tabs destroys the
            // outgoing NavBackStackEntry and the SaveableStateHolder it owns —
            // taking scroll position with it. Held here, the four roots keep
            // their place across a tab round-trip, and because
            // rememberLazyListState is already rememberSaveable-backed they
            // survive rotation and process death for free.
            //
            // This is also the only common ancestor of the bar and the graph,
            // which the scroll-to-top gesture needs.
            val boardListState = rememberLazyListState()
            val circleListState = rememberLazyListState()
            val safetyListState = rememberLazyListState()
            val deviceListState = rememberLazyListState()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentTab = tabForRoute(backStackEntry?.destination?.route)

            // Held here rather than inside the bar so the charging rule above
            // the bar and the ring around the glyph animate off one value.
            val sosProgress = remember { Animatable(0f) }
            val permissionsGranted by viewModel.permissionsGranted.collectAsStateWithLifecycle()
            // permissionsGranted is not itself the SMS grant, but it changes on
            // every resume, which is exactly when a grant made in the system
            // dialog becomes true. Reading checkSelfPermission in composition
            // would otherwise never see it.
            val canFireSos = remember(permissionsGranted, ready.safetySettings.emergencyContacts, ready.activeAlert) {
                viewModel.canFireSos()
            }

            val sosBarState = SosBarState(
                canFire = canFireSos,
                progress = sosProgress,
                onArmRejected = {
                    scope.launch {
                        when (viewModel.sosBlocker()) {
                            SosBlocker.NO_CONTACT -> {
                                val r = snackbarHostState.showSnackbar(
                                    message = "No emergency contact yet",
                                    actionLabel = "Add one",
                                    withDismissAction = true
                                )
                                if (r == SnackbarResult.ActionPerformed) {
                                    navController.navigate(Routes.SAFETY_CONTACTS)
                                }
                            }
                            SosBlocker.NO_SMS_PERMISSION -> {
                                val r = snackbarHostState.showSnackbar(
                                    message = "SafeShade cannot send texts yet",
                                    actionLabel = "Allow",
                                    withDismissAction = true
                                )
                                if (r == SnackbarResult.ActionPerformed) requestSms()
                            }
                            SosBlocker.ALERT_ALREADY_LIVE ->
                                snackbarHostState.showSnackbar("An alert is already open")
                            SosBlocker.NOT_READY, null -> Unit
                        }
                    }
                },
                onTap = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Hold the SOS for 5 seconds to send")
                    }
                },
                onFire = { viewModel.firePhoneSos() }
            )

            Scaffold(
                containerColor = colors.ground,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    BoardBottomBar(
                        navController = navController,
                        role = ready.role,
                        sos = sosBarState,
                        onReselect = { destination ->
                            scope.launch {
                                listStateFor(
                                    destination,
                                    boardListState, circleListState,
                                    safetyListState, deviceListState
                                ).animateScrollToItem(0)
                            }
                        }
                    )
                }
            ) { padding ->
                // The area's decorative accent, provided once for the whole
                // graph rather than passed to each of forty section headings.
                // Keyed off the tab so a screen inherits its area's colour with
                // no per-screen wiring, which is what stops one new section
                // shipping grey in a coloured screen.
                CompositionLocalProvider(LocalBoardAccent provides accentFor(currentTab, colors)) {
                    MainNavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        state = ready,
                        boardListState = boardListState,
                        circleListState = circleListState,
                        safetyListState = safetyListState,
                        deviceListState = deviceListState,
                        // The full inset set, not just the bottom: edge-to-edge
                        // means the status bar overlaps content otherwise, and the
                        // header row is the first thing it eats.
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }

        // The trip layer sits above everything, including the bottom bar. It is
        // not a dialog: a dialog can be dismissed by tapping outside it, and
        // this must be answered.
        AnimatedVisibility(
            visible = ready.activeAlert != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val alert = ready.activeAlert
            if (alert != null) {
                TripBanner(
                    title = alert.kind.label,
                    body = tripBody(alert.kind, ready.deviceSettings.wearerName),
                    onDismiss = {
                        viewModel.resolveActiveAlert(TripOutcome.DISMISSED, contacted = false)
                    },
                    dismissLabel = "Everything is fine",
                    onEscalate = {
                        viewModel.resolveActiveAlert(TripOutcome.CONTACTED, contacted = true)
                    },
                    escalateLabel = "Call for help now"
                )
            }
        }
    }
}

/**
 * Which decorative accent an area wears.
 *
 * Assigned by what the area is *about*, not by taste: Circle is people and
 * places, Safety is detection, Device is hardware. Board takes none — it is the
 * summary of all four, and tinting it would make it look like a fifth area
 * rather than the view over them.
 */
private fun accentFor(tab: BottomDestination?, colors: BoardColors): Color? = when (tab) {
    BottomDestination.CIRCLE -> colors.accentSky
    BottomDestination.SAFETY -> colors.accentSage
    BottomDestination.DEVICE -> colors.accentSand
    // The Board takes no ambient accent, because it is the view *over* the
    // other three rather than a fourth area — one colour across it would make
    // it look like a peer. Its rows colour themselves individually instead,
    // each by where it leads (see `accentForRoute`), so the icon column tells
    // you which part of the app a row belongs to before you read it.
    BottomDestination.BOARD, null -> null
}

private fun listStateFor(
    destination: BottomDestination,
    board: LazyListState,
    circle: LazyListState,
    safety: LazyListState,
    device: LazyListState
): LazyListState = when (destination) {
    BottomDestination.BOARD -> board
    BottomDestination.CIRCLE -> circle
    BottomDestination.SAFETY -> safety
    BottomDestination.DEVICE -> device
}

/**
 * The sentence under a trip headline.
 *
 * Written for whoever is holding the phone, which for three of the four
 * personas is not the person who fell — so it names them rather than saying
 * "you".
 */
private fun tripBody(kind: TripKind, wearerName: String): String {
    val who = wearerName.ifBlank { "the wearer" }
    return when (kind) {
        TripKind.FALL -> "The device detected a fall. If $who is fine, close this. Otherwise call for help."
        TripKind.SOS -> "The SOS button was held on the device. $who is asking for help."
        TripKind.PHONE_SOS -> "You raised an SOS from this phone. Your emergency contacts have been messaged."
        TripKind.MISSED_CHECKIN -> "$who did not answer a check-in."
        TripKind.ZONE_EXIT -> "$who left a safe zone."
        TripKind.JOURNEY_OVERDUE -> "$who has not arrived, and the journey time has passed."
    }
}
