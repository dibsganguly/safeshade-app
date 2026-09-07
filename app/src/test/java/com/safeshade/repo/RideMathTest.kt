package com.safeshade.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideMathTest {

    @Test
    fun `haversine distance between the same point is zero`() {
        assertEquals(0.0, RideMath.haversineMeters(12.9716, 77.5946, 12.9716, 77.5946), 0.001)
    }

    @Test
    fun `haversine distance for one degree of latitude is about 111km`() {
        // A well-known reference value: one degree of latitude is
        // approximately 111.2 km anywhere on Earth, since lines of longitude
        // converge but lines of latitude do not.
        val meters = RideMath.haversineMeters(0.0, 0.0, 1.0, 0.0)
        assertTrue("expected roughly 111000-111400 m, got $meters", meters in 111_000.0..111_400.0)
    }

    @Test
    fun `haversine distance for one degree of longitude at the equator is about 111km`() {
        // At the equator, cos(latitude) is 1, so a degree of longitude
        // spans the same distance as a degree of latitude.
        val meters = RideMath.haversineMeters(0.0, 0.0, 0.0, 1.0)
        assertTrue("expected roughly 111000-111400 m, got $meters", meters in 111_000.0..111_400.0)
    }

    @Test
    fun `first fix adds no distance and starts the accumulator`() {
        val acc = RideMath.accumulate(RideAccumulator(), 10.0, 20.0, speedMps = null, accuracyM = null, at = 1_000L)
        assertEquals(0.0, acc.distanceM, 0.0)
        assertEquals(1, acc.samples)
    }

    @Test
    fun `a second fix adds the haversine distance between them`() {
        var acc = RideMath.accumulate(RideAccumulator(), 12.9716, 77.5946, speedMps = null, accuracyM = null, at = 1_000L)
        acc = RideMath.accumulate(acc, 12.9720, 77.5950, speedMps = null, accuracyM = null, at = 2_000L)
        assertTrue("expected positive distance, got ${acc.distanceM}", acc.distanceM > 0.0)
        assertEquals(2, acc.samples)
    }

    @Test
    fun `a fix with accuracy worse than 50m is ignored entirely`() {
        var acc = RideMath.accumulate(RideAccumulator(), 12.9716, 77.5946, speedMps = null, accuracyM = null, at = 1_000L)
        val before = acc
        acc = RideMath.accumulate(acc, 12.9720, 77.5950, speedMps = null, accuracyM = 51f, at = 2_000L)
        assertEquals(before, acc)
    }

    @Test
    fun `a fix with accuracy at exactly 50m is accepted`() {
        var acc = RideMath.accumulate(RideAccumulator(), 12.9716, 77.5946, speedMps = null, accuracyM = null, at = 1_000L)
        acc = RideMath.accumulate(acc, 12.9720, 77.5950, speedMps = null, accuracyM = 50f, at = 2_000L)
        assertEquals(2, acc.samples)
    }

    @Test
    fun `max speed tracks the fastest fix seen`() {
        var acc = RideMath.accumulate(RideAccumulator(), 0.0, 0.0, speedMps = 2f, accuracyM = null, at = 1_000L)
        acc = RideMath.accumulate(acc, 0.001, 0.001, speedMps = 5f, accuracyM = null, at = 2_000L)
        acc = RideMath.accumulate(acc, 0.002, 0.002, speedMps = 1f, accuracyM = null, at = 3_000L)
        assertEquals(5f, acc.maxSpeedMps, 0f)
    }

    @Test
    fun `moving seconds accumulate only while speed is above the moving threshold`() {
        var acc = RideMath.accumulate(RideAccumulator(), 0.0, 0.0, speedMps = 2f, accuracyM = null, at = 0L)
        // 10s gap while moving fast: counts.
        acc = RideMath.accumulate(acc, 0.001, 0.001, speedMps = 2f, accuracyM = null, at = 10_000L)
        assertEquals(10, acc.movingSeconds)

        // 10s more, but this fix reports standing still: the gap that just
        // elapsed was moving (credited above), the still reading itself adds
        // no further moving time until a further fast fix arrives.
        acc = RideMath.accumulate(acc, 0.0011, 0.0011, speedMps = 0f, accuracyM = null, at = 20_000L)
        assertEquals(10, acc.movingSeconds)
    }

    @Test
    fun `a speed at or below the 0-8 threshold does not count as moving`() {
        var acc = RideMath.accumulate(RideAccumulator(), 0.0, 0.0, speedMps = 0.8f, accuracyM = null, at = 0L)
        acc = RideMath.accumulate(acc, 0.001, 0.001, speedMps = 0.8f, accuracyM = null, at = 10_000L)
        assertEquals(0, acc.movingSeconds)
    }

    @Test
    fun `a null speed never counts as moving and never raises max speed`() {
        var acc = RideMath.accumulate(RideAccumulator(), 0.0, 0.0, speedMps = null, accuracyM = null, at = 0L)
        acc = RideMath.accumulate(acc, 0.001, 0.001, speedMps = null, accuracyM = null, at = 10_000L)
        assertEquals(0, acc.movingSeconds)
        assertEquals(0f, acc.maxSpeedMps, 0f)
    }

    @Test
    fun `an ignored fix does not become the reference point for the next distance`() {
        var acc = RideMath.accumulate(RideAccumulator(), 12.9716, 77.5946, speedMps = null, accuracyM = null, at = 1_000L)
        // Dropped: bad accuracy.
        acc = RideMath.accumulate(acc, 40.0, 40.0, speedMps = null, accuracyM = 200f, at = 2_000L)
        val distanceAfterDrop = acc.distanceM
        // Should measure from the original point, not the dropped 40,40 one.
        acc = RideMath.accumulate(acc, 12.9720, 77.5950, speedMps = null, accuracyM = null, at = 3_000L)
        val added = acc.distanceM - distanceAfterDrop
        assertTrue("expected a small added distance, got $added", added in 0.0..1000.0)
    }

    @Test
    fun `a fresh accumulator has no last fix to measure from`() {
        val acc = RideAccumulator()
        assertNull(acc.lastLat)
        assertNull(acc.lastLon)
        assertNull(acc.lastAt)
    }
}
