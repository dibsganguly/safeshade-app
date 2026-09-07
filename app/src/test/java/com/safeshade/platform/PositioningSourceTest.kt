package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class PositioningSourceTest {

    @Test
    fun `a device fix is always GNSS_DEVICE regardless of provider string`() {
        val line = PositioningReadout.from(provider = "gps", accuracyM = 5f, ageMs = 1_000, fromDevice = true)
        assertEquals(PositioningSource.GNSS_DEVICE, line.source)
    }

    @Test
    fun `gps and fused providers map to PHONE_FUSED`() {
        assertEquals(
            PositioningSource.PHONE_FUSED,
            PositioningReadout.from("gps", 5f, 1_000, fromDevice = false).source,
        )
        assertEquals(
            PositioningSource.PHONE_FUSED,
            PositioningReadout.from("fused", 5f, 1_000, fromDevice = false).source,
        )
    }

    @Test
    fun `network provider maps to NETWORK`() {
        assertEquals(
            PositioningSource.NETWORK,
            PositioningReadout.from("network", 20f, 1_000, fromDevice = false).source,
        )
    }

    @Test
    fun `null provider maps to NONE`() {
        assertEquals(
            PositioningSource.NONE,
            PositioningReadout.from(null, null, null, fromDevice = false).source,
        )
    }

    @Test
    fun `an unrecognised provider also maps to NONE`() {
        assertEquals(
            PositioningSource.NONE,
            PositioningReadout.from("cell_gateway_fix", 100f, 1_000, fromDevice = false).source,
        )
    }

    @Test
    fun `accuracy renders as a rounded metre figure or a dash when unknown`() {
        assertEquals("±12 m", PositioningReadout.from("gps", 12.4f, 1_000, false).accuracyText)
        assertEquals("—", PositioningReadout.from("gps", null, 1_000, false).accuracyText)
        assertEquals("—", PositioningReadout.from("gps", -1f, 1_000, false).accuracyText)
    }

    @Test
    fun `age renders as just now, seconds, minutes, hours or a dash`() {
        assertEquals("just now", PositioningReadout.from("gps", 5f, 5_000, false).ageText)
        assertEquals("40 s ago", PositioningReadout.from("gps", 5f, 40_000, false).ageText)
        assertEquals("5 min ago", PositioningReadout.from("gps", 5f, 5 * 60_000L, false).ageText)
        assertEquals("2 h ago", PositioningReadout.from("gps", 5f, 2 * 3_600_000L, false).ageText)
        assertEquals("—", PositioningReadout.from("gps", 5f, null, false).ageText)
        assertEquals("—", PositioningReadout.from("gps", 5f, -1, false).ageText)
    }

    @Test
    fun `label strings are the ones the UI depends on`() {
        assertEquals("Wearable GPS", PositioningSource.GNSS_DEVICE.label)
        assertEquals("Phone", PositioningSource.PHONE_FUSED.label)
        assertEquals("Network", PositioningSource.NETWORK.label)
        assertEquals("Gateway cell fix", PositioningSource.CELL_GATEWAY.label)
        assertEquals("—", PositioningSource.NONE.label)
    }
}
