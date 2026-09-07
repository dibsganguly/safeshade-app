package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualLeashTest {

    private val rssiUnknown = -127

    @Test
    fun `unknown rssi produces Unknown`() {
        val leash = VirtualLeash()
        assertEquals(LeashState.Unknown, leash.step(null, 0))
        assertEquals(LeashState.Unknown, leash.step(rssiUnknown, 1_000))
    }

    @Test
    fun `a strong reading is Near`() {
        val leash = VirtualLeash()
        val state = leash.step(-60, 0)
        assertTrue(state is LeashState.Near)
        assertEquals(-60, (state as LeashState.Near).dbm)
    }

    @Test
    fun `a mid-range reading is Drifting`() {
        val leash = VirtualLeash()
        val state = leash.step(-78, 0)
        assertTrue(state is LeashState.Drifting)
    }

    @Test
    fun `a weak reading is Far but not yet Broken`() {
        val leash = VirtualLeash(graceMs = 20_000)
        val state = leash.step(-90, 0)
        assertTrue(state is LeashState.Far)
    }

    @Test
    fun `far becomes Broken only after the grace period elapses`() {
        val leash = VirtualLeash(graceMs = 20_000)
        leash.step(-90, 0)
        val stillFar = leash.step(-90, 19_000)
        assertTrue("expected still Far just under grace", stillFar is LeashState.Far)

        val broken = leash.step(-90, 20_000)
        assertTrue("expected Broken once grace has elapsed", broken is LeashState.Broken)
    }

    @Test
    fun `hysteresis prevents a boundary reading from flapping Near to Drifting`() {
        // nearDbm = -70, hysteresisDbm = 4 by default: once Near, only a
        // reading below -74 should push into Drifting/Far.
        val leash = VirtualLeash(nearDbm = -70, farDbm = -85, hysteresisDbm = 4)
        assertTrue(leash.step(-68, 0) is LeashState.Near)
        // Still inside the hysteresis band around -70: stays Near.
        assertTrue(leash.step(-72, 1_000) is LeashState.Near)
        assertTrue(leash.step(-73, 2_000) is LeashState.Near)
        // Now clearly past the band.
        assertTrue(leash.step(-76, 3_000) is LeashState.Drifting)
    }

    @Test
    fun `hysteresis prevents flapping back to Near from Drifting on a small recovery`() {
        val leash = VirtualLeash(nearDbm = -70, farDbm = -85, hysteresisDbm = 4)
        leash.step(-78, 0)
        // A small recovery within the band should not promote back to Near.
        assertTrue(leash.step(-71, 1_000) is LeashState.Drifting)
        // A clear recovery past the band does.
        assertTrue(leash.step(-65, 2_000) is LeashState.Near)
    }

    @Test
    fun `a dropped link while already far past grace reports Broken`() {
        val leash = VirtualLeash(graceMs = 5_000)
        leash.step(-90, 0)
        leash.step(-90, 5_000) // now Broken
        val afterDrop = leash.step(null, 6_000)
        assertTrue(afterDrop is LeashState.Broken)
    }

    @Test
    fun `a dropped link before grace elapses is only Unknown`() {
        val leash = VirtualLeash(graceMs = 20_000)
        leash.step(-90, 0)
        val afterDrop = leash.step(null, 1_000)
        assertEquals(LeashState.Unknown, afterDrop)
    }

    @Test
    fun `describe renders each state with the expected copy`() {
        val leash = VirtualLeash()
        assertEquals("—", leash.describe(LeashState.Unknown, 0))
        assertEquals("About arm's length", leash.describe(LeashState.Near(-60), 0))
        assertEquals("Drifting away", leash.describe(LeashState.Drifting(-78), 0))
        assertEquals("Out of range for 40 s", leash.describe(LeashState.Far(-90, 0), 40_000))
        assertEquals("Out of range for 40 s", leash.describe(LeashState.Broken(0), 40_000))
    }
}
