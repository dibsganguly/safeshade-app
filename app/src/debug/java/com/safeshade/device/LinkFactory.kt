package com.safeshade.device

import android.content.Context
import com.safeshade.BleManager
import com.safeshade.debug.FakeDeviceLink
import com.safeshade.debug.Scenario
import com.safeshade.debug.SwitchableDeviceLink
import kotlinx.coroutines.CoroutineScope

/**
 * Debug variant. Builds a link that can be switched between the real radio and
 * a scripted fake at runtime.
 *
 * Same object name and same signature as the release twin in `src/release`, so
 * `AppContainer` calls `LinkFactory.create(...)` and never knows which one it
 * got. The variant source set is the boundary; see the release file for why
 * this is not a `BuildConfig.DEBUG` branch.
 *
 * The link starts on the real radio. Nothing is faked until somebody explicitly
 * asks for it via `SwitchableDeviceLink.useFake(true)` or `playScenario(...)`,
 * so a debug build on real hardware behaves exactly like a release one.
 */
object LinkFactory {
    fun create(context: Context, scope: CoroutineScope): DeviceLink {
        val real = RealDeviceLink(BleManager(context.applicationContext), scope)
        return SwitchableDeviceLink(
            real = real,
            // Starts idle rather than mid-timeline: a fake nobody has asked for
            // should not be burning a wakeup a second behind a real connection.
            fake = FakeDeviceLink(scope, Scenario.DISCONNECTED),
            scope = scope,
            useFake = false
        )
    }

    /**
     * Reads the negotiated ATT MTU, seeing through the switchable wrapper.
     *
     * Without the unwrap every debug build would budget Medical ID payloads
     * against the 247-byte default even when the device negotiated something
     * smaller — and a debug build is exactly what is running on the bench, so
     * the truncation would only ever show up in release.
     */
    fun mtuProvider(link: DeviceLink): () -> Int = {
        val underlying = (link as? SwitchableDeviceLink)?.real ?: link
        (underlying as? RealDeviceLink)?.mtu ?: DEFAULT_MTU
    }

    /** `BleManager.DESIRED_MTU`. The value before negotiation completes. */
    private const val DEFAULT_MTU = 247
}
