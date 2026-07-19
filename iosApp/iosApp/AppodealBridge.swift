import Appodeal
import Foundation
import UIKit

// MARK: - Native ad view template (APDNativeAdView is a protocol in SDK 4.2)

@objcMembers
class FlatHubNativeAdView: UIView, APDNativeAdView {
    private let titleLabelView = UILabel()
    private let callToActionLabelView = UILabel()
    private let descriptionLabelView = UILabel()
    private let iconImageView = UIImageView()
    private let mediaView = UIView()
    private let contentRatingLabelView = UILabel()
    private let adChoicesContainerView = UIView()

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupLayout()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupLayout()
    }

    func titleLabel() -> UILabel { titleLabelView }
    func callToActionLabel() -> UILabel { callToActionLabelView }
    func descriptionLabel() -> UILabel { descriptionLabelView }
    func iconView() -> UIImageView { iconImageView }
    func mediaContainerView() -> UIView { mediaView }
    func contentRatingLabel() -> UILabel { contentRatingLabelView }
    func adChoicesView() -> UIView { adChoicesContainerView }

    func setRating(_ rating: NSNumber) {}

    class func nib() -> UINib {
        UINib(nibName: String(describing: FlatHubNativeAdView.self), bundle: Bundle(for: FlatHubNativeAdView.self))
    }

    private func setupLayout() {
        backgroundColor = UIColor.secondarySystemBackground
        layer.cornerRadius = 12
        clipsToBounds = true

        titleLabelView.font = .preferredFont(forTextStyle: .headline)
        titleLabelView.numberOfLines = 2
        descriptionLabelView.font = .preferredFont(forTextStyle: .subheadline)
        descriptionLabelView.numberOfLines = 3
        descriptionLabelView.textColor = .secondaryLabel
        callToActionLabelView.font = .preferredFont(forTextStyle: .caption1)
        callToActionLabelView.textAlignment = .center
        callToActionLabelView.backgroundColor = UIColor.systemBlue
        callToActionLabelView.textColor = .white
        callToActionLabelView.layer.cornerRadius = 8
        callToActionLabelView.clipsToBounds = true

        iconImageView.contentMode = .scaleAspectFill
        iconImageView.clipsToBounds = true
        iconImageView.layer.cornerRadius = 8

        mediaView.backgroundColor = UIColor.tertiarySystemFill
        mediaView.layer.cornerRadius = 8
        mediaView.clipsToBounds = true

        let textStack = UIStackView(arrangedSubviews: [titleLabelView, descriptionLabelView, callToActionLabelView])
        textStack.axis = .vertical
        textStack.spacing = 8

        let row = UIStackView(arrangedSubviews: [iconImageView, textStack, mediaView])
        row.axis = .horizontal
        row.spacing = 12
        row.alignment = .top
        row.translatesAutoresizingMaskIntoConstraints = false
        addSubview(row)

        NSLayoutConstraint.activate([
            row.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 12),
            row.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -12),
            row.topAnchor.constraint(equalTo: topAnchor, constant: 12),
            row.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -12),
            iconImageView.widthAnchor.constraint(equalToConstant: 48),
            iconImageView.heightAnchor.constraint(equalToConstant: 48),
            mediaView.widthAnchor.constraint(equalToConstant: 120),
            mediaView.heightAnchor.constraint(equalToConstant: 120),
            callToActionLabelView.heightAnchor.constraint(greaterThanOrEqualToConstant: 32),
        ])
    }
}

final class NativeAdContainerView: UIView {
    /// Matches FlatHubNativeAdView media row (~120) + padding.
    static let fallbackIntrinsicHeight: CGFloat = 160

    let style: String
    let reuseKey: String?
    var loadedAdView: UIView? {
        didSet { invalidateIntrinsicContentSize() }
    }

    init(style: String, reuseKey: String?) {
        self.style = style
        self.reuseKey = reuseKey
        // Non-zero initial frame: Compose UIKitView hosts are frame-based; starting at .zero
        // makes the interop container report 0×0 and breaks Auto Layout inside the ad template.
        super.init(
            frame: CGRect(
                x: 0,
                y: 0,
                width: UIScreen.main.bounds.width,
                height: Self.fallbackIntrinsicHeight
            )
        )
        // Must stay true — Compose drives size via frame / autoresizing mask.
        translatesAutoresizingMaskIntoConstraints = true
        backgroundColor = .clear
        clipsToBounds = true
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override var intrinsicContentSize: CGSize {
        CGSize(width: UIView.noIntrinsicMetric, height: Self.fallbackIntrinsicHeight)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        loadedAdView?.frame = bounds
    }
}

// MARK: - Bridge

@objc public final class AppodealBridge: NSObject {
    @objc public static let shared = AppodealBridge()

