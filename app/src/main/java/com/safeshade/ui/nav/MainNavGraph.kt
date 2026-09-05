package com.safeshade.ui.nav

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.safeshade.BuildConfig
import com.safeshade.MainActivity
import com.safeshade.data.CheckInRequest
import com.safeshade.data.EmergencyContact
import com.safeshade.data.GeofenceZone
import com.safeshade.data.JourneyState
import com.safeshade.data.LedPattern
import com.safeshade.data.MedicalId
import com.safeshade.data.MessageChannel
import com.safeshade.data.PersonaMode
import com.safeshade.data.Reminder
import com.safeshade.data.ReminderKind
import com.safeshade.data.TripOutcome
import com.safeshade.data.UserRole
import com.safeshade.device.ConnectionState
import com.safeshade.repo.AppState
import com.safeshade.repo.SyncStatus
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.LampState
import com.safeshade.ui.screens.board.BoardScreen
import com.safeshade.ui.screens.board.BoardUiState
import com.safeshade.ui.screens.board.BoardWay
import com.safeshade.ui.screens.circle.CheckInHistoryRow
import com.safeshade.ui.screens.circle.CheckInOutcome
import com.safeshade.ui.screens.circle.CheckInScreen
import com.safeshade.ui.screens.circle.CheckInUiState
import com.safeshade.ui.screens.circle.CircleMessagePreview
import com.safeshade.ui.screens.circle.CircleScreen
import com.safeshade.ui.screens.circle.CircleUiState
import com.safeshade.ui.screens.circle.CircleWay
import com.safeshade.ui.screens.circle.JourneyScreen
import com.safeshade.ui.screens.circle.JourneyUiState
import com.safeshade.ui.screens.circle.MessagesScreen
import com.safeshade.ui.screens.circle.MessagesUiState
import com.safeshade.ui.screens.circle.SimScreen
import com.safeshade.ui.screens.circle.SimUiState
import com.safeshade.ui.screens.circle.ThreadMessage
import com.safeshade.ui.screens.circle.ZoneEditorScreen
import com.safeshade.ui.screens.circle.ZoneEditorUiState
import com.safeshade.ui.screens.circle.ZonePickerScreen
import com.safeshade.ui.screens.circle.ZonePickerUiState
import com.safeshade.ui.screens.circle.ZonePresence
import com.safeshade.ui.screens.circle.ZoneRow
import com.safeshade.ui.screens.circle.ZonesScreen
import com.safeshade.ui.screens.circle.ZonesUiState
import com.safeshade.ui.screens.device.AckState
import com.safeshade.ui.screens.device.DeviceOnlyScreen
import com.safeshade.ui.screens.device.DeviceOnlyUiState
import com.safeshade.ui.screens.device.DeviceScreen
import com.safeshade.ui.screens.device.DeviceSettingsScreen
import com.safeshade.ui.screens.device.DeviceSettingsUiState
import com.safeshade.ui.screens.device.DeviceUiState
import com.safeshade.ui.screens.device.LightsScreen
import com.safeshade.ui.screens.device.LightsUiState
import com.safeshade.ui.screens.device.LocateScreen
import com.safeshade.ui.screens.device.LocateUiState
import com.safeshade.ui.screens.device.ModePickerScreen
import com.safeshade.ui.screens.device.ModePickerUiState
import com.safeshade.ui.screens.device.PairedDevicesScreen
import com.safeshade.ui.screens.device.PairedDevicesUiState
import com.safeshade.ui.screens.device.RemindersScreen
import com.safeshade.ui.screens.device.RemindersUiState
import com.safeshade.ui.screens.device.TelemetryScreen
import com.safeshade.ui.screens.device.TelemetryUiState
import com.safeshade.ui.screens.safety.ContactDraft
import com.safeshade.ui.screens.safety.ContactsScreen
import com.safeshade.ui.screens.safety.ContactsUiState
import com.safeshade.ui.screens.safety.EmergencyCardScreen
import com.safeshade.ui.screens.safety.EmergencyCardUiState
import com.safeshade.ui.screens.safety.FallSettingsScreen
import com.safeshade.ui.screens.safety.FallSettingsUiState
import com.safeshade.ui.screens.safety.MedicalIdScreen
import com.safeshade.ui.screens.safety.MedicalIdUiState
import com.safeshade.ui.screens.safety.SafetyScreen
import com.safeshade.ui.screens.safety.SafetyUiState
import com.safeshade.ui.screens.safety.ServicesScreen
import com.safeshade.ui.screens.safety.ServicesUiState
import com.safeshade.ui.screens.safety.SilentSosScreen
import com.safeshade.ui.screens.safety.SilentSosUiState
import com.safeshade.ui.screens.safety.TripDetailScreen
import com.safeshade.ui.screens.safety.TripDetailUiState
import com.safeshade.ui.screens.safety.TripLogScreen
import com.safeshade.ui.screens.safety.TripLogUiState
import com.safeshade.ui.screens.settings.AboutScreen
import com.safeshade.ui.screens.settings.AboutUiState
import com.safeshade.ui.screens.settings.AppearanceScreen
import com.safeshade.ui.screens.settings.AppearanceUiState
import com.safeshade.ui.screens.settings.CheckStatus
import com.safeshade.ui.screens.settings.DeveloperScreen
import com.safeshade.ui.screens.settings.DeveloperUiState
import com.safeshade.ui.screens.settings.ReliabilityCheck
import com.safeshade.ui.screens.settings.ReliabilityScreen
import com.safeshade.ui.screens.settings.ReliabilityUiState
import com.safeshade.ui.screens.settings.RoleScreen
import com.safeshade.ui.screens.settings.RoleUiState
import com.safeshade.ui.screens.settings.SettingsScreen
import com.safeshade.ui.screens.settings.SettingsUiState
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.vm.SafeShadeViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The app proper.
 *
 * Every destination is assembled here rather than inside the screens, which is
 * what lets the screens stay pure: each one takes a finished `XxxUiState` and a
 * set of callbacks and owns nothing. That has a cost — this file is long and
 * knows about every screen — and one large benefit: there is exactly one place
 * where "what the app knows" (`AppState.Ready`) is turned into "what a screen
 * draws", so a field added to the state cannot quietly reach one screen and
 * miss another.
 *
 * Three things in here are deliberate and easy to undo by accident:
 *
 *  1. **The graph never branches on [AppState.Ready.role].** Both roles walk
 *     the same routes; only the copy differs, and that is decided inside the
 *     `UiState`s. Swapping `startDestination` or rebuilding the graph when the
 *     role changes would empty the back stack under the user's feet.
 *  2. **Transient editing state lives here, not in the screens.** Drafts,
 *     confirmations and half-typed coordinates are held in `rememberSaveable`
 *     in the graph entry that owns them. The zone draft is hoisted one level
 *     higher still, above the `NavHost`, because the map picker and the editor
 *     genuinely share it — see [Routes.CIRCLE_ZONE_PICK].
 *  3. **No `BackHandler` anywhere.** Every sub-screen's `onBack` is a plain
 *     `popBackStack()`, and the screens that take no `onBack` at all rely on
 *     system back. The only dirty-form candidate is the medical ID, and it has
 *     no discard affordance, so intercepting back there would trap the user in
 *     a screen they cannot leave without saving.
 */
