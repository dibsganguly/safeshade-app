package com.safeshade.service

import com.safeshade.data.SmartHomeHook
import com.safeshade.data.SmartHomeProviders
import com.safeshade.data.SmartHomeTriggers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two rules that decide whether a house responds: the trigger matches and
 * the hook is on, and it has not already fired this minute.
 */
class SmartHomePlannerTest {

    private fun hook(
        id: String,
        trigger: String,
        enabled: Boolean = true
    ) = SmartHomeHook(
        id = id,
        name = id,
        trigger = trigger,
        provider = SmartHomeProviders.WEBHOOK,
        endpointUrl = "https://example.invalid/$id"
    ).copy(enabled = enabled)

    private val hooks = listOf(
        hook("fall-on", SmartHomeTriggers.FALL),
        hook("fall-off", SmartHomeTriggers.FALL, enabled = false),
        hook("sos-on", SmartHomeTriggers.SOS),
        hook("enter-on", SmartHomeTriggers.ZONE_ENTER),
        hook("fall-two", SmartHomeTriggers.FALL)
    )

    @Test
    fun `only the enabled hooks for that trigger fire`() {
        assertEquals(
            listOf("fall-on", "fall-two"),
            SmartHomePlanner.hooksFor(hooks, SmartHomeTriggers.FALL).map { it.id }
        )
    }

    @Test
    fun `a trigger nobody wired up fires nothing`() {
        assertTrue(
            SmartHomePlanner.hooksFor(hooks, SmartHomeTriggers.LOW_BATTERY).isEmpty()
        )
        assertTrue(SmartHomePlanner.hooksFor(emptyList(), SmartHomeTriggers.FALL).isEmpty())
    }

    @Test
    fun `zone enter and zone exit are not the same trigger`() {
        assertEquals(
            listOf("enter-on"),
            SmartHomePlanner.hooksFor(hooks, SmartHomeTriggers.ZONE_ENTER).map { it.id }
        )
        assertTrue(SmartHomePlanner.hooksFor(hooks, SmartHomeTriggers.ZONE_EXIT).isEmpty())
    }

    @Test
    fun `a hook that has never fired is due`() {
        assertTrue(SmartHomePlanner.due(null, 0L))
    }

    @Test
    fun `a second firing inside the minute is held back`() {
        assertFalse(SmartHomePlanner.due(1_000L, 1_000L))
        assertFalse(SmartHomePlanner.due(1_000L, 60_999L))
        assertTrue(SmartHomePlanner.due(1_000L, 61_000L))
        assertTrue(SmartHomePlanner.due(1_000L, 200_000L))
    }

    @Test
    fun `a clock that jumped backwards does not lock a hook out`() {
        assertTrue(SmartHomePlanner.due(10_000L, 5_000L))
    }
}
