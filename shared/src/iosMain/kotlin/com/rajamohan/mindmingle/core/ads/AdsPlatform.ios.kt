package com.rajamohan.mindmingle.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIView
import kotlin.coroutines.resume

actual object AdsPlatform {

    private var initialized = false

    /** Only true once iosApp has registered the GoogleMobileAds bridge. */
    actual val isSupported: Boolean
        get() = IosAdHost.isBridgeInstalled

    actual val isIos: Boolean = true

    actual fun initialize() {
        if (initialized || !IosAdHost.isBridgeInstalled) return
        initialized = true
        IosAdHost.initializeSdk()
    }

    /**
     * Handed to the Swift bridge, which owns the UMP SDK for the same reason it owns the ads SDK:
     * the framework is added in Xcode, not linked from Gradle. Until that bridge exists no ad is
     * served on iOS either, so there is nothing to gather consent for.
     */
    actual suspend fun requestConsent() {
        if (!IosAdHost.isBridgeInstalled) return

        suspendCancellableCoroutine { continuation ->
            IosAdHost.gatherConsent {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    actual suspend fun showInterstitial(unitId: String): Boolean {
        if (!IosAdHost.isBridgeInstalled) return false
        initialize()

        return suspendCancellableCoroutine { continuation ->
            IosAdHost.present(unitId) { shown ->
                if (continuation.isActive) continuation.resume(shown)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformBannerAd(unitId: String, modifier: Modifier) {
    if (!IosAdHost.isBridgeInstalled || unitId.isBlank()) {
        Box(modifier = modifier)
        return
    }

    UIKitView(
        factory = { IosAdHost.makeBannerView(unitId) ?: UIView() },
        modifier = modifier
    )
}