@Composable
fun MainNavGraph(
    navController: NavHostController,
    viewModel: SafeShadeViewModel,
    state: AppState.Ready,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val permissionsGranted by viewModel.permissionsGranted.collectAsStateWithLifecycle()

    val requestPermissions: () -> Unit = {
        (context as? MainActivity)?.requestCorePermissions()
    }

    // `NavHost` remembers the graph keyed on the builder lambda, so a builder
    // that closes over a *value* rebuilds all thirty-odd destinations every
    // time that value changes — and `AppState.Ready` changes roughly once a
    // second while the link is up, carrying fresh telemetry. Reading each one
    // through a `State` instead means the builder captures only
    // identity-stable references, so the graph is built once and each entry
    // still recomposes on its own when what it reads changes.
    val liveState = rememberUpdatedState(state)
    val liveWeather = rememberUpdatedState(weather)
    val liveLocation = rememberUpdatedState(location)
    val liveSyncing = rememberUpdatedState(isSyncing)
    val livePermissions = rememberUpdatedState(permissionsGranted)

    // ============================================
    // Editing state shared across two destinations
    // ============================================

    // `ZonePickerScreen.onConfirm` takes no payload — it cannot hand a point
    // back — so the whole zone draft has to live above both entries and the
    // picker commits into it directly. Held as separate primitives rather than
    // one draft object because `rememberSaveable` has no saver for a
    // `GeofenceZone` and would throw on restore.
    var zoneDraftId by rememberSaveable { mutableStateOf<String?>(null) }
    var zoneDraftName by rememberSaveable { mutableStateOf("") }
    var zoneDraftRadius by rememberSaveable { mutableFloatStateOf(200f) }
    var zoneDraftLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var zoneDraftLon by rememberSaveable { mutableStateOf<Double?>(null) }
    var zoneDraftExit by rememberSaveable { mutableStateOf(true) }
    var zoneDraftEnter by rememberSaveable { mutableStateOf(false) }

    var pickLatField by rememberSaveable { mutableStateOf("") }
    var pickLonField by rememberSaveable { mutableStateOf("") }
    var pickLocating by rememberSaveable { mutableStateOf(false) }
    var pickError by rememberSaveable { mutableStateOf<String?>(null) }

    // Seeded at *navigation* time rather than in a `LaunchedEffect` inside the
    // editor: the editor leaves composition while the picker is open, so an
    // effect there would re-run on the way back and throw away the point the
    // user just chose.
    val seedZoneDraft: (GeofenceZone?) -> Unit = { zone ->
        zoneDraftId = zone?.id
        zoneDraftName = zone?.name.orEmpty()
        zoneDraftRadius = zone?.radiusMeters ?: 200f
        zoneDraftLat = zone?.lat
        zoneDraftLon = zone?.lon
        zoneDraftExit = zone?.alertOnExit ?: true
        zoneDraftEnter = zone?.alertOnEnter ?: false
        pickLatField = zone?.lat?.toString().orEmpty()
        pickLonField = zone?.lon?.toString().orEmpty()
        pickError = null
    }

    // ============================================
    // State the model does not carry yet
    // ============================================
    // Each of these is shown by a screen but has no home in `AppState.Ready`
    // and no view-model writer. Kept here so the controls at least behave
    // consistently within a session, and listed in the handover so they are
    // not mistaken for persisted settings.

    var silentSosEnabled by rememberSaveable { mutableStateOf(false) }
    var stagedCallSeconds by rememberSaveable { mutableStateOf<Int?>(null) }
    var ledPattern by rememberSaveable { mutableStateOf(LedPattern.TORCH) }
    var quietHoursEnabled by rememberSaveable { mutableStateOf(false) }
    var quietStartHour by rememberSaveable { mutableIntStateOf(22) }
    var quietEndHour by rememberSaveable { mutableIntStateOf(7) }
    var medicationEnabled by rememberSaveable { mutableStateOf(false) }
    var medicationHour by rememberSaveable { mutableIntStateOf(9) }
    var medicationMinute by rememberSaveable { mutableIntStateOf(0) }
    var checkInIntervalMinutes by rememberSaveable { mutableIntStateOf(0) }

    NavHost(
        navController = navController,
        startDestination = Routes.BOARD,
        modifier = modifier,
        // Motion is decided from the *pair* of routes rather than per
        // destination, because a transition is resolved from the incoming
        // destination's spec and the outgoing one's: a bottom destination's
        // single `exitTransition` would otherwise have to serve both "switch
        // to a peer" (fade through) and "open a child" (shared axis X).
        enterTransition = {
            when (motionFor(initialState.route(), targetState.route())) {
                Motion.FADE_THROUGH -> NavMotion.fadeThroughEnter
                Motion.SHARED_Z -> NavMotion.sharedZEnter
                Motion.SHARED_X -> with(NavMotion) { sharedXEnter() }
            }
        },
        exitTransition = {
            when (motionFor(initialState.route(), targetState.route())) {
                Motion.FADE_THROUGH -> NavMotion.fadeThroughExit
                Motion.SHARED_Z -> NavMotion.sharedZExit
                Motion.SHARED_X -> with(NavMotion) { sharedXExit() }
            }
        },
        popEnterTransition = {
            when (motionFor(initialState.route(), targetState.route())) {
                Motion.FADE_THROUGH -> NavMotion.fadeThroughEnter
                Motion.SHARED_Z -> NavMotion.sharedZPopEnter
                Motion.SHARED_X -> with(NavMotion) { sharedXPopEnter() }
            }
        },
        popExitTransition = {
            when (motionFor(initialState.route(), targetState.route())) {
                Motion.FADE_THROUGH -> NavMotion.fadeThroughExit
                Motion.SHARED_Z -> NavMotion.sharedZPopExit
                Motion.SHARED_X -> with(NavMotion) { sharedXPopExit() }
            }
        }
    ) {

        // ============================================
        // Board
        // ============================================

        composable(Routes.BOARD) {
            val state = liveState.value
            val weather = liveWeather.value
            val isSyncing = liveSyncing.value
            val permissionsGranted = livePermissions.value
            val hasUnresolvedTrip =
                state.activeAlert != null || state.tripHistory.any { it.outcome == TripOutcome.PENDING }

            BoardScreen(
                state = BoardUiState(
                    connection = state.connection,
                    role = state.role,
                    headline = boardHeadline(state.connection, state.role, state.wearerName),
                    subline = boardSubline(state, permissionsGranted),
                    batteryPercent = state.telemetry.batteryLevel.takeIf { state.telemetry.isRealData },
                    signalDbm = state.rssiSmoothed.takeIf { state.connection.isUsable },
                    ways = boardWays(state),
                    temperatureC = weather.temp.takeIf { weather.isLoaded },
                    weatherCondition = weather.condition.takeIf { weather.isLoaded },
                    uvIndex = weather.uvIndex.takeIf { weather.isLoaded },
                    lastSyncLabel = weather.lastSyncTime.takeIf { weather.isLoaded },
                    isSyncing = isSyncing,
                    isRinging = state.isRinging,
                    hasUnresolvedTrip = hasUnresolvedTrip,
                    permissionsGranted = permissionsGranted
                ),
                onConnectToggle = {
                    // One control, two meanings: anything that is up or on its
                    // way up is torn down, everything else starts a scan.
                    if (state.connection.isUsable || state.connection.isBusy) viewModel.disconnect()
                    else viewModel.connect()
                },
                onRequestPermissions = requestPermissions,
                onSync = { viewModel.syncWeather() },
                onRing = { viewModel.ringDevice() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenWay = { route -> navController.navigate(route) }
            )
        }

        // No screen of its own yet. Registered so the route resolves rather
        // than throwing; the board's own link row points at DEVICE_PAIRED,
        // which is the screen that actually manages the link.
        composable(Routes.BOARD_LINK) { ComingSoonPlate() }

        // ============================================
        // Circle
        // ============================================

        composable(Routes.CIRCLE) {
            val state = liveState.value
            val location = liveLocation.value
            val isSyncing = liveSyncing.value
            val lastFix = state.lastKnownDeviceLocation ?: location.takeIf { it.isValid }
            val openCheckIn = state.checkIns.firstOrNull { it.isOpen }
            val journey = state.journey?.takeIf { it.state == JourneyState.ACTIVE }

            CircleScreen(
                state = CircleUiState(
                    role = state.role,
                    // The headline forms, not the raw ones. See
                    wearerName = state.wearerName,
                    guardianName = state.guardianName,
                    linkState = linkLamp(state.connection),
                    linkLabel = linkLabel(state.connection),
                    subline = circleSubline(state),
                    recentMessages = state.messages.takeLast(3).map { message ->
                        CircleMessagePreview(
                            id = message.id,
                            text = message.text,
                            fromGuardian = message.fromGuardian,
                            timeLabel = clockLabel(message.timestamp),
                            channel = message.channel
                        )
                    },
                    // There is no per-message read flag anywhere in the model,
                    // so "unread" is read as "sent to this phone and not yet
                    // answered". For a guardian nothing is ever owed a reply,
                    // so the count is honestly zero rather than invented.
                    unreadCount = if (state.role == UserRole.COMPANION) {
                        state.messages.count { it.fromGuardian && !it.replied }
                    } else {
                        0
                    },
                    quickMessages = quickPhrases(state.role),
                    outboundChannel = outboundChannel(state),
                    placeLabel = lastFix?.locationName?.takeIf { it.isNotBlank() }
                        ?: lastFix?.locality?.takeIf { it.isNotBlank() },
                    lastSeenLabel = lastFix?.capturedAt?.takeIf { it > 0L }?.let { "Location taken ${agoLabel(it)}" },
                    lat = lastFix?.lat,
                    lon = lastFix?.lon,
                    locationState = when {
                        lastFix == null -> LampState.UNKNOWN
                        System.currentTimeMillis() - lastFix.capturedAt < STALE_FIX_MS -> LampState.LIVE
                        else -> LampState.ATTENTION
                    },
                    isRefreshingLocation = isSyncing,
                    zones = zonesWay(state),
                    journey = CircleWay(
                        state = if (journey == null) LampState.OFF else LampState.LIVE,
                        stateLabel = if (journey == null) "None" else "Running",
                        detail = journey?.let {
                            "${it.label.ifBlank { "No destination" }} · due ${clockLabel(it.etaAt)}"
                        } ?: "No journey running"
                    ),
                    checkIn = CircleWay(
                        state = if (openCheckIn == null) LampState.OFF else LampState.ATTENTION,
                        stateLabel = if (openCheckIn == null) "None open" else "Waiting",
                        detail = openCheckIn?.let { "Asked at ${clockLabel(it.sentAt)}" }
                            ?: state.checkIns.lastOrNull { it.answeredAt != null }
                                ?.let { "Last answered at ${clockLabel(it.answeredAt!!)}" }
                    ),
                    sms = CircleWay(
                        state = if (state.devicePhoneNumber.isBlank()) LampState.OFF else LampState.LIVE,
                        stateLabel = if (state.devicePhoneNumber.isBlank()) "Not set up" else "Ready",
                        detail = when {
                            state.devicePhoneNumber.isBlank() -> "The device has no number stored"
                            state.smsAllowlist.isEmpty() -> "No allowlist, every sender accepted"
                            else -> "${state.smsAllowlist.size} numbers allowed"
                        }
                    )
                ),
                onOpenThread = { navController.navigate(Routes.CIRCLE_THREAD) },
                onSendQuickMessage = { text -> sendAsRole(viewModel, state.role, text) },
                onRefreshLocation = { viewModel.syncWeather() },
                onOpenZones = { navController.navigate(Routes.CIRCLE_ZONES) },
                onOpenJourney = { navController.navigate(Routes.CIRCLE_JOURNEY) },
                onOpenCheckIn = { navController.navigate(Routes.CIRCLE_CHECKIN) },
                onOpenSim = { navController.navigate(Routes.CIRCLE_SIM) }
            )
        }

        composable(Routes.CIRCLE_THREAD) {
            val state = liveState.value
            var draft by rememberSaveable { mutableStateOf("") }

            MessagesScreen(
                state = MessagesUiState(
                    role = state.role,
                    wearerName = state.wearerName,
                    guardianName = state.guardianName,
                    messages = state.messages
                        .sortedBy { it.timestamp }
                        .map { message ->
                            ThreadMessage(
                                id = message.id,
                                text = message.text,
                                fromGuardian = message.fromGuardian,
                                timeLabel = clockLabel(message.timestamp),
                                channel = message.channel,
                                // Deliberately not `message.replyText`.
                                // `MessagingRepository.ingestReply` both stamps
                                // the reply onto the guardian message it answers
                                // *and* appends it as a message of its own, so
                                // nesting it here would draw the same words
                                // twice — once under the question, once in
                                // sequence.
                                replyText = null
                            )
                        },
                    quickReplies = quickPhrases(state.role),
                    draft = draft,
                    outboundChannel = outboundChannel(state),
                    deviceInRange = state.connection.isUsable,
                    smsConfigured = state.devicePhoneNumber.isNotBlank()
                ),
                onDraftChange = { draft = it },
                onSend = {
                    // Directionality is the historical bug in this codebase:
                    // a companion's reply written to the guardian channel makes
                    // the firmware buzz the wearer with their own message.
                    sendAsRole(viewModel, state.role, draft)
                    draft = ""
                },
                onSendQuick = { text -> sendAsRole(viewModel, state.role, text) },
                onOpenSim = { navController.navigate(Routes.CIRCLE_SIM) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CIRCLE_ZONES) {
            val state = liveState.value
            ZonesScreen(
                state = ZonesUiState(
                    role = state.role,
                    wearerName = state.wearerName,
                    zones = state.zones.map { zone ->
                        // Only one transition is remembered at a time, so a
                        // zone that is not the one it names has no presence to
                        // report — "unknown" rather than a guess at "inside".
                        val transition = state.lastZoneTransition?.takeIf { it.zone.id == zone.id }
                        ZoneRow(
                            id = zone.id,
                            name = zone.name,
                            detail = zoneDetail(zone),
                            presence = when {
                                transition == null -> ZonePresence.UNKNOWN
                                transition.isInside -> ZonePresence.INSIDE
                                else -> ZonePresence.OUTSIDE
                            },
                            lastChangeLabel = transition?.let { agoLabel(it.at) }
                        )
                    },
                    // Geofences only fire in the background with the
                    // always-allow grant, which is a separate permission from
                    // the core set and cannot be requested from a dialog.
                    backgroundLocationGranted = hasBackgroundLocation(context)
                ),
                onAddZone = {
                    seedZoneDraft(null)
                    navController.navigate(Routes.zoneEdit(null))
                },
                onEditZone = { id ->
                    seedZoneDraft(state.zones.firstOrNull { it.id == id })
                    navController.navigate(Routes.zoneEdit(id))
                },
                onDeleteZone = { id -> viewModel.removeZone(id) },
                onRequestBackgroundLocation = { openAppSettings(context) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.CIRCLE_ZONE_EDIT}?${Routes.Args.ZONE_ID}={${Routes.Args.ZONE_ID}}",
            arguments = listOf(
                navArgument(Routes.Args.ZONE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            val state = liveState.value

            // Declared with its type so the lambda's trailing `popBackStack()`
            // (which returns Boolean) coerces to the `() -> Unit` the screen
            // asks for. Null means "this zone does not exist yet", which is
            // how the screen decides whether to show a delete at all.
            val deleteZone: (() -> Unit)? = zoneDraftId?.let { id ->
                {
                    viewModel.removeZone(id)
                    navController.popBackStack()
                }
            }

            ZoneEditorScreen(
                state = ZoneEditorUiState(
                    // Read from the draft, not from the argument:
                    // `Routes.zoneEdit(null)` emits `?zoneId=`, which arrives
                    // as an empty string rather than as an absent argument, so
                    // the argument alone cannot tell new from existing.
                    isNew = zoneDraftId == null,
                    name = zoneDraftName,
                    radiusMeters = zoneDraftRadius,
                    lat = zoneDraftLat,
                    lon = zoneDraftLon,
                    alertOnExit = zoneDraftExit,
                    alertOnEnter = zoneDraftEnter,
                    role = state.role,
                    wearerName = state.wearerName
                ),
                onNameChange = { zoneDraftName = it },
                onRadiusChange = { zoneDraftRadius = it },
                onAlertOnExitChange = { zoneDraftExit = it },
                onAlertOnEnterChange = { zoneDraftEnter = it },
                onPickOnMap = { navController.navigate(Routes.CIRCLE_ZONE_PICK) },
                onSave = {
                    val lat = zoneDraftLat
                    val lon = zoneDraftLon
                    // The screen disables the control when this is not true;
                    // re-checking here means a stale click cannot write a zone
                    // with no centre, which the geofence client rejects.
                    if (lat != null && lon != null && zoneDraftName.isNotBlank()) {
                        val existingId = zoneDraftId
                        if (existingId == null) {
                            viewModel.addZone(
                                GeofenceZone(
                                    name = zoneDraftName.trim(),
                                    lat = lat,
                                    lon = lon,
                                    radiusMeters = zoneDraftRadius,
                                    alertOnExit = zoneDraftExit,
                                    alertOnEnter = zoneDraftEnter
                                )
                            )
                        } else {
                            viewModel.updateZone(
                                GeofenceZone(
                                    id = existingId,
                                    name = zoneDraftName.trim(),
                                    lat = lat,
                                    lon = lon,
                                    radiusMeters = zoneDraftRadius,
                                    alertOnExit = zoneDraftExit,
                                    alertOnEnter = zoneDraftEnter
                                )
                            )
                        }
                        navController.popBackStack()
                    }
                },
                onDelete = deleteZone,
                onBack = { navController.popBackStack() }
            )
        }

        // A full-screen overlay above the editor rather than a sibling of it —
        // shared axis Z, and registered without arguments because the picked
        // point is committed straight into the shared draft above.
        composable(Routes.CIRCLE_ZONE_PICK) {
            val location = liveLocation.value
            val isSyncing = liveSyncing.value
            val parsedLat = pickLatField.toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
            val parsedLon = pickLonField.toDoubleOrNull()?.takeIf { it in -180.0..180.0 }

            // `syncWeather` is the only exposed path to a location fix, so the
            // button starts one and this waits for the result rather than
            // asking the fused provider a second time from the UI layer.
            LaunchedEffect(pickLocating, location) {
                if (pickLocating && location.isValid) {
                    pickLatField = location.lat.toString()
                    pickLonField = location.lon.toString()
                    pickLocating = false
                }
            }

            ZonePickerScreen(
                state = ZonePickerUiState(
                    lat = parsedLat,
                    lon = parsedLon,
                    radiusMeters = zoneDraftRadius,
                    latField = pickLatField,
                    lonField = pickLonField,
                    isLocating = pickLocating || isSyncing,
                    locationError = pickError,
                    canConfirm = parsedLat != null && parsedLon != null
                ),
                onLatFieldChange = { pickLatField = it },
                onLonFieldChange = { pickLonField = it },
                onUseCurrentLocation = {
                    pickError = null
                    if (hasFineLocation(context)) {
                        pickLocating = true
                        viewModel.syncWeather()
                    } else {
                        pickError = "Location permission has not been granted. Type the coordinates instead."
                    }
                },
                onPointPicked = { lat, lon ->
                    pickLatField = lat.toString()
                    pickLonField = lon.toString()
                },
                onConfirm = {
                    zoneDraftLat = parsedLat
                    zoneDraftLon = parsedLon
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Routes.CIRCLE_CHECKIN) {
            val state = liveState.value
            var deadlineMinutes by rememberSaveable { mutableIntStateOf(5) }
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

            val open = state.checkIns.firstOrNull { it.isOpen }
            // Only ticks while something is actually counting down. The key is
            // the open request's id, so the loop stops the moment it closes.
            LaunchedEffect(open?.id) {
                while (open != null) {
                    delay(1_000L)
                    now = System.currentTimeMillis()
                }
            }

            val answered = state.checkIns.lastOrNull { it.answeredAt != null }

            CheckInScreen(
                state = CheckInUiState(
                    role = state.role,
                    wearerName = state.wearerName,
                    guardianName = state.guardianName,
                    deadlineMinutes = deadlineMinutes,
                    hasOpenRequest = open != null,
                    sentAtLabel = open?.let { clockLabel(it.sentAt) },
                    dueAtLabel = open?.let { clockLabel(it.deadlineAt) },
                    secondsRemaining = open?.let { ((it.deadlineAt - now) / 1000L).toInt().coerceAtLeast(0) },
                    totalSeconds = open?.let { ((it.deadlineAt - it.sentAt) / 1000L).toInt().coerceAtLeast(1) },
                    escalated = open?.escalated == true,
                    answeredLabel = answered?.answeredAt?.let { "Answered at ${clockLabel(it)}" },
                    history = state.checkIns
                        .filterNot { it.isOpen }
                        .sortedByDescending { it.sentAt }
                        .map { it.toHistoryRow() },
                    contactsSummary = state.contactsLabel
                ),
                onDeadlineSelected = { deadlineMinutes = it },
                onSendCheckIn = { viewModel.requestCheckIn(deadlineMinutes) },
                // Withdrawing a question has no call of its own. Closing the
                // request through `answerCheckIn` would file it in the history
                // as answered, and a false entry in a log a guardian may read
                // months later is worse than a control that does nothing.
                onCancelRequest = {},
                // Answers whichever request is open rather than the one this
                // composition happened to see, so a request that arrived while
                // the screen was already up is still the one that closes.
                onRespondOk = { viewModel.answerOpenCheckIn() },
                onOpenContacts = { navController.navigate(Routes.SAFETY_CONTACTS) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CIRCLE_JOURNEY) {
            val state = liveState.value
            var destination by rememberSaveable { mutableStateOf("") }
            var etaMinutes by rememberSaveable { mutableIntStateOf(25) }
            var isCustomEta by rememberSaveable { mutableStateOf(false) }
            var customEtaText by rememberSaveable { mutableStateOf("") }
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

            // Mirrors `JourneyUiState.effectiveEtaMinutes`, computed from the
            // same three fields the screen is handed. 600 minutes is that
            // property's own ceiling; past it the entry is not a duration this
            // feature can act on.
            val effectiveEta: Int? =
                if (isCustomEta) customEtaText.toIntOrNull()?.takeIf { it in 1..600 } else etaMinutes

            val journey = state.journey?.takeIf {
                it.state == JourneyState.ACTIVE || it.state == JourneyState.ESCALATED
            }
            LaunchedEffect(journey?.id) {
                while (journey != null) {
                    delay(1_000L)
                    now = System.currentTimeMillis()
                }
            }

            JourneyScreen(
                state = JourneyUiState(
                    role = state.role,
                    wearerName = state.wearerName,
                    guardianName = state.guardianName,
                    destinationLabel = journey?.label ?: destination,
                    etaMinutes = etaMinutes,
                    isCustomEta = isCustomEta,
                    customEtaText = customEtaText,
                    isActive = journey != null,
                    minutesRemaining = journey?.let {
                        ((it.etaAt - now) / 60_000L).toInt().coerceAtLeast(0)
                    },
                    progress = journey?.let {
                        val total = (it.etaAt - it.startedAt).toFloat()
                        if (total <= 0f) 1f else ((now - it.startedAt) / total).coerceIn(0f, 1f)
                    },
                    etaClockLabel = journey?.let { clockLabel(it.etaAt) },
                    startedAtLabel = journey?.let { clockLabel(it.startedAt) },
                    isOverdue = journey != null && now > journey.etaAt,
                    escalated = journey?.state == JourneyState.ESCALATED,
                    contactsSummary = state.contactsLabel
                ),
                onDestinationChange = { destination = it },
                onEtaSelected = {
                    etaMinutes = it
                    isCustomEta = false
                },
                onCustomEtaSelected = { isCustomEta = true },
                onCustomEtaChange = { customEtaText = it },
                onStart = {
                    // Re-checked rather than trusted. The screen disables the
                    // control on the same condition, and a journey started with
                    // no destination escalates to contacts who are then told
                    // nothing useful about where to look.
                    val minutes = effectiveEta
                    if (destination.isNotBlank() && minutes != null) {
                        viewModel.startJourney(destination.trim(), minutes)
                    }
                },
                onArrived = { viewModel.arriveJourney() },
                onCancelJourney = { viewModel.cancelJourney() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CIRCLE_SIM) {
            val state = liveState.value
            var simNumber by rememberSaveable(state.devicePhoneNumber) {
                mutableStateOf(state.devicePhoneNumber)
            }
            var newNumber by rememberSaveable { mutableStateOf("") }

            // Built first so the handlers below can read the screen's own
            // limits and guards rather than restating them. A second copy of
            // "sixteen characters" is a second copy to get wrong.
            val simState = SimUiState(
                role = state.role,
                wearerName = state.wearerName,
                simNumber = simNumber,
                savedSimNumber = state.devicePhoneNumber,
                allowlist = state.smsAllowlist,
                newNumber = newNumber,
                isDeviceLinked = state.connection.isUsable,
                syncLabel = syncSummary(state.syncStatus)
            )

            SimScreen(
                state = simState,
                // Clamped as it is typed rather than on save. The firmware
                // truncates a longer number on the wire, and a number silently
                // shortened between here and the device is one that never
                // rings.
                onSimNumberChange = { simNumber = it.take(simState.maxNumberLength) },
                onNewNumberChange = { newNumber = it.take(simState.maxNumberLength) },
                onSaveSimNumber = { viewModel.setDevicePhoneNumber(simNumber.trim()) },
                onAddNumber = {
                    // `canAddNumber` also covers the firmware's eight-entry
                    // ceiling and duplicates, both of which the device would
                    // drop without saying so.
                    if (simState.canAddNumber) {
                        viewModel.setSmsAllowlist(state.smsAllowlist + newNumber.trim())
                        newNumber = ""
                    }
                },
                onRemoveNumber = { number ->
                    viewModel.setSmsAllowlist(state.smsAllowlist.filterNot { it == number })
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ============================================
        // Safety
        // ============================================

        composable(Routes.SAFETY) {
            val state = liveState.value
            val lastTrip = state.tripHistory.maxByOrNull { it.timestamp }

            SafetyScreen(
                state = SafetyUiState(
                    role = state.role,
                    wearerName = state.wearerName,
                    settings = state.safetySettings,
                    medicalId = state.medicalId,
                    activeMode = state.activeMode,
                    linkLive = state.connection.isUsable,
                    settingsSynced = state.syncStatus !is SyncStatus.Failed,
                    safeZoneCount = state.zones.size,
                    insideSafeZone = state.lastZoneTransition?.isInside,
                    unresolvedTripCount = state.tripHistory.count { it.outcome == TripOutcome.PENDING },
                    lastTripLabel = lastTrip?.let { "${it.kind.label} · ${agoLabel(it.timestamp)}" },
                    silentSosEnabled = silentSosEnabled
                ),
                onOpenFallSettings = { navController.navigate(Routes.SAFETY_FALL) },
                onOpenContacts = { navController.navigate(Routes.SAFETY_CONTACTS) },
                onOpenMedicalId = { navController.navigate(Routes.SAFETY_MEDICAL) },
                onOpenEmergencyCard = { navController.navigate(Routes.SAFETY_MEDICAL_CARD) },
                onOpenServices = { navController.navigate(Routes.SAFETY_SERVICES) },
                onOpenTrips = { navController.navigate(Routes.SAFETY_TRIPS) },
                onOpenSilentSos = { navController.navigate(Routes.SAFETY_SILENT) },
                onOpenZones = { navController.navigate(Routes.CIRCLE_ZONES) },
                onAutoCallChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(autoCallEmergency = it))
                },
                onSmsFallbackChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(smsFallbackEnabled = it))
                }
            )
        }

        composable(Routes.SAFETY_FALL) {
            val state = liveState.value
            FallSettingsScreen(
                state = FallSettingsUiState(
                    settings = state.safetySettings,
                    activeMode = state.activeMode,
                    role = state.role,
                    wearerName = state.wearerName,
                    linkLive = state.connection.isUsable,
                    settingsSynced = state.syncStatus !is SyncStatus.Failed,
                    // A four-digit PIN is the only validation this screen can
                    // fail, and the field already filters to digits.
                    pinError = state.safetySettings.parentalPin
                        .takeIf { it.isNotEmpty() && it.length != 4 }
                        ?.let { "The PIN is four digits" }
                ),
                onBack = { navController.popBackStack() },
                onSensitivityChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(fallSensitivity = it))
                },
                onAutoCallChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(autoCallEmergency = it))
                },
                onCountdownChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(fallCountdownSeconds = it))
                },
                onSosVolumeChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(sosVolumeLevel = it))
                },
                onSmsFallbackChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(smsFallbackEnabled = it))
                },
                onParentalControlsChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(parentalControlsEnabled = it))
                },
                onPinChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(parentalPin = it))
                },
                onOpenContacts = { navController.navigate(Routes.SAFETY_CONTACTS) }
            )
        }

        composable(Routes.SAFETY_CONTACTS) {
            val state = liveState.value
            val contacts = state.safetySettings.emergencyContacts

            var draftOpen by rememberSaveable { mutableStateOf(false) }
            var draftIndex by rememberSaveable { mutableStateOf<Int?>(null) }
            var draftName by rememberSaveable { mutableStateOf("") }
            var draftPhone by rememberSaveable { mutableStateOf("") }
            var draftPrimary by rememberSaveable { mutableStateOf(false) }

            // Writes the whole list in one go rather than calling
            // `addContact` / `removeContact` in sequence: an edit is a remove
            // plus an add, and those are two independent coroutines whose order
            // is not guaranteed — losing the race silently drops the edit.
            val commit: (List<EmergencyContact>) -> Unit = { updated ->
                viewModel.setSafetySettings(state.safetySettings.copy(emergencyContacts = updated))
            }

            ContactsScreen(
                state = ContactsUiState(
                    contacts = contacts,
                    draft = if (draftOpen) {
                        ContactDraft(draftIndex, draftName, draftPhone, draftPrimary)
                    } else {
                        null
                    },
                    wearerName = state.wearerName,
                    autoCallEnabled = state.safetySettings.autoCallEmergency,
                    smsFallbackEnabled = state.safetySettings.smsFallbackEnabled
                ),
                onBack = { navController.popBackStack() },
                onStartAdd = {
                    draftIndex = null
                    draftName = ""
                    draftPhone = ""
                    // The first contact anyone adds is the one that gets rung.
                    draftPrimary = contacts.isEmpty()
                    draftOpen = true
                },
                onStartEdit = { index ->
                    contacts.getOrNull(index)?.let { contact ->
                        draftIndex = index
                        draftName = contact.name
                        draftPhone = contact.phone
                        draftPrimary = contact.isPrimary
                        draftOpen = true
                    }
                },
                onDraftChange = { draft ->
                    draftName = draft.name
                    draftPhone = draft.phone
                    draftPrimary = draft.isPrimary
                },
                onSaveDraft = {
                    val edited = EmergencyContact(
                        name = draftName.trim(),
                        phone = draftPhone.trim(),
                        isPrimary = draftPrimary
                    )
                    if (edited.name.isNotBlank() && edited.phone.isNotBlank()) {
                        val index = draftIndex
                        val updated = contacts.toMutableList()
                        if (index == null || index !in updated.indices) updated.add(edited)
                        else updated[index] = edited
                        commit(
                            // Exactly one primary, always. Two contacts both
                            // marked primary makes `primaryContact` arbitrary.
                            if (edited.isPrimary) {
                                updated.map { it.copy(isPrimary = it.phone == edited.phone) }
                            } else {
                                updated
                            }
                        )
                        draftOpen = false
                    }
                },
                onCancelDraft = { draftOpen = false },
                onDelete = { index ->
                    if (index in contacts.indices) {
                        commit(contacts.filterIndexed { i, _ -> i != index })
                    }
                }
            )
        }

        composable(Routes.SAFETY_MEDICAL) {
            val state = liveState.value
            // The one genuinely dirty form in the app. Held here so the screen
            // stays stateless, and saved only on the explicit action — the
            // medical ID is pushed to the wearable on write, so autosaving
            // every keystroke would put a half-typed allergy on the device.
            var draft by rememberSaveable(stateSaver = MedicalIdSaver) {
                mutableStateOf(state.medicalId)
            }

            MedicalIdScreen(
                state = MedicalIdUiState(
                    medicalId = draft,
                    wearerName = state.wearerName,
                    linkLive = state.connection.isUsable,
                    isDirty = draft != state.medicalId,
                    lastSyncedLabel = syncSummary(state.syncStatus)
                ),
                onBack = { navController.popBackStack() },
                onChange = { draft = it },
                onSave = { viewModel.setMedicalId(draft) },
                onOpenCard = { navController.navigate(Routes.SAFETY_MEDICAL_CARD) }
            )
        }

        composable(Routes.SAFETY_MEDICAL_CARD) {
            val state = liveState.value
            EmergencyCardScreen(
                state = EmergencyCardUiState(
                    medicalId = state.medicalId,
                    wearerName = state.wearerName
                ),
                onBack = { navController.popBackStack() },
                onOpenMedicalId = {
                    // Usually the card was opened *from* the editor, so going
                    // "to" it means going back to it. Only push a new copy
                    // when it is not already on the stack.
                    if (!navController.popBackStack(Routes.SAFETY_MEDICAL, inclusive = false)) {
                        navController.navigate(Routes.SAFETY_MEDICAL)
                    }
                }
            )
        }

        composable(Routes.SAFETY_SERVICES) {
            ServicesScreen(
                state = ServicesUiState(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SAFETY_TRIPS) {
            val state = liveState.value
            TripLogScreen(
                state = TripLogUiState(
                    trips = state.tripHistory.sortedByDescending { it.timestamp },
                    wearerName = state.wearerName,
                    today = LocalDate.now()
                ),
                onBack = { navController.popBackStack() },
                onOpenTrip = { id -> navController.navigate(Routes.tripDetail(id)) },
                onOpenFallSettings = { navController.navigate(Routes.SAFETY_FALL) }
            )
        }

        composable(
            route = "${Routes.SAFETY_TRIP_DETAIL}/{${Routes.Args.TRIP_ID}}",
            arguments = listOf(navArgument(Routes.Args.TRIP_ID) { type = NavType.StringType })
        ) { entry ->
            val state = liveState.value
            val tripId = entry.arguments?.getString(Routes.Args.TRIP_ID)
            val trip = state.tripHistory.firstOrNull { it.id == tripId }
                ?: state.activeAlert?.takeIf { it.id == tripId }

            if (trip == null) {
                // The history was cleared, or the id is stale. Leaving rather
                // than drawing an empty trip is the only honest answer.
                LaunchedEffect(tripId) { navController.popBackStack() }
                return@composable
            }

            TripDetailScreen(
                state = TripDetailUiState(
                    trip = trip,
                    wearerName = state.wearerName,
                    lat = state.lastKnownDeviceLocation?.lat,
                    lon = state.lastKnownDeviceLocation?.lon,
                    // Only meaningful while the trip is the live one; a
                    // snapshot from months ago is not in the model.
                    sensor = state.telemetry.takeIf {
                        it.isRealData && trip.id == state.activeAlert?.id
                    },
                    contactedName = state.safetySettings.primaryContact?.name
                        ?.takeIf { trip.wasEmergencyContacted },
                    today = LocalDate.now()
                ),
                onBack = { navController.popBackStack() },
                onResolve = { outcome ->
                    val contacted = outcome == TripOutcome.CONTACTED
                    // Two calls, because they are not the same act. Resolving
                    // the *live* alert must also clear it, or the full-frame
                    // banner in `SafeShadeApp` stays up over a trip that has
                    // already been answered. `resolveTrip` closes any other
                    // entry, which is what stops a stale PENDING trip lighting
                    // the board's lamp forever.
                    if (trip.id == state.activeAlert?.id) {
                        viewModel.resolveActiveAlert(outcome, contacted)
                    } else {
                        viewModel.resolveTrip(trip.id, outcome, contacted)
                    }
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SAFETY_SILENT) {
            val state = liveState.value
            SilentSosScreen(
                state = SilentSosUiState(
                    silentSosEnabled = silentSosEnabled,
                    stagedCallSeconds = stagedCallSeconds,
                    callerName = "Home",
                    hasContacts = state.safetySettings.emergencyContacts.isNotEmpty(),
                    // The firmware capability is not reported over the link, so
                    // this claims nothing rather than promising a silent alert
                    // the device may not support.
                    deviceSupportsSilentAlert = false
                ),
                onBack = { navController.popBackStack() },
                onSilentSosChange = { silentSosEnabled = it },
                onStageCall = { stagedCallSeconds = it },
                onCancelStagedCall = { stagedCallSeconds = null },
                onOpenContacts = { navController.navigate(Routes.SAFETY_CONTACTS) }
            )
        }

        // ============================================
        // Device
        // ============================================

        composable(Routes.DEVICE) {
            val state = liveState.value
            DeviceScreen(
                state = DeviceUiState(
                    connection = state.connection,
                    deviceName = state.deviceSettings.name,
                    wearerName = state.wearerName,
                    iconType = state.deviceSettings.iconType,
                    batteryPercent = state.telemetry.batteryLevel.takeIf { state.telemetry.isRealData },
                    signalDbm = state.rssiSmoothed.takeIf { state.connection.isUsable },
                    mode = state.activeMode,
                    ledPattern = ledPattern,
                    lastSeenLabel = state.lastKnownDeviceLocation?.capturedAt
                        ?.takeIf { it > 0L }
                        ?.let { agoLabel(it) },
                    syncSummary = syncSummary(state.syncStatus),
                    activeReminderCount = listOf(medicationEnabled, checkInIntervalMinutes > 0).count { it },
                    pairedDeviceCount = state.pairedDevices.size,
                    hasTelemetry = state.telemetryHistory.isNotEmpty(),
                    isRinging = state.isRinging
                ),
                onOpenWay = { route -> navController.navigate(route) }
            )
        }

        composable(Routes.DEVICE_MODE) {
            val state = liveState.value
            var inFlight by remember { mutableStateOf<PersonaMode?>(null) }
            var ack by remember { mutableStateOf(AckState.IDLE) }
            var confirming by remember { mutableStateOf<PersonaMode?>(null) }

            // The device never sends a mode ack the app can see here, but the
            // repository writes the mode back into the profile once the switch
            // has actually been made, so agreement is the confirmation.
            LaunchedEffect(state.activeMode) {
                if (inFlight != null && state.activeMode == inFlight) {
                    ack = AckState.CONFIRMED
                    inFlight = null
                }
            }

            ModePickerScreen(
                state = ModePickerUiState(
                    connection = state.connection,
                    activeMode = state.activeMode,
                    inFlightMode = inFlight,
                    ack = ack,
                    confirmingMode = confirming
                ),
                onSelectMode = { mode ->
                    // A guardian-locked mode hides mode switching and the whole
                    // safety menu on the wearable, so it is never applied on a
                    // single tap.
                    if (mode.isGuardianLocked) {
                        confirming = mode
                    } else {
                        inFlight = mode
                        ack = AckState.PENDING
                        viewModel.setActiveMode(mode)
                    }
                },
                onConfirmMode = { mode ->
                    confirming = null
                    inFlight = mode
                    ack = AckState.PENDING
                    viewModel.setActiveMode(mode)
                },
                onCancelConfirm = { confirming = null }
            )
        }

        composable(Routes.DEVICE_SETTINGS) {
            val state = liveState.value
            // Keyed on the persisted value so an external change resets the
            // slider, while a drag between commits stays local.
            var sosVolume by remember(state.safetySettings.sosVolumeLevel) {
                mutableFloatStateOf(state.safetySettings.sosVolumeLevel)
            }

            DeviceSettingsScreen(
                state = DeviceSettingsUiState(
                    connection = state.connection,
                    deviceName = state.deviceSettings.name,
                    fallSensitivity = state.safetySettings.fallSensitivity,
                    sosVolume = sosVolume,
                    autoCallEnabled = state.safetySettings.autoCallEmergency,
                    parentalControlsEnabled = state.safetySettings.parentalControlsEnabled,
                    smsFallbackEnabled = state.safetySettings.smsFallbackEnabled,
                    quietHoursEnabled = quietHoursEnabled,
                    quietStartHour = quietStartHour,
                    quietEndHour = quietEndHour,
                    medicationEnabled = medicationEnabled,
                    medicationHour = medicationHour,
                    medicationMinute = medicationMinute,
                    checkInIntervalMinutes = checkInIntervalMinutes
                ),
                onFallSensitivityChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(fallSensitivity = it))
                },
                onSosVolumeChange = { sosVolume = it },
                onSosVolumeCommit = {
                    viewModel.setSafetySettings(state.safetySettings.copy(sosVolumeLevel = sosVolume))
                },
                onAutoCallChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(autoCallEmergency = it))
                },
                onParentalControlsChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(parentalControlsEnabled = it))
                },
                onSmsFallbackChange = {
                    viewModel.setSafetySettings(state.safetySettings.copy(smsFallbackEnabled = it))
                },
                onQuietHoursChange = { enabled ->
                    quietHoursEnabled = enabled
                    // Null on both ends is how the firmware is told to stop
                    // observing quiet hours at all, rather than to observe a
                    // window of zero length.
                    viewModel.setQuietHours(
                        if (enabled) quietStartHour else null,
                        if (enabled) quietEndHour else null
                    )
                },
                onMedicationChange = { enabled ->
                    medicationEnabled = enabled
                    viewModel.setMedicationTime(
                        if (enabled) medicationHour else null,
                        if (enabled) medicationMinute else null
                    )
                },
                onCheckInIntervalChange = { minutes ->
                    checkInIntervalMinutes = minutes
                    // The screens speak minutes; the wire takes seconds.
                    viewModel.setCheckInInterval(minutes * 60)
                },
                // These three ask the caller to raise a picker. There is no
                // shared dialog layer in this build and adding one here would
                // put screen chrome in the navigation graph, so they are left
                // for whoever owns the device screens.
                onEditQuietHours = {},
                onEditMedicationTime = {},
                onEditDeviceName = {},
                onOpenDeviceOnly = { navController.navigate(Routes.DEVICE_ONBOARD) }
            )
        }

        composable(Routes.DEVICE_ONBOARD) {
            val state = liveState.value
            DeviceOnlyScreen(
                state = DeviceOnlyUiState(deviceName = state.deviceSettings.name)
            )
        }

        composable(Routes.DEVICE_LOCATE) {
            val state = liveState.value
            var ringConfirmArmed by rememberSaveable { mutableStateOf(false) }
            val lastFix = state.lastKnownDeviceLocation

            LocateScreen(
                state = LocateUiState(
                    connection = state.connection,
                    deviceName = state.deviceSettings.name,
                    isRinging = state.isRinging,
                    ringConfirmArmed = ringConfirmArmed,
                    rssiDbm = state.rssiSmoothed.takeIf { state.connection.isUsable },
                    lastKnownPlace = lastFix?.locationName?.takeIf { it.isNotBlank() }
                        ?: lastFix?.locality?.takeIf { it.isNotBlank() },
                    lastKnownCoordinates = lastFix?.let { "%.5f, %.5f".format(it.lat, it.lon) },
                    lastKnownAgeLabel = lastFix?.capturedAt?.takeIf { it > 0L }?.let { agoLabel(it) }
                ),
                onArmRing = { ringConfirmArmed = true },
                onCancelRing = { ringConfirmArmed = false },
                onRing = {
                    ringConfirmArmed = false
                    viewModel.ringDevice()
                },
                onRingStopAcknowledged = { viewModel.acknowledgeRingStopped() },
                onOpenLastKnownLocation = {
                    lastFix?.let { fix -> openOnMap(context, fix.lat, fix.lon, state.deviceSettings.name) }
                }
            )
        }

        composable(Routes.DEVICE_TELEMETRY) {
            val state = liveState.value
            TelemetryScreen(
                state = TelemetryUiState(
                    connection = state.connection,
                    sensors = state.telemetry,
                    batteryHistory = state.telemetryHistory.map { it.batteryPercent },
                    linkHistory = state.telemetryHistory.map { it.rssiDbm },
                    historySummary = state.telemetryHistory.firstOrNull()
                        ?.let { "${state.telemetryHistory.size} samples since ${clockLabel(it.at)}" }
                ),
                // A documented no-op on the view model: the repository
                // already polls RSSI once a second while the link is Ready, so
                // an on-demand read would queue a GATT operation for a value
                // at most a second old. Called anyway, so the affordance goes
                // through the one place that explains itself.
                onReadSignal = { viewModel.readRssi() }
            )
        }

        composable(Routes.DEVICE_LIGHTS) {
            val state = liveState.value
            LightsScreen(
                state = LightsUiState(
                    connection = state.connection,
                    pattern = ledPattern,
                    // The LED pattern is not part of `AppState`, so there is
                    // nothing to compare a write against. Rather than claim a
                    // confirmation the app cannot see, this reports no write in
                    // flight and no acknowledgement at all.
                    inFlightPattern = null,
                    ack = AckState.IDLE
                ),
                onSelectPattern = { pattern ->
                    ledPattern = pattern
                    viewModel.setLedPattern(pattern)
                },
                onOpenDeviceOnly = { navController.navigate(Routes.DEVICE_ONBOARD) }
            )
        }

        composable(Routes.DEVICE_PAIRED) {
            val state = liveState.value
            val permissionsGranted = livePermissions.value
            var confirmingForget by rememberSaveable { mutableStateOf<String?>(null) }

            PairedDevicesScreen(
                state = PairedDevicesUiState(
                    connection = state.connection,
                    devices = state.pairedDevices,
                    // Nothing in the model records *which* address the current
                    // link is to, only that there is one.
                    connectedAddress = null,
                    isScanning = state.connection is ConnectionState.Scanning,
                    permissionsGranted = permissionsGranted,
                    confirmingForget = confirmingForget,
                    lastConnectedLabels = state.pairedDevices.associate {
                        it.address to agoLabel(it.lastConnected)
                    }
                ),
                onPairNew = { viewModel.connect() },
                onRequestPermissions = requestPermissions,
                // The link connects to whatever advertises the service UUID;
                // there is no connect-by-address on the view model, so this
                // starts the same scan.
                onConnect = { viewModel.connect() },
                onDisconnect = { viewModel.disconnect() },
                onForgetRequested = { address -> confirmingForget = address },
                onConfirmForget = { address ->
                    viewModel.removePairedDevice(address)
                    confirmingForget = null
                },
                onCancelForget = { confirmingForget = null }
            )
        }

        composable(Routes.DEVICE_REMINDERS) {
            val state = liveState.value
            RemindersScreen(
                state = RemindersUiState(
                    connection = state.connection,
                    mode = state.activeMode,
                    medication = Reminder(
                        kind = ReminderKind.MEDICATION,
                        hour = medicationHour,
                        minute = medicationMinute,
                        enabled = medicationEnabled,
                        label = "Medication"
                    ),
                    checkIn = Reminder(
                        kind = ReminderKind.CHECK_IN,
                        intervalMinutes = checkInIntervalMinutes,
                        enabled = checkInIntervalMinutes > 0,
                        label = "Check-in"
                    ),
                    exactAlarmsAllowed = canScheduleExactAlarms(context),
                    exactAlarmsGrantable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ),
                onMedicationEnabledChange = { enabled ->
                    medicationEnabled = enabled
                    viewModel.setMedicationTime(
                        if (enabled) medicationHour else null,
                        if (enabled) medicationMinute else null
                    )
                },
                // No picker layer, so the time itself cannot be changed here.
                onEditMedicationTime = {},
                onCheckInEnabledChange = { enabled ->
                    // An hour is the floor a re-enabled check-in falls back to,
                    // so switching it on never schedules a reminder every zero
                    // minutes.
                    val minutes = if (enabled) checkInIntervalMinutes.coerceAtLeast(60) else 0
                    checkInIntervalMinutes = minutes
                    viewModel.setCheckInInterval(minutes * 60)
                },
                onCheckInIntervalChange = { minutes ->
                    checkInIntervalMinutes = minutes
                    viewModel.setCheckInInterval(minutes * 60)
                },
                onGrantExactAlarms = { openExactAlarmSettings(context) }
            )
        }

        // ============================================
        // Settings — reached from the board's top bar
        // ============================================

        composable(Routes.SETTINGS) {
            val state = liveState.value
            SettingsScreen(
                state = SettingsUiState(
                    role = state.role,
                    darkMode = state.darkMode,
                    reliabilityIssueCount = reliabilityStatuses(context)
                        .count { it.value == CheckStatus.FAILING },
                    versionName = BuildConfig.VERSION_NAME
                ),
                onOpenWay = { route -> navController.navigate(route) }
            )
        }

        composable(Routes.SETTINGS_APPEARANCE) {
            val state = liveState.value
            AppearanceScreen(
                state = AppearanceUiState(selected = state.darkMode),
                onSelect = { viewModel.setDarkMode(it) }
            )
        }

        composable(Routes.SETTINGS_ROLE) {
            val state = liveState.value
            var confirming by remember { mutableStateOf<UserRole?>(null) }

            RoleScreen(
                state = RoleUiState(
                    role = state.role,
                    wearerName = state.wearerName,
                    confirming = confirming
                ),
                // Always confirmed: switching role rewords every screen in the
                // app, and doing that on a stray tap is disorienting in a way
                // the user cannot undo by tapping again.
                onSelectRole = { role -> confirming = role.takeIf { it != state.role } },
                onConfirmRole = { role ->
                    confirming = null
                    viewModel.setRole(role)
                },
                onCancelConfirm = { confirming = null }
            )
        }

        composable(Routes.SETTINGS_RELIABILITY) {
            ReliabilityScreen(
                state = ReliabilityUiState(
                    statuses = reliabilityStatuses(context),
                    manufacturer = Build.MANUFACTURER,
                    showOemAutostart = Build.MANUFACTURER.lowercase() in OEM_AUTOSTART_VENDORS
                ),
                // Firing a real test notification needs the alert layer, which
                // the view model does not surface.
                onSendTestAlert = {}
            )
        }

        composable(Routes.SETTINGS_ABOUT) {
            AboutScreen(
                state = AboutUiState(
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE.toString(),
                    buildLabel = if (BuildConfig.DEBUG) "Debug" else "Release",
                    firmwareTarget = FIRMWARE_TARGET
                )
            )
        }

        composable(Routes.SETTINGS_DEVELOPER) {
            DeveloperScreen(
                state = DeveloperUiState(
                    // The scenario list lives in the debug source set and has
                    // no release-safe accessor on the view model, so this is
                    // empty in every build rather than wrong in one.
                    scenarios = emptyList(),
                    usingFakeLink = false,
                    buildFingerprint = Build.FINGERPRINT
                ),
                onSelectScenario = {},
                onUseRealLink = {},
                // The kit gallery has no route constant, and this file may not
                // add one.
                onOpenKitGallery = {}
            )
        }
    }
}

