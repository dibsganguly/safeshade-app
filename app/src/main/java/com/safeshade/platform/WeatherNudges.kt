package com.safeshade.platform

/**
 * Turns raw weather and air-quality numbers into short, consequence-first
 * warnings a wearer or guardian can act on.
 *
 * The rule of this file: never hedge. A nudge names what will happen, not what
 * might. "Skin burns in about 15 minutes at this index" is a fact about UV
 * exposure at that index, not a forecast — so no field here ever needs the
 * words "planned", "simulated" or "coming soon" to stay honest, and none may
 * use them.
 *
 * Air quality reads Open-Meteo's free air-quality endpoint, called elsewhere
 * (this file has no network dependency):
 * `https://air-quality-api.open-meteo.com/v1/air-quality?latitude=..&longitude=..&current=european_aqi,pm2_5`
 */
object WeatherNudges {

    /**
     * @param uvIndex Open-Meteo `uv_index`, or null if unavailable.
     * @param tempC ambient temperature in Celsius, or null.
     * @param feelsLikeC Open-Meteo `apparent_temperature`, or null.
     * @param humidity relative humidity 0-100, or null.
     * @param aqiEuropean Open-Meteo `european_aqi` (1-100+, higher is worse), or null.
     * @param pm25 PM2.5 in micrograms per cubic metre, or null.
     * @param hourOfDay local hour 0-23, used only to gate the UV midday note.
     * @param persona a [com.safeshade.data.PersonaMode.wireName]-style string;
     *   only `"ELDERLY"` and `"KIDS"` change thresholds, everything else uses
     *   the general-population thresholds.
     */
    fun assess(
        uvIndex: Float?,
        tempC: Float?,
        feelsLikeC: Float?,
        humidity: Float?,
        aqiEuropean: Int?,
        pm25: Float?,
        hourOfDay: Int,
        persona: String,
    ): List<Nudge> {
        val nudges = mutableListOf<Nudge>()

        uvNudge(uvIndex, hourOfDay)?.let { nudges += it }
        heatNudge(feelsLikeC, persona)?.let { nudges += it }
        coldNudge(feelsLikeC, persona)?.let { nudges += it }
        airNudge(aqiEuropean, pm25)?.let { nudges += it }

        return nudges
    }

    private fun uvNudge(uvIndex: Float?, hourOfDay: Int): Nudge? {
        if (uvIndex == null || uvIndex < 6f) return null

        val (title, band) = when {
            uvIndex >= 11f -> "Extreme sun" to "Extreme"
            uvIndex >= 8f -> "Very strong sun" to "Very high"
            else -> "Strong sun" to "High"
        }

        val middayNote = if (hourOfDay in 10..16) " Exposure is strongest between 10am and 4pm." else ""

        return Nudge(
            kind = NudgeKind.UV,
            title = title,
            line = "UV index $uvIndex, the WHO \"$band\" band. Skin burns in about 15 minutes at this index.$middayNote",
        )
    }

    private fun heatNudge(feelsLikeC: Float?, persona: String): Nudge? {
        if (feelsLikeC == null) return null
        val threshold = if (isVulnerablePersona(persona)) 30f else 32f
        if (feelsLikeC < threshold) return null

        return if (feelsLikeC >= 40f) {
            Nudge(
                kind = NudgeKind.HEAT,
                title = "Dangerous heat",
                line = "Feels like ${feelsLikeC.toInt()}°C. Heat stroke risk rises sharply at this level; limit time outdoors and drink water often.",
            )
        } else {
            Nudge(
                kind = NudgeKind.HEAT,
                title = "Heat",
                line = "Feels like ${feelsLikeC.toInt()}°C. Sustained activity at this temperature causes heat exhaustion.",
            )
        }
    }

    private fun coldNudge(feelsLikeC: Float?, persona: String): Nudge? {
        if (feelsLikeC == null) return null
        val threshold = if (isVulnerablePersona(persona)) 3f else 0f
        if (feelsLikeC > threshold) return null

        return if (feelsLikeC <= -10f) {
            Nudge(
                kind = NudgeKind.COLD,
                title = "Severe cold",
                line = "Feels like ${feelsLikeC.toInt()}°C. Exposed skin can suffer frostbite within minutes at this temperature.",
            )
        } else {
            Nudge(
                kind = NudgeKind.COLD,
                title = "Cold",
                line = "Feels like ${feelsLikeC.toInt()}°C. Prolonged exposure at this temperature lowers body temperature.",
            )
        }
    }

    private fun airNudge(aqiEuropean: Int?, pm25: Float?): Nudge? {
        val band = aqiEuropean?.let { aqiBand(it) }

        if (band != null && band.ordinal >= AqiBand.POOR.ordinal) {
            return Nudge(
                kind = NudgeKind.AIR,
                title = band.title,
                line = "European AQI $aqiEuropean, \"${band.label}\" air. Breathing this air for an hour irritates the airway.",
            )
        }

        if (band == null && pm25 != null && pm25 > 35f) {
            return Nudge(
                kind = NudgeKind.AIR,
                title = "Poor air",
                line = "PM2.5 is ${pm25.toInt()} µg/m³. Breathing this air for an hour irritates the airway.",
            )
        }

        return null
    }

    private fun aqiBand(aqi: Int): AqiBand = when {
        aqi <= 20 -> AqiBand.GOOD
        aqi <= 40 -> AqiBand.FAIR
        aqi <= 60 -> AqiBand.MODERATE
        aqi <= 80 -> AqiBand.POOR
        aqi <= 100 -> AqiBand.VERY_POOR
        else -> AqiBand.EXTREMELY_POOR
    }

    private fun isVulnerablePersona(persona: String): Boolean =
        persona.equals("ELDERLY", ignoreCase = true) || persona.equals("KIDS", ignoreCase = true)

    private enum class AqiBand(val label: String, val title: String) {
        GOOD("good", "Air quality"),
        FAIR("fair", "Air quality"),
        MODERATE("moderate", "Air quality"),
        POOR("poor", "Poor air"),
        VERY_POOR("very poor", "Very poor air"),
        EXTREMELY_POOR("extremely poor", "Extremely poor air"),
    }
}

enum class NudgeKind { UV, HEAT, COLD, AIR }

data class Nudge(
    val kind: NudgeKind,
    val title: String,
    val line: String,
)

/** One air-quality reading, as returned by Open-Meteo's `current` block. */
data class AirQualityReading(
    val europeanAqi: Int?,
    val pm25: Float?,
)
