package io.flatzen.monetization.billing

import android.content.Context
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
import io.flatzen.monetization.MonetizationDefaults
import io.flatzen.monetization.util.roundToScale
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Play Billing. Requires Activity for [purchase].
 * Until products exist in Play Console, [isConfigured] can still be true if BillingClient connects —
 * purchase will fail gracefully. Prefer fallback Premium until keys/products ready.
 */
class PlayBillingBridge(
    context: Context,
) : PlatformBillingBridge, PurchasesUpdatedListener {

    private var purchaseContinuation: ((PurchaseResult) -> Unit)? = null
    private var cachedDetails: List<ProductDetails> = emptyList()
    private var connected = false

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connected = result.responseCode == BillingClient.BillingResponseCode.OK
            }

            override fun onBillingServiceDisconnected() {
                connected = false
            }
        })
    }

    override fun isConfigured(): Boolean = connected

    override suspend fun queryProducts(): List<SubscriptionProduct> {
        if (!connected) return emptyList()
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
        return suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { result, productDetailsList ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.resume(emptyList())
                    return@queryProductDetailsAsync
                }
                val list = productDetailsList.orEmpty()
                cachedDetails = list
                cont.resume(list.mapNotNull { it.toSubscriptionProduct() })
            }
        }
    }

    override suspend fun purchase(productId: String): PurchaseResult {
        val activity =
            CurrentActivityHolder.activity ?: return PurchaseResult.Error("Activity unavailable")
        val details = cachedDetails.find { it.productId == productId }
            ?: queryProducts().let { cachedDetails.find { d -> d.productId == productId } }
            ?: return PurchaseResult.Error("Product not found")
        val offer = details.subscriptionOfferDetails?.firstOrNull()
            ?: return PurchaseResult.Error("No offer")
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        return suspendCancellableCoroutine { cont ->
            purchaseContinuation = { cont.resume(it) }
            val launch = client.launchBillingFlow(activity, flowParams)
            if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
                purchaseContinuation = null
                cont.resume(PurchaseResult.Error(launch.debugMessage))
            }
        }
    }

    override suspend fun restore(): SubscriptionStatus {
        if (!connected) return SubscriptionStatus(SubscriptionTier.FREE)
        return suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { result, purchases ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.resume(SubscriptionStatus(SubscriptionTier.FREE))
                    return@queryPurchasesAsync
                }
                val active = purchases.firstOrNull {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (active != null) {
                    acknowledgeIfNeeded(active)
                    cont.resume(
                        SubscriptionStatus(
                            tier = SubscriptionTier.PREMIUM,
                            productId = active.products.firstOrNull(),
                            source = SubscriptionStatus.StatusSource.STORE,
                        )
                    )
                } else {
                    cont.resume(SubscriptionStatus(SubscriptionTier.FREE))
                }
            }
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
                    cont(PurchaseResult.Error("Empty purchases"))
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> cont(PurchaseResult.Cancelled)
            else -> cont(PurchaseResult.Error(result.debugMessage))
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { }
    }

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
        val title = when (productId) {
            MonetizationDefaults.PRODUCT_WEEK -> "1 неделя"
            MonetizationDefaults.PRODUCT_MONTH -> "1 месяц"
            MonetizationDefaults.PRODUCT_QUARTER -> "3 месяца"
            else -> name
        }
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
}
