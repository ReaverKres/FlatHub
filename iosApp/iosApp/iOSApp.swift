import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    init() {
        // Koin now initialized in MainViewController before Compose UI starts
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
