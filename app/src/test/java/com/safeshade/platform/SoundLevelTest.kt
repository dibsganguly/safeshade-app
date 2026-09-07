package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The meter's arithmetic, against signals whose level is known by construction
 * rather than by whatever the room sounded like.
 *
 * Note what the numbers actually are, because it is easy to expect the wrong
 * ones: with `dbfs + 90`, **full scale reads 90, not 120**. The 120 ceiling is
 * a clamp, and only an input above full scale can reach it - which is why the
 * cap is tested through [SoundMath.approxSplOf] directly rather than through a
 * sample buffer that cannot produce it.
 */
class SoundLevelTest {

    @Test
    fun `silence has no level`() {
        val silence = ShortArray(1600)
        assertEquals(0.0, SoundMath.rmsOf(silence), 0.0)
        assertTrue(SoundMath.dbfsOf(SoundMath.rmsOf(silence)).isInfinite())
    }

    @Test
    fun `silence reads as the floor, not as a negative number`() {
        assertEquals(30, SoundMath.approxSplOf(SoundMath.dbfsOf(0.0)))
        assertEquals(SoundMath.MIN_SPL, SoundMath.approxSplOf(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `an empty window is silence rather than an error`() {
        assertEquals(0.0, SoundMath.rmsOf(ShortArray(0)), 0.0)
    }

    @Test
    fun `full scale is zero dBFS and ninety on the approximate scale`() {
        // A square wave at full scale: RMS equals the amplitude.
        val fullScale = ShortArray(1600) { if (it % 2 == 0) 32767 else -32767 }
        val dbfs = SoundMath.dbfsOf(SoundMath.rmsOf(fullScale))
        assertEquals(0.0, dbfs, 0.01)
        assertEquals(90, SoundMath.approxSplOf(dbfs))
    }

    @Test
    fun `the scale is capped at 120 above full scale`() {
        // Only reachable arithmetically, which is the point of the clamp: no
        // phone microphone can honestly report a level above this.
        assertEquals(120, SoundMath.approxSplOf(40.0))
        assertEquals(SoundMath.MAX_SPL, SoundMath.approxSplOf(1000.0))
    }

    @Test
    fun `a sine 20 dB below full scale reads about 70`() {
        // RMS of a sine is peak over root two, so a peak of 0.1 * sqrt(2) *
        // full scale gives exactly -20 dBFS. Whole cycles, so the RMS is not
        // skewed by a partial one.
        val samplesPerCycle = 160
        val cycles = 10
        val peak = 0.1 * Math.sqrt(2.0) * SoundMath.FULL_SCALE
        val sine = ShortArray(samplesPerCycle * cycles) { i ->
            (peak * sin(2.0 * PI * i / samplesPerCycle)).roundToInt().toShort()
        }

        val dbfs = SoundMath.dbfsOf(SoundMath.rmsOf(sine))
        assertEquals(-20.0, dbfs, 0.2)

        val spl = SoundMath.approxSplOf(dbfs)
        assertTrue("expected about 70, was $spl", spl in 68..72)
    }

    @Test
    fun `the calibration note says the number is not a measurement`() {
        // The UI prints this verbatim, so the honest words have to be in it.
        val note = SoundLevelMeter.CALIBRATION_NOTE.lowercase()
        assertTrue(note.contains("uncalibrated"))
        assertTrue(note.contains("10 db"))
        assertTrue(note.contains("not a sound-level meter"))
    }
}
