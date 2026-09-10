package com.safeshade.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Callout
import com.safeshade.ui.board.Footnote
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

private const val TAG = "ReliabilityScreen"

/** The six things that can stop a fall alert reaching this phone. */
enum class ReliabilityCheck {
    NOTIFICATIONS,
    FULL_SCREEN_INTENT,
    EXACT_ALARMS,
    BACKGROUND_LOCATION,
    BATTERY_OPTIMISATION,
    OEM_AUTOSTART
}

/**
 * What a check currently reports.
 *
 * [UNKNOWN] is a real answer and not a placeholder. Autostart on MIUI cannot be
 * queried at all — there is no public API — so the honest report is that we
 * cannot tell, with the setting one tap away.
 */
enum class CheckStatus { PASSING, FAILING, UNKNOWN }

/** Everything the reliability screen draws. */
data class ReliabilityUiState(
    val statuses: Map<ReliabilityCheck, CheckStatus> = emptyMap(),
    /** For the OEM row's copy. `Build.MANUFACTURER`, read by the caller. */
    val manufacturer: String = "",
    /**
     * Whether to show the OEM autostart row at all.
     *
     * The caller decides, because it depends on the manufacturer and on
     * whether the relevant settings app is even installed.
     */
    val showOemAutostart: Boolean = false,
    /**
     * False below API 34, where a full-screen intent is granted at install time.
     *
     * There is no setting to send anyone to on those versions, so a row saying
     * "blocked" would be a false alarm on a screen whose entire value is being
     * believed when it does report something.
     */
    val showFullScreenIntent: Boolean = true,
    /** False below API 31, where exact alarms need no grant at all. */
    val showExactAlarms: Boolean = true,
    /** Preformatted, e.g. "Sent 2 minutes ago". Null when none has been sent. */
    val lastTestAlertLabel: String? = null
)

private fun ReliabilityUiState.statusOf(check: ReliabilityCheck): CheckStatus =
    statuses[check] ?: CheckStatus.UNKNOWN

/**
 * The permission doctor.
 *
 * Six settings, any one of which can make a fall alert simply not arrive, with
 * no error anywhere: a denied full-screen intent is silently downgraded to a
 * heads-up notification, an OEM battery killer stops the foreground service
 * without a callback, and inexact alarms miss a check-in deadline by a quarter
 * of an hour. None of that is observable from inside the app, which is why this
 * screen exists and why it ends with a test rather than a checklist.
 *
 * Every row opens a settings screen from *here* rather than through a callback.
 * That is deliberate: the fragile part of this feature is the intent resolution
 * itself — MIUI's autostart component name moves between versions and an
 * unresolvable explicit intent throws `ActivityNotFoundException` — so the
 * try/catch and the fallback chain belong in the same file as the row that
 * needs them, not spread across a caller that may forget.
 */
