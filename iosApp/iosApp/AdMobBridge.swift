import UIKit
import Shared

#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

/**
 Wires the Google Mobile Ads iOS SDK into the shared Kotlin ad layer.

 The shared module deliberately does not link GoogleMobileAds itself — doing that from Gradle
 would force a CocoaPods integration onto this Xcode project. Instead the Kotlin side declares
 `IosAdHost` (shared/src/iosMain/.../core/ads/IosAdHost.kt) and this file fills it in, so the
 shared framework keeps building whether or not the SDK is present.

 Setup (once):
   1. Xcode > File > Add Package Dependencies… > https://github.com/googleads/swift-package-manager-google-mobile-ads
      (SDK 12.0 or newer — this file uses the Swift API names introduced in v12).
   2. Info.plist: add `GADApplicationIdentifier` = your AdMob iOS app id
      (test value: ca-app-pub-3940256099942544~1458002511). The SDK crashes on launch without it.
   3. Info.plist: add `NSUserTrackingUsageDescription` if you request ATT consent.
   4. The UMP consent SDK ships inside the same Swift package, so `gatherConsent` below needs no
      extra dependency. Configure the message in AdMob > Privacy & messaging first, or the form
      never appears and European users are served nothing.

 Until step 1 is done, `canImport(GoogleMobileAds)` is false, the bridge is never installed,
 and the Discover deck simply serves no ads on iOS.
 */
enum AdMobBridge {

    /// Called once at app start from iOSApp.swift.
    static func install() {
        #if canImport(GoogleMobileAds)
        IosAdHost.shared.install(
            initialize: {
                MobileAds.shared.start(completionHandler: nil)
            },
            makeBanner: { unitId in
                makeBannerView(unitId: unitId)
            },
            presentInterstitial: { unitId, onFinished in
                InterstitialPresenter.shared.present(unitId: unitId) { shown in
                    onFinished(KotlinBoolean(bool: shown))
                }
            },
            gatherConsent: { onFinished in
                gatherConsent(onFinished: onFinished)
            }
        )
        #endif
    }

    #if canImport(GoogleMobileAds)
    /**
     Runs the UMP consent flow before any ad is requested.

     Google requires a consent mechanism for users in the EEA and the UK, and serves little or
     nothing to them without one. Which users need a form is decided by the SDK from their
     location — this never guesses — and the answer is remembered across launches, so anyone who
     has already decided sees nothing.

     Errors are swallowed deliberately: a consent check that cannot reach the network must not
     stop Discover from loading. The SDK then serves non-personalised ads or none, which is the
     right conservative outcome.
     */
    private static func gatherConsent(onFinished: @escaping () -> Void) {
        let parameters = RequestParameters()

        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { error in
            if let error {
                print("AdMobBridge: consent info update failed — \(error.localizedDescription)")
                onFinished()
                return
            }

            guard let root = topViewController() else {
                onFinished()
                return
            }

            // Shows a form only when one is required and unanswered; otherwise returns at once.
            ConsentForm.loadAndPresentIfRequired(from: root) { formError in
                if let formError {
                    print("AdMobBridge: consent form failed — \(formError.localizedDescription)")
                }
                onFinished()
            }
        }
    }
    #endif

    #if canImport(GoogleMobileAds)
    /// A 300x250 unit sized to match the Compose ad card slot.
    private static func makeBannerView(unitId: String) -> UIView {
        let banner = BannerView(adSize: AdSizeMediumRectangle)
        banner.adUnitID = unitId
        banner.rootViewController = topViewController()
        banner.load(Request())
        return banner
    }

    fileprivate static func topViewController() -> UIViewController? {
        let keyWindow = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }

        var top = keyWindow?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
    #endif
}

#if canImport(GoogleMobileAds)
/// Loads and shows one interstitial at a time, reporting back only once it is dismissed —
/// that is what keeps the Kotlin side's swipe gate closed for exactly as long as the ad runs.
private final class InterstitialPresenter: NSObject, FullScreenContentDelegate {

    static let shared = InterstitialPresenter()

    private var completion: ((Bool) -> Void)?
    private var interstitial: InterstitialAd?

    func present(unitId: String, completion: @escaping (Bool) -> Void) {
        guard let rootViewController = AdMobBridge.topViewController() else {
            completion(false)
            return
        }

        self.completion = completion

        InterstitialAd.load(with: unitId, request: Request()) { [weak self] ad, error in
            guard let self else { return }

            guard let ad, error == nil else {
                self.finish(shown: false)
                return
            }

            ad.fullScreenContentDelegate = self
            self.interstitial = ad
            ad.present(from: rootViewController)
        }
    }

    private func finish(shown: Bool) {
        let callback = completion
        completion = nil
        interstitial = nil
        callback?(shown)
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        finish(shown: true)
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        finish(shown: false)
    }
}
#endif
