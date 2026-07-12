import ComposeApp

enum AppLovinSetup {
    static func configureBridge() {
        AppLovinInstallerKt.installAppLovin(
            initialize: { sdkKey, onComplete in
                AppLovinBridge.shared.initialize(sdkKey: sdkKey, onComplete: onComplete)
            },
            isInitialized: {
                AppLovinBridge.shared.isInitialized()
            },
            showInterstitial: { adUnitId, onResult in
                AppLovinBridge.shared.showInterstitial(adUnitId: adUnitId, onResult: onResult)
            },
            showRewarded: { adUnitId, onResult in
                AppLovinBridge.shared.showRewarded(adUnitId: adUnitId, onResult: onResult)
            }
        )
    }
}
