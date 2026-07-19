package io.flatzen.monetization.ads

import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.Volatile
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
        if (!awaitInitialized() || placement.isBlank()) return AdLoadResult.Disabled
        AppodealNative.api?.prefetchNative(placement, count.coerceIn(1, 5))
        return AdLoadResult.Ready
    }

    override suspend fun prefetchMrec(placement: String): AdLoadResult {
        if (!awaitInitialized() || placement.isBlank()) return AdLoadResult.Disabled
        AppodealNative.api?.showMrec(placement)
        return AdLoadResult.Ready
    }

    override suspend fun showRewarded(placement: String): AdLoadResult {
        if (placement.isBlank()) return AdLoadResult.Disabled
        if (!awaitInitialized()) return AdLoadResult.Disabled
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

    private suspend fun awaitInitialized(): Boolean {
        if (isInitialized()) return true
        repeat(SDK_INIT_MAX_ATTEMPTS) {
            delay(SDK_INIT_POLL_MS)
            if (isInitialized()) return true
        }
        return false
    }

    private companion object {
        const val SDK_INIT_POLL_MS = 200L
        const val SDK_INIT_MAX_ATTEMPTS = 50
    }
}
