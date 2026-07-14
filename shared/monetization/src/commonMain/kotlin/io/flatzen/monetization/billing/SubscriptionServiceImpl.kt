package io.flatzen.monetization.billing

import io.flatzen.monetization.MonetizationDefaults
import io.flatzen.monetization.datastore.EncryptedSecureStore
import io.flatzen.monetization.time.TrustedTimeRepository
import io.flatzen.monetization.util.roundToScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Common orchestration around platform billing: trial, rewarded, fallback Premium, cache.
 * Platform [PlatformBillingBridge] talks to Play Billing / StoreKit.
 * Expiry comparisons use [TrustedTimeRepository] instead of raw device clock.
 */
class SubscriptionServiceImpl(
    private val secureStore: EncryptedSecureStore,
    private val bridge: PlatformBillingBridge,
    private val trustedTime: TrustedTimeRepository,
    private val premiumFallbackEnabled: () -> Boolean = { MonetizationDefaults.PREMIUM_FALLBACK_ENABLED },
    private val trialDays: () -> Long = { MonetizationDefaults.TRIAL_DAYS },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : SubscriptionService {

    private val _status = MutableStateFlow(
        if (premiumFallbackEnabled()) {
            SubscriptionStatus(
                SubscriptionTier.PREMIUM,
                source = SubscriptionStatus.StatusSource.FALLBACK
            )
        } else {
            SubscriptionStatus(SubscriptionTier.FREE)
        }
    )
    override val status: StateFlow<SubscriptionStatus> = _status.asStateFlow()

    init {
        scope.launch {
            combine(
                secureStore.observeSubscriptionCache(),
                secureStore.observeTrialStartedAt(),
                secureStore.observeRewardedUntil(),
                trustedTime.state.map { it.nowEpochMs },
            ) { cache, trialStarted, rewardedUntil, nowEpochMs ->
                resolveStatus(
                    cache.tier,
                    cache.expiresAtEpochMs,
                    cache.productId,
                    trialStarted,
                    rewardedUntil,
                    nowEpochMs,
                )
            }.collect { _status.value = it }
        }
        scope.launch {
            if (!premiumFallbackEnabled()) {
                startTrialIfNeeded()
            }
        }
        scope.launch {
            if (premiumFallbackEnabled()) return@launch
            bridge.observeEntitlementChanges().collect { storeStatus ->
                persistStoreStatus(storeStatus)
            }
        }
        // When a cached Premium expiry elapses, re-query the store once so renewals
        // extend access and true expirations clear the cache (esp. Play test SKUs).
        scope.launch {
            if (premiumFallbackEnabled()) return@launch
            var lastRefreshForExpiryMs: Long? = null
            combine(
                secureStore.observeSubscriptionCache(),
                trustedTime.state.map { it.nowEpochMs },
            ) { cache, nowMs -> cache to nowMs }
                .collect { (cache, nowMs) ->
                    val expires = cache.expiresAtEpochMs
                    if (cache.tier == SubscriptionTier.PREMIUM.name &&
                        expires != null &&
                        expires <= nowMs &&
                        lastRefreshForExpiryMs != expires
                    ) {
                        lastRefreshForExpiryMs = expires
                        if (bridge.isConfigured()) {
                            runCatching { persistStoreStatus(bridge.restore()) }
                        }
                    }
                }
        }
    }

    override fun observeStatus(): Flow<SubscriptionStatus> = status

    override suspend fun getProducts(): List<SubscriptionProduct> {
        val fromStore = bridge.queryProducts()
        if (fromStore.isNotEmpty()) return fromStore.map { it.withSavings() }
        return fallbackProducts()
    }

    override suspend fun purchase(productId: String): PurchaseResult {
        if (!bridge.isConfigured()) return PurchaseResult.NotConfigured
        val result = bridge.purchase(productId)
        if (result is PurchaseResult.Success) {
            // Prefer full store snapshot (tier + expiry); fall back to queried expiry.
            val restored = bridge.restore()
            if (restored.tier == SubscriptionTier.PREMIUM) {
                persistStoreStatus(restored.copy(productId = restored.productId ?: productId))
            } else {
                secureStore.saveSubscriptionCache(
                    tier = SubscriptionTier.PREMIUM.name,
                    expiresAtEpochMs = bridge.currentExpiryEpochMs(),
                    productId = productId,
                )
            }
        }
        return result
    }

    override suspend fun restore(): SubscriptionStatus {
        if (!bridge.isConfigured()) {
            return status.value
        }
        val restored = bridge.restore()
        persistStoreStatus(restored)
        return restored
    }

    private suspend fun persistStoreStatus(storeStatus: SubscriptionStatus) {
        when (storeStatus.tier) {
            SubscriptionTier.PREMIUM -> secureStore.saveSubscriptionCache(
                tier = SubscriptionTier.PREMIUM.name,
                expiresAtEpochMs = storeStatus.expiresAtEpochMs,
                productId = storeStatus.productId,
            )

            SubscriptionTier.FREE -> secureStore.saveSubscriptionCache(
                tier = SubscriptionTier.FREE.name,
                expiresAtEpochMs = null,
                productId = null,
            )
        }
    }

    override suspend fun grantRewardedPremium(hours: Long) {
        val now = trustedTime.nowEpochMs()
        val currentUntil = secureStore.loadRewardedUntil()
        val base = maxOf(now, currentUntil ?: 0L)
        secureStore.setRewardedUntil(base + hours.hours.inWholeMilliseconds)
    }

    override fun isBillingConfigured(): Boolean = bridge.isConfigured()

    override suspend fun startTrialIfNeeded() {
        secureStore.setTrialStartedAtIfAbsent(trustedTime.nowEpochMs())
    }

    private fun resolveStatus(
        cachedTier: String?,
        expiresAt: Long?,
        productId: String?,
        trialStarted: Long?,
        rewardedUntil: Long?,
        now: Long,
    ): SubscriptionStatus {
        if (premiumFallbackEnabled()) {
            return SubscriptionStatus(
                SubscriptionTier.PREMIUM,
                source = SubscriptionStatus.StatusSource.FALLBACK
            )
        }
        if (rewardedUntil != null && rewardedUntil > now) {
            return SubscriptionStatus(
                tier = SubscriptionTier.PREMIUM,
                expiresAtEpochMs = rewardedUntil,
                source = SubscriptionStatus.StatusSource.REWARDED,
            )
        }
        // Store/cache Premium must have a finite expiry. Null expiry used to mean
        // "forever" and kept Premium after Play subscriptions ended.
        if (cachedTier == SubscriptionTier.PREMIUM.name &&
            expiresAt != null &&
            expiresAt > now
        ) {
            return SubscriptionStatus(
                tier = SubscriptionTier.PREMIUM,
                expiresAtEpochMs = expiresAt,
                productId = productId,
                source = SubscriptionStatus.StatusSource.CACHE,
            )
        }
        if (trialStarted != null) {
            val trialEnd = trialStarted + trialDays().days.inWholeMilliseconds
            if (now < trialEnd) {
                return SubscriptionStatus(
                    tier = SubscriptionTier.PREMIUM,
                    expiresAtEpochMs = trialEnd,
                    source = SubscriptionStatus.StatusSource.TRIAL,
                )
            }
        }
        return SubscriptionStatus(SubscriptionTier.FREE)
    }

    private fun fallbackProducts(): List<SubscriptionProduct> = listOf(
        SubscriptionProduct(
            id = MonetizationDefaults.PRODUCT_WEEK,
            title = MonetizationDefaults.PRODUCT_WEEK,
            priceFormatted = "$${MonetizationDefaults.PRICE_WEEK_USD}",
            priceAmount = MonetizationDefaults.PRICE_WEEK_USD.toDouble(),
            currencyCode = "USD",
            periodWeeks = 1,
            savingsPercent = null,
        ),
        SubscriptionProduct(
            id = MonetizationDefaults.PRODUCT_MONTH,
            title = MonetizationDefaults.PRODUCT_MONTH,
            priceFormatted = "$${MonetizationDefaults.PRICE_MONTH_USD}",
            priceAmount = MonetizationDefaults.PRICE_MONTH_USD.toDouble(),
            currencyCode = "USD",
            periodWeeks = 4,
            savingsPercent = 38,
        ),
        SubscriptionProduct(
            id = MonetizationDefaults.PRODUCT_QUARTER,
            title = MonetizationDefaults.PRODUCT_QUARTER,
            priceFormatted = "$${MonetizationDefaults.PRICE_QUARTER_USD}",
            priceAmount = MonetizationDefaults.PRICE_QUARTER_USD.toDouble(),
            currencyCode = "USD",
            periodWeeks = 12,
            savingsPercent = 50,
        ),
    )

    private fun SubscriptionProduct.withSavings(): SubscriptionProduct {
        if (periodWeeks <= 1) return copy(savingsPercent = null)
        val weekPrice = MonetizationDefaults.PRICE_WEEK_USD.toDouble()
        val weeklyEquivalent = priceAmount / periodWeeks
        val savings = ((1.0 - weeklyEquivalent / weekPrice) * 100.0).roundToScale(0).toInt()
        return copy(savingsPercent = savings.coerceAtLeast(0))
    }
}

interface PlatformBillingBridge {
    fun isConfigured(): Boolean
    suspend fun queryProducts(): List<SubscriptionProduct>
    suspend fun purchase(productId: String): PurchaseResult
    suspend fun restore(): SubscriptionStatus
    suspend fun currentExpiryEpochMs(): Long?

    /** StoreKit 2 / Play entitlement push updates. Default: none. */
    fun observeEntitlementChanges(): Flow<SubscriptionStatus> = emptyFlow()
}
