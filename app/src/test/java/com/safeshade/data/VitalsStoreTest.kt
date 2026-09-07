package com.safeshade.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure half of the vitals store: JSON in, JSON out, and the threshold
 * comparison.
 *
 * No Robolectric and no DataStore here on purpose — [VitalsJson] and
 * [VitalsThresholds.assess] are free of Android precisely so this file can
 * exist, and everything else in `VitalsStore.kt` is read/write plumbing over
 * them.
 */
class VitalsStoreTest {

    // ============================================
    // Round trip
    // ============================================

    @Test
    fun `samples survive a round trip with every field set`() {
        val original = listOf(
            VitalsSample(
                id = "a",
                at = 1_700_000_000_000L,
                heartRateBpm = 72,
                spo2Percent = 98,
                tempC = 36.6f,
                ambientDb = 41.5,
                source = VitalsSample.SOURCE_PHONE,
                wearerId = "w1"
            ),
            VitalsSample(
                id = "b",
                at = 1_700_000_060_000L,
                heartRateBpm = 64,
                source = VitalsSample.SOURCE_DEVICE
            )
        )

        val decoded = VitalsJson.decodeSamples(VitalsJson.encodeSamples(original))

        assertEquals(original, decoded)
    }

    @Test
    fun `absent measurements stay null rather than becoming zero`() {
        val original = listOf(VitalsSample(id = "a", at = 1L, heartRateBpm = 55))

        val decoded = VitalsJson.decodeSamples(VitalsJson.encodeSamples(original)).single()

        assertEquals(55, decoded.heartRateBpm)
        assertNull(decoded.spo2Percent)
        assertNull(decoded.tempC)
        assertNull(decoded.ambientDb)
    }

    @Test
    fun `a blank or corrupt blob decodes to an empty list, not an exception`() {
        assertEquals(emptyList<VitalsSample>(), VitalsJson.decodeSamples(null))
        assertEquals(emptyList<VitalsSample>(), VitalsJson.decodeSamples(""))
        assertEquals(emptyList<VitalsSample>(), VitalsJson.decodeSamples("{not json"))
    }

    @Test
    fun `a row with no id or an unknown source is dropped, not repaired`() {
        // Hand-written JSON, because these shapes cannot be produced by the
        // encoder - they are what an older or corrupted build left behind.
        val json = """
            [
              {"id":"ok","at":5,"heartRateBpm":70,"source":"phone"},
              {"at":6,"heartRateBpm":70,"source":"phone"},
              {"id":"","at":7,"heartRateBpm":70,"source":"phone"},
              {"id":"bad","at":8,"heartRateBpm":70,"source":"simulated"},
              {"id":"nosource","at":9,"heartRateBpm":70}
            ]
        """.trimIndent()

        val decoded = VitalsJson.decodeSamples(json)

        assertEquals(listOf("ok"), decoded.map { it.id })
    }

    @Test
    fun `thresholds survive a round trip`() {
        val original = VitalsThresholds(hrLow = 45, hrHigh = 120, spo2Low = 92, tempHigh = 37.5f)

        assertEquals(original, VitalsJson.decodeThresholds(VitalsJson.encodeThresholds(original)))
    }

    @Test
    fun `missing threshold fields fall back to the defaults, field by field`() {
        val decoded = VitalsJson.decodeThresholds("""{"hrHigh":110}""")

        val default = VitalsThresholds()
        assertEquals(110, decoded.hrHigh)
        assertEquals(default.hrLow, decoded.hrLow)
        assertEquals(default.spo2Low, decoded.spo2Low)
        assertEquals(default.tempHigh, decoded.tempHigh, 0.0001f)
    }

    @Test
    fun `a blank or corrupt threshold blob decodes to the defaults`() {
        assertEquals(VitalsThresholds(), VitalsJson.decodeThresholds(null))
        assertEquals(VitalsThresholds(), VitalsJson.decodeThresholds("not json at all"))
    }

    // ============================================
    // assess()
    // ============================================

    private val t = VitalsThresholds()

    @Test
    fun `a normal reading crosses nothing`() {
        val flags = t.assess(
            VitalsSample(heartRateBpm = 72, spo2Percent = 98, tempC = 36.6f)
        )
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `each threshold raises its own flag`() {
        assertEquals(
            listOf(VitalsFlag.HR_LOW),
            t.assess(VitalsSample(heartRateBpm = 39))
        )
        assertEquals(
            listOf(VitalsFlag.HR_HIGH),
            t.assess(VitalsSample(heartRateBpm = 131))
        )
        assertEquals(
            listOf(VitalsFlag.SPO2_LOW),
            t.assess(VitalsSample(spo2Percent = 89))
        )
        assertEquals(
            listOf(VitalsFlag.TEMP_HIGH),
            t.assess(VitalsSample(tempC = 38.1f))
        )
    }

    @Test
    fun `the boundary value itself is fine, not a flag`() {
        // Documented in VitalsThresholds.assess: a threshold means "past this",
        // not "at this". Asserted explicitly because it is the one behaviour
        // that would be silently reversed by an editor reaching for `<=`.
        assertTrue(t.assess(VitalsSample(heartRateBpm = 40)).isEmpty())
        assertTrue(t.assess(VitalsSample(heartRateBpm = 130)).isEmpty())
        assertTrue(t.assess(VitalsSample(spo2Percent = 90)).isEmpty())
        assertTrue(t.assess(VitalsSample(tempC = 38.0f)).isEmpty())
    }

    @Test
    fun `absent measurements raise no flags`() {
        assertTrue(t.assess(VitalsSample()).isEmpty())
        assertTrue(t.assess(VitalsSample(ambientDb = 95.0)).isEmpty())
    }

    @Test
    fun `several crossings are all reported`() {
        val flags = t.assess(VitalsSample(heartRateBpm = 150, spo2Percent = 85, tempC = 39.2f))

        assertEquals(
            setOf(VitalsFlag.HR_HIGH, VitalsFlag.SPO2_LOW, VitalsFlag.TEMP_HIGH),
            flags.toSet()
        )
    }

    @Test
    fun `edited thresholds are what is applied`() {
        val strict = VitalsThresholds(hrLow = 55, hrHigh = 100, spo2Low = 95, tempHigh = 37.2f)

        assertEquals(
            setOf(VitalsFlag.HR_LOW, VitalsFlag.SPO2_LOW, VitalsFlag.TEMP_HIGH),
            strict.assess(VitalsSample(heartRateBpm = 50, spo2Percent = 94, tempC = 37.4f)).toSet()
        )
    }
}
