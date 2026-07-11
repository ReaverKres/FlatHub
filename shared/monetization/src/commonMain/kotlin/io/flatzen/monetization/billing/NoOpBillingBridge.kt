package io.flatzen.monetization.billing

/**
 * Stub / no-op billing until Play Console products & billing permission are wired.
 * App stays usable via [MonetizationDefaults.PREMIUM_FALLBACK_ENABLED].
 */
class NoOpBillingBridge : PlatformBillingBridge {
    override fun isConfigured(): Boolean = false
    override suspend fun queryProducts(): List<SubscriptionProduct> = emptyList()
    override suspend fun purchase(productId: String): PurchaseResult = PurchaseResult.NotConfigured
    override suspend fun restore(): SubscriptionStatus =
        SubscriptionStatus(SubscriptionTier.FREE, source = SubscriptionStatus.StatusSource.STORE)

    override suspend fun currentExpiryEpochMs(): Long? = null
}
