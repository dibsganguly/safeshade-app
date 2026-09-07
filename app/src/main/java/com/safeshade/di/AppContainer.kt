package com.safeshade.di

import android.content.Context
import com.safeshade.GeofenceManager
import com.safeshade.data.SafeShadePreferences
import com.safeshade.device.DeviceLink
import com.safeshade.device.LinkFactory
import com.safeshade.repo.AppStateRepository
import com.safeshade.repo.DeviceRepository
import com.safeshade.repo.JourneyRepository
import com.safeshade.repo.MessagingRepository
import com.safeshade.repo.ProfileRepository
import com.safeshade.repo.LateBoundSyncHooks
import com.safeshade.repo.SafetyRepository
import com.safeshade.repo.VoiceNoteRepository
import com.safeshade.repo.ZoneRepository
import com.safeshade.service.EscalationRunner
import com.safeshade.service.WearableWatch
import java.io.File
import com.safeshade.sendSmsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manual dependency injection: one object graph, constructed once, owned by the
 * `Application`.
 *
 * No Hilt, deliberately. Hilt means adding a Gradle plugin and a KSP processor
 * to a build already on AGP 9.1.1 with Kotlin 2.2.10, and a plugin-compatibility
 * failure there costs an afternoon of build archaeology to save perhaps forty
 * lines of constructor calls. The graph is this file; when it stops fitting on
 * one screen, that is the moment to reconsider.
 *
 * ### Construction order matters
 *
 * The dependencies below are not alphabetical. [JourneyRepository] escalates by
 * recording a trip through [SafetyRepository], and both [DeviceRepository] and
 * [ZoneRepository] start collectors in their `init` that touch the link. So the
 * order is: preferences and link first, then the repositories that only read
 * them, then the ones that depend on those, and [AppStateRepository] last
 * because it combines all of the others.
 *
 * ### The scope
 *
 * Every repository takes the application scope. Not one of them may use a
 * `viewModelScope`: fall alerts, geofence forwarding, check-in escalation and
 * journey overdue all have to fire with no ViewModel and no Activity alive —
 * that is precisely when they matter.
 */
