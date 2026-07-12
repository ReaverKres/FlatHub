package io.flatzen.monetization.billing

import io.flatzen.monetization.MonetizationDefaults
import io.flatzen.monetization.datastore.EncryptedSecureStore
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
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Common orchestration around platform billing: trial, rewarded, fallback Premium, cache.
 * Platform [PlatformBillingBridge] talks to Play Billing / StoreKit.
 */
class SubscriptionServiceImpl(
    private val secureStore: EncryptedSecureStore,
    private val bridge: PlatformBillingBridge,
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
            ) { cache, trialStarted, rewardedUntil ->
                resolveStatus(
                    cache.tier,
                    cache.expiresAtEpochMs,
                    cache.productId,
                    trialStarted,
                    rewardedUntil
                )
            }.collect { _status.value = it }
        }
        scope.launch {
            if (!premiumFallbackEnabled() && bridge.isConfigured()) {
                runCatching { restore() }
            }
            if (!premiumFallbackEnabled()) {
                startTrialIfNeeded()
            }
        }
        scope.launch {
            if (premiumFallbackEnabled()) return@launch
            bridge.observeEntitlementChanges().collect { storeStatus ->
                secureStore.saveSubscriptionCache(
                    tier = storeStatus.tier.name,
                    expiresAtEpochMs = storeStatus.expiresAtEpochMs,
                    productId = storeStatus.productId,
                )
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
            val expires = bridge.currentExpiryEpochMs()
            secureStore.saveSubscriptionCache(
                tier = SubscriptionTier.PREMIUM.name,
                expiresAtEpochMs = expires,
                productId = productId,
            )
        }
        return result
    }

    override suspend fun restore(): SubscriptionStatus {
        if (!bridge.isConfigured()) {
            return status.value
        }
        val restored = bridge.restore()
        if (restored.tier == SubscriptionTier.PREMIUM) {
            secureStore.saveSubscriptionCache(
                tier = SubscriptionTier.PREMIUM.name,
                expiresAtEpochMs = restored.expiresAtEpochMs,
                productId = restored.productId,
            )
        }
        return restored
    }

    override suspend fun grantRewardedPremium(hours: Long) {
        val until = Clock.System.now().toEpochMilliseconds() + hours.hours.inWholeMilliseconds
        secureStore.setRewardedUntil(until)
    }

    override fun isBillingConfigured(): Boolean = bridge.isConfigured()

    override suspend fun startTrialIfNeeded() {
        secureStore.setTrialStartedAtIfAbsent(Clock.System.now().toEpochMilliseconds())
    }

    private fun resolveStatus(
        cachedTier: String?,
        expiresAt: Long?,
        productId: String?,
        trialStarted: Long?,
        rewardedUntil: Long?,
    ): SubscriptionStatus {
        if (premiumFallbackEnabled()) {
            return SubscriptionStatus(
                SubscriptionTier.PREMIUM,
                source = SubscriptionStatus.StatusSource.FALLBACK
            )
        }
        val now = Clock.System.now().toEpochMilliseconds()
        if (rewardedUntil != null && rewardedUntil > now) {
            return SubscriptionStatus(
                tier = SubscriptionTier.PREMIUM,
                expiresAtEpochMs = rewardedUntil,
                source = SubscriptionStatus.StatusSource.REWARDED,
            )
        }
        if (cachedTier == SubscriptionTier.PREMIUM.name && (expiresAt == null || expiresAt > now)) {
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
