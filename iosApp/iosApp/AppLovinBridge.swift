import AppLovinSDK
import Foundation
import UIKit

@objc public final class AppLovinBridge: NSObject {
    @objc public static let shared = AppLovinBridge()

    private var sdkInitialized = false
    private static var activeSessions: [AnyObject] = []

    private override init() {
        super.init()
    }

    @objc public func initialize(sdkKey: String, onComplete: @escaping (Bool) -> Void) {
        guard !sdkKey.isEmpty else {
            onComplete(false)
            return
        }
        let initConfig = ALSdkInitializationConfiguration(sdkKey: sdkKey) { builder in
            builder.mediationProvider = ALMediationProviderMAX
        }
        ALSdk.shared().initialize(with: initConfig) { [weak self] _ in
            self?.sdkInitialized = true
            onComplete(true)
        }
    }

    @objc public func isInitialized() -> Bool {
        sdkInitialized && ALSdk.shared().isInitialized
    }

    @objc public func showInterstitial(adUnitId: String, onResult: @escaping (String) -> Void) {
        DispatchQueue.main.async {
            let session = InterstitialSession(adUnitId: adUnitId, onResult: onResult)
            Self.retain(session)
            session.start()
        }
    }

    @objc public func showRewarded(adUnitId: String, onResult: @escaping (String) -> Void) {
        DispatchQueue.main.async {
            let session = RewardedSession(adUnitId: adUnitId, onResult: onResult)
            Self.retain(session)
            session.start()
        }
    }

    fileprivate static func retain(_ session: AnyObject) {
        activeSessions.append(session)
    }

    fileprivate static func release(_ session: AnyObject) {
        activeSessions.removeAll { $0 === session }
    }
}

private final class InterstitialSession: NSObject, MAAdDelegate {
    private let adUnitId: String
    private let onResult: (String) -> Void
    private var interstitial: MAInterstitialAd?
    private var finished = false

    init(adUnitId: String, onResult: @escaping (String) -> Void) {
        self.adUnitId = adUnitId
        self.onResult = onResult
        super.init()
    }

    func start() {
        let ad = MAInterstitialAd(adUnitIdentifier: adUnitId)
        ad.delegate = self
        interstitial = ad
        ad.load()
    }

    private func complete(_ result: String) {
        guard !finished else { return }
        finished = true
        onResult(result)
        interstitial?.delegate = nil
        interstitial = nil
        AppLovinBridge.release(self)
    }

    func didLoad(_ ad: MAAd) {
        interstitial?.show()
    }

    func didFailToLoadAd(forAdUnitIdentifier adUnitIdentifier: String, withError error: MAError) {
        complete("no_fill")
    }

    func didHide(_ ad: MAAd) {
        complete("ready")
    }

    func didFail(toDisplay ad: MAAd, withError error: MAError) {
        complete("error:\(error.message)")
    }

    func didDisplay(_ ad: MAAd) {}
    func didClick(_ ad: MAAd) {}
}

private final class RewardedSession: NSObject, MARewardedAdDelegate {
    private let adUnitId: String
    private let onResult: (String) -> Void
    private var rewarded: MARewardedAd?
    private var rewardedGranted = false
    private var finished = false

    init(adUnitId: String, onResult: @escaping (String) -> Void) {
        self.adUnitId = adUnitId
        self.onResult = onResult
        super.init()
    }

    func start() {
        let ad = MARewardedAd.shared(withAdUnitIdentifier: adUnitId)
        ad.delegate = self
        rewarded = ad
        ad.load()
    }

    private func complete(_ result: String) {
        guard !finished else { return }
        finished = true
        onResult(result)
        rewarded?.delegate = nil
        rewarded = nil
        AppLovinBridge.release(self)
    }

    func didLoad(_ ad: MAAd) {
        rewarded?.show()
    }

    func didFailToLoadAd(forAdUnitIdentifier adUnitIdentifier: String, withError error: MAError) {
        complete("no_fill")
    }

    func didRewardUser(for ad: MAAd, with reward: MAReward) {
        rewardedGranted = true
    }

    func didHide(_ ad: MAAd) {
        complete(rewardedGranted ? "ready" : "no_fill")
    }

    func didFail(toDisplay ad: MAAd, withError error: MAError) {
        complete("error:\(error.message)")
    }

    func didDisplay(_ ad: MAAd) {}
    func didClick(_ ad: MAAd) {}
}
