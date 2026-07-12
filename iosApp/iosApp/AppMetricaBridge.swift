import AppMetricaCore
import Foundation

@objc public final class AppMetricaBridge: NSObject {
    @objc public static let shared = AppMetricaBridge()

    private override init() {
        super.init()
    }

    @objc public func activate(apiKey: String, sessionTimeout: Int, logs: Bool) {
        guard let configuration = AppMetricaConfiguration(apiKey: apiKey) else {
            return
        }
        configuration.sessionTimeout = UInt(sessionTimeout)
        configuration.areLogsEnabled = logs
        AppMetrica.activate(with: configuration)
    }

    @objc public func reportEvent(_ name: String, attributes: [String: Any]) {
        AppMetrica.reportEvent(name: name, parameters: attributes, onFailure: nil)
    }
}
