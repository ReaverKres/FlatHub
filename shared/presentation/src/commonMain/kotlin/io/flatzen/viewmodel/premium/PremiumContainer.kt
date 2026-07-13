package io.flatzen.viewmodel.premium

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.monetization.MonetizationDefaults
import io.flatzen.monetization.ads.AdLoadResult
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.billing.PurchaseResult
import io.flatzen.monetization.billing.SubscriptionProduct
import io.flatzen.monetization.billing.SubscriptionService
import io.flatzen.monetization.billing.SubscriptionStatus
import io.flatzen.monetization.billing.SubscriptionTier
import io.flatzen.monetization.config.MonetizationRemoteConfig
import io.flatzen.monetization.time.TrustStatus
import io.flatzen.monetization.time.TrustedTimeRepository
import io.flatzen.navigation.FlatHubCommand
import io.flatzen.navigation.FlatHubNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
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
    val message: PremiumMessage? = null,
    val isPremiumActive: Boolean = false,
    val statusSource: SubscriptionStatus.StatusSource? = null,
    val expiresAtEpochMs: Long? = null,
    val activeProductId: String? = null,
    val trustStatus: TrustStatus = TrustStatus.UNVERIFIED,
    val showDebugToggle: Boolean = MonetizationDefaults.DEBUG_PREMIUM_SCREEN_TOGGLE,
    /** null = use [isPremiumActive]; otherwise forces the UI branch for debug. */
    val debugForceActive: Boolean? = MonetizationDefaults.DEBUG_PREMIUM_SCREEN_FORCE_ACTIVE,
) : MVIState {
    val showActivePremium: Boolean
        get() = debugForceActive ?: isPremiumActive

    val showClockTamperWarning: Boolean
        get() = !showActivePremium && trustStatus == TrustStatus.SUSPECT

    val canWatchRewardedAd: Boolean
        get() = !purchasing && trustStatus != TrustStatus.SUSPECT

    val canExtendRewardedPremium: Boolean
        get() = isPremiumActive &&
                statusSource == SubscriptionStatus.StatusSource.REWARDED &&
                canWatchRewardedAd

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
    data object NavigateBack : PremiumIntent
}

sealed interface PremiumAction : MVIAction

class PremiumContainer(
    private val subscriptionService: SubscriptionService,
    private val trustedTimeRepository: TrustedTimeRepository,
    private val adService: AdService,
    private val monetizationRemoteConfig: MonetizationRemoteConfig,
    private val navigator: FlatHubNavigator,
) : Container<PremiumState, PremiumIntent, PremiumAction> {

    override val store = store(initial = PremiumState.Initial) {
        whileSubscribed {
            combine(
                subscriptionService.observeStatus(),
                trustedTimeRepository.state,
            ) { status, trusted ->
                status to trusted.status
            }.collect { (status, trustStatus) ->
                updateState {
                    copy(
                        isPremiumActive = status.tier == SubscriptionTier.PREMIUM,
                        statusSource = status.source,
                        expiresAtEpochMs = status.expiresAtEpochMs,
                        activeProductId = status.productId,
                        trustStatus = trustStatus,
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
                PremiumIntent.NavigateBack -> navigator.navigate(FlatHubCommand.NavigateBack)
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
            .onFailure {
                updateState {
                    copy(
                        loading = false,
                        message = PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_LOAD_PLANS),
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
                        message = PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_SUBSCRIPTION_ACTIVATED),
                        isPremiumActive = true,
                        debugForceActive = if (showDebugToggle) true else debugForceActive,
                    )
                }
            }

            PurchaseResult.Cancelled -> updateState {
                copy(purchasing = false, message = null)
            }

            is PurchaseResult.Error -> updateState {
                copy(purchasing = false, message = result.toPremiumMessage())
            }

            PurchaseResult.NotConfigured -> updateState {
                copy(
                    purchasing = false,
                    message = PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_PURCHASES_UNAVAILABLE),
                )
            }
        }
    }

    private suspend fun PremiumCtx.handleRestore() {
        updateState { copy(purchasing = true, message = null) }
        runCatching { subscriptionService.restore() }
            .onSuccess { status ->
                val isPremium = status.tier == SubscriptionTier.PREMIUM
                val message = if (isPremium) {
                    PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_RESTORED)
                } else {
                    PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_NO_ACTIVE_SUBSCRIPTION)
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
            .onFailure {
                updateState {
                    copy(
                        purchasing = false,
                        message = PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_RESTORE_FAILED),
                    )
                }
            }
    }

    private suspend fun PremiumCtx.handleWatchRewardedAd() {
        var blocked = false
        withState { blocked = trustStatus == TrustStatus.SUSPECT }
        if (blocked) return
        updateState { copy(purchasing = true, message = null) }
        when (val result = adService.showRewarded(monetizationRemoteConfig.rewardedAdUnit)) {
            AdLoadResult.Ready -> {
                subscriptionService.grantRewardedPremium(MonetizationDefaults.REWARDED_PREMIUM_HOURS)
                updateState {
                    copy(
                        purchasing = false,
                        message = PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_REWARDED_ACTIVATED),
                        isPremiumActive = true,
                        statusSource = SubscriptionStatus.StatusSource.REWARDED,
                        debugForceActive = if (showDebugToggle) true else debugForceActive,
                    )
                }
            }

            AdLoadResult.NoFill -> updateState {
                copy(
                    purchasing = false,
                    message = PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_AD_UNAVAILABLE),
                )
            }

            is AdLoadResult.Error -> updateState {
                copy(
                    purchasing = false,
                    message = result.message?.takeIf { it.isNotBlank() }?.let(PremiumMessage::Raw)
                        ?: PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_GENERIC),
                )
            }

            AdLoadResult.Disabled -> updateState {
                copy(
                    purchasing = false,
                    message = PremiumMessage.Localized(LocalizationKeys.PREMIUM_ERROR_AD_DISABLED),
                )
            }
        }
    }
}