class AppContainer(
    context: Context,
    /** Application-lifetime scope, owned by `SafeShadeApplication`. */
    val scope: CoroutineScope
) {

    private val appContext: Context = context.applicationContext

    val preferences = SafeShadePreferences(appContext)

    val geofenceManager = GeofenceManager(appContext)

    /**
     * The link.
     *
     * Which implementation this is depends on the build variant, not on a
     * runtime flag: `LinkFactory` has a debug twin and a release twin at the
     * same fully-qualified name. See either file for why.
     */
    val link: DeviceLink = LinkFactory.create(appContext, scope)

    /**
     * What the repositories tell the cloud when they write.
     *
     * Late-bound because `CloudContainer` is the **last** field of this class,
     * and that position is the enforcement of cloud being strictly additive
     * (handoff7 section 2): local DataStore stays the source of truth, and
     * nothing can quietly make the cloud a dependency of a repository by
     * reordering a constructor. So the repositories are handed this now, and it
     * is pointed at the real implementation in `init`.
     *
     * Until then it does nothing, which is the correct behaviour for the
     * handful of writes that can happen during `Application.onCreate`.
     */
    private val syncHooks = LateBoundSyncHooks()

    val deviceRepository = DeviceRepository(
        link = link,
        prefs = preferences,
        scope = scope,
        mtuProvider = LinkFactory.mtuProvider(link)
    )

    val profileRepository = ProfileRepository(preferences, link, scope, syncHooks)

    val safetyRepository = SafetyRepository(preferences, link, scope, syncHooks)

    val messagingRepository = MessagingRepository(
        prefs = preferences,
        link = link,
        scope = scope,
        // The SMS transport is injected as a lambda so MessagingRepository holds
        // no Context and stays unit-testable without Robolectric.
        sendSms = { phone, body -> sendSmsText(appContext, phone, body) },
        hooks = syncHooks,
        // The quiet word: a match in an incoming message becomes an alert of
        // its own kind, recorded like any other so the ladder and the log see it.
        raiseAlert = { alert -> safetyRepository.record(alert) }
    )

    val zoneRepository =
        ZoneRepository(preferences, link, geofenceManager, scope, syncHooks)

    val journeyRepository = JourneyRepository(preferences, safetyRepository, scope)

    /** The ride log: distance, time moving and top speed off the phone's own fixes, per journey. */
    val rideLogRepository = com.safeshade.repo.RideLogRepository(
        com.safeshade.data.DataStoreRideLogStore(appContext), scope
    )

    val voiceNoteRepository = VoiceNoteRepository(
        prefs = preferences,
        scope = scope,
        voiceDir = File(appContext.filesDir, "voice"),
        hooks = syncHooks
    )

    /**
     * The escalation ladder and the wearable watch: two things that act on
     * their own clock, after the repositories they read and before the UI.
     * Neither is started here; see [init], which is the one place things start.
     */
    val escalationRunner = EscalationRunner(
        appContext = appContext,
        prefs = preferences,
        safety = safetyRepository,
        link = link,
        scope = scope
    )

    val wearableWatch = WearableWatch(
        appContext = appContext,
        prefs = preferences,
        device = deviceRepository,
        scope = scope
    )

    /**
     * Vitals: the newest heart rate, blood oxygen and temperature, from the
     * wearable's telemetry or from Health Connect on this phone. Its own
     * DataStore file, so the store's write rate (a sample a minute while a
     * sensor reports) never contends with `safeshade_prefs`.
     */
    val healthConnectVitals = com.safeshade.platform.HealthConnectVitals(appContext)

    val vitalsRepository = com.safeshade.repo.VitalsRepository(
        store = com.safeshade.data.DataStoreVitalsStore(appContext),
        scope = scope,
        hooks = syncHooks
    )

    /**
     * Evidence: the microphone after a fall or an SOS. Its settings are hoisted
     * to a hot state so the repository's opt-in check can answer synchronously
     * at the moment a clip lands.
     */
    private val evidenceStore = com.safeshade.data.DataStoreEvidenceStore(appContext)

    val evidenceSettings: kotlinx.coroutines.flow.StateFlow<com.safeshade.data.EvidenceSettings> =
        evidenceStore.settings.stateIn(scope, SharingStarted.Eagerly, com.safeshade.data.EvidenceSettings())

    val evidenceRepository = com.safeshade.repo.EvidenceRepository(
        store = evidenceStore,
        scope = scope,
        evidenceDir = File(appContext.filesDir, com.safeshade.platform.EvidenceRecorder.DIR_NAME),
        hooks = syncHooks,
        uploadOptIn = { evidenceSettings.value.uploadToCloud }
    )

    /**
     * Sightings: other SafeShades heard on the radio, and which of this phone's
     * own wearables are marked lost. Own addresses come from the paired list,
     * which is DataStore-backed and empty for the first moments of a cold
     * start; the repository also treats the connected address as its own, and
     * the sweep is only ever started from a screen, by which time the list has
     * loaded.
     */
    val sightingsRepository = com.safeshade.repo.SightingsRepository(
        link = link,
        store = com.safeshade.data.DataStoreSightingsStore(appContext),
        scope = scope,
        ownAddresses = { profileRepository.pairedDevices.value.orEmpty().map { it.address }.toSet() },
        location = {
            com.safeshade.service.LastKnownLocation.state.value?.takeIf { it.isValid }?.let { fix ->
                Triple(fix.lat, fix.lon, (fix.accuracyM ?: 0f).toDouble())
            }
        }
    )

    /** Smart home: the automations and the runner that fires them on the real signals. */
    val smartHomeRepository = com.safeshade.repo.SmartHomeRepository(
        store = com.safeshade.data.DataStoreSmartHomeStore(appContext),
        scope = scope,
        syncHooks = syncHooks
    )

    /** One HTTP client for the webhooks and the firmware downloads. */
    val httpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient()

    val smartHomeRunner = com.safeshade.service.SmartHomeRunner(
        repository = smartHomeRepository,
        safety = safetyRepository,
        zones = zoneRepository,
        lowBattery = wearableWatch.state.map { it.lowBatteryAlerted },
        client = httpClient,
        scope = scope,
        wearerName = { (appStateRepository.state.value as? com.safeshade.repo.AppState.Ready)?.selectedWearer?.name.orEmpty() }
    )

    val appStateRepository = AppStateRepository(
        device = deviceRepository,
        profileRepo = profileRepository,
        safetyRepo = safetyRepository,
        messagingRepo = messagingRepository,
        zoneRepo = zoneRepository,
        journeyRepo = journeyRepository,
        voiceNoteRepo = voiceNoteRepository,
        scope = scope
    )

    /**
     * The cloud graph: client, auth, outbox, sync engine.
     *
     * Last, and after [appStateRepository], for the same reason that one is
     * last: it is the newest and most peripheral thing in the graph, nothing
     * above depends on it, and on a build with no Supabase project configured
     * it is inert by construction (see [com.safeshade.cloud.CloudContainer]).
     * Putting it earlier would suggest something below it needs it, and nothing
     * does.
     */
    val cloud = com.safeshade.cloud.CloudContainer(
        appContext = appContext,
        scope = scope,
        repositories = com.safeshade.cloud.CloudContainer.Repositories(
            profiles = profileRepository,
            safety = safetyRepository,
            messaging = messagingRepository,
            zones = zoneRepository,
            voice = voiceNoteRepository,
            vitals = vitalsRepository,
            evidence = evidenceRepository,
            evidenceDir = File(appContext.filesDir, com.safeshade.platform.EvidenceRecorder.DIR_NAME),
            smartHome = smartHomeRepository
        )
    )

    /**
     * Firmware: releases from the public table, images from the public bucket,
     * the install over the link. After [cloud] because it reads through the
     * cloud client; the adapter keeps the repository's own small interface.
     */
    val firmwareRepository = com.safeshade.repo.FirmwareRepository(
        source = object : com.safeshade.repo.FirmwareSource {
            private val firmwareCloud = com.safeshade.cloud.FirmwareCloud(cloud.client)
            override suspend fun releases(model: String): List<com.safeshade.data.FirmwareRelease> =
                when (val r = firmwareCloud.releases(model)) {
                    is com.safeshade.cloud.CloudResult.Ok -> r.value.mapNotNull { row ->
                        val id = row.id ?: return@mapNotNull null
                        com.safeshade.data.FirmwareRelease(
                            id = id,
                            model = row.model ?: model,
                            version = row.version ?: return@mapNotNull null,
                            versionCode = row.versionCode ?: 0,
                            storagePath = row.storagePath ?: return@mapNotNull null,
                            sha256 = row.sha256 ?: return@mapNotNull null,
                            byteSize = row.byteSize ?: 0L,
                            releaseNotes = row.releaseNotes,
                            mandatory = row.mandatory ?: false,
                            publishedAt = com.safeshade.cloud.parseServerInstant(row.publishedAt)?.toEpochMilli()
                        )
                    }
                    is com.safeshade.cloud.CloudResult.Failed -> throw java.io.IOException(r.reason)
                    com.safeshade.cloud.CloudResult.Disabled -> throw java.io.IOException("SafeShade Cloud is not configured on this build.")
                }
            override suspend fun downloadUrl(storagePath: String): String? = firmwareCloud.downloadUrl(storagePath)
        },
        store = com.safeshade.data.DataStoreFirmwareStore(appContext),
        downloader = com.safeshade.platform.OkHttpFirmwareDownloader(httpClient),
        link = link,
        scope = scope,
        filesDir = appContext.filesDir
    )

    /** Community sightings: reports what this phone hears, and asks where its own wearables were last heard. */
    val sightingsCloud = com.safeshade.cloud.SightingsCloud(cloud.client)

    init {
        // The last wire in the graph, and it has to be here: the repositories
        // were built before the cloud existed. See [syncHooks].
        syncHooks.bind(cloud.syncHooks)

        // The two watchers start once the whole graph exists: the runner
        // observes the safety repository's active alert and the watch polls the
        // device repository, and both are pure observers of state built above.
        escalationRunner.start()
        wearableWatch.start()
        smartHomeRunner.start()
        // The journey service reports fixes to a process-wide sink; point it here.
        com.safeshade.service.RideLogSink.attach(rideLogRepository)

        /*
         * The Quick Settings tile and the home-screen widget read the app's
         * state through two process-wide feeds. Both only ever open the app;
         * neither fires anything. Battery is null unless a real reading has
         * arrived, so the widget draws a dash rather than a zero.
         */
        scope.launch {
            appStateRepository.state.map { st ->
                val ready = st as? com.safeshade.repo.AppState.Ready
                com.safeshade.widget.WidgetSnapshot(
                    wearerName = ready?.let { r -> r.selectedWearer?.name?.takeIf { it.isNotBlank() } ?: r.deviceSettings.wearerName.takeIf { it.isNotBlank() } },
                    linkWord = when {
                        ready == null -> "—"
                        ready.connection is com.safeshade.device.ConnectionState.Ready -> "Connected"
                        else -> "Not connected"
                    },
                    batteryPercent = ready?.telemetry?.takeIf { it.isRealData }?.batteryLevel,
                    tripLine = ready?.activeAlert?.kind?.label
                        ?: ready?.journey?.takeIf { it.state == com.safeshade.data.JourneyState.ACTIVE }?.let { "Journey to ${it.label}" },
                    alertOpen = ready?.activeAlert != null
                )
            }
                // Telemetry lands in Ready about once a second while a wearable
                // is connected; the widget is rewritten only when its words change.
                .distinctUntilChanged()
                .collect { snapshot ->
                    com.safeshade.platform.SosTileState.update(snapshot.alertOpen)
                    runCatching { com.safeshade.widget.WidgetFeed.publish(appContext, snapshot) }
                }
        }

        // The evidence service runs in this process and hands each finished
        // recording here; a failure is left on the service's own state for the
        // page to show, since there is nothing to store for it.
        com.safeshade.service.EvidenceService.onRecorded = { result, alertId ->
            if (result is com.safeshade.platform.EvidenceRecordingResult.Recorded) {
                scope.launch {
                    evidenceRepository.add(
                        file = result.file.name,
                        durationMs = result.durationMs,
                        byteSize = result.bytes,
                        alertId = alertId
                    )
                }
            }
        }

        /*
         * Vitals off the link. The wearable's telemetry arrives about once a
         * second; a sample a minute is plenty for a history, and anything the
         * parser could not believe has already become null upstream, so a
         * payload with no vitals field records nothing at all.
         */
        scope.launch {
            var lastDeviceSampleAt = 0L
            deviceRepository.telemetry.collect { t ->
                if (!t.hasVitals) return@collect
                val now = System.currentTimeMillis()
                if (now - lastDeviceSampleAt < 60_000L) return@collect
                lastDeviceSampleAt = now
                vitalsRepository.record(
                    com.safeshade.data.VitalsSample(
                        at = now,
                        heartRateBpm = t.heartRateBpm,
                        spo2Percent = t.spo2Percent,
                        tempC = t.skinTempC,
                        source = com.safeshade.data.VitalsSample.SOURCE_DEVICE
                    )
                )
            }
        }

        /*
         * Migration runs here, once, on the application scope — never inside a
         * Flow operator.
         *
         * Calling `dataStore.edit` from within a `map` on `dataStore.data`
         * re-triggers that very read, and the resulting write -> emit -> write
         * loop spins until something gives out.
         *
         * It is not ordered against the repositories' own first reads, and it
         * does not need to be: the migration only *seeds* `profile_json_v1`
         * when it is absent, and a repository that reads the unseeded value
         * first gets the same defaults the migration is about to write. The one
         * observable effect is a second emission on a v1 install's first
         * launch, which lands while the UI is still on `AppState.Loading`.
         */
        scope.launch { preferences.migrateIfNeeded() }
    }
}
