package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherNudgesTest {

    private fun assess(
        uvIndex: Float? = null,
        tempC: Float? = null,
        feelsLikeC: Float? = null,
        humidity: Float? = null,
        aqiEuropean: Int? = null,
        pm25: Float? = null,
        hourOfDay: Int = 12,
        persona: String = "BACKPACK",
    ) = WeatherNudges.assess(uvIndex, tempC, feelsLikeC, humidity, aqiEuropean, pm25, hourOfDay, persona)

    @Test
    fun `all nulls produce no nudges`() {
        assertEquals(emptyList<Nudge>(), assess())
    }

    @Test
    fun `uv bands escalate from strong to very strong to extreme`() {
        assertEquals("Strong sun", assess(uvIndex = 6f).single { it.kind == NudgeKind.UV }.title)
        assertEquals("Strong sun", assess(uvIndex = 7.9f).single { it.kind == NudgeKind.UV }.title)
        assertEquals("Very strong sun", assess(uvIndex = 8f).single { it.kind == NudgeKind.UV }.title)
        assertEquals("Very strong sun", assess(uvIndex = 10.9f).single { it.kind == NudgeKind.UV }.title)
        assertEquals("Extreme sun", assess(uvIndex = 11f).single { it.kind == NudgeKind.UV }.title)
        assertTrue(assess(uvIndex = 5.9f).none { it.kind == NudgeKind.UV })
    }

    @Test
    fun `uv nudge names the midday hours only during 10am to 4pm`() {
        val middayLine = assess(uvIndex = 9f, hourOfDay = 13).single { it.kind == NudgeKind.UV }.line
        val eveningLine = assess(uvIndex = 9f, hourOfDay = 20).single { it.kind == NudgeKind.UV }.line

        assertTrue(middayLine.contains("10am"))
        assertTrue(!eveningLine.contains("10am"))
    }

    @Test
    fun `uv line states a consequence rather than a hedge`() {
        val line = assess(uvIndex = 8f).single { it.kind == NudgeKind.UV }.line
        assertTrue(line.contains("burns"))
    }

    @Test
    fun `heat bands escalate at general thresholds`() {
        assertTrue(assess(feelsLikeC = 31.9f).none { it.kind == NudgeKind.HEAT })
        assertEquals("Heat", assess(feelsLikeC = 32f).single { it.kind == NudgeKind.HEAT }.title)
        assertEquals("Dangerous heat", assess(feelsLikeC = 40f).single { it.kind == NudgeKind.HEAT }.title)
    }

    @Test
    fun `cold bands escalate at general thresholds`() {
        assertTrue(assess(feelsLikeC = 0.1f).none { it.kind == NudgeKind.COLD })
        assertEquals("Cold", assess(feelsLikeC = 0f).single { it.kind == NudgeKind.COLD }.title)
        assertEquals("Severe cold", assess(feelsLikeC = -10f).single { it.kind == NudgeKind.COLD }.title)
    }

    @Test
    fun `elderly and kids personas lower the heat and cold thresholds`() {
        assertTrue(assess(feelsLikeC = 31f, persona = "BACKPACK").none { it.kind == NudgeKind.HEAT })
        assertTrue(assess(feelsLikeC = 31f, persona = "ELDERLY").any { it.kind == NudgeKind.HEAT })
        assertTrue(assess(feelsLikeC = 31f, persona = "KIDS").any { it.kind == NudgeKind.HEAT })

        assertTrue(assess(feelsLikeC = 2f, persona = "BACKPACK").none { it.kind == NudgeKind.COLD })
        assertTrue(assess(feelsLikeC = 2f, persona = "ELDERLY").any { it.kind == NudgeKind.COLD })
        assertTrue(assess(feelsLikeC = 2f, persona = "KIDS").any { it.kind == NudgeKind.COLD })
    }

    @Test
    fun `air quality bands only nudge from poor upward`() {
        assertTrue(assess(aqiEuropean = 20).none { it.kind == NudgeKind.AIR })
        assertTrue(assess(aqiEuropean = 60).none { it.kind == NudgeKind.AIR })
        assertEquals("Poor air", assess(aqiEuropean = 61).single { it.kind == NudgeKind.AIR }.title)
        assertEquals("Poor air", assess(aqiEuropean = 80).single { it.kind == NudgeKind.AIR }.title)
        assertEquals("Very poor air", assess(aqiEuropean = 81).single { it.kind == NudgeKind.AIR }.title)
        assertEquals("Very poor air", assess(aqiEuropean = 100).single { it.kind == NudgeKind.AIR }.title)
        assertEquals("Extremely poor air", assess(aqiEuropean = 101).single { it.kind == NudgeKind.AIR }.title)
    }

    @Test
    fun `pm25 alone nudges even when the aqi is missing`() {
        assertTrue(assess(pm25 = 35f).none { it.kind == NudgeKind.AIR })
        assertTrue(assess(pm25 = 35.1f).any { it.kind == NudgeKind.AIR })
    }

    @Test
    fun `aqi below poor is not overridden by a high pm25 reading alone once aqi is known`() {
        // AQI itself decides the band when present; pm2.5 is the fallback only
        // when the AQI is missing entirely.
        val nudges = assess(aqiEuropean = 20, pm25 = 50f)
        assertTrue(nudges.none { it.kind == NudgeKind.AIR })
    }

    @Test
    fun `multiple simultaneous conditions each produce their own nudge`() {
        val nudges = assess(uvIndex = 9f, feelsLikeC = 33f, aqiEuropean = 70f.toInt())
        assertEquals(setOf(NudgeKind.UV, NudgeKind.HEAT, NudgeKind.AIR), nudges.map { it.kind }.toSet())
    }
}
