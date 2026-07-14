package io.flatzen.monetization.billing

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.days

/**
 * Google Play Billing. Requires Activity for [purchase].
 * All BillingClient calls are marshalled to the main thread as required by Play Billing.
 *
 * Play [Purchase] has no expiry field — period end is estimated as
 * `purchaseTime + billingPeriod` from [ProductDetails] (or product-id fallback).
 * When Play no longer returns PURCHASED, [restore] reports FREE.
 */
class PlayBillingBridge(
    context: Context,
) : PlatformBillingBridge, PurchasesUpdatedListener {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var purchaseContinuation: ((PurchaseResult) -> Unit)? = null
    private var cachedDetails: List<ProductDetails> = emptyList()
    private var connected = false
    private var connectCont: CancellableContinuation<Boolean>? = null
    private var lastActivePurchase: Purchase? = null

    private val entitlementPulse = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val connectionListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(result: BillingResult) {
            connected = result.responseCode == BillingClient.BillingResponseCode.OK
            if (!connected) {
                Log.w(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
            } else {
                entitlementPulse.tryEmit(Unit)
            }
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
            Log.w(TAG, "queryProductDetails timed out")
            return@onMain emptyList()
        }
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(
                TAG,
                "queryProductDetails failed: ${result.billingResult.responseCode} ${result.billingResult.debugMessage}",
            )
            return@onMain emptyList()
        }

        val list = result.productDetailsList.orEmpty()
        if (list.isEmpty()) {
            Log.w(TAG, "queryProductDetails returned no products")
        }
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
        if (!awaitConnection()) {
            return@onMain SubscriptionStatus(
                SubscriptionTier.FREE,
                source = SubscriptionStatus.StatusSource.STORE,
            )
        }
        queryActiveStatus()
    }

    override suspend fun currentExpiryEpochMs(): Long? = onMain {
        if (!awaitConnection()) return@onMain null
        val purchase = lastActivePurchase ?: queryActivePurchase()
        purchase?.let { expiryEpochMsFor(it) }
    }

    override fun observeEntitlementChanges(): Flow<SubscriptionStatus> = callbackFlow {
        val collectJob = launch {
            entitlementPulse
                .onStart { emit(Unit) }
                .collect {
                    if (awaitConnection()) {
                        trySend(queryActiveStatus())
                    }
                }
        }
        awaitClose { collectJob.cancel() }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val cont = purchaseContinuation
        purchaseContinuation = null
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val purchase = purchases?.firstOrNull {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                } ?: purchases?.firstOrNull()
                if (purchase != null && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    lastActivePurchase = purchase
                    acknowledgeIfNeeded(purchase)
                    cont?.invoke(PurchaseResult.Success)
                } else if (cont != null) {
                    cont(
                        PurchaseResult.Error(
                            message = "Empty purchases",
                            billingResponseCode = result.responseCode,
                        )
                    )
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> cont?.invoke(PurchaseResult.Cancelled)
            else -> cont?.invoke(
                PurchaseResult.Error(
                    message = result.debugMessage,
                    billingResponseCode = result.responseCode,
                )
            )
        }
        entitlementPulse.tryEmit(Unit)
    }

    private suspend fun queryActiveStatus(): SubscriptionStatus {
        val purchase = queryActivePurchase()
        return if (purchase != null) {
            acknowledgeIfNeeded(purchase)
            lastActivePurchase = purchase
            SubscriptionStatus(
                tier = SubscriptionTier.PREMIUM,
                expiresAtEpochMs = expiryEpochMsFor(purchase),
                productId = purchase.products.firstOrNull(),
                source = SubscriptionStatus.StatusSource.STORE,
            )
        } else {
            lastActivePurchase = null
            SubscriptionStatus(
                SubscriptionTier.FREE,
                source = SubscriptionStatus.StatusSource.STORE,
            )
        }
    }

    private suspend fun queryActivePurchase(): Purchase? {
        val purchasesResult = withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        }
        if (purchasesResult == null) {
            Log.w(TAG, "queryPurchasesAsync timed out")
            return null
        }
        if (purchasesResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            return null
        }
        return purchasesResult.purchasesList.firstOrNull {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
    }

    private fun expiryEpochMsFor(purchase: Purchase): Long {
        val productId = purchase.products.firstOrNull()
        val periodMs = billingPeriodMsFor(productId)
        return purchase.purchaseTime + periodMs
    }

    private fun billingPeriodMsFor(productId: String?): Long {
        val details = productId?.let { id -> cachedDetails.find { it.productId == id } }
        val iso = details
            ?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.lastOrNull()
            ?.billingPeriod
        parseIso8601PeriodMs(iso)?.let { return it }
        return fallbackPeriodMs(productId)
    }

    private fun fallbackPeriodMs(productId: String?): Long = when (productId) {
        MonetizationDefaults.PRODUCT_WEEK -> 7.days.inWholeMilliseconds
        MonetizationDefaults.PRODUCT_MONTH -> 30.days.inWholeMilliseconds
        MonetizationDefaults.PRODUCT_QUARTER -> 90.days.inWholeMilliseconds
        else -> 7.days.inWholeMilliseconds
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        runOnMain { client.acknowledgePurchase(params) { } }
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
            Log.w(TAG, "Billing connection timed out")
            false
        }
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
        return SubscriptionProduct(
            id = productId,
            title = name,
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

        /** Parses Play Billing ISO-8601 periods like P1W, P1M, P3M, P1Y, P7D. */
        fun parseIso8601PeriodMs(period: String?): Long? {
            if (period.isNullOrBlank()) return null
            val match = Regex("""^P(?:(\d+)Y)?(?:(\d+)M)?(?:(\d+)W)?(?:(\d+)D)?$""")
                .matchEntire(period.trim()) ?: return null
            val years = match.groupValues[1].toLongOrNull() ?: 0L
            val months = match.groupValues[2].toLongOrNull() ?: 0L
            val weeks = match.groupValues[3].toLongOrNull() ?: 0L
            val days = match.groupValues[4].toLongOrNull() ?: 0L
            if (years == 0L && months == 0L && weeks == 0L && days == 0L) return null
            return years * 365.days.inWholeMilliseconds +
                    months * 30.days.inWholeMilliseconds +
                    weeks * 7.days.inWholeMilliseconds +
                    days * 1.days.inWholeMilliseconds
        }
    }
}
