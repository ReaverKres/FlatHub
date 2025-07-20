import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        CommonApplication.initKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
