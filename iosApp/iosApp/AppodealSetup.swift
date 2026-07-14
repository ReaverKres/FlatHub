import ComposeApp
import UIKit

enum AppodealSetup {
    static func configureBridge() {
        AppodealInstallerKt.installAppodeal(
            initialize: { appKey, onComplete in
                AppodealBridge.shared.initialize(appKey: appKey) { success in
                    onComplete(KotlinBoolean(bool: success))
                }
            },
            isInitialized: {
                KotlinBoolean(bool: AppodealBridge.shared.isInitialized())
            },
            showRewarded: { placement, onResult in
                AppodealBridge.shared.showRewarded(placement: placement) { result in
                    _ = onResult(result)
                }
            },
            prefetchNative: { placement, count in
                AppodealBridge.shared.prefetchNative(placement: placement, count: count.intValue)
            },
            createMrecView: { placement in
                AppodealBridge.shared.createMrecView(placement: placement)
            },
            createNativeView: { placement, style, reuseKey in
                AppodealBridge.shared.createNativeView(
                    placement: placement,
                    style: style,
                    reuseKey: reuseKey as String?
                )
            },
            showMrec: { placement in
                AppodealBridge.shared.showMrec(placement: placement)
            },
            showNative: { view, placement in
                AppodealBridge.shared.showNative(view: view, placement: placement)
            },
            releaseView: { view in
                AppodealBridge.shared.releaseView(view)
            },
            clearNativeAdReuseCache: {
                AppodealBridge.shared.clearNativeAdReuseCache()
            }
        )
    }
}
