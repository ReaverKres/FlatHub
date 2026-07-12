package io.flatzen.monetization.tier

import io.flatzen.monetization.billing.SubscriptionService
import io.flatzen.monetization.billing.SubscriptionStatus
import io.flatzen.monetization.billing.SubscriptionTier
import io.flatzen.monetization.config.MonetizationRemoteConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes

enum class UserTier {
    FREE,
    PREMIUM,
}

interface UserTierProvider {
    fun observeTier(): Flow<UserTier>
    fun currentTier(): UserTier
    fun feedDelayMinutes(): Long
    fun shouldShowAds(): Boolean
    fun shouldShowUpsellBanner(bannerDismissed: Boolean): Boolean
}

class UserTierProviderImpl(
    private val subscriptionService: SubscriptionService,
    private val configProvider: () -> MonetizationRemoteConfig,
) : UserTierProvider {

    override fun observeTier(): Flow<UserTier> =
        subscriptionService.observeStatus().map { it.toUserTier() }

    override fun currentTier(): UserTier = subscriptionService.status.value.toUserTier()

    override fun feedDelayMinutes(): Long = configProvider().feedDelayMinutes

    override fun shouldShowAds(): Boolean {
        val config = configProvider()
        if (!config.adsEnabled) return false
        if (config.premiumFallbackEnabled) return false
        return currentTier() == UserTier.FREE
    }

    override fun shouldShowUpsellBanner(bannerDismissed: Boolean): Boolean {
        if (bannerDismissed) return false
        if (configProvider().premiumFallbackEnabled) return false
        return currentTier() == UserTier.FREE
    }

    private fun SubscriptionStatus.toUserTier(): UserTier {
        if (configProvider().premiumFallbackEnabled) return UserTier.PREMIUM
        return when (tier) {
            SubscriptionTier.PREMIUM -> UserTier.PREMIUM
            SubscriptionTier.FREE -> UserTier.FREE
        }
    }
}

fun <T> List<T>.applyFeedDelayFilter(
    tier: UserTier,
    delayMinutes: Long,
    publishedAt: (T) -> Instant?,
    now: Instant = Clock.System.now(),
): List<T> {
    if (tier == UserTier.PREMIUM || delayMinutes <= 0) return this
    val cutoff = now - delayMinutes.minutes
    return filter { item ->
        publishedAt(item)?.let { it <= cutoff } ?: true
    }
}