// ============================================
// Motion
// ============================================

private enum class Motion { FADE_THROUGH, SHARED_X, SHARED_Z }

/** Routes that sit *above* the hierarchy rather than inside it. */
private val OVERLAY_ROUTES = listOf(
    Routes.CIRCLE_ZONE_PICK,
    Routes.SAFETY_MEDICAL_CARD,
    Routes.SAFETY_TRIP_DETAIL
)

private val BOTTOM_ROUTES = BottomDestination.entries.map { it.route }.toSet()

/**
 * Which Material pattern this move deserves.
 *
 * Matched with `startsWith` rather than equality because a registered route
 * carries its argument placeholders — `safety/trips/detail/{tripId}` — while
 * the constants do not. The bottom destinations take no arguments, so those
 * stay an exact match and cannot swallow their own children.
 */
private fun motionFor(from: String?, to: String?): Motion = when {
    from.isOverlay() || to.isOverlay() -> Motion.SHARED_Z
    from.isBottom() && to.isBottom() -> Motion.FADE_THROUGH
    else -> Motion.SHARED_X
}

private fun String?.isOverlay(): Boolean =
    this != null && OVERLAY_ROUTES.any { startsWith(it) }

private fun String?.isBottom(): Boolean = this != null && this in BOTTOM_ROUTES

