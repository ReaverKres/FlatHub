package io.flatzen.viewmodel.premium

import androidx.compose.runtime.Immutable
import io.flatzen.monetization.MonetizationDefaults
import io.flatzen.monetization.ads.AdLoadResult
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.billing.PurchaseResult
import io.flatzen.monetization.billing.SubscriptionProduct
import io.flatzen.monetization.billing.SubscriptionService
import io.flatzen.monetization.billing.SubscriptionStatus
import io.flatzen.monetization.billing.SubscriptionTier
import io.flatzen.monetization.config.MonetizationRemoteConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed

private typealias PremiumCtx = PipelineContext<PremiumState, PremiumIntent, PremiumAction>

@Immutable
data class PremiumState(
    val products: ImmutableList<SubscriptionProduct> = persistentListOf(),
    val selectedProductId: String? = null,
    val loading: Boolean = false,
    val purchasing: Boolean = false,
    val message: String? = null,
    val isPremiumActive: Boolean = false,
    val statusSource: SubscriptionStatus.StatusSource? = null,
    val expiresAtEpochMs: Long? = null,
    val activeProductId: String? = null,
    val showDebugToggle: Boolean = MonetizationDefaults.DEBUG_PREMIUM_SCREEN_TOGGLE,
    /** null = use [isPremiumActive]; otherwise forces the UI branch for debug. */
    val debugForceActive: Boolean? = MonetizationDefaults.DEBUG_PREMIUM_SCREEN_FORCE_ACTIVE,
) : MVIState {
    val showActivePremium: Boolean
        get() = debugForceActive ?: isPremiumActive

    companion object {
        val Initial = PremiumState()
    }
}

sealed interface PremiumIntent : MVIIntent {
    data object Load : PremiumIntent
    data class SelectProduct(val productId: String) : PremiumIntent
    data object Purchase : PremiumIntent
    data object Restore : PremiumIntent
    data object WatchRewardedAd : PremiumIntent
    data class SetDebugForceActive(val forceActive: Boolean?) : PremiumIntent
}

sealed interface PremiumAction : MVIAction {
    data object NavigateBack : PremiumAction
}

class PremiumContainer(
    private val subscriptionService: SubscriptionService,
    private val adService: AdService,
    private val monetizationRemoteConfig: MonetizationRemoteConfig,
) : Container<PremiumState, PremiumIntent, PremiumAction> {

    override val store = store(initial = PremiumState.Initial) {
        whileSubscribed {
            subscriptionService.observeStatus().collect { status ->
                updateState {
                    copy(
                        isPremiumActive = status.tier == SubscriptionTier.PREMIUM,
                        statusSource = status.source,
                        expiresAtEpochMs = status.expiresAtEpochMs,
                        activeProductId = status.productId,
                    )
                }
            }
        }
        reduce { intent ->
            when (intent) {
                PremiumIntent.Load -> handleLoad()
                is PremiumIntent.SelectProduct -> updateState {
                    copy(selectedProductId = intent.productId, message = null)
                }

                PremiumIntent.Purchase -> handlePurchase()
                PremiumIntent.Restore -> handleRestore()
                PremiumIntent.WatchRewardedAd -> handleWatchRewardedAd()
                is PremiumIntent.SetDebugForceActive -> updateState {
                    copy(debugForceActive = intent.forceActive)
                }
            }
        }
    }

    private suspend fun PremiumCtx.handleLoad() {
        updateState { copy(loading = true, message = null) }
        runCatching { subscriptionService.getProducts() }
            .onSuccess { products ->
                var selectedId: String? = null
                withState {
                    selectedId = selectedProductId?.takeIf { id -> products.any { it.id == id } }
                        ?: products.lastOrNull()?.id
                                ?: products.firstOrNull()?.id
                }
                updateState {
                    copy(
                        products = products.toImmutableList(),
                        selectedProductId = selectedId,
                        loading = false,
                    )
                }
            }
            .onFailure { error ->
                updateState {
                    copy(
                        loading = false,
                        message = error.message ?: "Не удалось загрузить тарифы",
                    )
                }
            }
    }

    private suspend fun PremiumCtx.handlePurchase() {
        var productId: String? = null
        withState { productId = selectedProductId }
        val id = productId ?: return
        updateState { copy(purchasing = true, message = null) }
        when (val result = subscriptionService.purchase(id)) {
            PurchaseResult.Success -> {
                updateState {
                    copy(
                        purchasing = false,
                        message = "Подписка активирована",
                        isPremiumActive = true,
                        debugForceActive = if (showDebugToggle) true else debugForceActive,
                    )
                }
            }

            PurchaseResult.Cancelled -> updateState {
                copy(purchasing = false, message = null)
            }

            is PurchaseResult.Error -> updateState {
                copy(purchasing = false, message = result.message)
            }

            PurchaseResult.NotConfigured -> updateState {
                copy(purchasing = false, message = "Покупки временно недоступны")
            }
        }
    }

    private suspend fun PremiumCtx.handleRestore() {
        updateState { copy(purchasing = true, message = null) }
        runCatching { subscriptionService.restore() }
            .onSuccess { status ->
                val isPremium = status.tier == SubscriptionTier.PREMIUM
                val message = if (isPremium) {
                    "Подписка восстановлена"
                } else {
                    "Активная подписка не найдена"
                }
                updateState {
                    copy(
                        purchasing = false,
                        message = message,
                        isPremiumActive = isPremium,
                        statusSource = status.source,
                        expiresAtEpochMs = status.expiresAtEpochMs,
                        activeProductId = status.productId,
                        debugForceActive = when {
                            !showDebugToggle -> debugForceActive
                            isPremium -> true
                            else -> debugForceActive
                        },
                    )
                }
            }
            .onFailure { error ->
                updateState {
                    copy(
                        purchasing = false,
                        message = error.message ?: "Не удалось восстановить покупки",
                    )
                }
            }
    }

    private suspend fun PremiumCtx.handleWatchRewardedAd() {
        updateState { copy(purchasing = true, message = null) }
        when (val result = adService.showRewarded(monetizationRemoteConfig.rewardedAdUnit)) {
            AdLoadResult.Ready -> {
                subscriptionService.grantRewardedPremium(1)
                updateState {
                    copy(
                        purchasing = false,
                        message = "Premium на 1 час активирован",
                        isPremiumActive = true,
                        statusSource = SubscriptionStatus.StatusSource.REWARDED,
                        debugForceActive = if (showDebugToggle) true else debugForceActive,
                    )
                }
            }

            AdLoadResult.NoFill -> updateState {
                copy(purchasing = false, message = "Реклама сейчас недоступна")
            }

            is AdLoadResult.Error -> updateState {
                copy(purchasing = false, message = result.message)
            }

            AdLoadResult.Disabled -> updateState {
                copy(purchasing = false, message = "Реклама отключена")
            }
        }
    }
}
