package com.safeshade.platform

import android.content.Intent
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The availability decision, on the JVM.
 *
 * [mapAvailability] takes the three Android answers as plain arguments and the
 * Play intent as a lambda precisely so this file can exist: `getSdkStatus`,
 * `getPackageInfo` and `Intent` all throw "Stub!" in a local unit test, and
 * this module has no `isReturnDefaultValues`. The lambda is `{ null }` here,
 * which exercises every branch without constructing one.
 *
 * The case that matters most is [Availability.NotInstalled]: the Android 13
 * test phone has no Health Connect, so that is the state the screen will
 * actually be photographed in.
 */
class HealthConnectAvailabilityTest {

    private val noIntent: () -> Intent? = { null }

    @Test
    fun `an available provider is available`() {
        val result = mapAvailability(
            sdkStatus = HealthConnectClient.SDK_AVAILABLE,
            providerInstalled = true,
            sdkInt = 33,
            playStoreIntent = noIntent
        )

        assertEquals(Availability.Available, result)
    }

    @Test
    fun `available wins even on an old phone`() {
        // The status code is the authority when it says available; the API-level
        // guard below it must not override a provider that is demonstrably there.
        val result = mapAvailability(
            sdkStatus = HealthConnectClient.SDK_AVAILABLE,
            providerInstalled = true,
            sdkInt = Build.VERSION_CODES.O,
            playStoreIntent = noIntent
        )

        assertEquals(Availability.Available, result)
    }

    @Test
    fun `the update-required code with no provider installed means not installed`() {
        // This is the Redmi Note 10S, Android 13, out of the box: the code says
        // "update required" for a package that is not there at all.
        val result = mapAvailability(
            sdkStatus = HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
            providerInstalled = false,
            sdkInt = 33,
            playStoreIntent = noIntent
        )

        assertTrue(result is Availability.NotInstalled)
        assertNull((result as Availability.NotInstalled).playStoreIntent)
    }

    @Test
    fun `the same code with the provider installed means it needs an update`() {
        val result = mapAvailability(
            sdkStatus = HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
            providerInstalled = true,
            sdkInt = 33,
            playStoreIntent = noIntent
        )

        assertEquals(Availability.NeedsUpdate, result)
    }

    @Test
    fun `unavailable with nothing installed offers the store`() {
        val result = mapAvailability(
            sdkStatus = HealthConnectClient.SDK_UNAVAILABLE,
            providerInstalled = false,
            sdkInt = 33,
            playStoreIntent = noIntent
        )

        assertTrue(result is Availability.NotInstalled)
    }

    @Test
    fun `unavailable with the provider present is unsupported, with a reason`() {
        val result = mapAvailability(
            sdkStatus = HealthConnectClient.SDK_UNAVAILABLE,
            providerInstalled = true,
            sdkInt = 33,
            playStoreIntent = noIntent
        )

        assertTrue(result is Availability.Unsupported)
        assertTrue((result as Availability.Unsupported).reason.isNotBlank())
    }

    @Test
    fun `below Android 9 is unsupported whatever the package manager says`() {
        // minSdk is 26, so this is reachable and must not offer an install
        // button that leads to an app the phone cannot run.
        val result = mapAvailability(
            sdkStatus = HealthConnectClient.SDK_UNAVAILABLE,
            providerInstalled = false,
            sdkInt = Build.VERSION_CODES.O,
            playStoreIntent = noIntent
        )

        assertTrue(result is Availability.Unsupported)
    }

    @Test
    fun `every failure reason is plain English, never the exception's own words`() {
        val reasons = listOf(
            reasonFor(SecurityException("permission denial: android.permission.health.READ_HEART_RATE")),
            reasonFor(IllegalStateException("HealthConnectClient not available")),
            reasonFor(java.io.IOException("Failed to connect to /127.0.0.1:8080")),
            reasonFor(RuntimeException("android.os.DeadObjectException"))
        )

        reasons.forEach { reason ->
            assertTrue(reason.isNotBlank())
            assertTrue(reason.first().isUpperCase())
            listOf("Exception", "android.", "permission denial", "127.0.0.1").forEach { leak ->
                assertTrue("Leaked '$leak' into: $reason", !reason.contains(leak))
            }
        }
    }

    @Test
    fun `readings report their newest moment and their emptiness honestly`() {
        val empty = VitalsResult.Readings()
        assertTrue(empty.isEmpty)
        assertNull(empty.newestAt)

        val some = VitalsResult.Readings(
            heartRateBpm = 70, heartRateAt = 100L,
            spo2Percent = 97, spo2At = 300L,
            bodyTempC = 36.5f, bodyTempAt = 200L
        )
        assertTrue(!some.isEmpty)
        assertEquals(300L, some.newestAt)
    }
}
