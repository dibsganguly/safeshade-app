package com.safeshade.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Heart rate, blood oxygen and body temperature, read out of Health Connect.
 *
 * ### Why this exists
 *
 * The wearable's `HEALTH_CHAR` carries the Medical ID, not vitals, and the S1's
 * telemetry is accelerometer, battery and light. So on a phone paired with a
 * device that has no vitals sensor, the only honest source of a heart rate is
 * whatever the person's watch or fitness app already wrote to the phone's own
 * health store. This class reads that store and nothing else — it never
 * synthesises a number, and a store with no samples in the window produces
 * nulls, which the UI draws as a dash.
 *
 * ### The state most phones are actually in
 *
 * Health Connect is a separate app (`com.google.android.apps.healthdata`) that
 * ships preinstalled only on newer builds. The Android 13 test phone does not
 * have it. [Availability.NotInstalled] is therefore the normal case, not an
 * error case, and it carries a Play Store intent so the UI has somewhere to
 * send the user rather than a dead end.
 *
 * Two things about that state are easy to get wrong and are handled here:
 *
 *  - **`getSdkStatus` cannot tell "missing" from "too old".** It returns
 *    `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` for both. The two need different
 *    copy and a different button, so the provider package is looked up directly
 *    to split them — which is why `AndroidManifest.xml` declares it under
 *    `<queries>`; without that the lookup fails on every API 30+ phone that
 *    *does* have Health Connect.
 *  - **`HealthConnectClient.getOrCreate` throws when the provider is absent.**
 *    Every entry point below therefore checks [availability] first and returns a
 *    value rather than letting that exception escape.
 *
 * ### What the caller still has to handle
 *
 *  - [playStoreIntent] is built without `resolveActivity`, deliberately: Play
 *    is not in `<queries>` either, so on API 30+ the resolve would return null
 *    on a phone that has Play installed and the button would vanish on the one
 *    phone that needs it. The launcher must catch `ActivityNotFoundException`.
 *  - [permissionsContract] is safe to *build* at any time, but *launching* it
 *    with no provider installed crashes. Gate the launch on
 *    [Availability.Available].
 *
 * Nothing here throws to its caller and nothing here puts a raw exception
 * message in front of a person; the raw text goes to `Log.w` under
 * [TAG] and the caller gets plain English. No reading, timestamp or package
 * name is ever logged.
 */
class HealthConnectVitals(context: Context) {

    private val appContext = context.applicationContext

    /** Can this phone read vitals at all, and if not, what is the next move? */
    fun availability(): Availability = mapAvailability(
        sdkStatus = runCatching { HealthConnectClient.getSdkStatus(appContext) }
            .getOrElse { HealthConnectClient.SDK_UNAVAILABLE },
        providerInstalled = isProviderInstalled(),
        sdkInt = Build.VERSION.SDK_INT,
        playStoreIntent = { playStoreIntent() }
    )

    /**
     * The contract for asking Health Connect for the three read permissions.
     *
     * Health Connect grants these in its own UI, not the platform's, so this is
     * not `RequestMultiplePermissions` and the result is the set that came back
     * granted — which can be a subset. Compare it against [READ_PERMISSIONS].
     */
    fun permissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    /** The permissions to pass to [permissionsContract]'s launcher. */
    val requiredPermissions: Set<String> get() = READ_PERMISSIONS

    /**
     * Which of [READ_PERMISSIONS] this app currently holds.
     *
     * An empty set when there is no provider — the honest answer, since a
     * permission that cannot be exercised is not held in any useful sense.
     */
    suspend fun grantedPermissions(): Set<String> = grantedOrNull().orEmpty()

    /**
     * As [grantedPermissions], but null when the question could not be asked.
     *
     * The distinction is the difference between telling someone "you have not
     * allowed this" and "this could not be checked just now". The public
     * wrapper flattens the two because a caller drawing a checklist has nothing
     * else to draw; [latest] keeps them apart, because offering an Allow button
     * for a permission the user already granted is a lie about their phone.
     */
    private suspend fun grantedOrNull(): Set<String>? {
        if (availability() !is Availability.Available) return emptySet()
        return withContext(Dispatchers.IO) {
            runCatching {
                client()?.permissionController?.getGrantedPermissions().orEmpty()
                    .intersect(READ_PERMISSIONS)
            }.getOrElse {
                Log.w(TAG, "Could not read granted permissions: ${it.javaClass.simpleName}")
                null
            }
        }
    }

