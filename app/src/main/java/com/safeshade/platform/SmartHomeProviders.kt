package com.safeshade.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.safeshade.data.SmartHomeHook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * The three ways SafeShade reaches a smart home: opening the app that owns it,
 * checking whether this phone could commission a Matter device, and posting an
 * event to an endpoint.
 *
 * Every function here reports exactly what happened. There is no path that
 * returns success on the strength of having tried - a hook that says
 * "delivered" when nothing was delivered is worse than no hook, because the
 * user stops checking.
 */
object SmartHomeApps {

    /** Google Home. */
    const val GOOGLE_HOME_PACKAGE = "com.google.android.apps.chromecast.app"

    /** Amazon Alexa. */
    const val ALEXA_PACKAGE = "com.amazon.dee.app"

    /**
     * Opens Google Home, or says why it could not be opened.
     *
     * Needs a `<queries><package android:name="…chromecast.app" /></queries>`
     * entry in the manifest. Without one, `getLaunchIntentForPackage` returns
     * null on API 30+ even when the app is installed - package visibility does
     * not distinguish "not there" from "not visible to you", so the app would
     * be reported as missing to somebody who is looking at its icon.
     */
    fun openGoogleHome(context: Context): ConnectOutcome =
        open(context, GOOGLE_HOME_PACKAGE, "Google Home")

    /** Opens the Alexa app, or says why it could not be opened. See [openGoogleHome]. */
    fun openAlexa(context: Context): ConnectOutcome =
        open(context, ALEXA_PACKAGE, "Amazon Alexa")

    private fun open(context: Context, packageName: String, label: String): ConnectOutcome {
        val launch = runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName)
        }.getOrNull() ?: return ConnectOutcome.NotInstalled(playStoreIntent(packageName))

        return try {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ConnectOutcome.Opened(label)
        } catch (e: ActivityNotFoundException) {
            // The package answered the lookup and then would not start. Rare,
            // and it means disabled or mid-update rather than absent.
            ConnectOutcome.Failed("$label is installed but would not open.")
        } catch (e: SecurityException) {
            ConnectOutcome.Failed("Android would not let SafeShade open $label.")
        }
    }

    /** `market://` first; Play resolves it, and any store that handles it will too. */
    private fun playStoreIntent(packageName: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // ============================================
    // Matter
    // ============================================

    /**
     * Whether this phone is in a position to commission a Matter device.
     *
     * **What this reports and what it does not.** Commissioning itself runs
     * through Google Play Services' Home module
     * (`com.google.android.gms:play-services-home`), which this build does not
     * include. So this answers one question only - could this phone do it -
     * and answers it from the two things that decide it: whether Play Services
     * is present and working, and whether the OS is new enough (Matter
     * commissioning needs Android 8.1, API 27; this app's `minSdk` is 26, so
     * the check is real).
     *
     * The UI must say exactly that. "This phone can commission Matter devices"
     * is true and useful; anything implying SafeShade will do the commissioning
     * is not.
     */
    fun matterCommissioningAvailable(context: Context): MatterAvailability {
        val code = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        }.getOrElse { return MatterAvailability.NoGooglePlayServices("Google Play services could not be checked on this phone.") }
        return mapMatterAvailability(code, Build.VERSION.SDK_INT)
    }

    // ============================================
    // Posting
    // ============================================

    /**
     * Sends [event] to [hook]'s endpoint and reports what came back.
     *
     * Ten seconds on connect, read, write and the call as a whole. An alert is
     * worth waiting for, but not worth holding a coroutine on a home router
     * that has gone away, and the user is told what happened either way.
     *
     * The client is derived with [OkHttpClient.newBuilder] rather than
     * configured in place: the shared client belongs to the rest of the app,
     * and a timeout set here would silently become everybody's timeout.
     *
     * Nothing about the secret, the signature or the endpoint reaches a log or
     * a returned reason. See `SmartHomeStore`'s KDoc for why the URL counts.
     */
    suspend fun post(
        hook: SmartHomeHook,
        event: SmartHomeEvent,
        client: OkHttpClient
    ): WebhookResult {
        val body = WebhookSigner.bodyFor(event, hook.provider)
        val timestamp = event.at

        // Built here rather than inside the call, because `url(String)` throws
        // IllegalArgumentException on a malformed address and that is a
        // settings mistake to report, not a network failure.
        val request = try {
            val builder = Request.Builder()
                .url(hook.endpointUrl)
                .post(body.toRequestBody(JSON))
                .header("Content-Type", "application/json")
                .header("X-SafeShade-Timestamp", timestamp.toString())
                .header("X-SafeShade-Event", event.trigger)

            val secret = hook.secret
            if (!secret.isNullOrBlank()) {
                builder.header("X-SafeShade-Signature", WebhookSigner.sign(secret, timestamp, body))
            }
            builder.build()
        } catch (e: IllegalArgumentException) {
            return WebhookResult.Unreachable("That address is not one this phone can send to.")
        }

        val timed = client.newBuilder()
            .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                timed.newCall(request).execute().use { response ->
                    val snippet = runCatching {
                        response.body?.string().orEmpty().take(BODY_SNIPPET_CHARS)
                    }.getOrDefault("")
                    outcomeOf(response.code, snippet)
                }
            } catch (e: IOException) {
                WebhookResult.Unreachable(reasonForNetwork(e))
            }
        }
    }

    /** JSON, as every one of these bodies is. */
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private const val TIMEOUT_SECONDS = 10L
    private const val BODY_SNIPPET_CHARS = 200
}