private fun androidx.navigation.NavBackStackEntry.route(): String? = destination.route

// ============================================
// Derivations
// ============================================

private const val FIRMWARE_TARGET = "SafeShade ESP32 firmware"
private const val STALE_FIX_MS = 15 * 60 * 1000L

private val OEM_AUTOSTART_VENDORS =
    setOf("xiaomi", "redmi", "poco", "oppo", "realme", "vivo", "oneplus", "huawei", "honor")

private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val D_MMM: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

private fun clockLabel(at: Long): String =
    Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(HH_MM)

/** "4 minutes ago". Read at composition time, so it ages until the next frame. */
private fun agoLabel(at: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - at) / 60_000L).toInt()
    return when {
        minutes < 1 -> "just now"
        minutes == 1 -> "1 minute ago"
        minutes < 60 -> "$minutes minutes ago"
        minutes < 120 -> "1 hour ago"
        minutes < 1440 -> "${minutes / 60} hours ago"
        else -> "on ${Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(D_MMM)}"
    }
}

/** "Today 18:02" / "Yesterday 09:15" / "3 Mar 14:00". */
private fun dayTimeLabel(at: Long): String {
    val date = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val prefix = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(D_MMM)
    }
    return "$prefix ${clockLabel(at)}"
}

private fun linkLamp(connection: ConnectionState): LampState = when (connection) {
    is ConnectionState.Ready -> LampState.LIVE
    is ConnectionState.Connected, is ConnectionState.Connecting,
    is ConnectionState.Scanning, is ConnectionState.Found -> LampState.ATTENTION
    is ConnectionState.BluetoothUnavailable, is ConnectionState.ScanFailed -> LampState.TRIP
    is ConnectionState.Disconnected -> LampState.OFF
}

