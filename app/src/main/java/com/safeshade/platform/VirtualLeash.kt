package com.safeshade.platform

/**
 * A distance model built on smoothed BLE RSSI, for a Way's "how far is the
 * wearable" line and its broken-leash alert.
 *
 * RSSI is noisy, so a single crossing of a threshold is not enough — the state
 * machine applies [hysteresisDbm] on both the near/drifting and the
 * drifting/far boundaries, and only promotes a sustained far reading (or a
 * dropped link) to [LeashState.Broken] after it has held for [graceMs]. That
 * grace period exists because a wearer stepping behind a wall for ten seconds
 * is normal and a guardian alert for it would train them to ignore the alert
 * that matters.
 */
class VirtualLeash(
    private val nearDbm: Int = -70,
    private val farDbm: Int = -85,
    private val hysteresisDbm: Int = 4,
    private val graceMs: Long = 20_000,
) {

    /**
     * Android's own "no RSSI reading" sentinel, matching
     * `DeviceRepository.RSSI_UNKNOWN`. That constant is private to
     * `DeviceRepository`, so this is the same numeric value re-declared here
     * rather than a shared reference — keep the two in sync if either changes.
     */
    private val rssiUnknown = -127

    private var zone: Zone = Zone.UNKNOWN
    private var farSinceMs: Long? = null
    private var brokenSinceMs: Long? = null

    /** Advances the model with a new (possibly null/unknown) RSSI reading. */
    fun step(rssiDbm: Int?, nowMs: Long): LeashState {
        if (rssiDbm == null || rssiDbm == rssiUnknown) {
            // A dropped link. If we were already far and past grace, this is a
            // broken leash, not merely unknown — losing the link entirely is at
            // least as bad as a weak-but-present signal.
            if (zone == Zone.FAR) {
                val since = farSinceMs ?: nowMs
                if (brokenSinceMs == null && nowMs - since >= graceMs) brokenSinceMs = since
            }
            if (zone == Zone.BROKEN || brokenSinceMs != null) {
                zone = Zone.BROKEN
                return LeashState.Broken(brokenSinceMs ?: nowMs)
            }
            zone = Zone.UNKNOWN
            farSinceMs = null
            brokenSinceMs = null
            return LeashState.Unknown
        }

        zone = nextZone(zone, rssiDbm)

        return when (zone) {
            Zone.UNKNOWN -> {
                farSinceMs = null
                brokenSinceMs = null
                LeashState.Unknown
            }
            Zone.NEAR -> {
                farSinceMs = null
                brokenSinceMs = null
                LeashState.Near(rssiDbm)
            }
            Zone.DRIFTING -> {
                farSinceMs = null
                brokenSinceMs = null
                LeashState.Drifting(rssiDbm)
            }
            Zone.FAR -> {
                val since = farSinceMs ?: nowMs.also { farSinceMs = it }
                if (nowMs - since >= graceMs) {
                    brokenSinceMs = since
                    zone = Zone.BROKEN
                    LeashState.Broken(since)
                } else {
                    LeashState.Far(rssiDbm, since)
                }
            }
            Zone.BROKEN -> LeashState.Broken(brokenSinceMs ?: nowMs)
        }
    }

    private fun nextZone(current: Zone, rssiDbm: Int): Zone {
        // A reading inside the hysteresis band around a boundary never moves
        // the state across it; only a reading clearly past the boundary does.
        return when (current) {
            Zone.UNKNOWN -> zoneFor(rssiDbm)
            Zone.NEAR -> if (rssiDbm < nearDbm - hysteresisDbm) zoneFor(rssiDbm) else Zone.NEAR
            Zone.DRIFTING -> when {
                rssiDbm >= nearDbm + hysteresisDbm -> Zone.NEAR
                rssiDbm < farDbm -> Zone.FAR
                else -> Zone.DRIFTING
            }
            Zone.FAR, Zone.BROKEN -> if (rssiDbm >= farDbm + hysteresisDbm) zoneFor(rssiDbm) else Zone.FAR
        }
    }

    private fun zoneFor(rssiDbm: Int): Zone = when {
        rssiDbm >= nearDbm -> Zone.NEAR
        rssiDbm >= farDbm -> Zone.DRIFTING
        else -> Zone.FAR
    }

    /**
     * A short line for a Way's detail row: "About arm's length", "Drifting
     * away", "Out of range for 40 s". [nowMs] is required — [LeashState.Far]
     * and [LeashState.Broken] carry only *when* the leash went out of range,
     * not how long ago that was, so this stays pure rather than reading the
     * clock itself.
     */
    fun describe(state: LeashState, nowMs: Long): String = when (state) {
        is LeashState.Unknown -> "—"
        is LeashState.Near -> "About arm's length"
        is LeashState.Drifting -> "Drifting away"
        is LeashState.Far -> "Out of range for ${secondsAgo(state.sinceMs, nowMs)}"
        is LeashState.Broken -> "Out of range for ${secondsAgo(state.sinceMs, nowMs)}"
    }

    private fun secondsAgo(sinceMs: Long, nowMs: Long): String {
        val seconds = ((nowMs - sinceMs) / 1000).coerceAtLeast(0)
        return "${seconds} s"
    }

    private enum class Zone { UNKNOWN, NEAR, DRIFTING, FAR, BROKEN }
}

sealed interface LeashState {
    data object Unknown : LeashState
    data class Near(val dbm: Int) : LeashState
    data class Drifting(val dbm: Int) : LeashState
    data class Far(val dbm: Int, val sinceMs: Long) : LeashState
    data class Broken(val sinceMs: Long) : LeashState
}
