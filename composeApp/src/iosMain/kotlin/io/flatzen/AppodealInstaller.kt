package io.flatzen

import platform.UIKit.UIView
import io.flatzen.monetization.ads.installAppodeal as monetizationInstallAppodeal

/**
 * Re-export for Swift (`ComposeApp.AppodealInstallerKt.installAppodeal`).
 * Appodeal host is registered from `iosApp` via [AppodealSetup.configureBridge].
 */
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
    monetizationInstallAppodeal(
        initialize = initialize,
        isInitialized = isInitialized,
        showRewarded = showRewarded,
        prefetchNative = prefetchNative,
        createMrecView = createMrecView,
        createNativeView = createNativeView,
        showMrec = showMrec,
        showNative = showNative,
        releaseView = releaseView,
        clearNativeAdReuseCache = clearNativeAdReuseCache,
    )
}
