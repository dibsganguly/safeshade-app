package com.safeshade.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import com.safeshade.repo.AppState
import com.safeshade.ui.board.TripBanner
import com.safeshade.ui.nav.BoardBottomBar
import com.safeshade.ui.nav.MainNavGraph
import com.safeshade.ui.nav.OnboardingNavGraph
import com.safeshade.ui.screens.IntroScreen
import com.safeshade.ui.theme.board
import com.safeshade.ui.vm.SafeShadeViewModel

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
            Scaffold(
                containerColor = colors.ground,
                bottomBar = { BoardBottomBar(navController = navController, role = ready.role) }
            ) { padding ->
                MainNavGraph(
                    navController = navController,
                    viewModel = viewModel,
                    state = ready,
                    // The full inset set, not just the bottom: edge-to-edge
                    // means the status bar overlaps content otherwise, and the
                    // header row is the first thing it eats.
                    modifier = Modifier.padding(padding)
                )
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
        TripKind.MISSED_CHECKIN -> "$who did not answer a check-in."
        TripKind.ZONE_EXIT -> "$who left a safe zone."
        TripKind.JOURNEY_OVERDUE -> "$who has not arrived, and the journey time has passed."
    }
}
