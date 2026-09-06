package com.safeshade.ui.nav

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.saveable.rememberSaveable
import com.safeshade.MainActivity
import com.safeshade.data.UserRole
import com.safeshade.repo.AppState
import com.safeshade.ui.screens.onboarding.MedicalStartScreen
import com.safeshade.ui.screens.onboarding.PairScreen
import com.safeshade.ui.screens.onboarding.PermissionsScreen
import com.safeshade.ui.screens.onboarding.RoleForkScreen
import com.safeshade.ui.screens.onboarding.WearerScreen
import com.safeshade.ui.screens.onboarding.WelcomeScreen
import com.safeshade.ui.vm.SafeShadeViewModel

/**
 * First run.
 *
 * A separate graph with its own `NavController` rather than a nested graph in
 * the main one. Onboarding has no bottom bar, no cross-links and exactly one
 * way through it; sharing a controller with the main graph would put six
 * one-shot destinations on the app's permanent back stack for the rest of the
 * process's life.
 *
 * Motion is shared-axis X for every step, since the whole flow is one
 * hierarchy walked forwards. There are no peers here to fade between.
 *
 * The flow only ever *ends* by calling [SafeShadeViewModel.setOnboardingSeen],
 * which flips `AppState.Ready.onboardingSeen` and makes `SafeShadeApp` swap
 * this graph out for the main one. Nothing here navigates to the board itself.
 */