    /**
     * The newest heart rate, oxygen saturation and body temperature written
     * since [sinceMillis].
     *
     * Each record type is read in its own `runCatching`. One provider that
     * refuses one type — an older Health Connect that does not know a record,
     * a permission granted for two of the three — must not cost the caller the
     * other two, which is exactly what a single try around all three would do.
     *
     * Every field is independently nullable because one moment carries whatever
     * was readable then. Absent is null; there is no zero-as-absent here.
     */
    suspend fun latest(sinceMillis: Long): VitalsResult {
        if (availability() !is Availability.Available) return VitalsResult.NotAvailable

        val granted = grantedOrNull()
            ?: return VitalsResult.Failed("Health Connect could not say what SafeShade is allowed to read")
        if (granted.isEmpty()) return VitalsResult.NoPermission

        return withContext(Dispatchers.IO) {
            val hc = client() ?: return@withContext VitalsResult.NotAvailable
            val range = runCatching { TimeRangeFilter.after(Instant.ofEpochMilli(sinceMillis)) }
                .getOrElse { return@withContext VitalsResult.Failed("That time range could not be read") }

            var deniedAll = true
            var anyFailure: String? = null
            val origins = mutableListOf<String>()

            var hr: Int? = null
            var hrAt: Long? = null
            if (HealthPermission.getReadPermission(HeartRateRecord::class) in granted) {
                when (val r = readOne(hc, HeartRateRecord::class.java, range)) {
                    is OneRecord.Ok -> {
                        deniedAll = false
                        val record = r.record as HeartRateRecord
                        // A HeartRateRecord is an interval holding many samples;
                        // the record's own endTime is the window, not a reading.
                        val newest = record.samples.maxByOrNull { it.time }
                        if (newest != null) {
                            hr = newest.beatsPerMinute.toInt()
                            hrAt = newest.time.toEpochMilli()
                            record.metadata.dataOrigin.packageName
                                .takeIf { it.isNotBlank() }?.let(origins::add)
                        }
                    }
                    is OneRecord.Denied -> Unit
                    is OneRecord.Failed -> {
                        // A failed read is not a refused one. Leaving deniedAll
                        // true here would report a timeout as "not allowed" and
                        // send the user to a permission screen that is already
                        // showing the permission granted.
                        deniedAll = false
                        anyFailure = anyFailure ?: r.reason
                    }
                    OneRecord.Empty -> deniedAll = false
                }
            }

            var spo2: Int? = null
            var spo2At: Long? = null
            if (HealthPermission.getReadPermission(OxygenSaturationRecord::class) in granted) {
                when (val r = readOne(hc, OxygenSaturationRecord::class.java, range)) {
                    is OneRecord.Ok -> {
                        deniedAll = false
                        val record = r.record as OxygenSaturationRecord
                        spo2 = record.percentage.value.roundToInt()
                        spo2At = record.time.toEpochMilli()
                        record.metadata.dataOrigin.packageName
                            .takeIf { it.isNotBlank() }?.let(origins::add)
                    }
                    is OneRecord.Denied -> Unit
                    is OneRecord.Failed -> {
                        // A failed read is not a refused one. Leaving deniedAll
                        // true here would report a timeout as "not allowed" and
                        // send the user to a permission screen that is already
                        // showing the permission granted.
                        deniedAll = false
                        anyFailure = anyFailure ?: r.reason
                    }
                    OneRecord.Empty -> deniedAll = false
                }
            }

            var tempC: Float? = null
            var tempAt: Long? = null
            if (HealthPermission.getReadPermission(BodyTemperatureRecord::class) in granted) {
                when (val r = readOne(hc, BodyTemperatureRecord::class.java, range)) {
                    is OneRecord.Ok -> {
                        deniedAll = false
                        val record = r.record as BodyTemperatureRecord
                        tempC = record.temperature.inCelsius.toFloat()
                        tempAt = record.time.toEpochMilli()
                        record.metadata.dataOrigin.packageName
                            .takeIf { it.isNotBlank() }?.let(origins::add)
                    }
                    is OneRecord.Denied -> Unit
                    is OneRecord.Failed -> {
                        // A failed read is not a refused one. Leaving deniedAll
                        // true here would report a timeout as "not allowed" and
                        // send the user to a permission screen that is already
                        // showing the permission granted.
                        deniedAll = false
                        anyFailure = anyFailure ?: r.reason
                    }
                    OneRecord.Empty -> deniedAll = false
                }
            }

            // Every type this app is allowed to read came back refused. That is
            // a permission problem, not a read problem, and saying so sends the
            // user to the screen that can fix it.
            if (deniedAll) return@withContext VitalsResult.NoPermission
            val failure = anyFailure
            if (failure != null && hr == null && spo2 == null && tempC == null) {
                return@withContext VitalsResult.Failed(failure)
            }

            VitalsResult.Readings(
                heartRateBpm = hr,
                heartRateAt = hrAt,
                spo2Percent = spo2,
                spo2At = spo2At,
                bodyTempC = tempC,
                bodyTempAt = tempAt,
                // One name, when they all agree. Two watches writing to the same
                // store is a real situation and "Fitbit" would then be a claim
                // this app cannot support, so it says nothing instead.
                originPackage = origins.distinct().singleOrNull()
            )
        }
    }

