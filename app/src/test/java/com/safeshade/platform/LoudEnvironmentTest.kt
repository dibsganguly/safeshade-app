package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The loud-environment rule.
 *
 * The case worth reading is [a slammed door is not a loud environment]: the
 * whole reason [LoudEnvironment.SUSTAIN_MS] exists is that a level held for a
 * minute means something and a bang does not, so a five-second history must
 * not produce a verdict however loud it is.
 */
class LoudEnvironmentTest {

    private val now = 1_700_000_000_000L

    /** [count] windows ending at [endingAt], one every 250 ms, all at [db]. */
    private fun history(count: Int, db: Int, endingAt: Long = now): List<SoundReading> =
        (0 until count).map { i ->
            SoundReading(
                dbfsRounded = db - SoundMath.SPL_OFFSET_DB,
                approxDbSpl = db,
                at = endingAt - (count - 1 - i) * LoudEnvironment.WINDOW_MS
            )
        }

    @Test
    fun `nothing measured is not the same as quiet`() {
        assertEquals(LoudVerdict.NoData, LoudEnvironment.assess(emptyList(), now))
    }

    @Test
    fun `readings older than the window are no data`() {
        val stale = history(240, 95, endingAt = now - 10 * 60_000L)
        assertEquals(LoudVerdict.NoData, LoudEnvironment.assess(stale, now))
    }

    @Test
    fun `a slammed door is not a loud environment`() {
        // Five seconds at 100 dB. Loud, but nowhere near sustained.
        val bang = history(20, 100)
        assertTrue(LoudEnvironment.assess(bang, now) is LoudVerdict.NoData)
    }

    @Test
    fun `a minute at 90 dB is loud`() {
        val readings = history(240, 90)
        val verdict = LoudEnvironment.assess(readings, now)
        assertTrue("expected Loud, was $verdict", verdict is LoudVerdict.Loud)
        verdict as LoudVerdict.Loud
        assertEquals(readings.first().at, verdict.sinceMs)
        assertEquals(90, verdict.peakDb)
    }

    @Test
    fun `a minute at 80 dB is quiet`() {
        assertEquals(LoudVerdict.Quiet, LoudEnvironment.assess(history(240, 80), now))
    }

    @Test
    fun `exactly the warning level counts as loud`() {
        val verdict = LoudEnvironment.assess(history(240, LoudEnvironment.WARN_DB), now)
        assertTrue(verdict is LoudVerdict.Loud)
    }

    @Test
    fun `one spike does not drag a quiet minute over the line`() {
        // The median is what makes this hold: the mean of 239 readings at 60
        // plus one at 120 is still under 85, but a single louder spike in a
        // shorter history would move a mean and must not move this.
        val quiet = history(240, 60).toMutableList()
        quiet.add(SoundReading(dbfsRounded = 0, approxDbSpl = 120, at = now))
        assertEquals(LoudVerdict.Quiet, LoudEnvironment.assess(quiet, now))
    }

    @Test
    fun `half a minute loud inside a loud minute still reports the peak`() {
        val loud = history(120, 88, endingAt = now - 120 * LoudEnvironment.WINDOW_MS) +
            history(120, 96, endingAt = now)
        val verdict = LoudEnvironment.assess(loud, now)
        assertTrue(verdict is LoudVerdict.Loud)
        assertEquals(96, (verdict as LoudVerdict.Loud).peakDb)
    }
}
