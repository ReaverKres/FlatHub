package io.flatzen.monetization.billing

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import io.flatzen.monetization.MonetizationDefaults
import io.flatzen.monetization.util.roundToScale
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Google Play Billing. Requires Activity for [purchase].
 * All BillingClient calls are marshalled to the main thread as required by Play Billing.
 */
class PlayBillingBridge(
    context: Context,
) : PlatformBillingBridge, PurchasesUpdatedListener {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var purchaseContinuation: ((PurchaseResult) -> Unit)? = null
    private var cachedDetails: List<ProductDetails> = emptyList()
    private var connected = false
    private var connectCont: CancellableContinuation<Boolean>? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val connectionListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) {
            connected = result.responseCode == BillingClient.BillingResponseCode.OK
            connectCont?.takeIf { it.isActive }?.resume(connected)
            connectCont = null
        }

        override fun onBillingServiceDisconnected() {
            connected = false
            runOnMain { client.startConnection(this) }
        }
    }

    init {
        runOnMain { client.startConnection(connectionListener) }
    }

    override fun isConfigured(): Boolean = connected || client.isReady

    override suspend fun queryProducts(): List<SubscriptionProduct> = onMain {
        if (!awaitConnection()) return@onMain emptyList()

        val productList = listOf(
            MonetizationDefaults.PRODUCT_WEEK,
            MonetizationDefaults.PRODUCT_MONTH,
            MonetizationDefaults.PRODUCT_QUARTER,
        ).map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        val result = withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            client.queryProductDetails(params)
        }
        if (result == null) {
            return@onMain emptyList()
        }
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return@onMain emptyList()
        }

        val list = result.productDetailsList.orEmpty()
        cachedDetails = list
        list.mapNotNull { it.toSubscriptionProduct() }
    }

    override suspend fun purchase(productId: String): PurchaseResult = onMain {
        if (!awaitConnection()) {
            return@onMain PurchaseResult.Error(message = "Billing unavailable")
        }
        val activity =
            CurrentActivityHolder.activity
                ?: return@onMain PurchaseResult.Error(message = "Activity unavailable")
        val details = cachedDetails.find { it.productId == productId }
            ?: queryProducts().let { cachedDetails.find { d -> d.productId == productId } }
            ?: return@onMain PurchaseResult.Error(message = "Product not found")
        val offer = details.subscriptionOfferDetails?.firstOrNull()
            ?: return@onMain PurchaseResult.Error(message = "No offer")
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        suspendCancellableCoroutine<PurchaseResult> { cont ->
            purchaseContinuation = { result -> cont.resume(result) }
            val launch = client.launchBillingFlow(activity, flowParams)
            if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
                purchaseContinuation = null
                cont.resume(
                    PurchaseResult.Error(
                        message = launch.debugMessage,
                        billingResponseCode = launch.responseCode,
                    )
                )
            }
        }
    }

    override suspend fun restore(): SubscriptionStatus = onMain {
        if (!awaitConnection()) return@onMain SubscriptionStatus(SubscriptionTier.FREE)

        val purchasesResult = withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        }
        if (purchasesResult == null) {
            return@onMain SubscriptionStatus(SubscriptionTier.FREE)
        }
        if (purchasesResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return@onMain SubscriptionStatus(SubscriptionTier.FREE)
        }
        val active = purchasesResult.purchasesList.firstOrNull {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (active != null) {
            acknowledgeIfNeeded(active)
            SubscriptionStatus(
                tier = SubscriptionTier.PREMIUM,
                productId = active.products.firstOrNull(),
                source = SubscriptionStatus.StatusSource.STORE,
            )
        } else {
            SubscriptionStatus(SubscriptionTier.FREE)
        }
    }

    override suspend fun currentExpiryEpochMs(): Long? = null

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val cont = purchaseContinuation ?: return
        purchaseContinuation = null
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull()
                if (purchase != null) {
                    acknowledgeIfNeeded(purchase)
                    cont(PurchaseResult.Success)
                } else {
                    cont(
                        PurchaseResult.Error(
                            message = "Empty purchases",
                            billingResponseCode = result.responseCode,
                        )
                    )
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> cont(PurchaseResult.Cancelled)
            else -> cont(
                PurchaseResult.Error(
                    message = result.debugMessage,
                    billingResponseCode = result.responseCode,
                )
            )
        }
    }

    private suspend fun awaitConnection(): Boolean {
        if (connected || client.isReady) {
            connected = true
            return true
        }
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                runOnMain {
                    connectCont = cont
                    cont.invokeOnCancellation {
                        if (connectCont === cont) connectCont = null
                    }
                    if (connected || client.isReady) {
                        connected = true
                        connectCont = null
                        cont.resume(true)
                        return@runOnMain
                    }
                    client.startConnection(connectionListener)
                }
            }
        } ?: run {
            false
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        runOnMain { client.acknowledgePurchase(params) { } }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private suspend fun <T> onMain(block: suspend () -> T): T =
        withContext(Dispatchers.Main) { block() }

    private fun ProductDetails.toSubscriptionProduct(): SubscriptionProduct? {
        val offer = subscriptionOfferDetails?.firstOrNull() ?: return null
        val phase = offer.pricingPhases.pricingPhaseList.firstOrNull() ?: return null
        val micros = phase.priceAmountMicros
        val amount = (micros / 1_000_000.0).roundToScale(2)
        val currency = phase.priceCurrencyCode
        val formatted = if (currency.equals("BYN", ignoreCase = true)) {
            "${amount.roundToScale(2)} BYN"
        } else {
            phase.formattedPrice
        }
        val weeks = when (productId) {
            MonetizationDefaults.PRODUCT_WEEK -> 1
            MonetizationDefaults.PRODUCT_MONTH -> 4
            MonetizationDefaults.PRODUCT_QUARTER -> 12
            else -> 1
        }
        val title = name
        return SubscriptionProduct(
            id = productId,
            title = title,
            priceFormatted = formatted,
            priceAmount = amount,
            currencyCode = currency,
            periodWeeks = weeks,
            savingsPercent = null,
        )
    }

    private companion object {
        const val TAG = "PlayBillingBridge"
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val QUERY_TIMEOUT_MS = 20_000L
    }
}
