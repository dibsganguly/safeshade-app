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
import com.safeshade.repo.SafetyRepository
import com.safeshade.repo.ZoneRepository
import com.safeshade.sendSmsText
import kotlinx.coroutines.CoroutineScope
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

    val deviceRepository = DeviceRepository(
        link = link,
        prefs = preferences,
        scope = scope,
        mtuProvider = LinkFactory.mtuProvider(link)
    )

    val profileRepository = ProfileRepository(preferences, link, scope)

    val safetyRepository = SafetyRepository(preferences, link, scope)

    val messagingRepository = MessagingRepository(
        prefs = preferences,
        link = link,
        scope = scope,
        // The SMS transport is injected as a lambda so MessagingRepository holds
        // no Context and stays unit-testable without Robolectric.
        sendSms = { phone, body -> sendSmsText(appContext, phone, body) }
    )

    val zoneRepository = ZoneRepository(preferences, link, geofenceManager, scope)

    val journeyRepository = JourneyRepository(preferences, safetyRepository, scope)

    val appStateRepository = AppStateRepository(
        device = deviceRepository,
        profileRepo = profileRepository,
        safetyRepo = safetyRepository,
        messagingRepo = messagingRepository,
        zoneRepo = zoneRepository,
        journeyRepo = journeyRepository,
        scope = scope
    )

    init {
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
