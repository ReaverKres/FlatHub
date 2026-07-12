import ComposeApp

enum AppMetricaSetup {
    static func configureBridge() {
        AppMetricaIosBridge.shared.configure(
            activate: { apiKey, sessionTimeout, logs in
                AppMetricaBridge.shared.activate(
                    apiKey: apiKey,
                    sessionTimeout: Int(sessionTimeout),
                    logs: logs.boolValue
                )
            },
            report: { name, attributes in
                var params: [String: Any] = [:]
                attributes.forEach { key, value in
                    if let key = key as? String {
                        params[key] = value
                    }
                }
                AppMetricaBridge.shared.reportEvent(name, attributes: params)
            }
        )
    }
}
