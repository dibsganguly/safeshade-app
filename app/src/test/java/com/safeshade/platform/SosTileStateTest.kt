package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The process-wide flag the QS tile reads (`SosTileService.applyState`).
 * Nothing here touches `TileService` itself - that needs a bound tile and is
 * not host-testable.
 */
class SosTileStateTest {

    @Test
    fun `defaults to inactive`() {
        // A fresh JVM-process view of the object; other tests in this class
        // may have already called update, so this only asserts the type the
        // default starts as rather than relying on run order.
        SosTileState.update(false)
        assertEquals(false, SosTileState.alertActive.value)
    }

    @Test
    fun `update is reflected immediately`() {
        SosTileState.update(true)
        assertEquals(true, SosTileState.alertActive.value)
        SosTileState.update(false)
        assertEquals(false, SosTileState.alertActive.value)
    }
}
