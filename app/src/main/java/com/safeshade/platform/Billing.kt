package com.safeshade.platform

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Play Billing for the Plus/Pro cloud tiers — and the one honest thing about
 * it on this phone: **there is no Play Console listing for either product**,
 * so `purchase()` is expected to come back `Failed("This item is not
 * available", ...)` until the user finishes that Console setup. That failure
 * is not a bug in this file to be "fixed" by softening it into a fake success
 * or a `TODO` placeholder — handoff7 §10 is explicit that no "planned" or
 * "representative" state may ship, and a checkout button that lies about
 * succeeding is worse than one that reports the real, current, boring truth.
 * Every `BillingResponseCode` this library can return is mapped to a plain
 * sentence in [billingReason]; nothing here invents a friendlier code.
 */
class PlayBilling(context: Context) : PurchasesUpdatedListener {

    private var purchaseContinuation: CancellableContinuation<BillingOutcome<Purchase>>? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    /** The two subscription tiers this app sells, with Play's live price and offer token. */
    suspend fun queryPlans(): BillingOutcome<List<PlanOffer>> {
        val connected = ensureConnected()
        if (connected is BillingOutcome.Failed) return connected

        return when (val details = queryProductDetailsList(PRODUCT_IDS)) {
            is BillingOutcome.Failed -> details
            is BillingOutcome.Cancelled -> details
            is BillingOutcome.Ok -> BillingOutcome.Ok(details.value.mapNotNull { it.toPlanOffer() })
        }
    }

    /** Launches Play's purchase flow for [productId] and suspends until it resolves. */
    suspend fun purchase(activity: Activity, productId: String): BillingOutcome<Purchase> {
        val connected = ensureConnected()
        if (connected is BillingOutcome.Failed) return connected

        val productDetails = when (val result = queryProductDetailsList(listOf(productId))) {
            is BillingOutcome.Failed -> return result
            is BillingOutcome.Cancelled -> return result
            is BillingOutcome.Ok -> result.value.firstOrNull()
        } ?: return BillingOutcome.Failed("This item is not available", BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return BillingOutcome.Failed("This item is not available", BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        return suspendCancellableCoroutine { cont ->
            purchaseContinuation = cont
            val launchResult = client.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                purchaseContinuation = null
                cont.resume(BillingOutcome.Failed(billingReason(launchResult.responseCode, launchResult.debugMessage), launchResult.responseCode))
            }
            cont.invokeOnCancellation { purchaseContinuation = null }
        }
    }

    fun close() {
        client.endConnection()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val cont = purchaseContinuation ?: return
        purchaseContinuation = null
        when {
            result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED ->
                cont.resume(BillingOutcome.Cancelled)
            result.responseCode != BillingClient.BillingResponseCode.OK ->
                cont.resume(BillingOutcome.Failed(billingReason(result.responseCode, result.debugMessage), result.responseCode))
            purchases.isNullOrEmpty() ->
                cont.resume(BillingOutcome.Failed("Google Play reported success but returned no purchase", result.responseCode))
            else -> cont.resume(BillingOutcome.Ok(purchases.first()))
        }
    }

    private suspend fun ensureConnected(): BillingOutcome<Unit> {
        if (client.connectionState == BillingClient.ConnectionState.CONNECTED) return BillingOutcome.Ok(Unit)
        return suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(BillingOutcome.Ok(Unit))
                    } else {
                        cont.resume(BillingOutcome.Failed(billingReason(result.responseCode, result.debugMessage), result.responseCode))
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) {
                        cont.resume(
                            BillingOutcome.Failed(
                                "Lost connection to Google Play",
                                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                            )
                        )
                    }
                }
            })
        }
    }

    private suspend fun queryProductDetailsList(productIds: List<String>): BillingOutcome<List<ProductDetails>> {
        val products = productIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(products).build()

        return suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { result, productDetailsList ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.resume(BillingOutcome.Failed(billingReason(result.responseCode, result.debugMessage), result.responseCode))
                } else {
                    cont.resume(BillingOutcome.Ok(productDetailsList))
                }
            }
        }
    }

    private fun ProductDetails.toPlanOffer(): PlanOffer? {
        val offer = subscriptionOfferDetails?.firstOrNull() ?: return null
        val price = offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice ?: return null
        return PlanOffer(productId, title, price, offer.offerToken)
    }

    companion object {
        const val PRODUCT_PLUS_MONTHLY = "safeshade_plus_monthly"
        const val PRODUCT_PRO_MONTHLY = "safeshade_pro_monthly"
        private val PRODUCT_IDS = listOf(PRODUCT_PLUS_MONTHLY, PRODUCT_PRO_MONTHLY)
    }
}

/** One purchasable tier, as Play currently prices and offers it. */
data class PlanOffer(
    val productId: String,
    val title: String,
    val formattedPrice: String,
    val offerToken: String,
)

/** Result of a [PlayBilling] call. Nothing throws to the caller. */
sealed interface BillingOutcome<out T> {
    data class Ok<T>(val value: T) : BillingOutcome<T>
    data class Failed(val reason: String, val code: Int) : BillingOutcome<Nothing>
    data object Cancelled : BillingOutcome<Nothing>
}

/**
 * Every `BillingClient.BillingResponseCode` this library can hand back,
 * translated to a sentence a user can read on a checkout screen. [debug] is
 * Play's own debug message — appended where it adds something a code alone
 * would not, dropped where the code already says everything worth saying.
 */
internal fun billingReason(code: Int, debug: String?): String = when (code) {
    BillingClient.BillingResponseCode.OK -> "Success"
    BillingClient.BillingResponseCode.USER_CANCELED -> "Cancelled"
    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Google Play is unreachable — check your connection"
    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Billing is not available on this device"
    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "This item is not available"
    BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
        "This app is not set up correctly for billing" + (debug?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "")
    BillingClient.BillingResponseCode.ERROR -> debug?.takeIf { it.isNotBlank() } ?: "Google Play billing failed"
    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "You already own this plan"
    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "You do not own this item"
    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "Lost connection to Google Play"
    BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "This feature is not supported on this device"
    BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> "Google Play took too long to respond"
    BillingClient.BillingResponseCode.NETWORK_ERROR -> "A network error occurred while contacting Google Play"
    else -> debug?.takeIf { it.isNotBlank() } ?: "Google Play billing failed (code $code)"
}
