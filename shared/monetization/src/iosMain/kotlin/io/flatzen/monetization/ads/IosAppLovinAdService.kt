package io.flatzen.monetization.ads

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.Volatile
import kotlin.coroutines.resume

/**
 * AppLovin MAX — iOS. Swift host via [AppLovinNative]; orchestration stays in Kotlin.
 * Safe when ad unit / SDK key empty: methods return [AdLoadResult.Disabled].
 */
class IosAppLovinAdService : AdService {

    @Volatile
    private var initialized = false

    override fun initialize(sdkKey: String) {
        if (sdkKey.isBlank() || initialized) return
        val api = AppLovinNative.api ?: return
        api.initialize(sdkKey) { success ->
            initialized = success
        }
    }

    override fun isInitialized(): Boolean =
        initialized && (AppLovinNative.api?.isInitialized() == true)

    override suspend fun loadBanner(adUnitId: String): AdLoadResult {
        if (!isInitialized() || adUnitId.isBlank()) return AdLoadResult.Disabled
        return AdLoadResult.Ready
    }

    override suspend fun showInterstitial(adUnitId: String): AdLoadResult {
        if (!isInitialized() || adUnitId.isBlank()) return AdLoadResult.Disabled
        val api = AppLovinNative.api ?: return AdLoadResult.Disabled
        return suspendCancellableCoroutine { cont ->
            api.showInterstitial(adUnitId) { result ->
                if (cont.isActive) cont.resume(result.toAdLoadResult())
            }
        }
    }

    override suspend fun showRewarded(adUnitId: String): AdLoadResult {
        if (!isInitialized() || adUnitId.isBlank()) return AdLoadResult.Disabled
        val api = AppLovinNative.api ?: return AdLoadResult.Disabled
        return suspendCancellableCoroutine { cont ->
            api.showRewarded(adUnitId) { result ->
                if (cont.isActive) cont.resume(result.toAdLoadResult())
            }
        }
    }

    override fun destroy() {
        initialized = false
    }
}