private fun linkLabel(connection: ConnectionState): String = when (connection) {
    is ConnectionState.Ready -> "Reachable"
    is ConnectionState.Connected -> "Settling"
    is ConnectionState.Connecting -> "Connecting"
    is ConnectionState.Scanning -> "Looking"
    is ConnectionState.Found -> "Found ${connection.name}"
    is ConnectionState.BluetoothUnavailable -> "Bluetooth is off"
    is ConnectionState.ScanFailed -> "Scan failed"
    is ConnectionState.Disconnected -> "Not connected"
}

private fun boardHeadline(
    connection: ConnectionState,
    role: UserRole,
    wearerName: String
): String {
    val who = wearerName.ifBlank { if (role == UserRole.GUARDIAN) "The wearer" else "You" }
    return when (connection) {
        is ConnectionState.Ready -> if (role == UserRole.GUARDIAN) "$who is covered" else "You are covered"
        is ConnectionState.Connected -> "Almost ready"
        is ConnectionState.Connecting, is ConnectionState.Found -> "Connecting"
        is ConnectionState.Scanning -> "Looking for the device"
        is ConnectionState.BluetoothUnavailable -> "Bluetooth is off"
        is ConnectionState.ScanFailed -> "The scan did not start"
        is ConnectionState.Disconnected -> "The device is not connected"
    }
}

