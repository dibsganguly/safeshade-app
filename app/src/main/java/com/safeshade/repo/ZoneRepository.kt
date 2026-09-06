package com.safeshade.repo

import com.safeshade.GeofenceEventBus
import com.safeshade.GeofenceManager
import com.safeshade.cloud.dto.CloudTables
import com.safeshade.data.GeofenceZone
import com.safeshade.data.SafeShadePreferences
import com.safeshade.data.resolveWearerForDevice
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceLink
import com.safeshade.device.DeviceProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Safe zones: stored here, registered with the OS, and mirrored to the
 * wearable.
 *
 * The forwarding path is the reason this class collects on the application
 * scope. `GeofenceBroadcastReceiver` is instantiated fresh by the OS with no
 * reference to anything the app holds, and it fires precisely when the app is
 * *not* in the foreground — that is what a geofence is for. A collector tied to
 * a ViewModel would be dead at exactly the moment the event arrives.
 */
class ZoneRepository(
    private val prefs: SafeShadePreferences,
    private val link: DeviceLink,
    private val geofenceManager: GeofenceManager,
    private val scope: CoroutineScope,
    /** See [SyncHooks]. Nothing is queued for the cloud without one. */
    private val hooks: SyncHooks = SyncHooks.None
) {

    /** Null until the first DataStore read completes. See [ProfileRepository]. */
    val zones: StateFlow<List<GeofenceZone>?> =
        prefs.zones.stateIn(scope, SharingStarted.Eagerly, null)

    private val _lastTransition = MutableStateFlow<ZoneTransition?>(null)

    /** The most recent enter/exit, for the guardian screen's status line. */
    val lastTransition: StateFlow<ZoneTransition?> = _lastTransition.asStateFlow()

    /**
     * Ext writes that arrived while the link was not Ready.
     *
     * Geofence state is live, app-pushed, and deliberately *not* persisted by
     * the firmware, so the wearable knows nothing about zones until the app
     * tells it. Dropping a transition because the link happened to be down
     * would leave the device showing "inside" for as long as it stays
     * connected afterwards. Only the newest state per zone is worth replaying —
     * a queue of stale ins and outs would replay a history nobody needs — so
     * this is keyed by zone id.
     */
    private val pending = LinkedHashMap<String, Pair<String, String>>()
    private val pendingLock = Mutex()

    init {
        // Re-register with the OS whenever the stored list changes. Cheap, and
        // the alternative is a UI that shows a zone the system has never heard
        // of. Missing location permission surfaces through the callback rather
        // than throwing — GeofenceManager is annotated for that.
        prefs.zones
            .distinctUntilChanged()
            .onEach { geofenceManager.syncZones(it) }
            .launchIn(scope)

        GeofenceEventBus.events
            .onEach { (zoneId, isInside) -> onTransition(zoneId, isInside) }
            .launchIn(scope)

        // Flush queued transitions on the Ready edge.
        //
        // Deduping the boolean rather than filtering for Ready first is
        // deliberate: ConnectionState.Ready is a data object, so
        // `filter { it is Ready }.distinctUntilChanged()` collapses every
        // reconnect into a single emission and the flush would run once per
        // process. See DeviceRepository for the longer version of this.
        link.connectionState
            .map { it is ConnectionState.Ready }
            .distinctUntilChanged()
            .filter { it }
            .onEach { flushPending() }
            .launchIn(scope)
    }

    private suspend fun onTransition(zoneId: String, isInside: Boolean) {
        val zone = prefs.zones.first().firstOrNull { it.id == zoneId } ?: return
        _lastTransition.value = ZoneTransition(zone, isInside, System.currentTimeMillis())

        val payload = DeviceProtocol.geofence(zone, isInside)
        if (link.connectionState.value is ConnectionState.Ready) {
            link.writeExt(TAG_GEOFENCE, payload)
        } else {
            pendingLock.withLock { pending[zoneId] = TAG_GEOFENCE to payload }
        }
    }

    private suspend fun flushPending() {
        val queued = pendingLock.withLock {
            if (pending.isEmpty()) return
            val copy = pending.values.toList()
            pending.clear()
            copy
        }
        queued.forEach { (tag, payload) ->
            // Re-checked per write: the link can drop mid-flush, and each of
            // these would otherwise be dropped by the null guard with nothing
            // but a log line to show for it.
            if (link.connectionState.value is ConnectionState.Ready) {
                link.writeExt(tag, payload)
            }
        }
    }

    // ============================================
    // CRUD
    // ============================================

    private val listLock = Mutex()

    suspend fun addZone(zone: GeofenceZone) {
        // A zone drawn while one person is selected belongs to that person. The
        // field is nullable and a null one is a zone for the whole circle, so
        // an existing stamp is never overwritten.
        val stamped = if (zone.wearerId == null) zone.copy(wearerId = currentWearerId()) else zone
        val added = listLock.withLock {
            val current = prefs.zones.first()
            if (current.any { it.id == stamped.id }) return@withLock false
            prefs.setZones(current + stamped)
            true
        }
        if (added) hooks.onUpsert(CloudTables.ZONES, stamped.id)
    }

    suspend fun updateZone(zone: GeofenceZone) {
        listLock.withLock {
            val current = prefs.zones.first()
            prefs.setZones(current.map { if (it.id == zone.id) zone else it })
        }
        hooks.onUpsert(CloudTables.ZONES, zone.id)
    }

    suspend fun removeZone(zoneId: String) {
        listLock.withLock {
            val current = prefs.zones.first()
            prefs.setZones(current.filterNot { it.id == zoneId })
        }
        pendingLock.withLock { pending.remove(zoneId) }
        hooks.onDelete(CloudTables.ZONES, zoneId)
    }

    suspend fun clearZones() {
        val removed = listLock.withLock {
            val current = prefs.zones.first()
            prefs.setZones(emptyList())
            current
        }
        geofenceManager.clearZones()
        pendingLock.withLock { pending.clear() }
        // One tombstone each, not one "the list is empty" message. There is no
        // such message in the protocol, and a phone that has been offline would
        // otherwise never learn that these zones are gone.
        removed.forEach { hooks.onDelete(CloudTables.ZONES, it.id) }
    }

    /**
     * Zones arriving from another phone in the circle.
     *
     * Takes the same [listLock] as every local write, and deliberately does
     * **not** call [hooks]: a pulled row that queued its own push would be
     * echoed straight back to the server, whose `updated_at` trigger would make
     * it look like a change, which would pull it again. That loop has no exit.
     */
    suspend fun applyRemoteZones(transform: (List<GeofenceZone>) -> List<GeofenceZone>) {
        listLock.withLock {
            val current = prefs.zones.first()
            val updated = transform(current)
            if (updated != current) prefs.setZones(updated)
        }
    }

    /**
     * The person a new record belongs to: the wearer of the connected device,
     * or the selected one.
     *
     * The connected wearer is not the selected wearer - handoff7 section 6
     * item 2 - and for anything raised by a device, the device is the better
     * answer.
     */
    private suspend fun currentWearerId(): String? = resolveWearerForDevice(
        wearers = prefs.wearers.first(),
        address = link.deviceAddress.value,
        selectedWearerId = prefs.selectedWearerId.first()
    )?.id

    private companion object {
        const val TAG_GEOFENCE = "GEOFENCE"
    }
}

data class ZoneTransition(
    val zone: GeofenceZone,
    val isInside: Boolean,
    val at: Long
)
