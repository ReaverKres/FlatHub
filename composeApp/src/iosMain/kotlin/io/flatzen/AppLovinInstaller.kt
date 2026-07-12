package io.flatzen

import io.flatzen.monetization.ads.installAppLovin as monetizationInstallAppLovin

/**
 * Re-export for Swift (`ComposeApp.AppLovinInstallerKt.installAppLovin`).
 * Thin AppLovin host is registered from `iosApp` via [AppLovinSetup.configureBridge].
 */
fun installAppLovin(
    initialize: (sdkKey: String, onComplete: (Boolean) -> Unit) -> Unit,
    isInitialized: () -> Boolean,
    showInterstitial: (adUnitId: String, onResult: (String) -> Unit) -> Unit,
    showRewarded: (adUnitId: String, onResult: (String) -> Unit) -> Unit,
) {
    monetizationInstallAppLovin(
        initialize = initialize,
        isInitialized = isInitialized,
        showInterstitial = showInterstitial,
        showRewarded = showRewarded,
    )
}
