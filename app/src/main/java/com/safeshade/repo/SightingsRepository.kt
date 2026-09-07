package com.safeshade.repo

import com.safeshade.data.LostMode
import com.safeshade.data.MAX_SIGHTINGS
import com.safeshade.data.Sighting
import com.safeshade.data.SightingsStore
import com.safeshade.device.BleSighting
import com.safeshade.device.DeviceLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The rules about what a sighting is worth keeping, with no clock, no radio and
 * no store behind them.
 *
 * Pure so both rules can be asserted directly. They are the two that decide
 * whether this feature is useful or a battery drain, and neither is observable
 * from the outside once it has gone wrong: a dedupe window that is too tight
 * writes DataStore several times a second behind a sweep, and a ring cap that
 * does not hold lets the file grow without limit - both silently.
 */
object SightingPolicy {

    /**
     * How long one address stays "already recorded".
     *
     * A wearable advertises several times a second, and a phone that walks past
     * one for twenty seconds would otherwise store fifty rows saying the same
     * thing: this device was here, roughly now. Five minutes is chosen against
     * what a searcher actually needs - a trail of places, not a trail of
     * seconds - and against DataStore, which rewrites its whole file per write.
     */
    const val DEDUPE_WINDOW_MS: Long = 5 * 60 * 1000L

    /**
     * Addresses are compared case-insensitively.
     *
     * Android reports them uppercase, but the paired list is data the user's
     * own devices were saved into and has been through JSON, a QR code and a
     * cloud row on the way. A case mismatch there would make this phone record
     * its own wearable as a stranger's and report it to the community, which is
     * a privacy failure, not a cosmetic one.
     */
    fun normalize(address: String): String = address.trim().uppercase()

    /**
     * Whether [at] is a new enough sighting of [address] to be worth storing.
     *
     * Decided against the stored rows rather than against an in-memory map, so
     * the window survives a process death. The alternative re-records a device
     * seen two minutes ago just because Android reclaimed the app in between.
     */
    fun shouldRecord(existing: List<Sighting>, address: String, at: Long): Boolean {
        val key = normalize(address)
        val last = existing
            .filter { normalize(it.address) == key }
            .maxOfOrNull { it.at }
            ?: return true
        return at - last >= DEDUPE_WINDOW_MS
    }

    /**
     * Appends one row, oldest first, capped at [MAX_SIGHTINGS].
     *
     * Drops from the front, so the ring keeps the most recent window rather
     * than the first 200 rows this install ever saw.
     */
    fun append(existing: List<Sighting>, new: Sighting): List<Sighting> =
        (existing + new).takeLast(MAX_SIGHTINGS)
}

/**
 * What this phone has heard, and which of the user's own devices are missing.
 *
 * ### The two ways a lost device is found
 *
 * Lost mode with nothing connected is precisely the state this class is
 * designed for - by the time a wearable is lost, the link to it is long gone,
 * so nothing about it can be asked and everything must be overheard.
 *
 *  - **Near the guardian**, it is *this* phone's own periodic sweep that finds
 *    it. [startSweep] is a listen-only scan that connects to nothing, so it
 *    runs whether or not another device is connected, and a wearable under the
 *    sofa answers it exactly as it would answer a connect scan. That is why
 *    [ownLastSeen] exists at all: the moment this phone last heard each of its
 *    own devices is the "it is still in the house" signal, and it is recorded
 *    even though those addresses are deliberately kept out of the community
 *    store.
 *  - **Anywhere else**, no sweep of this phone's will ever reach it, and the
 *    only thing that can is somebody else's phone walking past. Every stranger
 *    the sweep hears is stored here and handed to `SightingsCloud.report` by
 *    the sync track, so that when a stranger's phone passes *this* user's
 *    missing device, their app does the same for them. The feature only works
 *    because it is symmetric.
 *
 * ### Why own devices are split out
 *
 * A sighting reported to the cloud says "somebody's phone was next to this
 * device, here, at this time". For a stranger's wearable that is the service.
 * For the user's own it would be a location history of the person carrying it,
 * uploaded continuously and by a path nobody asked for. So own addresses never
 * reach [recent]; they update [ownLastSeen], which is a timestamp and stays on
 * the phone.
 *
 * @param ownAddresses the user's paired devices, read at each sighting rather
 *   than captured once - a device paired after this object was constructed must
 *   not keep being reported to the community. Backed by DataStore in
 *   production, so it reads empty for the first moments of a cold start; see
 *   `ownAddressesNow` for what covers that window. Callers should also hold
 *   [startSweep] until the paired list has actually loaded, which this class
 *   cannot see from here.
 * @param location this phone's position as lat/lon/accuracy, or null when it
 *   has no fix. Null is a real answer and is stored as one; a sighting with no
 *   position still says the device is *somewhere near this phone*, and 0,0
 *   would say something false.
 */
