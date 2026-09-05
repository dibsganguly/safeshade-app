package com.safeshade

import com.safeshade.ui.board.aroundTheClock
import com.safeshade.ui.board.formatClock
import com.safeshade.ui.board.spanWords
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The day strip's arithmetic, which is all modular and none of which can be
 * checked by looking at the screen.
 *
 * A quiet-hours span that crosses midnight is the *normal* case - almost nobody
 * sets quiet hours inside a single calendar day - so the wrapping paths here
 * are the common ones, not the edge cases. Getting the handle-picking wrong
 * near midnight produces a control that grabs the far handle when you touch the
 * near one, which reads as the control being broken rather than as an off-by-one.
 */
class DayStripTest {

    @Test
    fun `distance takes the short way round midnight`() {
        // 23:30 and 00:30 are an hour apart, not twenty-three. This is the
        // whole reason the helper exists: plain subtraction gives 1380 here and
        // hands a drag just after midnight to the wrong handle.
        assertEquals(60, aroundTheClock(23 * 60 + 30, 30))
        assertEquals(60, aroundTheClock(30, 23 * 60 + 30))
    }

    @Test
    fun `distance is symmetric and zero at the same time`() {
        assertEquals(0, aroundTheClock(7 * 60, 7 * 60))
        assertEquals(aroundTheClock(9 * 60, 17 * 60), aroundTheClock(17 * 60, 9 * 60))
    }

    @Test
    fun `distance never exceeds half a day`() {
        // The furthest two times can be is twelve hours, by definition of
        // "the short way round".
        assertEquals(12 * 60, aroundTheClock(0, 12 * 60))
        for (minutes in 0 until 24 * 60 step 15) {
            val d = aroundTheClock(0, minutes)
            assert(d in 0..(12 * 60)) { "distance $d out of range for $minutes" }
        }
    }

    @Test
    fun `a span that crosses midnight is measured forwards`() {
        // 22:00 to 07:00 is nine hours of quiet, not minus fifteen.
        assertEquals("9 hours", spanWords(22 * 60, 7 * 60))
    }

    @Test
    fun `a span inside one day is measured the obvious way`() {
        assertEquals("8 hours", spanWords(9 * 60, 17 * 60))
        assertEquals("1 hour", spanWords(60, 120))
        assertEquals("30 min", spanWords(0, 30))
        assertEquals("7h 30m", spanWords(22 * 60 + 30, 6 * 60))
    }

    @Test
    fun `equal ends are no span at all, not a whole day`() {
        // Ambiguous in principle; resolved as "nothing" because the setting it
        // backs treats a zero-length quiet period as off, and silently turning
        // quiet hours on for twenty-four hours would be the worse guess.
        assertEquals("nothing selected", spanWords(3 * 60, 3 * 60))
    }

    @Test
    fun `the clock is 24-hour and zero-padded`() {
        assertEquals("00:00", formatClock(0))
        assertEquals("07:05", formatClock(7 * 60 + 5))
        assertEquals("19:30", formatClock(19 * 60 + 30))
        assertEquals("23:55", formatClock(23 * 60 + 55))
    }

    @Test
    fun `the clock clamps rather than wrapping or throwing`() {
        // Fed from stored settings that predate the control, so a value out of
        // range has to render as something rather than crash a screen.
        assertEquals("00:00", formatClock(-1))
        assertEquals("23:59", formatClock(24 * 60))
        assertEquals("23:59", formatClock(Int.MAX_VALUE))
    }
}
