import SwiftUI

@main
struct iOSApp: App {

    init() {
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