@Composable
fun ReliabilityScreen(
    state: ReliabilityUiState,
    onSendTestAlert: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            ScreenHeader(
                title = "Alert reliability",
                subtitle = "Android can block an alert without telling either of us. " +
                    "These are the settings that decide whether a fall reaches " +
                    "this phone.",
                onBack = onBack
            )
        }

        item("checks-heading") { SectionPlate(title = "Checks") }

        item("checks") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                CheckWay(
                    status = state.statusOf(ReliabilityCheck.NOTIFICATIONS),
                    name = "Notifications",
                    consequence = "Without this, nothing appears when a fall is detected.",
                    icon = SafeShadeIcons.NotificationBell,
                    onOpen = { context.openNotificationSettings() }
                )
                if (state.showFullScreenIntent) {
                    Hairline()
                    CheckWay(
                        status = state.statusOf(ReliabilityCheck.FULL_SCREEN_INTENT),
                        name = "Full-screen alerts",
                        // The wording matters: this one degrades rather than
                        // fails, and a user told it is "broken" will not believe
                        // the alert they can plainly see arriving.
                        consequence = "Without this, a fall alert still arrives but as " +
                            "a banner rather than taking over the lock screen.",
                        icon = SafeShadeIcons.FullScreen,
                        onOpen = { context.openFullScreenIntentSettings() }
                    )
                }
                if (state.showExactAlarms) {
                    Hairline()
                    CheckWay(
                        status = state.statusOf(ReliabilityCheck.EXACT_ALARMS),
                        name = "Exact alarms",
                        consequence = "Without this, medication reminders and check-in " +
                            "deadlines drift by up to 15 minutes.",
                        icon = SafeShadeIcons.DailyReminder,
                        onOpen = { context.openExactAlarmSettings() }
                    )
                }
                Hairline()
                CheckWay(
                    status = state.statusOf(ReliabilityCheck.BACKGROUND_LOCATION),
                    name = "Location all the time",
                    consequence = "Without this, safe-zone alerts stop as soon as the " +
                        "app is not on screen.",
                    icon = SafeShadeIcons.Gps,
                    // There is no dialog for "allow all the time" after the
                    // first refusal; the app details page is the only route.
                    onOpen = { context.openAppDetails() }
                )
                Hairline()
                CheckWay(
                    status = state.statusOf(ReliabilityCheck.BATTERY_OPTIMISATION),
                    name = "Battery optimisation",
                    consequence = "With optimisation on, Android drops the Bluetooth " +
                        "link within minutes of the screen going off.",
                    icon = SafeShadeIcons.BatteryOptimisationOn,
                    onOpen = { context.openBatteryOptimisationSettings() }
                )
                if (state.showOemAutostart) {
                    Hairline()
                    CheckWay(
                        status = state.statusOf(ReliabilityCheck.OEM_AUTOSTART),
                        name = "Autostart",
                        consequence = "On ${state.manufacturer.ifBlank { "this" }} phones " +
                            "the system stops the app after a reboot unless autostart " +
                            "is allowed. There is no way to check this from inside the " +
                            "app, so it has to be confirmed by hand.",
                        icon = SafeShadeIcons.Restart,
                        onOpen = { context.openAutostartSettings() }
                    )
                }
            }
        }

        // ---------- The test ----------
        item("test-heading") { SectionPlate(title = "Test") }

        item("test-callout") {
            Callout(
                lead = "A pass here is not a guarantee.",
                sentence = "Every check above can pass and an alert can still be killed by something this app cannot see. Sending a test is the only way to watch it happen, or watch it not."
            )
        }

        item("test") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Nameplate("Send a test alert")
                    if (state.lastTestAlertLabel != null) {
                        Text(
                            text = state.lastTestAlertLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkFaint
                        )
                    }
                    BoardButton(
                        label = "Send a Test Alert",
                        supporting = "Lock the phone first. It should take over the screen within a few seconds.",
                        // A callback, unlike the rows: firing a test alert needs
                        // the notifier and the alert machinery, which is the
                        // caller's, and it must be the same code path a real
                        // fall takes or the test proves nothing.
                        onClick = onSendTestAlert,
                        weight = ButtonWeight.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item("footnote") {
            Footnote("This app never asks for a permission it does not use. Location is for weather and safe zones; SMS is the fallback when Bluetooth is out of range.")
        }
    }
}

/** One check as a way. Tapping it opens the setting that fixes it. */
@Composable
private fun CheckWay(
    status: CheckStatus,
    name: String,
    consequence: String,
    icon: ImageVector,
    onOpen: () -> Unit
) {
    Way(
        name = name,
        state = when (status) {
            CheckStatus.PASSING -> LampState.LIVE
            // Amber rather than red: nothing has gone wrong yet. Red here would
            // be crying wolf on a screen whose whole purpose is to be believed
            // when it does report something.
            CheckStatus.FAILING -> LampState.ATTENTION
            CheckStatus.UNKNOWN -> LampState.UNKNOWN
        },
        stateLabel = when (status) {
            CheckStatus.PASSING -> "Allowed"
            CheckStatus.FAILING -> "Blocked"
            CheckStatus.UNKNOWN -> "Unknown"
        },
        help = consequence,
        icon = icon,
        onClick = onOpen
    )
}

// ============================================================================
// Settings intents
//
// Every one of these can fail. An OEM ROM may not ship the activity, an
// explicit component name may have moved between versions, and a manufacturer
// may have removed the screen entirely. An unresolvable intent throws
// ActivityNotFoundException, and on some ROMs a guarded activity throws
// SecurityException instead — so each attempt is caught individually and the
// chain always ends at the app's own details page, which every Android build
// has.
// ============================================================================

