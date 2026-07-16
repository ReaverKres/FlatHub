import FirebaseCore
import FirebaseRemoteConfig
import Foundation
import ComposeApp

@objc public final class FirebaseRemoteConfigBridge: NSObject {
    @objc public static let shared = FirebaseRemoteConfigBridge()

    private var remoteConfig: RemoteConfig?

    private override init() {
        super.init()
    }

    @objc public func configure(timeoutSeconds: Int) {
        guard FirebaseApp.app() != nil else { return }
        let config = RemoteConfig.remoteConfig()
        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = 0
        settings.fetchTimeout = TimeInterval(timeoutSeconds)
        config.configSettings = settings
        config.setDefaults([
            "consentManagerEnabled": true as NSObject,
            "adsEnabled": true as NSObject,
            "premiumFallbackEnabled": false as NSObject,
            // Empty = all registered PL platforms.
            "enabled_platforms_pl": "" as NSObject,
        ])
        remoteConfig = config
    }

    @objc public func fetchAndActivate(onComplete: @escaping (Bool) -> Void) {
        guard let config = remoteConfig else {
            onComplete(false)
            return
        }
        config.fetchAndActivate { _, error in
            onComplete(error == nil)
        }
    }

    @objc public func getString(_ key: String) -> String {
        remoteConfig?.configValue(forKey: key).stringValue ?? ""
    }

    @objc public func getLong(_ key: String) -> String {
        String(remoteConfig?.configValue(forKey: key).numberValue.int64Value ?? 0)
    }

    @objc public func getBool(_ key: String) -> String {
        (remoteConfig?.configValue(forKey: key).boolValue ?? false) ? "true" : "false"
    }

    @objc public func isLoaded() -> Bool {
        guard let config = remoteConfig else { return false }
        switch config.lastFetchStatus {
        case .success, .throttled:
            return true
        default:
            return false
        }
    }
}

enum FirebaseRemoteConfigSetup {
    static func configureBridge() {
        RemoteConfigIosBridge.shared.configure(
            init: { timeout in
                FirebaseRemoteConfigBridge.shared.configure(timeoutSeconds: Int(timeout))
            },
            fetchAndActivate: { onComplete in
                FirebaseRemoteConfigBridge.shared.fetchAndActivate { success in
                    onComplete(KotlinBoolean(bool: success))
                }
            },
            getString: { key in
                FirebaseRemoteConfigBridge.shared.getString(key)
            },
            getLong: { key in
                FirebaseRemoteConfigBridge.shared.getLong(key)
            },
            getBool: { key in
                FirebaseRemoteConfigBridge.shared.getBool(key)
            },
            isLoaded: {
                KotlinBoolean(bool: FirebaseRemoteConfigBridge.shared.isLoaded())
            }
        )
    }
}