@Composable
fun OnboardingNavGraph(
    viewModel: SafeShadeViewModel,
    state: AppState.Ready,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Re-read on every permission refresh. `permissionsGranted` is the app's
    // single "can we use the link" flag, but this screen has to report three
    // grants separately — a user who allowed Bluetooth and refused location
    // needs to see which one is still missing, not one amber line.
    val permissionsGranted by viewModel.permissionsGranted.collectAsStateWithLifecycle()
    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * The role as chosen *in this session*.
     *
     * `state.role` cannot answer "has the user chosen yet" — it is a
     * non-nullable enum that defaults to GUARDIAN, so reading it would show
     * the fork pre-answered and let the user walk past the single most
     * load-bearing question in the app without touching it.
     */
    var pickedRole by rememberSaveable { mutableStateOf<UserRole?>(null) }

    /**
     * The wearer's name while it is being typed.
     *
     * A buffer, not the record: it is persisted through
     * [SafeShadeViewModel.setWearerName] on the way out of the step, and again
     * at the end of the flow. Driving the text field from the persisted value
     * instead would round-trip DataStore on every keystroke and drop
     * characters typed faster than the write returns, and this is the one
     * field nearly every Guardian-role string in the app reads.
     */
    var wearerName by rememberSaveable { mutableStateOf(state.deviceSettings.wearerName) }
    var wearerAvatar by rememberSaveable {
        mutableStateOf(state.deviceSettings.wearerAvatarId.ifBlank { com.safeshade.ui.board.AvatarSpec.PRESETS.first().encode() })
    }

    // Only the three fields `MedicalStartScreen` edits. Kept local and written
    // once at the end, so a half-typed blood type never reaches DataStore.
    var bloodType by rememberSaveable { mutableStateOf(state.medicalId.bloodType) }
    var contactName by rememberSaveable { mutableStateOf(state.medicalId.contactName) }
    var contactNumber by rememberSaveable { mutableStateOf(state.medicalId.emergencyContact) }

    NavHost(
        navController = navController,
        startDestination = Routes.ONBOARDING_WELCOME,
        modifier = modifier,
        // `with(NavMotion)` rather than `NavMotion.sharedXEnter()`: those four
        // are member extension functions on the transition scope, so the object
        // has to be the dispatch receiver. An import will not compile.
        enterTransition = { with(NavMotion) { sharedXEnter() } },
        exitTransition = { with(NavMotion) { sharedXExit() } },
        popEnterTransition = { with(NavMotion) { sharedXPopEnter() } },
        popExitTransition = { with(NavMotion) { sharedXPopExit() } }
    ) {

        composable(Routes.ONBOARDING_WELCOME) {
            WelcomeScreen(onStart = { navController.navigate(Routes.ONBOARDING_ROLE) })
        }

        composable(Routes.ONBOARDING_ROLE) {
            RoleForkScreen(
                selected = pickedRole,
                onSelect = { role ->
                    pickedRole = role
                    // Written immediately rather than at the end of the flow:
                    // every screen after this one is worded from the role, so
                    // it has to be true before the next step renders.
                    viewModel.setRole(role)
                },
                onContinue = { navController.navigate(Routes.ONBOARDING_WEARER) }
            )
        }

        composable(Routes.ONBOARDING_WEARER) {
            WearerScreen(
                role = pickedRole ?: state.role,
                wearerName = wearerName,
                onWearerNameChange = { wearerName = it },
                avatarId = wearerAvatar,
                onAvatarChange = { wearerAvatar = it },
                onContinue = {
                    viewModel.setWearerName(wearerName)
                    viewModel.setWearerAvatar(wearerAvatar)
                    // A Companion is their own wearer, so "Me" on the Profile
                    // page is the same person; a Guardian names themselves
                    // there later.
                    if ((pickedRole ?: state.role) == UserRole.COMPANION) {
                        viewModel.setOwner(wearerName, wearerAvatar)
                    }
                    navController.navigate(Routes.ONBOARDING_PERMISSIONS)
                }
            )
        }

        composable(Routes.ONBOARDING_PERMISSIONS) {
            // Read before the three checks below, and used by one of them.
            // `checkSelfPermission` is a plain call with no snapshot state
            // behind it, so touching the flow here is what makes this screen
            // recompose after the system dialog is answered.
            val linkGranted = permissionsGranted

            PermissionsScreen(
                bluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    linkGranted &&
                        granted(Manifest.permission.BLUETOOTH_SCAN) &&
                        granted(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    // Pre-31 there is no runtime Bluetooth permission at all;
                    // scanning rides on location, which is the next row down.
                    linkGranted
                },
                locationGranted = granted(Manifest.permission.ACCESS_FINE_LOCATION),
                notificationsGranted =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        granted(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        true
                    },
                onRequest = { (context as? MainActivity)?.requestCorePermissions() },
                onContinue = { navController.navigate(Routes.ONBOARDING_PAIR) }
            )
        }

        composable(Routes.ONBOARDING_PAIR) {
            PairScreen(
                connection = state.connection,
                deviceName = state.deviceSettings.name,
                onScan = { viewModel.connect() },
                onContinue = { navController.navigate(Routes.ONBOARDING_MEDICAL) }
            )
        }

        composable(Routes.ONBOARDING_MEDICAL) {
            MedicalStartScreen(
                medicalId = state.medicalId.copy(
                    bloodType = bloodType,
                    contactName = contactName,
                    emergencyContact = contactNumber
                ),
                onBloodTypeChange = { bloodType = it },
                onContactNameChange = { contactName = it },
                onContactNumberChange = { contactNumber = it },
                onFinish = {
                    viewModel.setMedicalId(
                        state.medicalId.copy(
                            bloodType = bloodType,
                            contactName = contactName,
                            emergencyContact = contactNumber
                        )
                    )
                    // Written again in case the user reached this step by a
                    // route that skipped the wearer step's own save — back out
                    // and forward again, say. The write is idempotent, and the
                    // name being wrong is visible on every screen afterwards.
                    viewModel.setWearerName(wearerName)
                    viewModel.setWearerAvatar(wearerAvatar)
                    // Last, and only here. Flipping this is what tears the
                    // whole graph down, so anything that still needs writing
                    // must already have been written.
                    viewModel.setOnboardingSeen(true)
                }
            )
        }
    }
}