/**
 * Tries each intent in order and falls back to this app's details page.
 *
 * Deliberately catches broadly. The point of the fallback is that the user ends
 * up somewhere useful no matter what the ROM does; an exception type nobody
 * anticipated is exactly the case the fallback exists for, and letting it
 * propagate would crash the app from a settings row.
 */
private fun Context.openFirstAvailable(vararg candidates: Intent) {
    for (intent in candidates) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (e: ActivityNotFoundException) {
            Log.i(TAG, "No activity for ${intent.action ?: intent.component}", e)
        } catch (e: SecurityException) {
            Log.i(TAG, "Not permitted to open ${intent.action ?: intent.component}", e)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open ${intent.action ?: intent.component}", e)
        }
    }
    try {
        startActivity(appDetailsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // Nothing left to try. Better a row that does nothing than a crash from
        // inside the screen whose job is to make the app more reliable.
        Log.w(TAG, "Could not open app details either", e)
    }
}

private fun Context.appDetailsIntent(): Intent = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", packageName, null)
)

private fun Context.openAppDetails() = openFirstAvailable(appDetailsIntent())

private fun Context.openNotificationSettings() {
    openFirstAvailable(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    )
}

/**
 * API 34+ only. Below that, a full-screen intent is granted at install time and
 * there is no screen to send anyone to, so this falls through to app details.
 */
private fun Context.openFullScreenIntentSettings() {
    val candidates: Array<Intent> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.fromParts("package", packageName, null)
            )
        )
    } else {
        emptyArray()
    }
    openFirstAvailable(*candidates)
}

/** API 31+ only. Exact alarms need no grant below that. */
private fun Context.openExactAlarmSettings() {
    val candidates: Array<Intent> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.fromParts("package", packageName, null)
            )
        )
    } else {
        emptyArray()
    }
    openFirstAvailable(*candidates)
}

/**
 * The optimisation *list*, not the per-app request dialog.
 *
 * `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` requires the
 * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission, which this app does not
 * declare — Play's policy on it is narrow and a safety companion is not
 * obviously inside it. The list screen needs nothing and gets the user to the
 * same toggle one tap later.
 */
private fun Context.openBatteryOptimisationSettings() {
    openFirstAvailable(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

/**
 * OEM autostart managers.
 *
 * These are explicit component names on other manufacturers' system apps, which
 * is as fragile as it sounds: the class moves between ROM versions, is absent
 * on some builds, and is guarded on others. They are tried in order and every
 * failure is caught, ending at app details.
 */
private fun Context.openAutostartSettings() {
    openFirstAvailable(
        Intent().setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        ),
        Intent().setComponent(
            ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
        )
    )
}

// ============================================================================
// Previews
// ============================================================================

private val previewReliability = ReliabilityUiState(
    statuses = mapOf(
        ReliabilityCheck.NOTIFICATIONS to CheckStatus.PASSING,
        ReliabilityCheck.FULL_SCREEN_INTENT to CheckStatus.FAILING,
        ReliabilityCheck.EXACT_ALARMS to CheckStatus.FAILING,
        ReliabilityCheck.BACKGROUND_LOCATION to CheckStatus.PASSING,
        ReliabilityCheck.BATTERY_OPTIMISATION to CheckStatus.PASSING,
        ReliabilityCheck.OEM_AUTOSTART to CheckStatus.UNKNOWN
    ),
    manufacturer = "Xiaomi",
    showOemAutostart = true,
    lastTestAlertLabel = "Last test sent 4 minutes ago"
)

@Preview(name = "Reliability · light", showBackground = true)
@Composable
private fun ReliabilityPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ReliabilityScreen(state = previewReliability, onSendTestAlert = {})
        }
    }
}

@Preview(name = "Reliability · dark", showBackground = true)
@Composable
private fun ReliabilityPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ReliabilityScreen(state = previewReliability, onSendTestAlert = {})
        }
    }
}

@Preview(name = "Reliability · all clear", showBackground = true, fontScale = 1.3f)
@Composable
private fun ReliabilityPreviewAllClear() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ReliabilityScreen(
                state = ReliabilityUiState(
                    statuses = ReliabilityCheck.entries.associateWith { CheckStatus.PASSING },
                    showOemAutostart = false
                ),
                onSendTestAlert = {}
            )
        }
    }
}
