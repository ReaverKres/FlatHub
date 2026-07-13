package io.flatzen.monetization.ads

import kotlin.concurrent.Volatile
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Appodeal — iOS. Swift host via [AppodealNative]; orchestration stays in Kotlin.
 * Safe when app key empty: methods return [AdLoadResult.Disabled].
 */
class IosAppodealAdService : AdService {

    @Volatile
    private var initialized = false

    override fun initialize(androidAppKey: String, iosAppKey: String) {
        if (iosAppKey.isBlank() || initialized) return
        val api = AppodealNative.api ?: return
        api.initialize(iosAppKey) { success ->
            initialized = success
        }
    }

    override fun isInitialized(): Boolean =
        initialized && (AppodealNative.api?.isInitialized() == true)

    override suspend fun prefetchNative(placement: String, count: Int): AdLoadResult {
        if (!isInitialized() || placement.isBlank()) return AdLoadResult.Disabled
        return AdLoadResult.Ready
    }

    override suspend fun prefetchMrec(placement: String): AdLoadResult {
        if (!isInitialized() || placement.isBlank()) return AdLoadResult.Disabled
        AppodealNative.api?.showMrec(placement)
        return AdLoadResult.Ready
    }

    override suspend fun showRewarded(placement: String): AdLoadResult {
        if (!isInitialized() || placement.isBlank()) return AdLoadResult.Disabled
        val api = AppodealNative.api ?: return AdLoadResult.Disabled
        return suspendCancellableCoroutine { cont ->
            api.showRewarded(placement) { result ->
                if (cont.isActive) cont.resume(result.toAdLoadResult())
            }
        }
    }

    override fun destroy() {
        initialized = false
    }
}
