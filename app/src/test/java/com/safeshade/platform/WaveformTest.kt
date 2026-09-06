package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [bucketWaveform] — the pure reduction that turns raw `maxAmplitude` samples
 * into the 40-bar waveform the Talk thread draws. The edge cases matter more
 * than the common case: an empty recording, a shorter-than-40-samples
 * recording (padding, not a crash), and total silence (would otherwise divide
 * by zero).
 */
class WaveformTest {

    @Test
    fun `empty samples produce a flat zero waveform of the requested size`() {
        val result = bucketWaveform(emptyList(), buckets = 40)
        assertEquals(40, result.size)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun `all-silent samples do not divide by zero`() {
        val result = bucketWaveform(List(100) { 0 }, buckets = 40)
        assertEquals(40, result.size)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun `fewer samples than buckets are padded rather than crashing`() {
        val result = bucketWaveform(listOf(100, 200, 300), buckets = 40)
        assertEquals(40, result.size)
    }

    @Test
    fun `values are normalised into 0 to 1`() {
        val result = bucketWaveform(listOf(0, 8000, 16000, 32767), buckets = 4)
        assertEquals(4, result.size)
        assertTrue(result.all { it in 0f..1f })
        // The loudest bucket normalises to exactly 1.
        assertEquals(1f, result.max(), 0.0001f)
    }

    @Test
    fun `more samples than buckets are reduced, not truncated blindly`() {
        val samples = (1..400).map { it }
        val result = bucketWaveform(samples, buckets = 40)
        assertEquals(40, result.size)
        // Amplitude rises steadily, so the reduced waveform should too.
        for (i in 1 until result.size) {
            assertTrue(result[i] >= result[i - 1])
        }
    }
}
