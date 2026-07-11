package io.flatzen.monetization.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class SubscriptionTier {
    FREE,
    PREMIUM,
}

data class SubscriptionProduct(
    val id: String,
    val title: String,
    val priceFormatted: String,
    val priceAmount: Double,
    val currencyCode: String,
    val periodWeeks: Int,
    val savingsPercent: Int?,
)

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Error(val message: String) : PurchaseResult
    data object NotConfigured : PurchaseResult
}

data class SubscriptionStatus(
    val tier: SubscriptionTier,
    val expiresAtEpochMs: Long? = null,
    val productId: String? = null,
    val source: StatusSource = StatusSource.FALLBACK,
) {
    enum class StatusSource {
        FALLBACK,
        STORE,
        TRIAL,
        REWARDED,
        CACHE,
    }
}

interface SubscriptionService {
    val status: StateFlow<SubscriptionStatus>
    fun observeStatus(): Flow<SubscriptionStatus>
    suspend fun getProducts(): List<SubscriptionProduct>
    suspend fun purchase(productId: String): PurchaseResult
    suspend fun restore(): SubscriptionStatus
    suspend fun grantRewardedPremium(hours: Long)
    suspend fun startTrialIfNeeded()
    fun isBillingConfigured(): Boolean
}