private fun boardSubline(state: AppState.Ready, permissionsGranted: Boolean): String = when {
    !permissionsGranted -> "Bluetooth and location are needed before the device can be found"
    state.activeAlert != null -> "There is a trip waiting for an answer"
    state.connection.isUsable -> "${state.activeMode.label} mode · ${state.safetySettings.fallSensitivity.label} fall sensitivity"
    state.pairedDevices.isEmpty() -> "No device has been paired yet"
    else -> "Last paired with ${state.pairedDevices.first().name}"
}

private fun circleSubline(state: AppState.Ready): String = when {
    state.connection.isUsable -> "Messages go straight to the device"
    state.devicePhoneNumber.isNotBlank() -> "Bluetooth is out of range, so messages go by SMS"
    else -> "Out of range, and no SIM number is stored for the device"
}

private fun outboundChannel(state: AppState.Ready): MessageChannel =
    if (state.connection.isUsable) MessageChannel.BLE else MessageChannel.SMS

/**
 * The board's shortcut rows.
 *
 * These are the circuits worth a glance from the front panel: the four bottom
 * destinations are already one tap away, so repeating them here would waste the
 * only part of the app a user reads at arm's length.
 */
private fun boardWays(state: AppState.Ready): List<BoardWay> {
    val zones = zonesWay(state)
    val checkInOpen = state.checkIns.any { it.isOpen }
    val journeyRunning = state.journey?.state == JourneyState.ACTIVE
    val contacts = state.safetySettings.emergencyContacts

    return listOf(
        BoardWay(
            key = "zones",
            name = "Safe zones",
            state = zones.state,
            stateLabel = zones.stateLabel,
            detail = zones.detail,
            icon = Icons.Outlined.Place,
            route = Routes.CIRCLE_ZONES
        ),
        BoardWay(
            key = "checkin",
            name = "Check-in",
            state = if (checkInOpen) LampState.ATTENTION else LampState.OFF,
            stateLabel = if (checkInOpen) "Waiting" else "None open",
            icon = Icons.Outlined.Schedule,
            route = Routes.CIRCLE_CHECKIN
        ),
        BoardWay(
            key = "journey",
            name = "Journey",
            state = if (journeyRunning) LampState.LIVE else LampState.OFF,
            stateLabel = if (journeyRunning) "Running" else "None",
            icon = Icons.Outlined.DirectionsWalk,
            route = Routes.CIRCLE_JOURNEY
        ),
        BoardWay(
            key = "trips",
            name = "Trip log",
            state = when {
                state.tripHistory.any { it.outcome == TripOutcome.PENDING } -> LampState.TRIP
                state.tripHistory.isEmpty() -> LampState.OFF
                else -> LampState.LIVE
            },
            stateLabel = when {
                state.tripHistory.any { it.outcome == TripOutcome.PENDING } -> "Needs an answer"
                state.tripHistory.isEmpty() -> "Nothing logged"
                else -> "${state.tripHistory.size} logged"
            },
            icon = Icons.Outlined.History,
            route = Routes.SAFETY_TRIPS
        ),
        BoardWay(
            key = "contacts",
            name = "Emergency contacts",
            // Amber rather than off when empty: an emergency contact list with
            // nothing in it is the single most common reason the whole product
            // does nothing when it matters.
            state = if (contacts.isEmpty()) LampState.ATTENTION else LampState.LIVE,
            stateLabel = if (contacts.isEmpty()) "None set" else "${contacts.size} set",
            icon = Icons.Outlined.Contacts,
            route = Routes.SAFETY_CONTACTS
        ),
        BoardWay(
            key = "medical",
            name = "Medical ID",
            state = if (state.medicalId.isUsable) LampState.LIVE else LampState.ATTENTION,
            stateLabel = if (state.medicalId.isUsable) "Filled in" else "Incomplete",
            detail = "${state.medicalId.filledFieldCount} of 11 fields",
            icon = Icons.Outlined.MedicalServices,
            route = Routes.SAFETY_MEDICAL
        ),
        BoardWay(
            key = "link",
            name = "Paired devices",
            state = linkLamp(state.connection),
            stateLabel = linkLabel(state.connection),
            detail = "${state.pairedDevices.size} remembered",
            icon = Icons.Outlined.Bluetooth,
            // Deliberately DEVICE_PAIRED and not BOARD_LINK: the most prominent
            // row on the panel must not lead to a placeholder.
            route = Routes.DEVICE_PAIRED
        )
    )
}

