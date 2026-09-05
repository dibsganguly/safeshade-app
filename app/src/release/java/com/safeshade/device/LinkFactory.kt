package com.safeshade.device

import android.content.Context
import com.safeshade.BleManager
import kotlinx.coroutines.CoroutineScope

/**
 * Release variant. Builds the real link and nothing else.
 *
 * There is a debug twin of this file at the same fully-qualified name, in
 * `src/debug`, which returns a link that can be switched to a scripted fake.
 * The variant source set decides which one is compiled, so the fake — its
 * fabricated telemetry, its scripted fall alerts — is not merely unreachable in
 * a release build, it is not present in the APK at all.
 *
 * That is the whole reason this is two files rather than one with an
 * `if (BuildConfig.DEBUG)` branch. A runtime check still compiles the fake in,
 * where a single mis-evaluated condition, a stale build config, or anything
 * that flips that flag produces a fall alert the user never had. On a
 * fall-detection product that is not a risk worth taking to save a file.
 */
object LinkFactory {
    fun create(context: Context, scope: CoroutineScope): DeviceLink =
        RealDeviceLink(BleManager(context.applicationContext), scope)

    /**
     * Reads the negotiated ATT MTU off whatever [create] returned.
     *
     * MTU is not on [DeviceLink] — it is meaningless for anything that is not a
     * real GATT connection — so unwrapping it is variant-specific, which makes
     * this the right file for it. `DeviceRepository` needs the real number
     * because an over-long Medical ID write is truncated by the ATT layer
     * silently rather than rejected.
     */
    fun mtuProvider(link: DeviceLink): () -> Int =
        { (link as? RealDeviceLink)?.mtu ?: DEFAULT_MTU }

    /** `BleManager.DESIRED_MTU`. The value before negotiation completes. */
    private const val DEFAULT_MTU = 247
}