    // ============================================
    // Internals
    // ============================================

    private sealed interface OneRecord {
        data class Ok(val record: Any) : OneRecord
        data object Empty : OneRecord
        data object Denied : OneRecord
        data class Failed(val reason: String) : OneRecord
    }

    private suspend fun <T : androidx.health.connect.client.records.Record> readOne(
        hc: HealthConnectClient,
        type: Class<T>,
        range: TimeRangeFilter
    ): OneRecord = try {
        val response = hc.readRecords(
            ReadRecordsRequest(
                recordType = type.kotlin,
                timeRangeFilter = range,
                ascendingOrder = false,
                pageSize = 1
            )
        )
        response.records.firstOrNull()?.let { OneRecord.Ok(it) } ?: OneRecord.Empty
    } catch (e: SecurityException) {
        // Not a failure worth a red banner: the user simply has not allowed
        // this one yet, and the permission screen is the answer.
        Log.w(TAG, "Read refused for ${type.simpleName}")
        OneRecord.Denied
    } catch (e: Exception) {
        Log.w(TAG, "Read failed for ${type.simpleName}: ${e.javaClass.simpleName}")
        OneRecord.Failed(reasonFor(e))
    }

    /** Null rather than a throw when the provider is gone. See the class KDoc. */
    private fun client(): HealthConnectClient? =
        runCatching { HealthConnectClient.getOrCreate(appContext) }.getOrElse {
            Log.w(TAG, "Health Connect client unavailable: ${it.javaClass.simpleName}")
            null
        }

    private fun isProviderInstalled(): Boolean = try {
        appContext.packageManager.getPackageInfo(PROVIDER_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    } catch (e: Exception) {
        false
    }

    /**
     * Opens Health Connect's Play listing, with the onboarding deep link Google
     * documents so the user lands in setup rather than on a bare store page.
     *
     * Not resolve-checked. See the class KDoc for why that would be worse.
     */
    private fun playStoreIntent(): Intent? = runCatching {
        Intent(Intent.ACTION_VIEW)
            .setPackage(PLAY_PACKAGE)
            .setData(Uri.parse(PLAY_URI))
            .putExtra("overlay", true)
            .putExtra("callerId", appContext.packageName)
    }.getOrNull()

    private companion object {
        const val TAG = "SafeShadeVitals"
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
        const val PLAY_PACKAGE = "com.android.vending"
        const val PLAY_URI =
            "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
    }
}

/** Every read permission this app asks Health Connect for. */
val READ_PERMISSIONS: Set<String> = setOf(
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    HealthPermission.getReadPermission(BodyTemperatureRecord::class)
)

/**
 * Whether vitals can be read on this phone.
 *
 * [NotInstalled] is the ordinary state on most Android 13 handsets, and it is
 * not an error — it carries the way out of it.
 */
sealed interface Availability {