class SightingsRepository(
    private val link: DeviceLink,
    private val store: SightingsStore,
    private val scope: CoroutineScope,
    private val ownAddresses: () -> Set<String>,
    private val location: () -> Triple<Double, Double, Double>?,
    /** Injected only so lost-mode timestamps are assertable; sighting times come off the radio. */
    private val now: () -> Long = System::currentTimeMillis
) {

    /** Every stranger this phone has heard. Null until the first read completes. */
    val recent: StateFlow<List<Sighting>?> =
        store.sightings.stateIn(scope, SharingStarted.Eagerly, null)

    /** The user's own devices they have marked missing. Null until loaded. */
    val lost: StateFlow<List<LostMode>?> =
        store.lost.stateIn(scope, SharingStarted.Eagerly, null)

    private val _ownLastSeen = MutableStateFlow<Map<String, Long>>(emptyMap())

    /**
     * When this phone itself last heard each of its own devices, by address.
     *
     * In memory only, and deliberately: it is a fact about this session's
     * radio, and a value restored from disk after a restart would claim the
     * device was heard when in truth nothing has listened yet. A missing entry
     * means "not heard since the app started", which is the honest answer.
     */
    val ownLastSeen: StateFlow<Map<String, Long>> = _ownLastSeen.asStateFlow()

    /**
     * Serialises read-modify-write on the sighting list.
     *
     * A twenty-second sweep in a crowded place delivers results faster than a
     * DataStore write completes, and two overlapping appends would each read
     * the same list and write back a version missing the other's row.
     */
    private val writeLock = Mutex()

    init {
        scope.launch {
            link.sightings.collect { record(it) }
        }
    }

    /**
     * Starts a listen-only sweep.
     *
     * Twenty seconds is long enough to hear a device advertising on a one to
     * two second interval several times over, including through a wall or a
     * bag, and short enough that a sweep is not a meaningful share of the
     * radio's day. It connects to nothing, so it is safe while connected.
     *
     * Android refuses an app's sixth scan start inside 30 seconds, silently, so
     * callers must space sweeps out - this is a periodic errand, not something
     * to fire on a button press.
     */
    fun startSweep() = link.startSightingScan(SWEEP_MS)

    /** Ends a sweep early. */
    fun stopSweep() = link.stopSightingScan()

    /**
     * The addresses that must never reach the community store.
     *
     * The paired list, plus the address this phone is currently talking to.
     * That second term closes a real startup race: `ownAddresses` is backed by
     * DataStore and reads empty for the first moments after launch, which is
     * exactly when the auto-reconnect scan fires and reports the user's own
     * wearable. Without it, the first sighting of every cold start is the
     * user's own device, filed as a stranger's and uploaded as one.
     *
     * A device this phone is connected to is the user's by definition,
     * whatever the paired list has finished loading.
     */
    private fun ownAddressesNow(): Set<String> =
        ownAddresses().asSequence()
            .plus(link.deviceAddress.value)
            .filter { it.isNotBlank() }
            .map(SightingPolicy::normalize)
            .toSet()

    private suspend fun record(sighting: BleSighting) {
        val key = SightingPolicy.normalize(sighting.address)

        if (key in ownAddressesNow()) {
            // Own device: a timestamp, and nothing that could become a location
            // history of the person wearing it. Not deduped - it is one entry
            // per address either way, and the freshest answer is the useful one.
            _ownLastSeen.value = _ownLastSeen.value + (key to sighting.at)
            return
        }

        writeLock.withLock {
            val existing = store.sightings.first()
            if (!SightingPolicy.shouldRecord(existing, key, sighting.at)) return@withLock
            val fix = location()
            store.setSightings(
                SightingPolicy.append(
                    existing,
                    Sighting(
                        address = key,
                        name = sighting.name?.takeIf { it.isNotBlank() },
                        rssi = sighting.rssi,
                        at = sighting.at,
                        lat = fix?.first,
                        lon = fix?.second,
                        accuracyM = fix?.third
                    )
                )
            )
        }
    }

    // ============================================
    // Lost mode
    // ============================================

    /**
     * Marks one of the user's own devices missing.
     *
     * Re-marking a device already lost keeps the original [LostMode.since]: the
     * question a searcher asks is "how long has it been gone", and resetting
     * the clock every time somebody reopened the screen would answer it wrong.
     */
    suspend fun markLost(address: String, label: String) {
        val key = SightingPolicy.normalize(address)
        lostLock.withLock {
            val current = store.lost.first()
            if (current.any { SightingPolicy.normalize(it.address) == key }) {
                // Only the label is refreshed; the moment it went missing is not.
                store.setLost(
                    current.map {
                        if (SightingPolicy.normalize(it.address) == key) it.copy(label = label) else it
                    }
                )
                return@withLock
            }
            store.setLost(current + LostMode(address = key, since = now(), label = label))
        }
    }

    /** Clears lost mode for one device. */
    suspend fun markFound(address: String) {
        val key = SightingPolicy.normalize(address)
        lostLock.withLock {
            store.setLost(store.lost.first().filterNot { SightingPolicy.normalize(it.address) == key })
        }
    }

    private val lostLock = Mutex()

    // ============================================
    // The cloud handoff
    // ============================================

    /**
     * Sightings not yet accepted by the cloud, oldest first.
     *
     * The sync track reports these and then calls [markReported]. Nothing here
     * talks to the network: a repository that both stored and uploaded would
     * make the storage rules untestable without a server.
     */
    suspend fun unreported(): List<Sighting> = store.sightings.first().filterNot { it.reported }

    /**
     * Marks the named rows reported.
     *
     * Keyed on address *and* time rather than address alone, because the ring
     * holds several rows per address once a device has been passed more than
     * once, and flagging all of them because one was accepted would drop the
     * rest on the floor unreported.
     */
    suspend fun markReported(reported: Collection<Pair<String, Long>>) {
        if (reported.isEmpty()) return
        val keys = reported.map { SightingPolicy.normalize(it.first) to it.second }.toSet()
        writeLock.withLock {
            store.setSightings(
                store.sightings.first().map {
                    if ((SightingPolicy.normalize(it.address) to it.at) in keys && !it.reported) {
                        it.copy(reported = true)
                    } else {
                        it
                    }
                }
            )
        }
    }

    private companion object {
        /** See [startSweep]. */
        const val SWEEP_MS = 20_000L
    }
}