    private var sdkInitialized = false
    private static var activeSessions: [AnyObject] = []
    private var trackedViews = NSHashTable<UIView>.weakObjects()
    private var mrecViews: [String: APDMRECView] = [:]
    private var nativeQueues: [String: APDNativeAdQueue] = [:]
    /// Keeps APDNativeAd across LazyList dispose so scrolling back does not burn a new fill.
    private var reusedNativeAds: [String: APDNativeAd] = [:]

    private override init() {
        super.init()
    }

    @objc public func initialize(appKey: String, onComplete: @escaping (Bool) -> Void) {
        guard !appKey.isEmpty else {
            onComplete(false)
            return
        }
        let adTypes: AppodealAdType = [.nativeAd, .MREC, .rewardedVideo]
        Appodeal.setAutocache(true, types: .nativeAd)
        Appodeal.setAutocache(true, types: .MREC)
        // Rewarded creatives are large video files; cache only on demand in showRewarded.
        Appodeal.setAutocache(false, types: .rewardedVideo)
        Appodeal.setLogLevel(.verbose)
        Appodeal.initialize(withApiKey: appKey, types: adTypes)
        sdkInitialized = true
        onComplete(true)
    }

    @objc public func isInitialized() -> Bool {
        sdkInitialized
    }

    @objc public func showRewarded(placement: String, onResult: @escaping (String) -> Void) {
        DispatchQueue.main.async {
            let session = RewardedSession(placement: placement, onResult: onResult)
            Self.retain(session)
            session.start()
        }
    }

    /// Warm native creatives into the Appodeal / APDNativeAdQueue cache without showing UI.
    /// Both style queues are warmed: Home uses `app_wall`, Swipe uses `content_stream`.
    @objc public func prefetchNative(placement: String, count: Int) {
        DispatchQueue.main.async {
            Appodeal.cacheAd(.nativeAd)
            // loadAd fills the autocache; count is a soft hint — queue pulls as SDK allows.
            _ = count
            for style in ["content_stream", "app_wall"] {
                let queue = self.nativeQueue(for: style, placement: placement)
                queue.placement = placement
                queue.loadAd()
            }
        }
    }

    @objc public func createMrecView(placement: String) -> UIView {
        let view = APDMRECView() ?? APDMRECView(frame: CGRect(x: 0, y: 0, width: 300, height: 250))
        view.translatesAutoresizingMaskIntoConstraints = false
        view.placement = placement
        view.autocache = true
        mrecViews[placement] = view
        trackedViews.add(view)
        return view
    }

    @objc public func createNativeView(placement: String, style: String, reuseKey: String?) -> UIView {
        let container = NativeAdContainerView(style: style, reuseKey: reuseKey)
        trackedViews.add(container)
        _ = nativeQueue(for: style, placement: placement)
        return container
    }

    @objc public func clearNativeAdReuseCache() {
        DispatchQueue.main.async {
            self.reusedNativeAds.removeAll()
        }
    }

    @objc public func showMrec(placement: String) {
        DispatchQueue.main.async {
            guard let view = self.mrecViews[placement] else { return }
            guard let rootVC = Self.topViewController() else { return }
            view.rootViewController = rootVC
            view.placement = placement
            if view.isReady {
                return
            }
            view.loadAd()
        }
    }

