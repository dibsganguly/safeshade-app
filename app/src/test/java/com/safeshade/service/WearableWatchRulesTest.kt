package com.safeshade.service

import com.safeshade.data.WearableWatchMarks
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearableWatchRulesTest {

    private fun marks(
        lastConnectedAt: Long? = null,
        offlineAlerted: Boolean = false,
        lowBatteryAlerted: Boolean = false
    ) = WearableWatchMarks(
        lastConnectedAt = lastConnectedAt,
        lastAddress = "",
        offlineAlerted = offlineAlerted,
        lowBatteryAlerted = lowBatteryAlerted
    )

    private fun decide(
        now: Long = 0L,
        connected: Boolean = false,
        marks: WearableWatchMarks = marks(),
        offlineAlertMinutes: Int = 30,
        lowBatteryPercent: Int = 20,
        batteryLevel: Int = 50,
        isRealData: Boolean = true
    ) = decideWatchAlerts(
        now = now,
        connected = connected,
        marks = marks,
        offlineAlertMinutes = offlineAlertMinutes,
        lowBatteryPercent = lowBatteryPercent,
        batteryLevel = batteryLevel,
        isRealData = isRealData
    )

    // --- offline: zero threshold / no baseline ---

    @Test
    fun zero_offline_threshold_is_silent() {
        val d = decide(
            offlineAlertMinutes = 0,
            connected = false,
            marks = marks(lastConnectedAt = 0L),
            now = 1_000_000_000L
        )
        assertFalse(d.raiseOffline)
    }

    @Test
    fun null_last_connected_never_raises_offline() {
        val d = decide(
            connected = false,
            marks = marks(lastConnectedAt = null),
            offlineAlertMinutes = 30,
            now = 1_000_000_000L
        )
        assertFalse(d.raiseOffline)
    }

    // --- offline: boundary ---

    @Test
    fun raises_offline_exactly_at_threshold_boundary() {
        val lastConnectedAt = 0L
        val thresholdMs = 30 * 60_000L
        val d = decide(
            connected = false,
            marks = marks(lastConnectedAt = lastConnectedAt),
            offlineAlertMinutes = 30,
            now = lastConnectedAt + thresholdMs
        )
        assertTrue(d.raiseOffline)
    }

    @Test
    fun does_not_raise_offline_one_ms_before_threshold() {
        val lastConnectedAt = 0L
        val thresholdMs = 30 * 60_000L
        val d = decide(
            connected = false,
            marks = marks(lastConnectedAt = lastConnectedAt),
            offlineAlertMinutes = 30,
            now = lastConnectedAt + thresholdMs - 1
        )
        assertFalse(d.raiseOffline)
    }

    @Test
    fun does_not_reraise_offline_when_already_alerted() {
        val lastConnectedAt = 0L
        val d = decide(
            connected = false,
            marks = marks(lastConnectedAt = lastConnectedAt, offlineAlerted = true),
            offlineAlertMinutes = 30,
            now = lastConnectedAt + 30 * 60_000L
        )
        assertFalse(d.raiseOffline)
    }

    @Test
    fun connected_never_raises_offline() {
        val lastConnectedAt = 0L
        val d = decide(
            connected = true,
            marks = marks(lastConnectedAt = lastConnectedAt),
            offlineAlertMinutes = 30,
            now = lastConnectedAt + 999 * 60_000L
        )
        assertFalse(d.raiseOffline)
    }

    // --- low battery: preconditions ---

    @Test
    fun low_battery_needs_connected() {
        val d = decide(connected = false, isRealData = true, lowBatteryPercent = 20, batteryLevel = 10)
        assertFalse(d.raiseLowBattery)
    }

    @Test
    fun low_battery_needs_real_data() {
        val d = decide(connected = true, isRealData = false, lowBatteryPercent = 20, batteryLevel = 10)
        assertFalse(d.raiseLowBattery)
    }

    @Test
    fun low_battery_needs_percent_greater_than_zero() {
        val d = decide(connected = true, isRealData = true, lowBatteryPercent = 0, batteryLevel = 0)
        assertFalse(d.raiseLowBattery)
    }

    // --- low battery: raise ---

    @Test
    fun raises_low_battery_at_level_equal_to_threshold() {
        val d = decide(connected = true, isRealData = true, lowBatteryPercent = 20, batteryLevel = 20)
        assertTrue(d.raiseLowBattery)
    }

    @Test
    fun does_not_raise_low_battery_when_already_alerted() {
        val d = decide(
            connected = true,
            isRealData = true,
            lowBatteryPercent = 20,
            batteryLevel = 10,
            marks = marks(lowBatteryAlerted = true)
        )
        assertFalse(d.raiseLowBattery)
    }

    // --- low battery: clear ---

    @Test
    fun clear_low_battery_does_not_clear_at_exact_recovery_boundary() {
        val d = decide(
            connected = true,
            isRealData = true,
            lowBatteryPercent = 20,
            batteryLevel = 20 + RECOVERY_MARGIN,
            marks = marks(lowBatteryAlerted = true)
        )
        assertFalse(d.clearLowBattery)
    }

    @Test
    fun clear_low_battery_clears_one_above_recovery_boundary() {
        val d = decide(
            connected = true,
            isRealData = true,
            lowBatteryPercent = 20,
            batteryLevel = 20 + RECOVERY_MARGIN + 1,
            marks = marks(lowBatteryAlerted = true)
        )
        assertTrue(d.clearLowBattery)
    }

    @Test
    fun clear_low_battery_only_when_alerted() {
        val d = decide(
            connected = true,
            isRealData = true,
            lowBatteryPercent = 20,
            batteryLevel = 20 + RECOVERY_MARGIN + 1,
            marks = marks(lowBatteryAlerted = false)
        )
        assertFalse(d.clearLowBattery)
    }
}
