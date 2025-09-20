import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Koin now initialized in MainViewController before Compose UI starts
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
