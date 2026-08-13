package com.rajamohan.mindmingle.core.ads

import platform.UIKit.UIView

/**
 * The Swift side of AdMob on iOS.
 *
 * The GoogleMobileAds framework is distributed as an Xcode dependency (SPM/CocoaPods), not as
 * something Gradle can put on Kotlin/Native's compile path without dragging a whole CocoaPods
 * integration into this project. So iOS ads are wired the other way round: `iosApp` registers
 * three closures here at launch (see iosApp/iosApp/AdMobBridge.swift) and the shared Kotlin ad
 * layer calls them. Nothing here depends on the ad SDK, so the framework still builds when the
 * host app has not added GoogleMobileAds yet — ads simply stay off until it does.
 */
object IosAdHost {

    /** Set by Swift once GoogleMobileAds is linked; nothing serves until this flips true. */
    var isBridgeInstalled: Boolean = false
        private set

    private var initializer: (() -> Unit)? = null
    private var bannerFactory: ((String) -> UIView)? = null
    private var interstitialPresenter: ((String, (Boolean) -> Unit) -> Unit)? = null

    /**
     * Called from Swift at app start.
     *
     * @param initialize starts the Mobile Ads SDK.
     * @param makeBanner builds a loaded 300x250 GADBannerView for the given ad unit id.
     * @param presentInterstitial loads + presents an interstitial and invokes its callback with
     *   true once the user dismisses it, false when it could not be shown.
     */
    fun install(
        initialize: () -> Unit,
        makeBanner: (String) -> UIView,
        presentInterstitial: (String, (Boolean) -> Unit) -> Unit
    ) {
        initializer = initialize
        bannerFactory = makeBanner
        interstitialPresenter = presentInterstitial
        isBridgeInstalled = true
    }

    internal fun initializeSdk() {
        initializer?.invoke()
    }

    internal fun makeBannerView(unitId: String): UIView? = bannerFactory?.invoke(unitId)

    internal fun present(unitId: String, onFinished: (Boolean) -> Unit) {
        val presenter = interstitialPresenter
        if (presenter == null) {
            onFinished(false)
            return
        }
        presenter(unitId, onFinished)
    }
}
