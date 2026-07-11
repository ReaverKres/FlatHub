import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    init() {
        // StoreKit 2 host before Compose/Koin billing starts using it.
        FlatZenStoreKit2.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
