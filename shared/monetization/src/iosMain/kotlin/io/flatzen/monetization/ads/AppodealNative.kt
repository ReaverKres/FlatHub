package io.flatzen.monetization.ads

import platform.UIKit.UIView
import kotlin.concurrent.Volatile

/**
 * Callback-based Appodeal API implemented in Swift (SDK is Swift/ObjC only).
 * Prefer [installAppodeal] from `iosApp` at launch.
 *
 * Ad [onResult] values: `ready` | `no_fill` | `error:<message>`
 */
interface AppodealNativeApi {
    fun initialize(appKey: String, onComplete: (Boolean) -> Unit)
    fun isInitialized(): Boolean
    fun showRewarded(placement: String, onResult: (String) -> Unit)
    fun prefetchNative(placement: String, count: Int)
    fun createMrecView(placement: String): UIView
    fun createNativeView(placement: String, style: String, reuseKey: String?): UIView
    fun showMrec(placement: String)
    fun showNative(view: UIView, placement: String)
    fun releaseView(view: UIView)
    fun clearNativeAdReuseCache()
}

object AppodealNative {
    @Volatile
    var api: AppodealNativeApi? = null
        private set

    fun install(api: AppodealNativeApi) {
        this.api = api
    }

    fun isInstalled(): Boolean = api != null
}

fun installAppodeal(
    initialize: (appKey: String, onComplete: (Boolean) -> Unit) -> Unit,
    isInitialized: () -> Boolean,
    showRewarded: (placement: String, onResult: (String) -> Unit) -> Unit,
    prefetchNative: (placement: String, count: Int) -> Unit,
    createMrecView: (placement: String) -> UIView,
    createNativeView: (placement: String, style: String, reuseKey: String?) -> UIView,
    showMrec: (placement: String) -> Unit,
    showNative: (view: UIView, placement: String) -> Unit,
    releaseView: (view: UIView) -> Unit,
    clearNativeAdReuseCache: () -> Unit,
) {
    AppodealNative.install(
        object : AppodealNativeApi {
            override fun initialize(appKey: String, onComplete: (Boolean) -> Unit) =
                initialize(appKey, onComplete)

            override fun isInitialized(): Boolean = isInitialized()

            override fun showRewarded(placement: String, onResult: (String) -> Unit) =
                showRewarded(placement, onResult)

            override fun prefetchNative(placement: String, count: Int) =
                prefetchNative(placement, count)

            override fun createMrecView(placement: String): UIView = createMrecView(placement)

            override fun createNativeView(
                placement: String,
                style: String,
                reuseKey: String?,
            ): UIView = createNativeView(placement, style, reuseKey)

            override fun showMrec(placement: String) = showMrec(placement)

            override fun showNative(view: UIView, placement: String) = showNative(view, placement)

            override fun releaseView(view: UIView) = releaseView(view)

            override fun clearNativeAdReuseCache() = clearNativeAdReuseCache()
        },
    )
}

internal fun String.toAdLoadResult(): AdLoadResult = when {
    this == "ready" -> AdLoadResult.Ready
    this == "no_fill" -> AdLoadResult.NoFill
    startsWith("error:") -> AdLoadResult.Error(drop(6))
    else -> AdLoadResult.Error(this)
}
