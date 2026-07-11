package io.flatzen.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import io.flatzen.monetization.datastore.EncryptedSecureStore
import io.flatzen.monetization.tier.UserTierProvider
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun RememberPremiumUpsellBanner(
    navigateToPremium: () -> Unit,
): (@Composable () -> Unit)? {
    val userTierProvider: UserTierProvider = koinInject()
    val secureStore: EncryptedSecureStore = koinInject()
    val bannerDismissed by secureStore.observeBannerDismissed().collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val show = userTierProvider.shouldShowUpsellBanner(bannerDismissed)
    if (!show) return null
    return {
        PremiumUpsellBanner(
            onClick = navigateToPremium,
            onDismiss = {
                scope.launch { secureStore.setBannerDismissed(true) }
            },
        )
    }
}
