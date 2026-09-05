package com.safeshade

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeshade.data.DarkModePreference
import com.safeshade.repo.AppState
import com.safeshade.ui.SafeShadeApp
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.vm.SafeShadeViewModel

/**
 * The only Activity.
 *
 * Its whole job is: hold the splash until state is readable, own the runtime
 * permission launchers (which need an Activity), and hand off to Compose.
 * Everything else — connection lifecycle, alert handling, persistence — lives
 * in the repositories on an application scope, so none of it dies when this
 * Activity is recreated or destroyed.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: SafeShadeViewModel by viewModels { SafeShadeViewModel.Factory }

    /**
     * Permissions needed before the app can do its core job at all.
     *
     * Requested from the onboarding screen that explains them rather than fired
     * at launch: a system dialog shown before the user knows what the app is
     * gets denied, and a denied Bluetooth permission makes the product inert.
     */
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.refreshPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()

        // Hold the splash until persisted state arrives, but never for long.
        // An unbounded hold reads as a hang, and on MIUI in particular a slow
        // first DataStore read would look like a failed launch.
        val startedAt = android.os.SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition {
            viewModel.appState.value is AppState.Loading &&
                android.os.SystemClock.uptimeMillis() - startedAt < 700L
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.appState.collectAsStateWithLifecycle()
            val preference = (state as? AppState.Ready)?.darkMode ?: DarkModePreference.SYSTEM
            val dark = when (preference) {
                DarkModePreference.LIGHT -> false
                DarkModePreference.DARK -> true
                DarkModePreference.SYSTEM -> isSystemInDarkTheme()
            }
            SafeShadeTheme(darkTheme = dark) {
                SafeShadeApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The user may have granted from system settings while we were away.
        viewModel.refreshPermissions()
    }

    /**
     * Asks for SEND_SMS, on its own.
     *
     * Deliberately not folded into [requestCorePermissions]. SEND_SMS is a
     * restricted permission and the cold-start dialog is the worst possible
     * place to ask for it — the user has not yet seen anything that sends a
     * text, so it reads as an app overreaching. It is requested instead at the
     * moment the SOS control is pressed without it, where the reason is
     * immediate and the ask is one tap from an explanation.
     *
     * Until this lands, `sendSmsText` returns PermissionMissing on every
     * install, which silently disabled the SMS fallbacks in AlertActionReceiver
     * and ReminderReceiver too.
     */
    fun requestSmsPermission() {
        requestPermissions.launch(arrayOf(Manifest.permission.SEND_SMS))
    }

    /** Called from the UI when a screen needs the core permissions. */
    fun requestCorePermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        requestPermissions.launch(needed.toTypedArray())
    }
}
