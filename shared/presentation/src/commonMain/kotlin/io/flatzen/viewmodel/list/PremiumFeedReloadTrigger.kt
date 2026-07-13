package io.flatzen.viewmodel.list

import io.flatzen.monetization.tier.UserTier
import io.flatzen.monetization.tier.UserTierProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Reloads the home feed when the user gains Premium after being on the free tier
 * (purchase, restore, rewarded ad, trial, etc.).
 */
class PremiumFeedReloadTrigger(
    userTierProvider: UserTierProvider,
    flatSearchContainer: FlatSearchContainer,
    scope: CoroutineScope,
) {
    init {
        scope.launch {
            var previousTier = userTierProvider.currentTier()
            userTierProvider.observeTier().collect { tier ->
                if (previousTier == UserTier.FREE && tier == UserTier.PREMIUM) {
                    flatSearchContainer.store.intent(
                        FlatListIntent.SearchFlats(isLoadMore = false, isRefreshing = false),
                    )
                }
                previousTier = tier
            }
        }
    }
}
