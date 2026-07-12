package io.flatzen.monetization.ads

import kotlin.concurrent.Volatile

/**
 * Callback-based AppLovin MAX API implemented in Swift (SDK is Swift/ObjC only).
 * Prefer [installAppLovin] from `iosApp` at launch.
 *
 * Ad [onResult] values: `ready` | `no_fill` | `error:<message>`
 */
interface AppLovinNativeApi {
    fun initialize(sdkKey: String, onComplete: (Boolean) -> Unit)
    fun isInitialized(): Boolean
    fun showInterstitial(adUnitId: String, onResult: (String) -> Unit)
    fun showRewarded(adUnitId: String, onResult: (String) -> Unit)
}

object AppLovinNative {
    @Volatile
    var api: AppLovinNativeApi? = null
        private set

    fun install(api: AppLovinNativeApi) {
        this.api = api
    }

    fun isInstalled(): Boolean = api != null
}

fun installAppLovin(
    initialize: (sdkKey: String, onComplete: (Boolean) -> Unit) -> Unit,
    isInitialized: () -> Boolean,
    showInterstitial: (adUnitId: String, onResult: (String) -> Unit) -> Unit,
    showRewarded: (adUnitId: String, onResult: (String) -> Unit) -> Unit,
) {
    AppLovinNative.install(
        object : AppLovinNativeApi {
            override fun initialize(sdkKey: String, onComplete: (Boolean) -> Unit) =
                initialize(sdkKey, onComplete)

            override fun isInitialized(): Boolean = isInitialized()

            override fun showInterstitial(adUnitId: String, onResult: (String) -> Unit) =
                showInterstitial(adUnitId, onResult)

            override fun showRewarded(adUnitId: String, onResult: (String) -> Unit) =
                showRewarded(adUnitId, onResult)
        },
    )
}

internal fun String.toAdLoadResult(): AdLoadResult = when {
    this == "ready" -> AdLoadResult.Ready
    this == "no_fill" -> AdLoadResult.NoFill
    startsWith("error:") -> AdLoadResult.Error(drop(6))
    else -> AdLoadResult.Error(this)
}
