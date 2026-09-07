package com.safeshade.service

import com.safeshade.data.SmartHomeHook
import com.safeshade.data.SmartHomeProviders
import com.safeshade.data.SmartHomeTriggers
import com.safeshade.data.TripKind
import com.safeshade.platform.SmartHomeApps
import com.safeshade.platform.SmartHomeEvent
import com.safeshade.platform.WebhookResult
import com.safeshade.repo.SafetyRepository
import com.safeshade.repo.SmartHomeRepository
import com.safeshade.repo.ZoneRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

/**
 * Turns what happens to the wearer into the calls their house is waiting for.
 *
 * ### Where each trigger's signal actually comes from
 *
 * Every one of the six triggers in `smart_home_hooks.trigger` has a real
 * source in this app. None of them is inferred, and none is left to a timer
 * that pretends:
 *
 * - `fall` - `SafetyRepository.activeAlert` with `TripKind.FALL`.
 * - `sos` - the same flow with `TripKind.SOS` (the button on the wearable) or
 *   `TripKind.PHONE_SOS` (the control in this app), or `TripKind.QUIET_WORD`
 *   (the spoken code word).
 * - `check_in_missed` - the same flow with `TripKind.MISSED_CHECKIN`, which
 *   `SafetyRepository.escalateOverdueCheckIns` records when a deadline passes.
 * - `zone_enter` / `zone_exit` - `ZoneRepository.lastTransition`, which the
 *   geofence broadcast feeds.
 * - `low_battery` - [lowBattery], which `AppContainer` supplies from
 *   `WearableWatch.state.lowBatteryAlerted`.
 *
 * Zone crossings are taken from `lastTransition` and **not** from the alert
 * flow, so that a design change which starts logging zone exits as trips
 * cannot make the house fire twice for one crossing.
 *
 * ### Why this runs on the application scope
 *
 * Same reason as [EscalationRunner]. A fall arrives when the phone is in a
 * pocket. A collector tied to a ViewModel would be dead at the moment the
 * event it exists for is emitted.
 *
 * ### Debounce
 *
 * One firing per hook per trigger per minute, in memory. Geofences in
 * particular flap - a person sitting on the boundary of a zone produces a
 * stream of enters and exits - and an unthrottled hook would turn that into a
 * porch light strobing at three in the morning.
 */
