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
import com.safeshade.data.EmergencyContact
import com.safeshade.data.SosBlocker
import com.safeshade.data.SosOutcome
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import com.safeshade.placeEmergencyCall
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

    // Read here rather than inside the trip layer because the banner lives
    // outside the navigation branch. Durable state, not a one-shot event: the
    // trip is recorded *before* the messages are sent, so the banner is already
    // on screen while the sends are still in flight and has to be able to tell
    // the truth at every point in between.
    val sosOutcome by viewModel.sosOutcome.collectAsStateWithLifecycle()

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
            // Keyed on the SMS grant specifically, not on the Bluetooth one.
            // An earlier version keyed this on `permissionsGranted` on the
            // reasoning that it "changes on every resume" — it does not: it
            // holds the link permissions, and a StateFlow does not emit when
            // the value is unchanged. Granting SEND_SMS therefore left the
            // control disarmed, on the one install where arming it is the whole
            // point. `smsGranted` is refreshed by the same `refreshPermissions`
            // call, from both the permission-result callback and onResume.
            val smsGranted by viewModel.smsGranted.collectAsStateWithLifecycle()
            val canFireSos = remember(smsGranted, ready.safetySettings.emergencyContacts, ready.activeAlert) {
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
                val phoneSos = alert.kind == TripKind.PHONE_SOS
                // Who the escalate button actually calls. Named on the button,
                // because "Call for help now" told the user nothing about who
                // would be dialled — and, worse, dialled nobody: it only wrote
                // CONTACTED to the log and closed the banner.
                val firstContact: EmergencyContact? =
                    ready.safetySettings.emergencyContacts.firstOrNull()

                TripBanner(
                    title = alert.kind.label,
                    body = if (phoneSos) {
                        sosBody(sosOutcome, ready.safetySettings.emergencyContacts.size)
                    } else {
                        tripBody(alert.kind, ready.deviceSettings.wearerName)
                    },
                    onDismiss = {
                        // On a phone SOS the messages have gone out, so closing
                        // this is "I am safe now", not "nothing happened" — the
                        // history entry has to say somebody was contacted or a
                        // guardian reading it later sees an alert that appears
                        // to have reached nobody.
                        viewModel.resolveActiveAlert(
                            TripOutcome.DISMISSED,
                            contacted = phoneSos && (sosOutcome as? SosOutcome.Sent)?.anyReached == true
                        )
                        viewModel.clearSosOutcome()
                    },
                    dismissLabel = if (phoneSos) "I am safe now" else "Everything is fine",
                    // No contact means nothing to call, so the button is absent
                    // rather than drawn and dead.
                    onEscalate = firstContact?.let { contact ->
                        {
                            placeEmergencyCall(context, contact)
                            viewModel.resolveActiveAlert(TripOutcome.CONTACTED, contacted = true)
                            viewModel.clearSosOutcome()
                        }
                    },
                    escalateLabel = firstContact?.let { "Call ${it.name}" } ?: "Call for help now"
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
        // Never reached in practice — a phone SOS renders `sosBody` instead,
        // which knows whether the messages actually went. Kept honest anyway,
        // because a stale trip restored from history has no outcome to read.
        TripKind.PHONE_SOS -> "You raised an SOS from this phone."
        TripKind.MISSED_CHECKIN -> "$who did not answer a check-in."
        TripKind.ZONE_EXIT -> "$who left a safe zone."
        TripKind.JOURNEY_OVERDUE -> "$who has not arrived, and the journey time has passed."
    }
}

/**
 * What the phone SOS actually did, in one sentence.
 *
 * This exists because the previous body asserted "your emergency contacts have
 * been messaged" unconditionally, and it was on screen *before* a single
 * message had been attempted — the trip is recorded first, deliberately, so the
 * banner is up while the sends are still in flight. If `SmsManager` then failed
 * on every number, the screen still said they had been told.
 *
 * `EmergencyActions`' own header names this exact failure as the reason that
 * file exists: "a UI that looked like it had sent an emergency alert and a
 * logcat line nobody was reading." So the three states are distinct and none of
 * them overstates: in flight, some or all reached, none reached.
 *
 * @param contactCount how many were being written to, so the in-flight line can
 *   be specific before any result exists.
 */
private fun sosBody(outcome: SosOutcome?, contactCount: Int): String = when (outcome) {
    null -> if (contactCount == 1) {
        "Sending to your emergency contact…"
    } else {
        "Sending to your $contactCount emergency contacts…"
    }

    SosOutcome.NoContact ->
        "There was nobody to send to. Add an emergency contact in Safety."

    is SosOutcome.Sent -> buildString {
        when {
            outcome.reached.isEmpty() ->
                append("The message could not be sent. Call for help directly.")

            outcome.failed.isEmpty() ->
                append("Sent to ${outcome.reached.joinToString(", ")}.")

            else -> append(
                "Sent to ${outcome.reached.joinToString(", ")}. " +
                    "Could not reach ${outcome.failed.joinToString(", ")}."
            )
        }
        // Both of these change what the reader should do next, so they are
        // worth the extra clause. Without the location note a person cannot
        // tell whether anyone knows where they are.
        if (outcome.reached.isNotEmpty() && !outcome.hasLocation) {
            append(" No location was available to include.")
        }
        if (outcome.onDevice) append(" The device has been alerted too.")
    }
}