    /** The provider is installed and current. Permissions may still be missing. */
    data object Available : Availability

    /**
     * There is no Health Connect on this phone.
     *
     * @param playStoreIntent where to send the user. Null only when the intent
     *   could not be built at all; the launcher must still catch
     *   `ActivityNotFoundException`, because a phone with no Play Store gives
     *   no warning until the launch.
     */
    data class NotInstalled(val playStoreIntent: Intent?) : Availability

    /** Installed, but older than the version this app was built against. */
    data object NeedsUpdate : Availability

    /** This phone cannot run Health Connect at all. [reason] is shown as written. */
    data class Unsupported(val reason: String) : Availability
}

/**
 * The outcome of one read. Modelled as a value for the same reason as
 * `CloudResult`: a caller cannot accidentally succeed by forgetting a catch.
 */
sealed interface VitalsResult {

    /**
     * A read happened. Any field may be null, and null means "nothing was
     * written for this in the window" — never zero, never a placeholder.
     *
     * @param originPackage the app that wrote the readings, when they all came
     *   from one. Null when several apps contributed or none named itself.
     */
    data class Readings(
        val heartRateBpm: Int? = null,
        val heartRateAt: Long? = null,
        val spo2Percent: Int? = null,
        val spo2At: Long? = null,
        val bodyTempC: Float? = null,
        val bodyTempAt: Long? = null,
        val originPackage: String? = null
    ) : VitalsResult {

        /** True when the read produced no measurement at all. */
        val isEmpty: Boolean
            get() = heartRateBpm == null && spo2Percent == null && bodyTempC == null

        /** The newest moment any of these was measured, or null when empty. */
        val newestAt: Long?
            get() = listOfNotNull(heartRateAt, spo2At, bodyTempAt).maxOrNull()
    }

    /** The read was attempted and did not work. [reason] is plain English. */
    data class Failed(val reason: String) : VitalsResult

    /** There is no Health Connect to read from. Not an error. */
    data object NotAvailable : VitalsResult

    /** Health Connect is there, but has not been allowed to share these. */
    data object NoPermission : VitalsResult
}

/**
 * The availability decision, with the Android calls lifted out.
 *
 * Pure and therefore testable: `getSdkStatus`, `getPackageInfo` and `Intent`
 * are all Android, and this module's unit tests run on the JVM where those
 * throw "Stub!". The intent arrives as a lambda so a test can pass `{ null }`
 * without constructing one.
 */
internal fun mapAvailability(
    sdkStatus: Int,
    providerInstalled: Boolean,
    sdkInt: Int,
    playStoreIntent: () -> Intent?
): Availability = when {
    sdkStatus == HealthConnectClient.SDK_AVAILABLE -> Availability.Available

    // Below Android 9 there is no Health Connect at all, whatever the status
    // code says. minSdk here is 26, so this branch is reachable.
    sdkInt < Build.VERSION_CODES.P ->
        Availability.Unsupported("Health Connect needs Android 9 or newer")

    // The one code that means both "missing" and "out of date". The package
    // lookup is what separates them.
    sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
        if (providerInstalled) Availability.NeedsUpdate
        else Availability.NotInstalled(playStoreIntent())

    // SDK_UNAVAILABLE with the app present is a provider that will not start.
    providerInstalled ->
        Availability.Unsupported("Health Connect is installed but not working on this phone")

    else -> Availability.NotInstalled(playStoreIntent())
}

/** Plain English for a read that failed. Never the exception's own words. */
internal fun reasonFor(e: Throwable): String = when (e) {
    is SecurityException -> "SafeShade has not been allowed to read this"
    is IllegalStateException -> "Health Connect is not ready on this phone"
    is java.io.IOException -> "Health Connect could not be reached"
    else -> "Health Connect could not be read just now"
}