class SmartHomeRunner(
    private val repository: SmartHomeRepository,
    private val safety: SafetyRepository,
    private val zones: ZoneRepository,
    /**
     * Whether the wearable's battery is currently below the user's threshold.
     *
     * `AppContainer` supplies `wearableWatch.state.map { it.lowBatteryAlerted }`.
     * Fired on the false-to-true edge only.
     *
     * Known limit: `WatchState` starts false in memory and the persisted mark
     * is loaded on the watch's first tick, so a process that restarts while
     * the battery is still low produces one more edge, and therefore one more
     * firing. That is a repeated true statement rather than a false one, and
     * the minute debounce keeps it to one.
     */
    private val lowBattery: Flow<Boolean>,
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
    /** The person the house is being told about. Empty when the app has no name. */
    private val wearerName: () -> String,
    /** Overridable so a test can drive the debounce clock. */
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    /** Last firing per (hook id, trigger). In memory; see the class doc. */
    private val lastFired = mutableMapOf<Pair<String, String>, Long>()
    private val debounceLock = Mutex()

    /** Starts observing. Called once, from `AppContainer`. */
    fun start() {
        // Keyed on the alert's identity, like EscalationRunner: the repository
        // updates the event in place as its outcome changes, and firing again
        // on each of those would call the house every time somebody tapped a
        // button on the alarm screen.
        safety.activeAlert
            .filterNotNull()
            .distinctUntilChanged { old, new -> old.id == new.id }
            .onEach { alert ->
                val trigger = triggerFor(alert.kind) ?: return@onEach
                fire(
                    trigger = trigger,
                    at = alert.timestamp,
                    detail = alert.note?.takeIf { it.isNotBlank() } ?: alert.kind.label
                )
            }
            .launchIn(scope)

        zones.lastTransition
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { transition ->
                val trigger =
                    if (transition.isInside) SmartHomeTriggers.ZONE_ENTER
                    else SmartHomeTriggers.ZONE_EXIT
                fire(
                    trigger = trigger,
                    at = transition.at,
                    // The zone's own centre, which is a real coordinate this
                    // app holds. Not a claim about where the person is
                    // standing - it is where the zone is.
                    lat = transition.zone.lat,
                    lon = transition.zone.lon,
                    detail = if (transition.isInside) {
                        "Arrived in ${transition.zone.name}"
                    } else {
                        "Left ${transition.zone.name}"
                    }
                )
            }
            .launchIn(scope)

        // The false-to-true edge only. `distinctUntilChanged` then a filter,
        // rather than a filter alone, so a flow that re-emits true does not
        // call the house again.
        lowBattery
            .distinctUntilChanged()
            .onEach { low ->
                if (low) {
                    fire(
                        trigger = SmartHomeTriggers.LOW_BATTERY,
                        at = now(),
                        detail = "The wearable's battery is low"
                    )
                }
            }
            .launchIn(scope)
    }

    /**
     * Which trigger an alert kind is, or null when it is not one the house
     * hears about.
     *
     * `ZONE_EXIT` and `JOURNEY_OVERDUE` return null on purpose: zone crossings
     * arrive on their own flow, and there is no `journey_overdue` value in the
     * server's trigger constraint, so a hook could not be stored for it.
     */
    private fun triggerFor(kind: TripKind): String? = when (kind) {
        TripKind.FALL -> SmartHomeTriggers.FALL
        // A quiet word is an SOS raised without anybody in the room hearing it
        // raised, so it is the `sos` trigger. The house being told is the
        // whole point of the feature; the *phone* stays quiet, not the house.
        TripKind.SOS, TripKind.PHONE_SOS, TripKind.QUIET_WORD -> SmartHomeTriggers.SOS
        TripKind.MISSED_CHECKIN -> SmartHomeTriggers.CHECK_IN_MISSED
        TripKind.ZONE_EXIT, TripKind.JOURNEY_OVERDUE -> null
    }

    /**
     * Fires every enabled hook for [trigger].
     *
     * Each post is launched rather than awaited: a hook pointing at a router
     * that has gone away holds a connection for the full ten seconds, and
     * three of those in series would be half a minute during which the *next*
     * alert's collector is blocked.
     */
    private suspend fun fire(
        trigger: String,
        at: Long,
        lat: Double? = null,
        lon: Double? = null,
        detail: String? = null
    ) {
        // Waits for the first store read rather than reading `.value`, which is
        // null until it completes. Treating "not loaded yet" as "no hooks"
        // would drop the house's call for a fall that arrived in the second
        // after launch - which is exactly when a phone is being picked up.
        val loaded = repository.hooks.filterNotNull().first()
        val matching = SmartHomePlanner.hooksFor(loaded, trigger)
        if (matching.isEmpty()) return

        val event = SmartHomeEvent(
            trigger = trigger,
            wearerName = wearerName(),
            at = at,
            lat = lat,
            lon = lon,
            detail = detail
        )

        for (hook in matching) {
            val allowed = debounceLock.withLock {
                val key = hook.id to trigger
                if (!SmartHomePlanner.due(lastFired[key], now())) {
                    false
                } else {
                    lastFired[key] = now()
                    true
                }
            }
            if (!allowed) continue
            scope.launch { deliver(hook, event) }
        }
    }

    /**
     * One hook, one event, and the truth about what happened written down.
     *
     * A `matter` hook has nothing to POST to - Matter devices are commissioned
     * into a hub, not called over HTTP, and this build does not include the
     * Play Services Home module that would commission one. It is recorded as a
     * firing with that as its reason rather than skipped silently, because a
     * hook that never appears in its own log looks to the user like a hook
     * that was never reached, and they would spend the evening checking their
     * router. See `SmartHomeApps.matterCommissioningAvailable`.
     */
    private suspend fun deliver(hook: SmartHomeHook, event: SmartHomeEvent) {
        if (hook.provider == SmartHomeProviders.MATTER) {
            repository.recordFiring(
                hookId = hook.id,
                statusCode = null,
                error = "SafeShade reports whether this phone could set up Matter devices; " +
                    "it does not send events to them. Use a webhook or Home Assistant " +
                    "to have your hub act on this."
            )
            return
        }

        when (val result = SmartHomeApps.post(hook, event, client)) {
            is WebhookResult.Delivered ->
                repository.recordFiring(hook.id, result.statusCode, null)

            is WebhookResult.Rejected ->
                repository.recordFiring(
                    hookId = hook.id,
                    statusCode = result.statusCode,
                    error = rejectionReason(result)
                )

            is WebhookResult.Unreachable ->
                repository.recordFiring(hook.id, null, result.reason)
        }
    }

    /**
     * What a refusal is written down as.
     *
     * The far end's own words when it gave any, because "unknown webhook id"
     * from Home Assistant is worth more than any sentence this app could
     * invent. Never the URL - see `SmartHomeStore`'s KDoc.
     */
    private fun rejectionReason(result: WebhookResult.Rejected): String {
        val said = result.bodySnippet.takeIf { it.isNotBlank() }
        val head = when (result.statusCode) {
            401, 403 -> "That address refused the call as not allowed (${result.statusCode})."
            404 -> "Nothing is listening at that address (404)."
            in 500..599 -> "That address had an error of its own (${result.statusCode})."
            else -> "That address refused the call (${result.statusCode})."
        }
        return if (said == null) head else "$head It said: $said"
    }
}

/**
 * Which hooks a trigger fires, as a pure function.
 *
 * Pure and separate so the two rules that decide whether somebody's house
 * responds to a fall - the trigger has to match, and the hook has to be
 * enabled - can be tested without a network, a store or a clock.
 */
object SmartHomePlanner {

    /** The enabled hooks whose trigger is [trigger], in list order. */
    fun hooksFor(hooks: List<SmartHomeHook>, trigger: String): List<SmartHomeHook> =
        hooks.filter { it.enabled && it.trigger == trigger }

    /**
     * Whether a hook that last fired at [lastFiredAt] may fire again at [now].
     *
     * Null means it has never fired, which is always due. See the runner's
     * class doc for why the window exists.
     */
    fun due(lastFiredAt: Long?, now: Long, windowMs: Long = DEBOUNCE_MS): Boolean {
        if (lastFiredAt == null) return true
        // A clock that went backwards - a manual time change, an NTP
        // correction - must not lock a hook out until it catches up.
        if (now < lastFiredAt) return true
        return now - lastFiredAt >= windowMs
    }

    /** One firing per hook per trigger per minute. */
    const val DEBOUNCE_MS: Long = 60_000L
}