/**
 * The status code turned into an outcome.
 *
 * Pure, and separate from [SmartHomeApps.post], because this is the decision
 * worth testing and `Response` is not constructible in a JVM unit test without
 * standing up a server.
 *
 * Anything 2xx is delivered. Everything else is a rejection carrying the code
 * and whatever the far end said about it - a receiver's own error message
 * ("unknown webhook id") is the single most useful thing a user debugging a
 * hook can be shown.
 */
internal fun outcomeOf(code: Int, bodySnippet: String): WebhookResult =
    if (code in 200..299) {
        WebhookResult.Delivered(code)
    } else {
        WebhookResult.Rejected(code, bodySnippet.trim().take(200))
    }

/**
 * The Matter decision, with the Android calls lifted out.
 *
 * Pure and therefore testable, exactly like `mapAvailability` in
 * `HealthConnectVitals`: `GoogleApiAvailability` and `Build.VERSION` are both
 * Android, and this module's unit tests run on the JVM where those throw
 * "Stub!".
 *
 * @param gmsCode a `ConnectionResult` code from
 *   `GoogleApiAvailability.isGooglePlayServicesAvailable`.
 */
internal fun mapMatterAvailability(gmsCode: Int, sdkInt: Int): MatterAvailability = when {
    // Checked before Play Services, because on a phone too old for
    // commissioning the state of Play Services does not change the answer.
    sdkInt < Build.VERSION_CODES.O_MR1 ->
        MatterAvailability.Unsupported("Matter setup needs Android 8.1 or newer.")

    gmsCode == ConnectionResult.SUCCESS -> MatterAvailability.Available

    gmsCode == ConnectionResult.SERVICE_MISSING ->
        MatterAvailability.NoGooglePlayServices("This phone does not have Google Play services.")

    gmsCode == ConnectionResult.SERVICE_DISABLED ->
        MatterAvailability.NoGooglePlayServices("Google Play services is turned off on this phone.")

    gmsCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ->
        MatterAvailability.NoGooglePlayServices("Google Play services needs updating on this phone.")

    gmsCode == ConnectionResult.SERVICE_INVALID ->
        MatterAvailability.NoGooglePlayServices("Google Play services is installed but not working on this phone.")

    gmsCode == ConnectionResult.SERVICE_UPDATING ->
        MatterAvailability.NoGooglePlayServices("Google Play services is updating. Try again in a moment.")

    else ->
        MatterAvailability.NoGooglePlayServices("Google Play services is not available on this phone.")
}

/**
 * Plain English for a network failure, by what actually went wrong.
 *
 * Never the exception's own words: `java.net.UnknownHostException:
 * homeassistant.local` in a settings screen tells the user nothing and leaks
 * the endpoint into the UI.
 */
internal fun reasonForNetwork(e: IOException): String = when (e) {
    is UnknownHostException -> "That address could not be found. Check the name or IP."
    is SocketTimeoutException -> "The address did not answer within 10 seconds."
    is ConnectException -> "Nothing answered at that address. Check it is switched on and reachable."
    is SSLException -> "The secure connection was refused. Check the certificate at that address."
    else -> "The phone could not reach that address just now."
}

/**
 * What opening a smart-home app did.
 *
 * Three outcomes rather than a boolean because the user's next step differs in
 * each: nothing, install it, or fix something.
 */
sealed interface ConnectOutcome {

    /** The app was launched. [appLabel] is what to name in the confirmation. */
    data class Opened(val appLabel: String) : ConnectOutcome

    /**
     * The app is not on this phone, or is not visible to SafeShade.
     *
     * [playIntent] opens its store page. It is an [Intent] rather than a
     * boolean so the screen can offer the install without knowing package
     * names.
     */
    data class NotInstalled(val playIntent: Intent) : ConnectOutcome

    /** It is there and would not start. [reason] is plain English. */
    data class Failed(val reason: String) : ConnectOutcome
}

/** Whether this phone could commission a Matter device. See [SmartHomeApps.matterCommissioningAvailable]. */
sealed interface MatterAvailability {

    /** Play Services is working and the OS is new enough. */
    data object Available : MatterAvailability

    /** Play Services is missing, off, or out of date. [reason] says which. */
    data class NoGooglePlayServices(val reason: String) : MatterAvailability

    /** This phone cannot, whatever Play Services is doing. */
    data class Unsupported(val reason: String) : MatterAvailability
}

/** What one POST to a hook's endpoint did. */
sealed interface WebhookResult {

    /** The endpoint accepted it. [statusCode] is the 2xx it answered with. */
    data class Delivered(val statusCode: Int) : WebhookResult

    /**
     * The endpoint answered and refused.
     *
     * [bodySnippet] is the first 200 characters of whatever it said, kept
     * because a receiver's own words are the most useful thing a user
     * debugging a hook can be shown.
     */
    data class Rejected(val statusCode: Int, val bodySnippet: String) : WebhookResult

    /** Nothing answered. [reason] is plain English about why. */
    data class Unreachable(val reason: String) : WebhookResult
}
