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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safeshade.cloud.CloudResult
import com.safeshade.cloud.CloudTier
import com.safeshade.cloud.InviteStatus
import com.safeshade.ui.screens.circle.TalkNote
import com.safeshade.ui.screens.circle.TalkScreen
import com.safeshade.ui.screens.circle.TalkUiState
import com.safeshade.platform.VoiceNoteResult
import com.safeshade.platform.VoicePlayer
import com.safeshade.platform.VoiceRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.safeshade.ui.screens.circle.GuardiansScreen
import com.safeshade.ui.screens.circle.GuardiansUiState
import com.safeshade.cloud.CloudSession
import com.safeshade.ui.screens.profile.EmailsScreen
import com.safeshade.ui.screens.profile.EmailsUiState
import com.safeshade.ui.screens.profile.PrivacyScreen
import com.safeshade.ui.screens.profile.PrivacyUiState
import com.safeshade.ui.screens.profile.AccountScreen
import com.safeshade.ui.screens.profile.AccountWay
import com.safeshade.ui.screens.profile.ProfilePerson
import com.safeshade.ui.screens.circle.HeatmapScreen
import com.safeshade.ui.screens.circle.HeatmapUiState
import com.safeshade.ui.screens.circle.HeatPoint
import com.safeshade.ui.screens.circle.PeopleScreen
import com.safeshade.ui.screens.circle.WearerCard
import com.safeshade.ui.screens.circle.PersonListRow
import com.safeshade.ui.screens.circle.WearerEditorScreen
import com.safeshade.ui.screens.circle.WearerEditorUiState
import com.safeshade.platform.OverpassClient
import com.safeshade.ui.screens.safety.NearbyUiState
import okhttp3.OkHttpClient
import android.app.Activity
import androidx.compose.runtime.DisposableEffect
import com.safeshade.platform.BillingOutcome
import com.safeshade.platform.PlanOffer
import com.safeshade.platform.PlayBilling
import com.safeshade.ui.screens.profile.PlanScreen
import com.safeshade.ui.screens.profile.PlanTier
import com.safeshade.ui.screens.profile.PlanUiState
import com.safeshade.data.Wearer
import com.safeshade.data.LocationState
import com.safeshade.ActionResult
import com.safeshade.dialNumber
import com.safeshade.data.WearerResult
import com.safeshade.ui.screens.profile.SignInActions
import com.safeshade.ui.screens.profile.SignInScreen
import com.safeshade.ui.vm.CloudViewModel
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
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import com.safeshade.data.UserRole
import com.safeshade.device.ConnectionState
import com.safeshade.platform.PhoneNumbers
import com.safeshade.repo.AppState
import com.safeshade.repo.SendResult
import com.safeshade.repo.SyncStatus
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.KitGallery
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.syncKey
import com.safeshade.ui.icons.SafeShadeIcons
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
import com.safeshade.ui.screens.device.DeviceScreen
import com.safeshade.ui.screens.device.DeviceSettingsScreen
import com.safeshade.ui.screens.device.DeviceSettingsUiState
import com.safeshade.ui.screens.device.DeviceUiState
import com.safeshade.ui.screens.device.LightsScreen
import com.safeshade.ui.screens.device.LightsUiState
import com.safeshade.ui.screens.device.LocateScreen
import com.safeshade.ui.screens.device.LocateUiState
import com.safeshade.ui.screens.device.ModeCompareScreen
import com.safeshade.ui.screens.device.ModeCompareUiState
import com.safeshade.ui.screens.device.ModeDetailScreen
import com.safeshade.ui.screens.device.ModeDetailUiState
import com.safeshade.ui.screens.device.ModePickerScreen
import com.safeshade.ui.screens.device.ModePickerUiState
import com.safeshade.ui.screens.device.PairedDevicesScreen
import com.safeshade.ui.screens.device.PairScreen
import com.safeshade.ui.screens.device.RidesScreen
import com.safeshade.ui.screens.device.FirmwareScreen
import com.safeshade.ui.screens.device.FirmwareUiState
import com.safeshade.ui.screens.device.FirmwareReleaseRow
import com.safeshade.ui.screens.circle.SmartHomeScreen
import com.safeshade.ui.screens.circle.SmartHomeUiState
import com.safeshade.ui.screens.circle.SmartHookRow
import com.safeshade.ui.screens.circle.PlatformRow
import com.safeshade.ui.screens.circle.SmartHookEditorScreen
import com.safeshade.ui.screens.circle.SmartHookEditorUiState
import com.safeshade.ui.screens.circle.SmartHookTriggerLabels
import com.safeshade.ui.screens.circle.SmartHookProviderLabels
import com.safeshade.ui.screens.device.LostModeScreen
import com.safeshade.ui.screens.device.LostUiState
import com.safeshade.ui.screens.device.LostDeviceRow
import com.safeshade.ui.screens.device.RidesUiState
import com.safeshade.ui.screens.device.RideRow
import com.safeshade.ui.screens.device.PairUiState
import com.safeshade.data.DeviceModel
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
import com.safeshade.ui.screens.safety.EscalationScreen
import com.safeshade.ui.screens.safety.EscalationUiState
import com.safeshade.ui.screens.safety.WatchSettingsScreen
import com.safeshade.ui.screens.safety.VitalsScreen
import com.safeshade.ui.screens.safety.EvidenceScreen
import com.safeshade.ui.screens.safety.EvidenceUiState
import com.safeshade.ui.screens.safety.EvidenceClipRow
import androidx.compose.runtime.mutableStateListOf
import com.safeshade.ui.screens.safety.VitalsUiState
import com.safeshade.ui.screens.safety.VitalsHistoryRow
import com.safeshade.ui.screens.safety.HealthConnectState
import com.safeshade.ui.screens.safety.WatchUiState
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
import com.safeshade.ui.screens.settings.CheckStatus
import com.safeshade.ui.screens.settings.DeveloperScreen
import com.safeshade.ui.screens.settings.DeveloperUiState
import com.safeshade.ui.screens.settings.ReliabilityCheck
import com.safeshade.ui.screens.settings.ReliabilityScreen
import com.safeshade.ui.screens.settings.ReliabilityUiState
import com.safeshade.ui.screens.settings.RoleScreen
import com.safeshade.ui.screens.settings.RoleUiState
import com.safeshade.ui.screens.profile.ProfileEditScreen
import com.safeshade.ui.screens.profile.ProfileEditUiState
import com.safeshade.ui.screens.profile.ProfileScreen
import com.safeshade.ui.screens.profile.ProfileTarget
import com.safeshade.ui.screens.profile.ProfileUiState
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.vm.SafeShadeViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    // Hoisted in SafeShadeApp rather than remembered inside each root — see the
    // note there. The bar destroys a tab's back stack entry when you leave it,
    // so scroll position cannot live inside the screen any more.
    boardListState: LazyListState,
    circleListState: LazyListState,
    safetyListState: LazyListState,
    deviceListState: LazyListState,
    // Hoisted from the host for the same reason the list states are: the three
    // message send paths live in here, and a failed send has a reason written
    // to be shown to somebody.
    snackbarHostState: SnackbarHostState,
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
    val connectedAddress by viewModel.connectedAddress.collectAsStateWithLifecycle()

    val liveState = rememberUpdatedState(state)
    val liveConnectedAddress = rememberUpdatedState(connectedAddress)
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
        pickLatField = zone?.lat?.let { coordinate(it) }.orEmpty()
        pickLonField = zone?.lon?.let { coordinate(it) }.orEmpty()
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
            val boardAir by viewModel.airQuality.collectAsStateWithLifecycle()
            val isSyncing = liveSyncing.value
            val permissionsGranted = livePermissions.value
            val hasUnresolvedTrip =
                state.activeAlert != null || state.tripHistory.any { it.outcome == TripOutcome.PENDING }

            BoardScreen(
                state = BoardUiState(
                    connection = state.connection,
                    role = state.role,
                    protectedName = state.deviceSettings.wearerName,
                    headline = boardHeadline(state.connection, state.role, state.wearerName),
                    subline = boardSubline(state, permissionsGranted),
                    batteryPercent = state.telemetry.batteryLevel.takeIf { state.telemetry.isRealData },
                    signalDbm = state.rssiSmoothed.takeIf { state.connection.isUsable },
                    ways = boardWays(state),
                    temperatureC = weather.temp.takeIf { weather.isLoaded },
                    rainChance = weather.rainChance.takeIf { weather.isLoaded },
                    weatherCondition = weather.condition.takeIf { weather.isLoaded },
                    uvIndex = weather.uvIndex.takeIf { weather.isLoaded },
                    nudges = com.safeshade.platform.WeatherNudges.assess(
                        uvIndex = weather.uvIndex.takeIf { weather.isLoaded },
                        tempC = weather.temp.takeIf { weather.isLoaded },
                        feelsLikeC = weather.temp.takeIf { weather.isLoaded },
                        humidity = weather.humidity.takeIf { weather.isLoaded },
                        aqiEuropean = boardAir?.europeanAqi,
                        pm25 = boardAir?.pm25,
                        hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                        persona = state.activeMode.name
                    ).map { it.title to it.line },
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
                onOpenWay = { route -> navController.navigate(route) },
                listState = boardListState
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
            val circleScope = rememberCoroutineScope()
            val state = liveState.value
            val circleCloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val circleCloud by circleCloudVm.cloudState.collectAsStateWithLifecycle()
            val circleSession by circleCloudVm.session.collectAsStateWithLifecycle()
            val location = liveLocation.value
            val isSyncing = liveSyncing.value
            val lastFix = state.lastKnownDeviceLocation ?: location.takeIf { it.isValid }
            val openCheckIn = state.checkIns.firstOrNull { it.isOpen }
            val journey = state.journey?.takeIf { it.state == JourneyState.ACTIVE }

            val circleHooks by viewModel.smartHooks.collectAsStateWithLifecycle()
            CircleScreen(
                state = CircleUiState(
                    wearers = if (state.role == UserRole.GUARDIAN) state.wearers.map { w -> wearerCard(state, w, connectedAddress, lastFix) } else emptyList(),
                    talk = state.voiceNotes.let { notes ->
                        val unheard = notes.count { !it.listened && it.fromGuardian != (state.role == UserRole.GUARDIAN) }
                        when {
                            notes.isEmpty() -> CircleWay(LampState.OFF, "None", "Hold to talk, let go to send")
                            unheard > 0 -> CircleWay(LampState.ATTENTION, "$unheard new", "Voice notes not heard yet")
                            else -> CircleWay(LampState.LIVE, "${notes.size}", "Last at ${clockLabel(notes.maxOf { it.createdAt })}")
                        }
                    },
                    guardians = when {
                        circleSession !is CloudSession.SignedIn -> CircleWay(LampState.OFF, "Only you", "Sign in to share the board with another phone")
                        circleCloud.members.size <= 1 -> CircleWay(LampState.OFF, "Only you", "Invite a guardian by email")
                        else -> CircleWay(LampState.LIVE, "${circleCloud.members.size} people", circleCloud.invites.count { it.status is InviteStatus.Pending }.let { if (it > 0) "$it invitation waiting" else "Everyone has accepted" })
                    },
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
                    smartHome = run {
                        val armed = circleHooks.orEmpty().count { it.enabled }
                        val failed = circleHooks.orEmpty().any { it.enabled && it.lastError != null }
                        CircleWay(
                            state = when { failed -> LampState.TRIP; armed > 0 -> LampState.LIVE; else -> LampState.OFF },
                            stateLabel = when { failed -> "Failed"; armed > 0 -> "$armed armed"; else -> "None" },
                            detail = if (armed > 0) "What the house does on a fall, an SOS or a zone crossing" else "Webhooks, Home Assistant, IFTTT"
                        )
                    },
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
                onSendQuickMessage = { text ->
                    val result = sendAsRole(viewModel, state.role, text)
                    // Launched rather than awaited: `showSnackbar` suspends
                    // until the snackbar is dismissed, and awaiting it here
                    // would hold the row in its sending state for the whole
                    // two seconds the message is on screen.
                    if (result is SendResult.Failed) {
                        circleScope.launch { snackbarHostState.showSnackbar(result.reason) }
                    }
                    result is SendResult.Sent
                },
                onRefreshLocation = { viewModel.syncWeather() },
                onOpenZones = { navController.navigate(Routes.CIRCLE_ZONES) },
                onOpenJourney = { navController.navigate(Routes.CIRCLE_JOURNEY) },
                onOpenCheckIn = { navController.navigate(Routes.CIRCLE_CHECKIN) },
                onOpenSim = { navController.navigate(Routes.CIRCLE_SIM) },
                onOpenPeople = { navController.navigate(Routes.CIRCLE_PEOPLE) },
                onOpenHeatmap = { navController.navigate(Routes.CIRCLE_HEATMAP) },
                onOpenGuardians = { navController.navigate(Routes.CIRCLE_GUARDIANS) },
                onOpenTalk = { navController.navigate(Routes.CIRCLE_TALK) },
                onOpenPerson = { id -> navController.navigate(Routes.personEdit(id)) },
                onLocate = { navController.navigate(Routes.DEVICE_LOCATE) },
                onOpenSmartHome = { navController.navigate(Routes.CIRCLE_SMART_HOME) },
                onCallWearable = {
                    // The wearable's SIM, dialled through the phone's dialler;
                    // the number is the one stored under SIM and SMS. A blank
                    // number never reaches here - the button is disabled and
                    // says why.
                    val result = dialNumber(context, state.devicePhoneNumber)
                    if (result is ActionResult.Failed) {
                        circleScope.launch { snackbarHostState.showSnackbar(result.reason) }
                    }
                },
                listState = circleListState
            )
        }

        composable(Routes.CIRCLE_TALK) {
            val state = liveState.value
            val context = LocalContext.current
            val talkScope = rememberCoroutineScope()
            val talkCloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val talkSession by talkCloudVm.session.collectAsStateWithLifecycle()
            val recorder = remember { VoiceRecorder(context.applicationContext) }
            val player = remember { VoicePlayer() }
            var recording by remember { mutableStateOf(false) }
            var recordStart by remember { mutableStateOf(0L) }
            var elapsed by remember { mutableStateOf(0) }
            var playingId by remember { mutableStateOf<String?>(null) }
            var progress by remember { mutableStateOf(0f) }
            var error by remember { mutableStateOf<String?>(null) }

            val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                error = if (granted) null else "Microphone permission not granted. Allow it in Settings to record."
            }
            DisposableEffect(Unit) { onDispose { player.stop() } }
            LaunchedEffect(recording) {
                while (recording) {
                    elapsed = (System.currentTimeMillis() - recordStart).toInt()
                    kotlinx.coroutines.delay(100)
                }
            }

            fun stopRecording() {
                if (!recording) return
                recording = false
                talkScope.launch {
                    when (val r = recorder.stop()) {
                        is VoiceNoteResult.Ok -> {
                            error = null
                            viewModel.addVoiceNote(
                                fileName = r.file.name,
                                durationMs = r.durationMs,
                                waveform = r.waveform,
                                wearerId = state.selectedWearerId,
                                fromGuardian = state.role == UserRole.GUARDIAN,
                                authorName = if (state.role == UserRole.GUARDIAN) state.ownerName else state.wearerName
                            )
                        }
                        is VoiceNoteResult.Failed -> error = r.reason
                    }
                }
            }

            TalkScreen(
                state = TalkUiState(
                    role = state.role,
                    wearerName = state.wearerName,
                    guardianName = state.guardianName,
                    notes = state.voiceNotes.sortedBy { it.createdAt }.map { n ->
                        TalkNote(
                            id = n.id,
                            fromGuardian = n.fromGuardian,
                            authorName = n.authorName,
                            timeLabel = clockLabel(n.createdAt),
                            durationMs = n.durationMs,
                            waveform = n.waveform,
                            uploadState = n.uploadState,
                            listened = n.listened,
                            onThisPhone = n.file.isNotBlank() && java.io.File(java.io.File(context.filesDir, "voice"), n.file).exists()
                        )
                    },
                    recording = recording,
                    recordingMs = elapsed,
                    playingId = playingId,
                    progress = progress,
                    signedIn = talkSession is CloudSession.SignedIn,
                    error = error
                ),
                onHoldStart = {
                    if (recording) return@TalkScreen
                    val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        askMic.launch(android.Manifest.permission.RECORD_AUDIO)
                        return@TalkScreen
                    }
                    player.stop(); playingId = null; progress = 0f
                    val file = recorder.start()
                    if (file == null) {
                        error = "The microphone could not be started. Another app may be using it."
                    } else {
                        error = null
                        recordStart = System.currentTimeMillis()
                        elapsed = 0
                        recording = true
                    }
                },
                onHoldEnd = { stopRecording() },
                onPlay = { id ->
                    val note = state.voiceNotes.firstOrNull { it.id == id } ?: return@TalkScreen
                    val file = java.io.File(java.io.File(context.filesDir, "voice"), note.file)
                    if (note.file.isBlank() || !file.exists()) {
                        error = "This note is not on this phone yet."
                        return@TalkScreen
                    }
                    error = null
                    playingId = id
                    progress = 0f
                    player.play(file, onProgress = { progress = it }, onDone = { playingId = null; progress = 0f })
                    if (!note.listened) viewModel.markVoiceNoteListened(id)
                },
                onStop = { player.stop(); playingId = null; progress = 0f },
                onOpenSignIn = { navController.navigate(Routes.SETTINGS_SIGN_IN) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CIRCLE_THREAD) {
            val state = liveState.value
            val threadScope = rememberCoroutineScope()
            var draft by rememberSaveable { mutableStateOf("") }
            // `MessagesUiState.isSending` has existed since the screen was
            // written and nothing ever set it, so the field stayed enabled,
            // the button stayed pressable and its "Sending" label had never
            // once appeared. It is real state now.
            var sending by remember { mutableStateOf(false) }
            val threadCloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val threadSync by threadCloudVm.outboxStates.collectAsStateWithLifecycle()

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
                                replyText = null,
                                syncState = threadSync[syncKey("messages", message.id)]
                            )
                        },
                    quickReplies = quickPhrases(state.role),
                    draft = draft,
                    outboundChannel = outboundChannel(state),
                    deviceInRange = state.connection.isUsable,
                    smsConfigured = state.devicePhoneNumber.isNotBlank(),
                    isSending = sending
                ),
                onDraftChange = { draft = it },
                onSend = {
                    // Directionality is the historical bug in this codebase:
                    // a companion's reply written to the guardian channel makes
                    // the firmware buzz the wearer with their own message.
                    val text = draft
                    threadScope.launch {
                        sending = true
                        val result = sendAsRole(viewModel, state.role, text)
                        sending = false
                        when (result) {
                            // The draft is cleared only once the message is
                            // away. A failed send used to empty the box and
                            // say nothing, so the words were gone and so was
                            // the message.
                            is SendResult.Sent -> if (draft == text) draft = ""
                            is SendResult.Failed ->
                                snackbarHostState.showSnackbar(result.reason)
                        }
                    }
                },
                onSendQuick = { text ->
                    threadScope.launch {
                        sending = true
                        val result = sendAsRole(viewModel, state.role, text)
                        sending = false
                        if (result is SendResult.Failed) {
                            snackbarHostState.showSnackbar(result.reason)
                        }
                    }
                },
                onOpenSim = { navController.navigate(Routes.CIRCLE_SIM) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CIRCLE_ZONES) {
            val state = liveState.value
            val zonesScope = rememberCoroutineScope()
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
                onGuideTo = if (state.connection.isUsable) { zoneId ->
                    val zone = state.zones.firstOrNull { it.id == zoneId }
                    if (zone != null) {
                        zonesScope.launch {
                            val ok = viewModel.guideTo(zone.lat, zone.lon, zone.name).await()
                            snackbarHostState.showSnackbar(
                                if (ok) "The wearable is guiding to ${zone.name}." else "The wearable did not acknowledge the destination."
                            )
                        }
                    }
                } else null,
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
                    pickLatField = coordinate(location.lat)
                    pickLonField = coordinate(location.lon)
                    pickLocating = false
                }
            }

            ZonePickerScreen(
                state = ZonePickerUiState(
                    lat = parsedLat,
                    lon = parsedLon,
                    radiusMeters = zoneDraftRadius,
                    isLocating = pickLocating || isSyncing,
                    locationError = pickError,
                    canConfirm = parsedLat != null && parsedLon != null
                ),
                // The picker sets the radius now as well as the centre, writing
                // the same draft the editor writes. Both screens read it back,
                // so a size chosen over the map is the size the editor shows.
                onRadiusChange = { zoneDraftRadius = it },
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
                    pickLatField = coordinate(lat)
                    pickLonField = coordinate(lon)
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
                onWalkHome = { viewModel.startJourney("Home", 20) },
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
            val hubRun by viewModel.escalationRun.collectAsStateWithLifecycle()
            val hubWatch by viewModel.watchState.collectAsStateWithLifecycle()
            val hubVitals by viewModel.vitalsSamples.collectAsStateWithLifecycle()
            val hubVitalsFlags by viewModel.vitalsFlags.collectAsStateWithLifecycle()
            val newestVitals = hubVitals.orEmpty().filter { it.hasVitals }.maxByOrNull { it.at }

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
                    silentSosEnabled = silentSosEnabled,
                    wearableOfflineSince = hubWatch.offlineSince,
                    wearableOfflineAlerted = hubWatch.offlineAlerted,
                    escalationRunning = hubRun?.let { !it.isFinished } == true,
                    vitalsLine = newestVitals?.let { vitalsLine(it) },
                    vitalsFlagged = hubVitalsFlags.isNotEmpty()
                ),
                onOpenVitals = { navController.navigate(Routes.SAFETY_VITALS) },
                onOpenEvidence = { navController.navigate(Routes.SAFETY_EVIDENCE) },
                onOpenFallSettings = { navController.navigate(Routes.SAFETY_FALL) },
                onOpenEscalation = { navController.navigate(Routes.SAFETY_ESCALATION) },
                onOpenWatch = { navController.navigate(Routes.SAFETY_WATCH) },
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
                },
                listState = safetyListState
            )
        }

        composable(Routes.SAFETY_ESCALATION) {
            val state = liveState.value
            val run by viewModel.escalationRun.collectAsStateWithLifecycle()
            EscalationScreen(
                state = EscalationUiState(
                    settings = state.safetySettings.escalation,
                    contacts = state.safetySettings.emergencyContacts,
                    wearerName = state.wearerName,
                    directCallsAllowed = state.safetySettings.autoCallEmergency,
                    run = run?.takeIf { !it.isFinished }
                ),
                onChange = { viewModel.setEscalation(it) },
                onOpenContacts = { navController.navigate(Routes.SAFETY_CONTACTS) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SAFETY_VITALS) {
            val state = liveState.value
            val samples by viewModel.vitalsSamples.collectAsStateWithLifecycle()
            val thresholds by viewModel.vitalsThresholds.collectAsStateWithLifecycle()
            val flags by viewModel.vitalsFlags.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            // Health Connect's state is re-read on every visit and after every
            // action, because installing it or granting a permission happens in
            // another app and nothing here is told.
            var availability by remember { mutableStateOf(viewModel.healthConnectAvailability()) }
            var granted by remember { mutableStateOf(false) }
            var reading by remember { mutableStateOf(false) }
            var readError by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(availability) {
                granted = availability is com.safeshade.platform.Availability.Available && viewModel.healthConnectGranted()
            }
            val askHealth = rememberLauncherForActivityResult(viewModel.healthConnectContract()) { result ->
                granted = result.containsAll(viewModel.healthConnectPermissions)
                if (!granted) readError = "Health Connect did not grant all three readings."
            }
            val readNow: () -> Unit = {
                if (!reading) {
                    reading = true; readError = null
                    scope.launch {
                        readError = viewModel.readHealthConnect(state.selectedWearer?.id)
                        reading = false
                    }
                }
            }
            // The first visit with permission reads once, so the plate is not a
            // bank of dashes when a reading was there for the asking.
            LaunchedEffect(granted) { if (granted) readNow() }

            val newest = samples.orEmpty().filter { it.hasVitals }.maxByOrNull { it.at }
            VitalsScreen(
                state = VitalsUiState(
                    wearerName = state.wearerName,
                    heartRateBpm = newest?.heartRateBpm,
                    spo2Percent = newest?.spo2Percent,
                    tempC = newest?.tempC,
                    measuredAt = newest?.at,
                    sourceLabel = newest?.let { if (it.source == com.safeshade.data.VitalsSample.SOURCE_DEVICE) "Wearable" else "Phone" },
                    deviceLive = state.connection.isUsable && state.telemetry.isRealData,
                    deviceReportsVitals = state.telemetry.hasVitals,
                    healthConnect = when (availability) {
                        is com.safeshade.platform.Availability.Available -> if (granted) HealthConnectState.READY else HealthConnectState.NO_PERMISSION
                        is com.safeshade.platform.Availability.NotInstalled -> HealthConnectState.NOT_INSTALLED
                        com.safeshade.platform.Availability.NeedsUpdate -> HealthConnectState.NEEDS_UPDATE
                        is com.safeshade.platform.Availability.Unsupported -> HealthConnectState.UNSUPPORTED
                    },
                    reading = reading,
                    readError = readError,
                    hrLow = thresholds.hrLow,
                    hrHigh = thresholds.hrHigh,
                    spo2Low = thresholds.spo2Low,
                    tempHigh = thresholds.tempHigh,
                    flags = flags.map { f -> vitalsFlagLine(f, newest, thresholds) },
                    history = samples.orEmpty().filter { it.hasVitals }.sortedByDescending { it.at }.take(8).map {
                        VitalsHistoryRow(
                            at = it.at,
                            heartRateBpm = it.heartRateBpm,
                            spo2Percent = it.spo2Percent,
                            tempC = it.tempC,
                            sourceLabel = if (it.source == com.safeshade.data.VitalsSample.SOURCE_DEVICE) "Wearable" else "Phone"
                        )
                    }
                ),
                onReadNow = readNow,
                onInstallHealthConnect = {
                    val intent = (availability as? com.safeshade.platform.Availability.NotInstalled)?.playStoreIntent
                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                    try {
                        context.startActivity(intent)
                    } catch (_: android.content.ActivityNotFoundException) {
                        readError = "This phone has no Play Store to open."
                    }
                },
                onAllowHealthConnect = {
                    if (availability is com.safeshade.platform.Availability.Available) {
                        askHealth.launch(viewModel.healthConnectPermissions)
                    } else {
                        availability = viewModel.healthConnectAvailability()
                    }
                },
                onThresholds = { hrLow, hrHigh, spo2Low, tempHigh ->
                    viewModel.setVitalsThresholds(com.safeshade.data.VitalsThresholds(hrLow, hrHigh, spo2Low, tempHigh))
                },
                onBack = { navController.popBackStack() }
            )
            // Coming back from the Play Store or from Health Connect's own
            // permission page changes the answer; ask again when the screen resumes.
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) availability = viewModel.healthConnectAvailability()
                }
                lifecycleOwner.lifecycle.addObserver(obs)
                onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
            }
        }

        composable(Routes.SAFETY_EVIDENCE) {
            val state = liveState.value
            val clips by viewModel.evidenceClips.collectAsStateWithLifecycle()
            val settings by viewModel.evidenceSettings.collectAsStateWithLifecycle()
            val service by viewModel.evidenceService.collectAsStateWithLifecycle()
            val evidenceCloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val session by evidenceCloudVm.session.collectAsStateWithLifecycle()
            val context = LocalContext.current

            var micGranted by remember { mutableStateOf(viewModel.hasMicPermission()) }
            val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                micGranted = granted
            }
            var startError by remember { mutableStateOf<String?>(null) }

            // The meter lives exactly as long as this screen and its rocker say.
            val meter = remember { com.safeshade.platform.SoundLevelMeter(context) }
            var meterOn by remember { mutableStateOf(false) }
            val reading by meter.reading.collectAsStateWithLifecycle()
            val readings = remember { mutableStateListOf<com.safeshade.platform.SoundReading>() }
            LaunchedEffect(reading) {
                val r = reading ?: return@LaunchedEffect
                readings.add(r)
                if (readings.size > 600) readings.removeAt(0)
            }
            DisposableEffect(Unit) { onDispose { meter.stop(); meter.release() } }
            val verdict = com.safeshade.platform.LoudEnvironment.assess(readings.toList(), System.currentTimeMillis())

            val player = remember { VoicePlayer() }
            var playingId by remember { mutableStateOf<String?>(null) }
            var progress by remember { mutableFloatStateOf(0f) }
            DisposableEffect(Unit) { onDispose { player.stop() } }

            val recording = service as? com.safeshade.service.EvidenceServiceState.Recording
            val finished = service as? com.safeshade.service.EvidenceServiceState.Finished
            val serviceError = (finished?.result as? com.safeshade.platform.EvidenceRecordingResult.Failed)?.reason

            EvidenceScreen(
                state = EvidenceUiState(
                    wearerName = state.wearerName,
                    recordOnFall = settings.recordOnFall,
                    recordOnSos = settings.recordOnSos,
                    durationSeconds = settings.durationSeconds,
                    uploadToCloud = settings.uploadToCloud,
                    signedIn = session is CloudSession.SignedIn,
                    micGranted = micGranted,
                    recordingElapsedMs = recording?.elapsedMs,
                    recordingTotalMs = recording?.totalMs,
                    recordError = startError ?: serviceError,
                    soundDb = if (meterOn) reading?.approxDbSpl else null,
                    meterOn = meterOn,
                    loudLine = (verdict as? com.safeshade.platform.LoudVerdict.Loud)?.let {
                        "Loud for ${((System.currentTimeMillis() - it.sinceMs) / 60_000L).coerceAtLeast(1)} min · peak ${it.peakDb} dB"
                    },
                    calibrationNote = if (meterOn) com.safeshade.platform.SoundLevelMeter.CALIBRATION_NOTE else "",
                    clips = clips.orEmpty().sortedByDescending { it.capturedAt }.map { evidenceRow(it, state) },
                    playingId = playingId,
                    playProgress = progress
                ),
                onRecordOnFall = { viewModel.setEvidenceSettings(settings.copy(recordOnFall = it)) },
                onRecordOnSos = { viewModel.setEvidenceSettings(settings.copy(recordOnSos = it)) },
                onDuration = { viewModel.setEvidenceSettings(settings.copy(durationSeconds = it)) },
                onUploadToCloud = { viewModel.setEvidenceSettings(settings.copy(uploadToCloud = it)) },
                onTestRecording = {
                    startError = null
                    if (!viewModel.startEvidence(alertId = null, seconds = 10)) {
                        startError = "The microphone could not be started. Another app may be using it, or the permission was withdrawn."
                    }
                },
                onStopRecording = { viewModel.stopEvidence() },
                onMeter = { on ->
                    if (on) {
                        if (!micGranted) { askMic.launch(android.Manifest.permission.RECORD_AUDIO) }
                        else if (meter.start()) { meterOn = true; readings.clear() }
                        else startError = "The microphone could not be opened for the meter."
                    } else { meter.stop(); meterOn = false }
                },
                onPlay = { id ->
                    val clip = clips.orEmpty().firstOrNull { it.id == id }
                    val file = clip?.let { java.io.File(context.filesDir, "${com.safeshade.platform.EvidenceRecorder.DIR_NAME}/${it.file}") }
                    if (file != null && file.exists()) {
                        player.stop()
                        playingId = id; progress = 0f
                        player.play(file, onProgress = { progress = it }, onDone = { playingId = null; progress = 0f })
                    } else {
                        startError = "That recording is no longer on this phone."
                    }
                },
                onStop = { player.stop(); playingId = null; progress = 0f },
                onDelete = { id -> if (playingId == id) { player.stop(); playingId = null }; viewModel.deleteEvidence(id) },
                onRequestMic = { askMic.launch(android.Manifest.permission.RECORD_AUDIO) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SAFETY_WATCH) {
            val state = liveState.value
            val watch by viewModel.watchState.collectAsStateWithLifecycle()
            WatchSettingsScreen(
                state = WatchUiState(
                    offlineAlertMinutes = state.safetySettings.offlineAlertMinutes,
                    lowBatteryPercent = state.safetySettings.lowBatteryPercent,
                    wearerName = state.wearerName,
                    connected = state.connection.isUsable,
                    offlineSince = watch.offlineSince,
                    offlineAlerted = watch.offlineAlerted,
                    lowBatteryAlerted = watch.lowBatteryAlerted,
                    batteryPercent = state.telemetry.takeIf { it.isRealData }?.batteryLevel
                ),
                onChange = { minutes, percent -> viewModel.setWatchThresholds(minutes, percent) },
                onBack = { navController.popBackStack() }
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
            var draftRelationship by rememberSaveable { mutableStateOf("") }
            var draftAvatar by rememberSaveable { mutableStateOf("") }

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
                        ContactDraft(draftIndex, draftName, draftPhone, draftPrimary, draftRelationship, avatarId = draftAvatar)
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
                    draftRelationship = ""
                    draftAvatar = ""
                    draftOpen = true
                },
                onStartEdit = { index ->
                    contacts.getOrNull(index)?.let { contact ->
                        draftIndex = index
                        draftName = contact.name
                        // Normalised on load as well as on input, so a contact
                        // saved before the field enforced bare digits edits on
                        // the same terms as a new one. Without this the draft
                        // holds "+91 891736 60065" while the field renders 11
                        // digits, which leaves the character counter counting
                        // the wrong string and the cursor mapping working
                        // across two different lengths.
                        draftPhone = PhoneNumbers.digitsOf(contact.phone)
                        draftPrimary = contact.isPrimary
                        draftRelationship = contact.relationship
                        draftAvatar = contact.avatarId
                        draftOpen = true
                    }
                },
                onDraftChange = { draft ->
                    draftName = draft.name
                    draftPhone = draft.phone
                    draftPrimary = draft.isPrimary
                    // Every field the draft carries has to be read back here.
                    // A field the screen edits but this lambda ignores is
                    // echoed back unchanged on the next recomposition, so it
                    // looks like typing into a box that erases itself - worse
                    // than an unwired field, because it reads as a bug in the
                    // keyboard rather than as something not finished.
                    draftRelationship = draft.relationship
                    draftAvatar = draft.avatarId
                },
                onSaveDraft = {
                    val edited = EmergencyContact(
                        name = draftName.trim(),
                        phone = draftPhone.trim(),
                        isPrimary = draftPrimary,
                        relationship = draftRelationship.trim(),
                        avatarId = draftAvatar
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

        composable(
            route = "${Routes.SAFETY_MEDICAL}?${Routes.Args.WEARER_ID}={${Routes.Args.WEARER_ID}}",
            arguments = listOf(navArgument(Routes.Args.WEARER_ID) { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            val state = liveState.value
            // A wearer other than the primary edits their own record; blank,
            // or the primary's id, edits the mirrored one exactly as before.
            val wearerId = entry.arguments?.getString(Routes.Args.WEARER_ID).orEmpty()
            val other = state.wearers.firstOrNull { it.id == wearerId }
                ?.takeIf { it.id != state.wearers.firstOrNull()?.id }
            val stored = other?.medicalId ?: state.medicalId
            // The one genuinely dirty form in the app. Held here so the screen
            // stays stateless, and saved only on the explicit action — the
            // medical ID is pushed to the wearable on write, so autosaving
            // every keystroke would put a half-typed allergy on the device.
            var draft by rememberSaveable(stateSaver = MedicalIdSaver) {
                mutableStateOf(stored)
            }

            MedicalIdScreen(
                state = MedicalIdUiState(
                    medicalId = draft,
                    wearerName = other?.name ?: state.wearerName,
                    linkLive = state.connection.isUsable,
                    isDirty = draft != stored,
                    lastSyncedLabel = syncSummary(state.syncStatus)
                ),
                onBack = { navController.popBackStack() },
                onChange = { draft = it },
                onSave = {
                    if (other == null) viewModel.setMedicalId(draft)
                    else viewModel.updateWearer(other.copy(medicalId = draft))
                },
                onOpenCard = { navController.navigate(Routes.SAFETY_MEDICAL_CARD) }
            )
        }

        composable(Routes.SAFETY_MEDICAL_CARD) {
            val state = liveState.value
            val nfcContext = LocalContext.current
            val nfcState = remember { com.safeshade.platform.nfcAvailability(nfcContext) }
            val nfcArmed by viewModel.nfcArmed.collectAsStateWithLifecycle()
            val nfcResult by viewModel.nfcResult.collectAsStateWithLifecycle()
            DisposableEffect(Unit) { onDispose { viewModel.armNfcWrite(false) } }
            EmergencyCardScreen(
                state = EmergencyCardUiState(
                    medicalId = state.medicalId,
                    wearerName = state.wearerName,
                    tagWord = when {
                        nfcResult != null -> "Done"
                        nfcArmed -> "Hold a tag"
                        nfcState is com.safeshade.platform.NfcAvailability.Available -> "Ready"
                        nfcState is com.safeshade.platform.NfcAvailability.Disabled -> "NFC is off"
                        else -> "—"
                    },
                    tagLine = nfcResult ?: when (nfcState) {
                        is com.safeshade.platform.NfcAvailability.Available -> if (nfcArmed) "Hold a blank NFC tag against the back of the phone." else "Writes this card to an NFC tag, for a Spark or a keyring."
                        is com.safeshade.platform.NfcAvailability.Disabled -> "Turn NFC on in the phone's settings first."
                        com.safeshade.platform.NfcAvailability.NoNfc -> "This phone has no NFC, so it cannot write a tag."
                    },
                    tagLamp = when {
                        nfcResult != null && !nfcResult!!.startsWith("Written") -> LampState.TRIP
                        nfcResult != null -> LampState.LIVE
                        nfcArmed -> LampState.ATTENTION
                        nfcState is com.safeshade.platform.NfcAvailability.Available -> LampState.OFF
                        else -> LampState.UNKNOWN
                    },
                    tagArmed = nfcArmed
                ),
                onWriteTag = if (nfcState is com.safeshade.platform.NfcAvailability.Available) { armed -> viewModel.armNfcWrite(armed) } else null,
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
            val state = liveState.value
            val location = liveLocation.value
            // The search point: the phone's fix, else the first safe zone
            // (home, for most households), else nothing - and "nothing" is
            // said on the screen rather than searched around 0,0.
            val fix = location.takeIf { it.isValid }
            val zone = state.zones.firstOrNull()
            val point = fix?.let { Triple(it.lat, it.lon, it.locationName.ifBlank { it.locality }.ifBlank { "your location" }) }
                ?: zone?.let { Triple(it.lat, it.lon, it.name.ifBlank { "your safe zone" }) }
            val overpass = remember { OverpassClient(OkHttpClient(), context.cacheDir) }
            var nearby by remember { mutableStateOf(NearbyUiState(noPoint = point == null)) }
            var refreshToken by remember { mutableStateOf(0) }
            LaunchedEffect(point?.first, point?.second, refreshToken) {
                if (point == null) { nearby = NearbyUiState(noPoint = true); return@LaunchedEffect }
                nearby = nearby.copy(loading = true, aroundLabel = point.third, noPoint = false)
                val result = overpass.nearby(point.first, point.second)
                nearby = NearbyUiState(result = result, loading = false, aroundLabel = point.third)
            }
            ServicesScreen(
                state = ServicesUiState(nearby = nearby),
                onBack = { navController.popBackStack() },
                onRefreshNearby = { refreshToken++ }
            )
        }

        composable(Routes.SAFETY_TRIPS) {
            val state = liveState.value
            val tripCloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val tripSync by tripCloudVm.outboxStates.collectAsStateWithLifecycle()
            TripLogScreen(
                state = TripLogUiState(
                    trips = state.tripHistory.sortedByDescending { it.timestamp },
                    wearerName = state.wearerName,
                    today = LocalDate.now(),
                    syncStates = tripSync
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
            val detailRun by viewModel.escalationRun.collectAsStateWithLifecycle()
            val detailClips by viewModel.evidenceClips.collectAsStateWithLifecycle()
            val detailContext = LocalContext.current
            val detailPlayer = remember { VoicePlayer() }
            var detailPlaying by remember { mutableStateOf<String?>(null) }
            DisposableEffect(Unit) { onDispose { detailPlayer.stop() } }

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
                    escalation = detailRun?.takeIf { it.alertId == trip.id },
                    recordings = detailClips.orEmpty().filter { it.alertId == trip.id }
                        .sortedByDescending { it.capturedAt }.map { evidenceRow(it, state) },
                    playingId = detailPlaying,
                    today = LocalDate.now()
                ),
                onSavePdf = {
                    val report = com.safeshade.platform.IncidentReport(
                        id = trip.id,
                        wearerName = state.wearerName,
                        kind = trip.kind.label,
                        at = trip.timestamp,
                        outcome = trip.outcome.label,
                        contacted = trip.wasEmergencyContacted,
                        lat = state.lastKnownDeviceLocation?.lat,
                        lon = state.lastKnownDeviceLocation?.lon,
                        medicalId = state.medicalId,
                        contacts = state.safetySettings.emergencyContacts,
                        timeline = buildList {
                            add(trip.timestamp to trip.kind.label)
                            detailRun?.takeIf { it.alertId == trip.id }?.steps?.forEach { step ->
                                val o = step.outcome
                                if (o is com.safeshade.service.StepOutcome.Dialled) add(o.at to "Dialled ${when (val t = step.target) { is com.safeshade.service.EscalationTarget.Contact -> t.name; is com.safeshade.service.EscalationTarget.Emergency -> t.number }}: ${o.result}")
                            }
                        },
                        evidenceNote = detailClips.orEmpty().count { it.alertId == trip.id }
                            .takeIf { it > 0 }?.let { "$it microphone recording(s) on the guardian's phone." }
                    )
                    when (val r = com.safeshade.platform.IncidentPdf.render(detailContext, report)) {
                        is com.safeshade.platform.IncidentPdfResult.Written ->
                            (com.safeshade.sharePdf(detailContext, r.file, "Share this report") as? ActionResult.Failed)?.reason
                        is com.safeshade.platform.IncidentPdfResult.Failed -> r.reason
                    }
                },
                onPlayRecording = { id ->
                    val clip = detailClips.orEmpty().firstOrNull { it.id == id } ?: return@TripDetailScreen
                    val file = java.io.File(detailContext.filesDir, "${com.safeshade.platform.EvidenceRecorder.DIR_NAME}/${clip.file}")
                    if (file.exists()) {
                        detailPlayer.stop(); detailPlaying = id
                        detailPlayer.play(file, onProgress = {}, onDone = { detailPlaying = null })
                    }
                },
                onStopRecording = { detailPlayer.stop(); detailPlaying = null },
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
            var quietDraft by rememberSaveable(state.safetySettings.quietWord) { mutableStateOf(state.safetySettings.quietWord) }
            var quietError by remember { mutableStateOf<String?>(null) }
            SilentSosScreen(
                state = SilentSosUiState(
                    silentSosEnabled = silentSosEnabled,
                    stagedCallSeconds = stagedCallSeconds,
                    callerName = "Home",
                    hasContacts = state.safetySettings.emergencyContacts.isNotEmpty(),
                    // The firmware capability is not reported over the link, so
                    // this claims nothing rather than promising a silent alert
                    // the device may not support.
                    deviceSupportsSilentAlert = false,
                    quietWord = quietDraft,
                    quietWordError = quietError
                ),
                onBack = { navController.popBackStack() },
                onQuietWordChange = { typed ->
                    quietDraft = typed
                    quietError = when (com.safeshade.platform.QuietWord.validate(typed)) {
                        com.safeshade.platform.QuietWordValidation.Ok -> null
                        com.safeshade.platform.QuietWordValidation.TooShort -> if (typed.isBlank()) null else "Three letters or more."
                        com.safeshade.platform.QuietWordValidation.TooCommon -> "Too ordinary. It would trip on an everyday message."
                        com.safeshade.platform.QuietWordValidation.ContainsDigitsOnly -> "Use letters, not just numbers."
                    }
                    if (quietError == null) viewModel.setSafetySettings(state.safetySettings.copy(quietWord = typed.trim()))
                },
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
            val deviceLeash by viewModel.leash.collectAsStateWithLifecycle()
            val deviceRides by viewModel.rides.collectAsStateWithLifecycle()
            val deviceLost by viewModel.lostDevices.collectAsStateWithLifecycle()
            DeviceScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                state = DeviceUiState(
                    connection = state.connection,
                    deviceName = state.deviceSettings.name,
                    wearerName = state.wearerName,
                    ownerName = state.ownerName,
                    ownerAvatarId = state.ownerAvatarId,
                    wearerAvatarId = state.deviceSettings.wearerAvatarId,
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
                    isRinging = state.isRinging,
                    rideCount = deviceRides.orEmpty().size,
                    lostCount = deviceLost.orEmpty().size,
                    leashLamp = when (deviceLeash) {
                        is com.safeshade.platform.LeashState.Near -> LampState.LIVE
                        is com.safeshade.platform.LeashState.Drifting -> LampState.ATTENTION
                        is com.safeshade.platform.LeashState.Far, is com.safeshade.platform.LeashState.Broken -> LampState.TRIP
                        com.safeshade.platform.LeashState.Unknown -> LampState.UNKNOWN
                    },
                    leashWord = when (deviceLeash) {
                        is com.safeshade.platform.LeashState.Near -> "Near"
                        is com.safeshade.platform.LeashState.Drifting -> "Drifting"
                        is com.safeshade.platform.LeashState.Far -> "Far"
                        is com.safeshade.platform.LeashState.Broken -> "Gone"
                        com.safeshade.platform.LeashState.Unknown -> null
                    },
                    leashLine = com.safeshade.platform.VirtualLeash().describe(deviceLeash, System.currentTimeMillis()).takeIf { deviceLeash != com.safeshade.platform.LeashState.Unknown }
                ),
                onOpenWay = { route -> navController.navigate(route) },
                listState = deviceListState
            )
        }

        composable(Routes.DEVICE_MODE) {
            val state = liveState.value
            ModePickerScreen(
                state = ModePickerUiState(
                    connection = state.connection,
                    activeMode = state.activeMode
                ),
                onOpenMode = { mode -> navController.navigate(Routes.modeDetail(mode)) },
                onCompare = { navController.navigate(Routes.DEVICE_MODE_COMPARE) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.DEVICE_MODE_DETAIL}/{${Routes.Args.MODE}}",
            arguments = listOf(navArgument(Routes.Args.MODE) { type = NavType.StringType })
        ) { entry ->
            val state = liveState.value
            val mode = entry.arguments?.getString(Routes.Args.MODE)
                ?.let { name -> PersonaMode.entries.firstOrNull { it.name == name } }
            if (mode == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }
            var inFlight by remember { mutableStateOf<PersonaMode?>(null) }
            var ack by remember { mutableStateOf(AckState.IDLE) }
            var confirming by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            // The result is the wearable's actual answer, not the tap. On a
            // dead link the profile is stored and the page reads "stored";
            // on a live one the ack window decides between confirmed and no
            // reply. Nothing here draws a confirmation before it is known.
            fun apply() {
                inFlight = mode
                ack = AckState.PENDING
                scope.launch {
                    val usable = state.connection.isUsable
                    val acked = viewModel.setActiveMode(mode).await()
                    ack = when {
                        acked -> AckState.CONFIRMED
                        usable -> AckState.NO_RESPONSE
                        else -> AckState.IDLE
                    }
                    inFlight = null
                }
            }

            ModeDetailScreen(
                state = ModeDetailUiState(
                    mode = mode,
                    connection = state.connection,
                    activeMode = state.activeMode,
                    inFlightMode = inFlight,
                    ack = ack,
                    confirming = confirming,
                    fallSensitivity = state.safetySettings.fallSensitivity
                ),
                onUse = {
                    // A guardian-locked mode hides mode switching and the whole
                    // safety menu on the wearable, so it is never applied on a
                    // single tap.
                    if (mode.isGuardianLocked) confirming = true else apply()
                },
                onConfirm = {
                    confirming = false
                    apply()
                },
                onCancelConfirm = { confirming = false },
                onOpenFeature = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DEVICE_MODE_COMPARE) {
            val state = liveState.value
            ModeCompareScreen(
                state = ModeCompareUiState(
                    connection = state.connection,
                    activeMode = state.activeMode,
                    fallSensitivity = state.safetySettings.fallSensitivity
                ),
                onOpenMode = { mode -> navController.navigate(Routes.modeDetail(mode)) },
                onBack = { navController.popBackStack() }
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
                // These two used to be `{}`, and the comment above them said a
                // picker was left "for whoever owns the device screens". That
                // is why the medication time could not be changed anywhere in
                // the shipped app: the button existed, the route existed, and
                // the handler did nothing. The screens set the value inline
                // now, so there is no picker to raise and no dialog layer to
                // add to the navigation graph.
                onQuietWindowChange = { startHour, endHour ->
                    quietStartHour = startHour
                    quietEndHour = endHour
                    // Only written through while the feature is on. Off is
                    // carried by nulls on both ends, and writing a window here
                    // would switch it back on behind the user.
                    if (quietHoursEnabled) viewModel.setQuietHours(startHour, endHour)
                },
                onMedicationTimeChange = { hour, minute ->
                    medicationHour = hour
                    medicationMinute = minute
                    if (medicationEnabled) viewModel.setMedicationTime(hour, minute)
                },
                // Still a picker, and still not raised here. Unlike the two
                // above it is not dead: the device name is edited on the
                // device-settings screen itself.
                onEditDeviceName = {},
                onBack = { navController.popBackStack() }
            )
        }


        composable(Routes.DEVICE_LOCATE) {
            val state = liveState.value
            var ringConfirmArmed by rememberSaveable { mutableStateOf(false) }
            val lastFix = state.lastKnownDeviceLocation

            val positioning = com.safeshade.platform.PositioningReadout.from(
                provider = lastFix?.provider,
                accuracyM = lastFix?.accuracyM,
                ageMs = lastFix?.fixAt?.takeIf { it > 0L }?.let { System.currentTimeMillis() - it },
                fromDevice = lastFix?.fromDevice == true
            )
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
                    lastKnownAgeLabel = lastFix?.capturedAt?.takeIf { it > 0L }?.let { agoLabel(it) },
                    positioningSource = positioning.source.label,
                    positioningAccuracy = positioning.accuracyText,
                    positioningAge = positioning.ageText
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
                },
                onBack = { navController.popBackStack() }
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
                onReadSignal = { viewModel.readRssi() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DEVICE_LIGHTS) {
            val state = liveState.value
            LightsScreen(
                state = LightsUiState(
                    connection = state.connection,
                    mode = state.activeMode,
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
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CIRCLE_SMART_HOME) {
            val state = liveState.value
            val hooks by viewModel.smartHooks.collectAsStateWithLifecycle()
            val firings by viewModel.smartFirings.collectAsStateWithLifecycle()
            val context = LocalContext.current
            var google by remember { mutableStateOf<PlatformRow?>(null) }
            var alexa by remember { mutableStateOf<PlatformRow?>(null) }
            var matter by remember { mutableStateOf<PlatformRow?>(null) }
            fun outcomeRow(name: String, o: com.safeshade.platform.ConnectOutcome): PlatformRow = when (o) {
                is com.safeshade.platform.ConnectOutcome.Opened -> PlatformRow(name, LampState.LIVE, "Opened", "Opened ${o.appLabel}. Point a routine there at one of the automations above.")
                is com.safeshade.platform.ConnectOutcome.NotInstalled -> PlatformRow(name, LampState.OFF, "Not installed", "The $name app is not on this phone. Tap again to open the Play Store.")
                is com.safeshade.platform.ConnectOutcome.Failed -> PlatformRow(name, LampState.TRIP, "Failed", o.reason)
            }
            SmartHomeScreen(
                state = SmartHomeUiState(
                    wearerName = state.wearerName,
                    hooks = hooks.orEmpty().map { h ->
                        SmartHookRow(
                            id = h.id,
                            name = h.name,
                            triggerLabel = SmartHookTriggerLabels[h.trigger] ?: h.trigger,
                            providerLabel = SmartHookProviderLabels[h.provider] ?: h.provider,
                            enabled = h.enabled,
                            lastLine = h.lastFiredAt?.let { at ->
                                if (h.lastError != null) "Failed ${agoLabel(at)}: ${h.lastError}"
                                else "Fired ${agoLabel(at)}${h.lastStatusCode?.let { " · $it" } ?: ""}"
                            },
                            lastFailed = h.lastError != null
                        )
                    },
                    googleHome = google ?: SmartHomeUiState().googleHome,
                    alexa = alexa ?: SmartHomeUiState().alexa,
                    matter = matter ?: SmartHomeUiState().matter,
                    recent = firings.orEmpty().sortedByDescending { it.at }.take(8).map { f ->
                        val name = hooks.orEmpty().firstOrNull { it.id == f.hookId }?.name ?: "Automation"
                        "${agoLabel(f.at)} · $name · ${f.error ?: "answered ${f.statusCode ?: "—"}"}"
                    }
                ),
                onAdd = { navController.navigate(Routes.smartHookEdit(null)) },
                onOpen = { id -> navController.navigate(Routes.smartHookEdit(id)) },
                onToggle = { id, enabled -> hooks.orEmpty().firstOrNull { it.id == id }?.let { viewModel.setHookEnabled(it, enabled) } },
                onGoogleHome = {
                    val o = com.safeshade.platform.SmartHomeApps.openGoogleHome(context)
                    if (o is com.safeshade.platform.ConnectOutcome.NotInstalled && google?.word == "Not installed") runCatching { context.startActivity(o.playIntent) }
                    google = outcomeRow("Google Home", o)
                },
                onAlexa = {
                    val o = com.safeshade.platform.SmartHomeApps.openAlexa(context)
                    if (o is com.safeshade.platform.ConnectOutcome.NotInstalled && alexa?.word == "Not installed") runCatching { context.startActivity(o.playIntent) }
                    alexa = outcomeRow("Amazon Alexa", o)
                },
                onMatter = {
                    matter = when (val m = com.safeshade.platform.SmartHomeApps.matterCommissioningAvailable(context)) {
                        com.safeshade.platform.MatterAvailability.Available -> PlatformRow("Matter", LampState.LIVE, "Ready", "This phone's Google Play services can commission Matter devices. Commissioning itself happens in Google Home.")
                        is com.safeshade.platform.MatterAvailability.NoGooglePlayServices -> PlatformRow("Matter", LampState.TRIP, "Unavailable", m.reason)
                        is com.safeshade.platform.MatterAvailability.Unsupported -> PlatformRow("Matter", LampState.OFF, "Unsupported", m.reason)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.CIRCLE_SMART_HOOK_EDIT}?${Routes.Args.HOOK_ID}={${Routes.Args.HOOK_ID}}",
            arguments = listOf(navArgument(Routes.Args.HOOK_ID) { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            val hookId = entry.arguments?.getString(Routes.Args.HOOK_ID).orEmpty().ifBlank { null }
            val hooks by viewModel.smartHooks.collectAsStateWithLifecycle()
            val existing = hooks.orEmpty().firstOrNull { it.id == hookId }
            val scope = rememberCoroutineScope()
            var draft by remember(existing?.id) {
                mutableStateOf(
                    SmartHookEditorUiState(
                        id = existing?.id,
                        name = existing?.name.orEmpty(),
                        trigger = existing?.trigger ?: "fall",
                        provider = existing?.provider ?: "webhook",
                        endpointUrl = existing?.endpointUrl.orEmpty(),
                        secret = existing?.secret.orEmpty()
                    )
                )
            }
            // One id for the whole edit, so a test firing recorded before Save
            // belongs to the hook that is then saved, not to a stranger.
            val newId = rememberSaveable { java.util.UUID.randomUUID().toString() }
            fun toHook() = com.safeshade.data.SmartHomeHook(
                id = existing?.id ?: newId,
                name = draft.name.trim(),
                trigger = draft.trigger,
                provider = draft.provider,
                endpointUrl = draft.endpointUrl.trim(),
                secret = draft.secret.trim().ifBlank { null },
                enabled = existing?.enabled ?: true,
                lastFiredAt = existing?.lastFiredAt,
                lastError = existing?.lastError,
                lastStatusCode = existing?.lastStatusCode
            )
            SmartHookEditorScreen(
                state = draft,
                onName = { draft = draft.copy(name = it) },
                onTrigger = { draft = draft.copy(trigger = it) },
                onProvider = { draft = draft.copy(provider = it) },
                onUrl = { draft = draft.copy(endpointUrl = it, urlError = if (it.isBlank()) null else viewModel.validateHookUrl(it.trim())) },
                onSecret = { draft = draft.copy(secret = it) },
                onTest = {
                    draft = draft.copy(testing = true, testLine = null)
                    scope.launch {
                        val line = when (val r = viewModel.testHook(toHook())) {
                            is com.safeshade.platform.WebhookResult.Delivered -> "Delivered. The address answered ${r.statusCode}."
                            is com.safeshade.platform.WebhookResult.Rejected -> "Rejected with ${r.statusCode}: ${r.bodySnippet.ifBlank { "no body" }}"
                            is com.safeshade.platform.WebhookResult.Unreachable -> "Not reached: ${r.reason}"
                        }
                        draft = draft.copy(testing = false, testLine = line)
                    }
                },
                onSave = {
                    draft = draft.copy(saving = true, saveError = null)
                    scope.launch {
                        runCatching { viewModel.saveHook(toHook(), isNew = existing == null) }
                            .onSuccess { navController.popBackStack() }
                            .onFailure { draft = draft.copy(saving = false, saveError = "Could not save: ${it.message ?: "unknown reason"}") }
                    }
                },
                onDelete = existing?.let { { viewModel.removeHook(it.id); navController.popBackStack() } },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DEVICE_FIRMWARE) {
            val state = liveState.value
            val step by viewModel.otaStep.collectAsStateWithLifecycle()
            val releases by viewModel.firmwareReleases.collectAsStateWithLifecycle()
            val installed by viewModel.installedFirmware.collectAsStateWithLifecycle()
            val fwCloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val fwSession by fwCloudVm.session.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            val model = DeviceModel.fromAdvertisedName(state.deviceSettings.name)
            var checking by remember { mutableStateOf(false) }
            var checkError by remember { mutableStateOf<String?>(null) }
            var lastChecked by rememberSaveable { mutableLongStateOf(0L) }
            var versionNote by remember { mutableStateOf<String?>(null) }
            var downloaded by remember { mutableStateOf(setOf<String>()) }
            var otaJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

            val newest = viewModel.let { releases.orEmpty().maxByOrNull { it.versionCode } }
                ?.takeIf { r -> installed == null || com.safeshade.repo.FirmwareRepository.compareVersions(r.version, installed!!) > 0 }

            FirmwareScreen(
                state = FirmwareUiState(
                    model = model,
                    deviceName = state.deviceSettings.name,
                    connected = state.connection.isUsable,
                    installedVersion = installed,
                    versionNote = versionNote,
                    lastCheckedLabel = lastChecked.takeIf { it > 0L }?.let { agoLabel(it) },
                    checking = checking,
                    checkError = checkError,
                    releases = releases.orEmpty().sortedByDescending { it.versionCode }.map { r ->
                        FirmwareReleaseRow(
                            id = r.id,
                            version = r.version,
                            sizeLabel = "%.1f MB".format(r.byteSize / 1_048_576.0),
                            publishedLabel = r.publishedAt?.let { agoLabel(it) },
                            notes = r.releaseNotes?.takeIf { it.isNotBlank() },
                            mandatory = r.mandatory,
                            downloaded = r.id in downloaded
                        )
                    },
                    newestId = newest?.id,
                    step = step,
                    signedIn = fwSession is CloudSession.SignedIn
                ),
                onCheck = {
                    if (!checking) {
                        checking = true; checkError = null
                        scope.launch {
                            when (val c = viewModel.checkFirmware(model)) {
                                is com.safeshade.repo.CloudCheck.Found -> Unit
                                is com.safeshade.repo.CloudCheck.Failed -> checkError = c.reason
                            }
                            lastChecked = System.currentTimeMillis()
                            checking = false
                        }
                    }
                },
                onAskVersion = {
                    scope.launch {
                        versionNote = when (val v = viewModel.queryFirmwareVersion()) {
                            is com.safeshade.device.OtaProtocol.VersionReply.Version -> null
                            com.safeshade.device.OtaProtocol.VersionReply.AcknowledgedNoVersion -> "The wearable acknowledged the question but reported no version."
                            com.safeshade.device.OtaProtocol.VersionReply.Timeout -> "The wearable did not answer the version question."
                        }
                    }
                },
                onDownload = { id ->
                    val r = releases.orEmpty().firstOrNull { it.id == id }
                    if (r != null) otaJob = scope.launch {
                        val result = viewModel.downloadFirmware(r)
                        if (result is com.safeshade.device.OtaProtocol.OtaStep.Verifying) downloaded = downloaded + id
                    }
                },
                onInstall = { id ->
                    val r = releases.orEmpty().firstOrNull { it.id == id }
                    if (r != null) otaJob = scope.launch { viewModel.installFirmware(r) }
                },
                onCancel = { otaJob?.cancel(); otaJob = null },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DEVICE_LOST) {
            val state = liveState.value
            val permissionsGranted = livePermissions.value
            val lost by viewModel.lostDevices.collectAsStateWithLifecycle()
            val ownSeen by viewModel.ownLastSeen.collectAsStateWithLifecycle()
            val recent by viewModel.sightings.collectAsStateWithLifecycle()
            val lostCloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val lostSession by lostCloudVm.session.collectAsStateWithLifecycle()
            var sweeping by remember { mutableStateOf(false) }
            var lastSweepAt by rememberSaveable { mutableLongStateOf(0L) }
            var reporting by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(sweeping) {
                if (sweeping) { delay(20_000L); sweeping = false }
            }
            val dayAgo = System.currentTimeMillis() - 86_400_000L
            var community by remember { mutableStateOf<Map<String, com.safeshade.cloud.dto.DeviceSightingRow>>(emptyMap()) }
            val lostAddresses = lost.orEmpty().map { it.address }
            LaunchedEffect(lostAddresses, lostSession) {
                if (lostAddresses.isNotEmpty() && lostSession is CloudSession.SignedIn) {
                    community = viewModel.communityLastSeen(lostAddresses)
                }
            }
            LaunchedEffect(reporting, lostSession, recent?.size) {
                if (reporting && lostSession is CloudSession.SignedIn) viewModel.reportSightings()
            }
            LostModeScreen(
                state = LostUiState(
                    devices = state.pairedDevices.map { d ->
                        val mark = lost.orEmpty().firstOrNull { it.address.equals(d.address, ignoreCase = true) }
                        val wearer = state.wearers.firstOrNull { w -> w.deviceAddresses.any { it.equals(d.address, ignoreCase = true) } }
                        LostDeviceRow(
                            address = d.address,
                            name = d.name,
                            label = wearer?.name?.takeIf { it.isNotBlank() }?.let { "$it's ${d.name}" } ?: d.name,
                            lost = mark != null,
                            lostSinceLabel = mark?.let { agoLabel(it.since) },
                            ownLastSeenLabel = ownSeen[d.address.uppercase()]?.let { agoLabel(it) },
                            communityLastSeenLabel = community[d.address.uppercase()]?.let { row ->
                                com.safeshade.cloud.parseServerInstant(row.seenAt)?.toEpochMilli()?.let { agoLabel(it) }
                            },
                            communityLat = community[d.address.uppercase()]?.lat,
                            communityLon = community[d.address.uppercase()]?.lon
                        )
                    },
                    sweeping = sweeping,
                    permissionsGranted = permissionsGranted,
                    signedIn = lostSession is CloudSession.SignedIn,
                    heardOthersToday = recent.orEmpty().count { it.at > dayAgo },
                    reporting = reporting,
                    lastSweepLabel = lastSweepAt.takeIf { it > 0L }?.let { agoLabel(it) }
                ),
                onMarkLost = { address ->
                    val d = state.pairedDevices.firstOrNull { it.address == address }
                    viewModel.markLost(address, d?.name ?: address)
                },
                onMarkFound = { viewModel.markFound(it) },
                onSweep = {
                    viewModel.sweepForSightings()
                    sweeping = true
                    lastSweepAt = System.currentTimeMillis()
                },
                onOpenMap = { lat, lon ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon(SafeShade)"))
                    runCatching { navController.context.startActivity(intent) }
                },
                onReporting = { reporting = it },
                onRequestPermissions = requestPermissions,
                onOpenSignIn = { navController.navigate(Routes.SETTINGS_SIGN_IN) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DEVICE_RIDES) {
            val state = liveState.value
            val rides by viewModel.rides.collectAsStateWithLifecycle()
            val list = rides.orEmpty().sortedByDescending { it.startedAt }
            RidesScreen(
                state = RidesUiState(
                    wearerName = state.wearerName,
                    rides = list.map { r ->
                        RideRow(
                            id = r.id,
                            whenLabel = agoLabel(r.startedAt),
                            distanceKm = r.distanceM / 1000.0,
                            movingMinutes = r.movingSeconds / 60,
                            maxKmh = r.maxSpeedMps * 3.6,
                            samples = r.samples,
                            live = r.endedAt == null
                        )
                    },
                    totalKm = list.sumOf { it.distanceM } / 1000.0,
                    totalMinutes = list.sumOf { it.movingSeconds } / 60,
                    bikeMode = state.activeMode == PersonaMode.BIKE
                ),
                onClear = { viewModel.clearRides() },
                onStartJourney = { navController.navigate(Routes.CIRCLE_JOURNEY) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DEVICE_PAIR) {
            val state = liveState.value
            val permissionsGranted = livePermissions.value
            val context = LocalContext.current
            var chosen by rememberSaveable { mutableStateOf(DeviceModel.S1.name) }
            val foundName = when (val c = state.connection) {
                is ConnectionState.Found -> c.name
                ConnectionState.Ready, ConnectionState.Connected, ConnectionState.Connecting -> state.deviceSettings.name.takeIf { it.isNotBlank() }
                else -> null
            }
            PairScreen(
                state = PairUiState(
                    chosen = DeviceModel.valueOf(chosen),
                    connection = state.connection,
                    permissionsGranted = permissionsGranted,
                    foundName = foundName,
                    foundModel = foundName?.let { DeviceModel.fromAdvertisedName(it) },
                    nfcAvailable = com.safeshade.platform.nfcAvailability(context) is com.safeshade.platform.NfcAvailability.Available
                ),
                onChoose = { chosen = it.name },
                onSearch = { viewModel.connect() },
                onStop = { viewModel.disconnect() },
                onRequestPermissions = requestPermissions,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DEVICE_PAIRED) {
            val state = liveState.value
            val permissionsGranted = livePermissions.value
            val connectedAddress = liveConnectedAddress.value
            var confirmingForget by rememberSaveable { mutableStateOf<String?>(null) }

            PairedDevicesScreen(
                state = PairedDevicesUiState(
                    connection = state.connection,
                    devices = state.pairedDevices,
                    // The real address, not null. This used to be hardcoded
                    // null with a comment saying the model did not record which
                    // device the link was to - so `isConnected` was false for
                    // every row forever, and a device you were actively
                    // connected to still showed "Saved" and a Connect button.
                    connectedAddress = connectedAddress.takeIf { it.isNotBlank() },
                    isScanning = state.connection is ConnectionState.Scanning,
                    permissionsGranted = permissionsGranted,
                    confirmingForget = confirmingForget,
                    lastConnectedLabels = state.pairedDevices.associate {
                        it.address to agoLabel(it.lastConnected)
                    }
                ),
                onPairNew = { navController.navigate(Routes.DEVICE_PAIR) },
                onRequestPermissions = requestPermissions,
                onConnect = { address -> viewModel.connectTo(address) },
                onDisconnect = { viewModel.disconnect() },
                onForgetRequested = { address -> confirmingForget = address },
                onConfirmForget = { address ->
                    viewModel.removePairedDevice(address)
                    confirmingForget = null
                },
                onCancelForget = { confirmingForget = null },
                onBack = { navController.popBackStack() }
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
                onMedicationTimeChange = { hour, minute ->
                    medicationHour = hour
                    medicationMinute = minute
                    if (medicationEnabled) viewModel.setMedicationTime(hour, minute)
                },
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
                onGrantExactAlarms = { openExactAlarmSettings(context) },
                onBack = { navController.popBackStack() }
            )
        }

        // ============================================
        // Settings — reached from the board's top bar
        // ============================================

        composable(Routes.SETTINGS) {
            val state = liveState.value
            val cloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val session by cloudVm.session.collectAsStateWithLifecycle()
            ProfileScreen(
                state = ProfileUiState(
                    ownerName = state.ownerName,
                    ownerAvatarId = state.ownerAvatarId,
                    role = state.role,
                    wearerName = state.deviceSettings.wearerName,
                    wearerAvatarId = state.deviceSettings.wearerAvatarId,
                    deviceName = state.deviceSettings.name,
                    darkMode = state.darkMode,
                    reliabilityIssueCount = reliabilityStatuses(context)
                        .count { it.value == CheckStatus.FAILING },
                    versionName = BuildConfig.VERSION_NAME,
                    account = accountWay(session),
                    people = state.wearers.map { w -> ProfilePerson(w.id, w.name, w.avatarId, wearerDetail(state, w)) }
                ),
                onEditOwner = { navController.navigate(Routes.profileEdit(ProfileTarget.OWNER)) },
                onEditWearer = { navController.navigate(Routes.profileEdit(ProfileTarget.WEARER)) },
                onOpenPerson = { id -> navController.navigate(Routes.personEdit(id)) },
                onAddPerson = { navController.navigate(Routes.personEdit(null)) },
                onOpenWay = { route -> navController.navigate(route) },
                onSelectDarkMode = { viewModel.setDarkMode(it) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.SETTINGS_PROFILE_EDIT}/{${Routes.Args.TARGET}}",
            arguments = listOf(navArgument(Routes.Args.TARGET) { type = NavType.StringType })
        ) { entry ->
            val state = liveState.value
            val target = entry.arguments?.getString(Routes.Args.TARGET)
                ?.let { name -> ProfileTarget.entries.firstOrNull { it.name == name } }
                ?: ProfileTarget.OWNER
            // A Companion is their own wearer, so editing "you" edits both
            // records; a Guardian's wearer is a different person.
            val companion = state.role == UserRole.COMPANION
            val editsWearer = target == ProfileTarget.WEARER || companion
            ProfileEditScreen(
                state = ProfileEditUiState(
                    target = target,
                    name = if (editsWearer) state.deviceSettings.wearerName else state.ownerName,
                    avatarId = if (editsWearer) state.deviceSettings.wearerAvatarId else state.ownerAvatarId
                ),
                onSave = { name, avatarId ->
                    if (editsWearer) viewModel.setWearer(name, avatarId)
                    if (target == ProfileTarget.OWNER || companion) viewModel.setOwner(name, avatarId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }


        composable(Routes.CIRCLE_GUARDIANS) {
            val cloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val cloudState by cloudVm.cloudState.collectAsStateWithLifecycle()
            val session by cloudVm.session.collectAsStateWithLifecycle()
            GuardiansScreen(
                state = GuardiansUiState(
                    signedIn = session is CloudSession.SignedIn,
                    hasCircle = cloudState.hasCircle,
                    members = cloudState.members,
                    invites = cloudState.invites,
                    selfUserId = (session as? CloudSession.SignedIn)?.userId
                ),
                onInvite = { email, role -> cloudVm.invite(email, role) },
                onOpenSignIn = { navController.navigate(Routes.SETTINGS_SIGN_IN) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CIRCLE_HEATMAP) {
            val state = liveState.value
            val location = liveLocation.value
            val cloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val cloudState by cloudVm.cloudState.collectAsStateWithLifecycle()
            val gated = cloudState.effectiveTier == CloudTier.FREE
            var communityOn by rememberSaveable { mutableStateOf(false) }
            var cells by remember { mutableStateOf<List<HeatPoint>>(emptyList()) }
            var cellsStatus by remember { mutableStateOf<String?>(null) }
            var cellsLoading by remember { mutableStateOf(false) }
            val fix = state.lastKnownDeviceLocation ?: location.takeIf { it.isValid }
            // The household's own places: zones and the last fix. Alerts carry
            // a place as text, not a fix, so they cannot be drawn until a fix
            // is stamped on them; nothing is invented for them here.
            val own = buildList {
                state.zones.forEach { add(HeatPoint(it.lat, it.lon, 1, own = true)) }
                fix?.let { add(HeatPoint(it.lat, it.lon, 1, own = true)) }
            }
            val center = fix ?: state.zones.firstOrNull()?.let { LocationState(lat = it.lat, lon = it.lon, isValid = true) }
            LaunchedEffect(communityOn, gated, center?.lat, center?.lon) {
                if (!communityOn || gated || center == null) { cells = emptyList(); cellsStatus = null; return@LaunchedEffect }
                cellsLoading = true
                // About 25 km each way around the centre; the view opens at
                // zoom 13 and a pinch out is still inside it.
                val d = 0.22
                when (val r = cloudVm.heatmapIn(center.lat - d, center.lat + d, center.lon - d, center.lon + d)) {
                    is CloudResult.Ok -> { cells = r.value.map { HeatPoint(it.lat, it.lon, it.count) }; cellsStatus = if (r.value.isEmpty()) "No community cells around here yet. A cell needs five alerts before it is drawn." else null }
                    is CloudResult.Failed -> cellsStatus = r.reason
                    CloudResult.Disabled -> cellsStatus = "This build has no SafeShade Cloud project"
                }
                cellsLoading = false
            }
            HeatmapScreen(
                state = HeatmapUiState(
                    centerLat = center?.lat,
                    centerLon = center?.lon,
                    cells = cells,
                    own = own,
                    communityOn = communityOn,
                    gated = gated,
                    tierLabel = PlanTier.fromKey(cloudState.effectiveTier.wire).label,
                    status = cellsStatus,
                    loading = cellsLoading
                ),
                onToggleCommunity = { communityOn = it },
                onOpenPlan = { navController.navigate(Routes.SETTINGS_PLAN) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CIRCLE_PEOPLE) {
            val state = liveState.value
            PeopleScreen(
                role = state.role,
                people = state.wearers.map { w ->
                    val bound = w.deviceAddresses.isNotEmpty()
                    val live = bound && state.connection.isUsable &&
                        w.deviceAddresses.any { it.equals(connectedAddress, ignoreCase = true) }
                    PersonListRow(
                        id = w.id,
                        name = w.name,
                        avatarId = w.avatarId,
                        detail = wearerDetail(state, w),
                        state = when {
                            live -> LampState.LIVE
                            bound -> LampState.OFF
                            else -> LampState.UNKNOWN
                        },
                        stateLabel = when {
                            live -> "Live"
                            bound -> "Off"
                            else -> "No wearable"
                        },
                        isSelf = w.isSelf
                    )
                },
                onOpen = { id -> navController.navigate(Routes.personEdit(id)) },
                onAdd = { navController.navigate(Routes.personEdit(null)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.CIRCLE_PERSON_EDIT}?${Routes.Args.WEARER_ID}={${Routes.Args.WEARER_ID}}",
            arguments = listOf(navArgument(Routes.Args.WEARER_ID) { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            val state = liveState.value
            val id = entry.arguments?.getString(Routes.Args.WEARER_ID).orEmpty()
            val existing = state.wearers.firstOrNull { it.id == id }
            val wearer = existing ?: Wearer()
            WearerEditorScreen(
                state = WearerEditorUiState(
                    wearer = wearer,
                    isNew = existing == null,
                    pairedDevices = state.pairedDevices,
                    connectedAddress = connectedAddress.takeIf { state.connection.isUsable },
                    medicalFieldsFilled = wearer.medicalId.filledFieldCount,
                    canRemove = existing != null && !wearer.isSelf && state.wearers.size > 1
                ),
                onSave = { edited ->
                    if (existing == null) viewModel.addWearer(edited).await()
                    else viewModel.updateWearer(edited).await()
                },
                onRemove = { viewModel.removeWearer(wearer.id).await() },
                onOpenMedicalId = { navController.navigate(Routes.medicalId(wearer.id)) },
                onOpenPairing = { navController.navigate(Routes.DEVICE_PAIRED) },
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_PLAN) {
            val cloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val session by cloudVm.session.collectAsStateWithLifecycle()
            val cloudState by cloudVm.cloudState.collectAsStateWithLifecycle()
            val planScope = rememberCoroutineScope()
            val billing = remember { PlayBilling(context.applicationContext) }
            DisposableEffect(Unit) { onDispose { billing.close() } }
            var offers by remember { mutableStateOf<List<PlanOffer>?>(null) }
            var offersError by remember { mutableStateOf<String?>(null) }
            var purchasing by remember { mutableStateOf<String?>(null) }
            var purchaseError by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                when (val r = billing.queryPlans()) {
                    is BillingOutcome.Ok -> offers = r.value
                    is BillingOutcome.Failed -> offersError = r.reason
                    BillingOutcome.Cancelled -> Unit
                }
            }
            PlanScreen(
                state = PlanUiState(
                    current = PlanTier.fromKey(cloudState.effectiveTier.wire),
                    overridden = cloudState.devTierOverride != null,
                    offers = offers,
                    offersError = offersError,
                    purchasing = purchasing,
                    purchaseError = purchaseError,
                    signedIn = session is CloudSession.SignedIn,
                    showDeveloper = BuildConfig.DEBUG
                ),
                onChoose = { tier ->
                    val productId = tier.productId ?: return@PlanScreen
                    val activity = context as? Activity ?: return@PlanScreen
                    purchasing = productId
                    purchaseError = null
                    planScope.launch {
                        when (val r = billing.purchase(activity, productId)) {
                            is BillingOutcome.Ok -> Unit // the subscription row, once synced, is what changes the tier
                            is BillingOutcome.Failed -> purchaseError = r.reason
                            BillingOutcome.Cancelled -> Unit
                        }
                        purchasing = null
                    }
                },
                onOpenSignIn = { navController.navigate(Routes.SETTINGS_SIGN_IN) },
                onSetOverride = { tier ->
                    planScope.launch { cloudVm.setDevTierOverride(tier?.let { CloudTier.fromWire(it.key) }) }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_EMAILS) {
            val cloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val session by cloudVm.session.collectAsStateWithLifecycle()
            val cloudState by cloudVm.cloudState.collectAsStateWithLifecycle()
            EmailsScreen(
                state = EmailsUiState(
                    signedIn = session is CloudSession.SignedIn,
                    email = (session as? CloudSession.SignedIn)?.email,
                    prefs = cloudState.emailPreferences
                ),
                onChange = { cloudVm.setEmailPreferences(it) },
                onSendWeeklyNow = { cloudVm.sendWeeklyReportNow() },
                onOpenSignIn = { navController.navigate(Routes.SETTINGS_SIGN_IN) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_PRIVACY) {
            val state = liveState.value
            val cloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val session by cloudVm.session.collectAsStateWithLifecycle()
            val cloudState by cloudVm.cloudState.collectAsStateWithLifecycle()
            val privacyScope = rememberCoroutineScope()
            PrivacyScreen(
                state = PrivacyUiState(
                    signedIn = session is CloudSession.SignedIn,
                    sharePlaces = cloudState.shareAlertPlaces,
                    simStored = state.devicePhoneNumber.isNotBlank(),
                    zoneCount = state.zones.size,
                    voiceNoteCount = state.voiceNotes.size
                ),
                onSharePlacesChange = { on -> privacyScope.launch { cloudVm.setShareAlertPlaces(on) } },
                onOpenAccount = { navController.navigate(Routes.SETTINGS_ACCOUNT) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_ACCOUNT) {
            val cloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val session by cloudVm.session.collectAsStateWithLifecycle()
            val sync by cloudVm.syncSummary.collectAsStateWithLifecycle()
            val cloudState by cloudVm.cloudState.collectAsStateWithLifecycle()
            val accountScope = rememberCoroutineScope()
            AccountScreen(
                session = session,
                sync = sync,
                onSyncNow = { accountScope.launch { cloudVm.syncNow() } },
                onSignOut = { cloudVm.signOut() },
                onDeleteAccount = { cloudVm.deleteAccount() },
                onOpenSignIn = { navController.navigate(Routes.SETTINGS_SIGN_IN) },
                onOpenPlan = { navController.navigate(Routes.SETTINGS_PLAN) },
                onOpenEmails = { navController.navigate(Routes.SETTINGS_EMAILS) },
                emailsLabel = cloudState.emailPreferences?.let { p ->
                    val on = listOf(p.alerts, p.circle, p.account, p.weeklyReport).count { it }
                    when (on) { 4 -> "All on"; 0 -> "All off"; else -> "$on of 4 on" }
                },
                planLabel = PlanTier.fromKey(cloudState.effectiveTier.wire).label,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_SIGN_IN) {
            val cloudVm: CloudViewModel = viewModel(factory = CloudViewModel.Factory)
            val session by cloudVm.session.collectAsStateWithLifecycle()
            SignInScreen(
                actions = SignInActions(
                    requestCode = { cloudVm.requestEmailCode(it) },
                    verifyCode = { email, code -> cloudVm.verifyEmailCode(email, code) },
                    signInWithPassword = { email, pw -> cloudVm.signInWithPassword(email, pw) },
                    signUpWithPassword = { email, pw -> cloudVm.signUpWithPassword(email, pw) },
                    signInWithGoogle = if (cloudVm.googleAvailable) { ctx -> cloudVm.signInWithGoogle(ctx) } else null
                ),
                signedIn = session is CloudSession.SignedIn,
                onSignedIn = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
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
                onCancelConfirm = { confirming = null },
                onBack = { navController.popBackStack() }
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
                onSendTestAlert = {},
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_ABOUT) {
            AboutScreen(
                state = AboutUiState(
                    versionName = BuildConfig.VERSION_NAME,
                    versionCode = BuildConfig.VERSION_CODE.toString(),
                    buildLabel = if (BuildConfig.DEBUG) "Debug" else "Release",
                    firmwareTarget = FIRMWARE_TARGET
                ),
                onBack = { navController.popBackStack() }
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
                onOpenKitGallery = { navController.navigate(Routes.SETTINGS_KIT) },
                onReplayOnboarding = { viewModel.setOnboardingSeen(false) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS_KIT) {
            KitGallery()
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
    // A tab switch fades; everything else slides. `from` is deliberately not
    // required to be a bottom route: since the bar stopped restoring saved
    // stacks, leaving a tab from three levels down lands on the *next tab's
    // root*, so the move is `safety/contacts -> board` and the old
    // `from.isBottom() && to.isBottom()` missed it and slid.
    //
    // The comparison goes through `tabForRoute` rather than a prefix test on
    // `to`, and that is load-bearing: the obvious shorthand
    // `from?.startsWith("$to/") != true` gets system-back from `settings` to
    // `device` wrong, because "settings" does not start with "device/" and it
    // would fade a pop. `tabForRoute` knows settings belongs to Device.
    to.isBottom() && tabForRoute(from) != tabForRoute(to) -> Motion.FADE_THROUGH
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

/** "now", "4 min", "7 h", "3 d": for a mono readout that has no room for a sentence. */
private fun agoShort(at: Long, now: Long = System.currentTimeMillis()): String {
    val m = ((now - at) / 60_000L).coerceAtLeast(0L)
    return when {
        m < 1 -> "now"
        m < 60 -> "$m min"
        m < 48 * 60 -> "${m / 60} h"
        else -> "${m / (60 * 24)} d"
    }
}

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
        is ConnectionState.Disconnected -> "Not connected"
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
            icon = SafeShadeIcons.SafeZone,
            route = Routes.CIRCLE_ZONES
        ),
        BoardWay(
            key = "checkin",
            name = "Check-in",
            state = if (checkInOpen) LampState.ATTENTION else LampState.OFF,
            stateLabel = if (checkInOpen) "Waiting" else "None open",
            icon = SafeShadeIcons.CheckIn,
            route = Routes.CIRCLE_CHECKIN
        ),
        BoardWay(
            key = "journey",
            name = "Journey",
            state = if (journeyRunning) LampState.LIVE else LampState.OFF,
            stateLabel = if (journeyRunning) "Running" else "None",
            icon = SafeShadeIcons.WalkingPerson,
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
            icon = SafeShadeIcons.History,
            route = Routes.SAFETY_TRIPS
        ),
        BoardWay(
            key = "contacts",
            name = "Emergency contacts",
            // Amber rather than off when empty: an emergency contact list with
            // nothing in it is the single most common reason the whole product
            // does nothing when it matters.
            state = if (contacts.isEmpty()) LampState.ATTENTION else LampState.LIVE,
            stateLabel = if (contacts.isEmpty()) "None" else "${contacts.size} set",
            icon = SafeShadeIcons.EmergencyContacts,
            route = Routes.SAFETY_CONTACTS
        ),
        BoardWay(
            key = "medical",
            name = "Medical ID",
            state = if (state.medicalId.isUsable) LampState.LIVE else LampState.ATTENTION,
            stateLabel = if (state.medicalId.isUsable) "Filled in" else "Incomplete",
            detail = "${state.medicalId.filledFieldCount} of 11 fields",
            icon = SafeShadeIcons.MedicalId,
            route = Routes.SAFETY_MEDICAL
        ),
        BoardWay(
            key = "link",
            name = "Paired devices",
            state = linkLamp(state.connection),
            stateLabel = linkLabel(state.connection),
            detail = "${state.pairedDevices.size} remembered",
            icon = SafeShadeIcons.PairedDevices,
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
/**
 * Sends as the current role and reports the outcome.
 *
 * This used to be fire-and-forget, which is how a screen came to draw a
 * confirmation tick for a send that had failed. It suspends now; the view
 * model owns the coroutine underneath, so a caller that is cancelled loses
 * only its own acknowledgement.
 */
private suspend fun sendAsRole(
    viewModel: SafeShadeViewModel,
    role: UserRole,
    text: String
): SendResult {
    val clean = text.trim()
    if (clean.isEmpty()) return SendResult.Failed("Nothing to send")
    return viewModel.sendMessage(role, clean).await()
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

/**
 * A coordinate as a person should see it.
 *
 * Six decimal places is about 11cm, which is far finer than any consumer GPS
 * and far finer than a safe zone measured in hundreds of metres. A raw
 * `Double.toString` put "22.575875335849915" in a text field the user is meant
 * to be able to read and correct - seventeen digits of false precision from a
 * single tap on a map.
 */
private fun coordinate(value: Double): String = "%.6f".format(value).trimEnd('0').trimEnd('.')

/**
 * The Profile page's cloud row, from the session and nothing else. "Saved on
 * this phone only" is what a guest sees, because it is true, and it is not
 * an error.
 */
private fun accountWay(session: CloudSession): AccountWay = when (session) {
    is CloudSession.SignedIn -> AccountWay(
        state = LampState.LIVE,
        label = "Signed in",
        detail = session.email ?: "Signed in",
        tappable = true
    )
    CloudSession.Guest -> AccountWay(
        state = LampState.OFF,
        label = "Off",
        detail = "Saved on this phone only. Tap to sign in.",
        tappable = true
    )
    CloudSession.Loading -> AccountWay(
        state = LampState.UNKNOWN,
        label = "\u2026",
        detail = "Reading the saved session",
        tappable = false
    )
    CloudSession.Disabled -> AccountWay(
        state = LampState.OFF,
        label = "Off",
        detail = "This build has no SafeShade Cloud project",
        tappable = false
    )
}

/** "Wears SafeShade S1 · Elderly", or what is true instead. */
private fun wearerDetail(state: AppState.Ready, w: Wearer): String {
    val device = state.pairedDevices.firstOrNull { d -> w.deviceAddresses.any { it.equals(d.address, ignoreCase = true) } }
    val wears = device?.let { "Wears ${it.name.ifBlank { "a wearable" }}" } ?: "No wearable bound"
    return "$wears · ${w.activeMode.label}"
}

/**
 * One wearer's plate on the family dashboard, from what this phone knows.
 *
 * Battery is only ever the connected wearable's own figure, and only for the
 * person bound to that address; every other plate shows a dash, because the
 * phone has no number for them and will not invent one. The place is the
 * last fix that reached this phone; it is the household's, not the person's,
 * until fixes are stamped with a wearer, so it is shown on the bound plate
 * only. The last alert is the newest trip stamped with this person, falling
 * back to the newest unstamped one for the primary wearer alone.
 */
private fun wearerCard(
    state: AppState.Ready,
    w: Wearer,
    connectedAddress: String,
    lastFix: LocationState?
): WearerCard {
    val bound = w.deviceAddresses.isNotEmpty()
    val isConnectedOne = state.connection.isUsable &&
        w.deviceAddresses.any { it.equals(connectedAddress, ignoreCase = true) }
    val primary = state.wearers.firstOrNull()?.id == w.id
    val trip = state.tripHistory
        .filter { it.wearerId == w.id || (primary && it.wearerId == null) }
        .maxByOrNull { it.timestamp }
    return WearerCard(
        id = w.id,
        name = w.name,
        avatarId = w.avatarId,
        modeLabel = if (bound) w.activeMode.label else "No wearable bound",
        linkState = when {
            isConnectedOne -> LampState.LIVE
            bound -> LampState.OFF
            else -> LampState.UNKNOWN
        },
        linkLabel = when {
            isConnectedOne -> "Connected"
            bound -> "Not connected"
            else -> "No wearable"
        },
        batteryLabel = if (isConnectedOne && state.telemetry.isRealData) "${state.telemetry.batteryLevel} %" else null,
        // Mono readouts, so an age not a sentence; the place itself is on
        // the Where plate below, where there is room for it.
        placeLabel = if ((isConnectedOne || primary) && lastFix != null && lastFix.capturedAt > 0L) {
            "${agoShort(lastFix.capturedAt)} ago"
        } else null,
        lastAlertLabel = trip?.let { "${it.kind.short} · ${agoShort(it.timestamp)}" },
        canCall = bound && state.devicePhoneNumber.isNotBlank()
    )
}

/** One word for a readout: "Fall", "SOS", "Button". */
private val TripKind.short: String
    get() = when (this) {
        TripKind.FALL -> "Fall"
        TripKind.SOS, TripKind.PHONE_SOS -> "SOS"
        TripKind.MISSED_CHECKIN -> "Check-in"
        TripKind.ZONE_EXIT -> "Zone"
        TripKind.JOURNEY_OVERDUE -> "Journey"
        TripKind.QUIET_WORD -> "Quiet word"
    }

/** "72 bpm · 98% · 36.6°" for the hub's Vitals way; only the fields that were measured. */
private fun vitalsLine(sample: com.safeshade.data.VitalsSample): String = listOfNotNull(
    sample.heartRateBpm?.let { "$it bpm" },
    sample.spo2Percent?.let { "$it%" },
    sample.tempC?.let { "%.1f°".format(it) }
).joinToString(" · ") + " · " + agoLabel(sample.at)

/** One flag as the row's name. The word order is what the Vitals page keys its lamps on. */
private fun vitalsFlagLine(
    flag: com.safeshade.data.VitalsFlag,
    sample: com.safeshade.data.VitalsSample?,
    t: com.safeshade.data.VitalsThresholds
): String = when (flag) {
    com.safeshade.data.VitalsFlag.HR_LOW -> "Heart rate ${sample?.heartRateBpm ?: "—"}, below ${t.hrLow}"
    com.safeshade.data.VitalsFlag.HR_HIGH -> "Heart rate ${sample?.heartRateBpm ?: "—"}, above ${t.hrHigh}"
    com.safeshade.data.VitalsFlag.SPO2_LOW -> "Oxygen ${sample?.spo2Percent ?: "—"}%, below ${t.spo2Low}%"
    com.safeshade.data.VitalsFlag.TEMP_HIGH -> "Temperature ${sample?.tempC?.let { "%.1f°".format(it) } ?: "—"}, above %.1f°".format(t.tempHigh)
}

/** One evidence clip as a row, with where the file is as its state. */
private fun evidenceRow(clip: com.safeshade.data.EvidenceClip, state: AppState.Ready): EvidenceClipRow {
    val cause = when {
        clip.alertId == null -> "Test recording"
        state.tripHistory.firstOrNull { it.id == clip.alertId }?.kind == TripKind.PHONE_SOS -> "After an SOS"
        else -> "After a fall"
    }
    val (where, lamp) = when (clip.upload) {
        com.safeshade.data.EvidenceUploadState.LOCAL_ONLY -> "On this phone" to LampState.OFF
        com.safeshade.data.EvidenceUploadState.QUEUED -> "Sending to SafeShade Cloud" to LampState.ATTENTION
        com.safeshade.data.EvidenceUploadState.UPLOADED -> "On SafeShade Cloud" to LampState.LIVE
        com.safeshade.data.EvidenceUploadState.FAILED -> "Not sent: ${clip.uploadReason ?: "the upload did not finish"}" to LampState.TRIP
    }
    return EvidenceClipRow(
        id = clip.id,
        timeLabel = agoLabel(clip.capturedAt),
        durationMs = clip.durationMs,
        causeLabel = cause,
        whereLabel = where,
        whereState = lamp,
        onThisPhone = true
    )
}