    @objc public func showNative(view: UIView, placement: String) -> Bool {
        let attach: () -> Bool = {
            guard let container = view as? NativeAdContainerView else { return false }

            let queue = self.nativeQueue(for: container.style, placement: placement)
            queue.placement = placement

            // Off-screen probe (no superview): reserve only when reuseKey can hold the creative.
            if container.superview == nil {
                if let key = container.reuseKey, self.reusedNativeAds[key] != nil {
                    return true
                }
                if let key = container.reuseKey, self.reusedNativeAds[key] == nil {
                    let ads = queue.getNativeAds(ofCount: 1)
                    if let fetched = ads.first {
                        self.reusedNativeAds[key] = fetched
                        queue.loadAd()
                        return true
                    }
                    queue.loadAd()
                }
                return false
            }

            // Compose UIKit interop is frame-based. Attaching into a 0×0 host permanently
            // breaks FlatHubNativeAdView constraints (NSAutoresizingMask height/width == 0).
            let hostSize = container.bounds.size
            let parentSize = container.superview?.bounds.size ?? .zero
            guard max(hostSize.width, parentSize.width) > 1,
                  max(hostSize.height, parentSize.height) > 1
            else {
                return false
            }

            let nativeAd: APDNativeAd
            if let key = container.reuseKey, let existing = self.reusedNativeAds[key] {
                nativeAd = existing
            } else {
                let ads = queue.getNativeAds(ofCount: 1)
                guard let fetched = ads.first else {
                    queue.loadAd()
                    return false
                }
                if let key = container.reuseKey {
                    self.reusedNativeAds[key] = fetched
                }
                nativeAd = fetched
                queue.loadAd()
            }

            guard let rootVC = Self.topViewController() else { return false }

            guard let adView = try? nativeAd.getViewForPlacement(
                placement,
                withRootViewController: rootVC
            ) else {
                if let key = container.reuseKey {
                    self.reusedNativeAds.removeValue(forKey: key)
                }
                queue.loadAd()
                return false
            }

            if container.loadedAdView === adView, adView.superview === container {
                adView.frame = container.bounds
                return true
            }

            container.loadedAdView?.removeFromSuperview()
            // Frame + autoresizing — do not pin with Auto Layout against Compose's mask constraints.
            adView.translatesAutoresizingMaskIntoConstraints = true
            adView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            let width = max(hostSize.width, parentSize.width, UIScreen.main.bounds.width)
            let height = max(
                hostSize.height,
                parentSize.height,
                NativeAdContainerView.fallbackIntrinsicHeight
            )
            adView.frame = container.bounds.width > 1 && container.bounds.height > 1
                ? container.bounds
                : CGRect(origin: .zero, size: CGSize(width: width, height: height))
            container.addSubview(adView)
            container.loadedAdView = adView
            adView.isOpaque = false
            adView.backgroundColor = .clear
            container.setNeedsLayout()
            container.layoutIfNeeded()
            return true
        }

        if Thread.isMainThread {
            return attach()
        }
        return DispatchQueue.main.sync(execute: attach)
    }

    @objc public func releaseView(_ view: UIView) {
        if let mrecView = view as? APDMRECView, let placement = mrecView.placement {
            mrecViews.removeValue(forKey: placement)
        }
        if let container = view as? NativeAdContainerView {
            container.loadedAdView?.removeFromSuperview()
            container.loadedAdView = nil
        }
        trackedViews.remove(view)
    }

    private func nativeQueue(for style: String, placement: String) -> APDNativeAdQueue {
        if let existing = nativeQueues[style] {
            existing.placement = placement
            return existing
        }

        let settings = APDNativeAdSettings.default()
        settings.adViewClass = FlatHubNativeAdView.self
        settings.type = .auto

        let queue = APDNativeAdQueue(
            sdk: nil,
            settings: settings,
            delegate: nil,
            autocache: true
        )
        queue.placement = placement
        nativeQueues[style] = queue
        return queue
    }

    fileprivate static func retain(_ session: AnyObject) {
        activeSessions.append(session)
    }

    fileprivate static func release(_ session: AnyObject) {
        activeSessions.removeAll { $0 === session }
    }

    fileprivate static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
        let window = scenes.flatMap(\.windows).first { $0.isKeyWindow }
        var controller = window?.rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }
}

private final class RewardedSession: NSObject, AppodealRewardedVideoDelegate {
    private let placement: String
    private let onResult: (String) -> Void
    private var rewardedGranted = false
    private var finished = false

    init(placement: String, onResult: @escaping (String) -> Void) {
        self.placement = placement
        self.onResult = onResult
        super.init()
    }

    func start() {
        guard let rootVC = AppodealBridge.topViewController() else {
            complete("error:No root view controller")
            return
        }
        Appodeal.setRewardedVideoDelegate(self)
        if Appodeal.canShow(.rewardedVideo, forPlacement: placement) {
            Appodeal.showAd(.rewardedVideo, forPlacement: placement, rootViewController: rootVC)
        } else {
            Appodeal.cacheAd(.rewardedVideo)
            if Appodeal.canShow(.rewardedVideo, forPlacement: placement) {
                Appodeal.showAd(.rewardedVideo, forPlacement: placement, rootViewController: rootVC)
            } else {
                complete("no_fill")
            }
        }
    }

    private func complete(_ result: String) {
        guard !finished else { return }
        finished = true
        onResult(result)
        AppodealBridge.release(self)
    }

    func rewardedVideoDidFinishPresenting(forPlacementName placementName: String, finished: Bool) {
        if finished {
            rewardedGranted = true
        }
    }

    func rewardedVideoDidDismiss(forPlacementName placementName: String, finished: Bool) {
        complete(rewardedGranted ? "ready" : "no_fill")
    }

    func rewardedVideoDidFailToPresent(forPlacementName placementName: String) {
        complete("no_fill")
    }
}
