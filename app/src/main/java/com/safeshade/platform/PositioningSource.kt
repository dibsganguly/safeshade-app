package com.safeshade.platform

/** Where a location fix came from, for display next to the fix itself. */
enum class PositioningSource(val label: String) {
    GNSS_DEVICE("Wearable GPS"),
    PHONE_FUSED("Phone"),
    NETWORK("Network"),
    CELL_GATEWAY("Gateway cell fix"),
    NONE("—"),
}

/** One rendered line describing a location fix's provenance and freshness. */
data class PositioningLine(
    val source: PositioningSource,
    /** "±12 m", or "—" when [accuracyM] is unknown. */
    val accuracyText: String,
    /** "just now", "40 s ago", "5 min ago", or "—" when [ageMs] is unknown. */
    val ageText: String,
)

object PositioningReadout {

    /**
     * @param provider the Android `LocationManager`/`FusedLocationProvider`
     *   provider string ("gps", "fused", "network"), or null.
     * @param accuracyM the fix's reported accuracy radius in metres, or null.
     * @param ageMs how long ago the fix was taken, or null when unknown.
     * @param fromDevice true when the fix came over BLE from the wearable's
     *   own GNSS rather than from this phone.
     */
    fun from(provider: String?, accuracyM: Float?, ageMs: Long?, fromDevice: Boolean): PositioningLine {
        val source = when {
            fromDevice -> PositioningSource.GNSS_DEVICE
            provider == null -> PositioningSource.NONE
            provider.equals("gps", ignoreCase = true) -> PositioningSource.PHONE_FUSED
            provider.equals("fused", ignoreCase = true) -> PositioningSource.PHONE_FUSED
            provider.equals("network", ignoreCase = true) -> PositioningSource.NETWORK
            else -> PositioningSource.NONE
        }

        val accuracyText = if (accuracyM != null && accuracyM.isFinite() && accuracyM >= 0f) {
            "±${accuracyM.toInt()} m"
        } else {
            "—"
        }

        val ageText = ageText(ageMs)

        return PositioningLine(source, accuracyText, ageText)
    }

    private fun ageText(ageMs: Long?): String {
        if (ageMs == null || ageMs < 0) return "—"
        val seconds = ageMs / 1000
        return when {
            seconds < 10 -> "just now"
            seconds < 60 -> "$seconds s ago"
            seconds < 3600 -> "${seconds / 60} min ago"
            else -> "${seconds / 3600} h ago"
        }
    }
}