private fun zonesWay(state: AppState.Ready): CircleWay {
    val transition = state.lastZoneTransition
    return CircleWay(
        state = when {
            state.zones.isEmpty() -> LampState.OFF
            transition == null -> LampState.UNKNOWN
            transition.isInside -> LampState.LIVE
            else -> LampState.ATTENTION
        },
        stateLabel = when {
            state.zones.isEmpty() -> "None"
            transition == null -> "Not known"
            transition.isInside -> "Inside ${transition.zone.name}"
            else -> "Outside ${transition.zone.name}"
        },
        detail = if (state.zones.isEmpty()) {
            "No safe zones have been set"
        } else {
            "${state.zones.size} zones · told when leaving"
        }
    )
}

private fun zoneDetail(zone: GeofenceZone): String {
    val alerts = listOfNotNull(
        "told when leaving".takeIf { zone.alertOnExit },
        "told when arriving".takeIf { zone.alertOnEnter }
    ).ifEmpty { listOf("no alerts") }
    return "${zone.radiusMeters.toInt()} m · ${alerts.joinToString(" and ")}"
}

private fun CheckInRequest.toHistoryRow(): CheckInHistoryRow {
    val outcome = when {
        answeredAt != null -> CheckInOutcome.ANSWERED
        escalated -> CheckInOutcome.MISSED
        else -> CheckInOutcome.CANCELLED
    }
    return CheckInHistoryRow(
        id = id,
        label = dayTimeLabel(sentAt),
        outcome = outcome,
        detail = when (outcome) {
            CheckInOutcome.ANSWERED -> {
                val seconds = ((answeredAt!! - sentAt) / 1000L).toInt()
                if (seconds < 60) "Answered in $seconds seconds"
                else "Answered in ${seconds / 60} minutes"
            }
            CheckInOutcome.MISSED ->
                "No answer after ${((deadlineAt - sentAt) / 60_000L).toInt()} minutes"
            CheckInOutcome.CANCELLED -> null
        }
    )
}

