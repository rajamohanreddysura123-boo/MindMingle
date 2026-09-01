import SwiftUI
import FirebaseCore

@main
struct iOSApp: App {

    init() {
        // Must run before anything touches Firebase. The shared Kotlin layer talks to the same
        // native SDK through dev.gitlive wrappers, so without this every Auth/Firestore call
        // from Kotlin traps on a missing default FirebaseApp. Reads GoogleService-Info.plist.
        FirebaseApp.configure()
        // Hands the GoogleMobileAds SDK to the shared Kotlin ad layer. No-op until the
        // GoogleMobileAds package is added to this target — see AdMobBridge.swift.
        AdMobBridge.install()
        // Hands the Razorpay SDK to the shared Kotlin payment layer. No-op until the
        // Razorpay package is added to this target — see RazorpayBridge.swift.
        RazorpayBridge.install()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
