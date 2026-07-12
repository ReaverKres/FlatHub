import ComposeApp

enum AppLovinSetup {
    static func configureBridge() {
        AppLovinInstallerKt.installAppLovin(
            initialize: { sdkKey, onComplete in
                AppLovinBridge.shared.initialize(sdkKey: sdkKey) { success in
                    onComplete(KotlinBoolean(bool: success))
                }
            },
            isInitialized: {
                KotlinBoolean(bool: AppLovinBridge.shared.isInitialized())
            },
            showInterstitial: { adUnitId, onResult in
                AppLovinBridge.shared.showInterstitial(adUnitId: adUnitId) { result in
                    _ = onResult(result)
                }
            },
            showRewarded: { adUnitId, onResult in
                AppLovinBridge.shared.showRewarded(adUnitId: adUnitId) { result in
                    _ = onResult(result)
                }
            }
        )
    }
}