/** Who wears the device. Blank until somebody has said. */
private val AppState.Ready.wearerName: String get() = deviceSettings.wearerName

/**
 * Who watches over the wearer.
 *
 * The model has no field of its own for this. The primary emergency contact is
 * who a companion's copy is actually about, so it stands in rather than leaving
 * every sentence with a hole.
 */
private val AppState.Ready.guardianName: String
    get() = safetySettings.primaryContact?.name.orEmpty()

private val AppState.Ready.contactsLabel: String
    get() = contactsSummary(safetySettings.emergencyContacts)

private fun contactsSummary(contacts: List<EmergencyContact>): String = when (contacts.size) {
    0 -> "your emergency contacts"
    1 -> contacts.first().name
    2 -> "${contacts[0].name} and ${contacts[1].name}"
    else -> "${contacts[0].name}, ${contacts[1].name} and ${contacts.size - 2} more"
}

private fun syncSummary(status: SyncStatus): String? = when (status) {
    SyncStatus.Idle -> null
    is SyncStatus.Syncing -> "Sending ${status.stage}"
    is SyncStatus.Synced -> "Sent to the device at ${clockLabel(status.at)}"
    is SyncStatus.Failed -> "${status.stage} was not acknowledged at ${clockLabel(status.at)}"
}

/** The canned phrases each end of the link is offered. */
private fun quickPhrases(role: UserRole): List<String> = when (role) {
    UserRole.GUARDIAN -> listOf("Where are you?", "Call me", "Coming to get you", "Are you OK?")
    UserRole.COMPANION -> listOf("I am OK", "On my way", "Call me", "Reached safely")
}

/**
 * Sends on the channel that matches who is holding the phone.
 *
 * The two are not interchangeable: `MESSAGE` makes the firmware buzz and show
 * an incoming message, `REPLY` sends one back. A companion's reply written to
 * the message characteristic reaches the wearer as a brand-new message from
 * themselves, which is a bug this codebase has shipped before.
 */
private fun sendAsRole(viewModel: SafeShadeViewModel, role: UserRole, text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    when (role) {
        UserRole.GUARDIAN -> viewModel.sendGuardianMessage(clean)
        UserRole.COMPANION -> viewModel.sendCompanionReply(clean)
    }
}

private val MedicalIdSaver = listSaver<MedicalId, Any>(
    save = {
        listOf(
            it.bloodType, it.emergencyContact, it.contactName, it.allergies, it.age,
            it.conditions, it.medications, it.secondaryContactName, it.secondaryContact,
            it.organDonor, it.notes
        )
    },
    restore = {
        MedicalId(
            bloodType = it[0] as String,
            emergencyContact = it[1] as String,
            contactName = it[2] as String,
            allergies = it[3] as String,
            age = it[4] as Int,
            conditions = it[5] as String,
            medications = it[6] as String,
            secondaryContactName = it[7] as String,
            secondaryContact = it[8] as String,
            organDonor = it[9] as Boolean,
            notes = it[10] as String
        )
    }
)

// ============================================
// Platform reads
// ============================================
// These answer questions `AppState` does not carry and no repository owns —
// they are properties of the phone, not of the app.

private fun hasFineLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun hasBackgroundLocation(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // Before API 29 a foreground grant is a background grant.
        hasFineLocation(context)
    }

private fun canScheduleExactAlarms(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    } else {
        true
    }

private fun reliabilityStatuses(context: Context): Map<ReliabilityCheck, CheckStatus> {
    fun boolStatus(value: Boolean) = if (value) CheckStatus.PASSING else CheckStatus.FAILING

    val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        boolStatus(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    } else {
        CheckStatus.PASSING
    }

    val fullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        boolStatus(
            context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == true
        )
    } else {
        CheckStatus.PASSING
    }

    val battery = context.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(context.packageName)
        ?.let { boolStatus(it) }
        ?: CheckStatus.UNKNOWN

    return mapOf(
        ReliabilityCheck.NOTIFICATIONS to notifications,
        ReliabilityCheck.FULL_SCREEN_INTENT to fullScreen,
        ReliabilityCheck.EXACT_ALARMS to boolStatus(canScheduleExactAlarms(context)),
        ReliabilityCheck.BACKGROUND_LOCATION to boolStatus(hasBackgroundLocation(context)),
        ReliabilityCheck.BATTERY_OPTIMISATION to battery,
        // There is no public API for OEM autostart on any of the vendors that
        // implement it, so "we cannot tell" is the only honest answer.
        ReliabilityCheck.OEM_AUTOSTART to CheckStatus.UNKNOWN
    )
}

private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.fromParts("package", context.packageName, null)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { openAppSettings(context) }
}

private fun openOnMap(context: Context, lat: Double, lon: Double, label: String) {
    runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/**
 * The stand-in for a route that has no screen yet.
 *
 * Registered rather than omitted so that a row pointing at it lands somewhere
 * legible instead of throwing `IllegalArgumentException` out of the navigator.
 */
@Composable
private fun ComingSoonPlate(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.gutter),
        contentAlignment = Alignment.TopCenter
    ) {
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Coming in this build",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.board.inkMuted,
                modifier = Modifier.padding(Spacing.lg)
            )
        }
    }
}
