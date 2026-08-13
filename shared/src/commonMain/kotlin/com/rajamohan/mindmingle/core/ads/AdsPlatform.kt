package com.rajamohan.mindmingle.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The one ad surface the shared code knows about. Google publishes no cross-platform
 * Mobile Ads SDK — Android uses `com.google.android.gms:play-services-ads`, iOS uses the
 * `GoogleMobileAds` framework, and desktop has no SDK at all — so the deck logic talks to
 * this expect object and each platform binds its own SDK behind it.
 */
expect object AdsPlatform {

    /** True only where a real Mobile Ads SDK exists (Android, iOS). False on desktop. */
    val isSupported: Boolean

    /** Picks which ad unit ids apply — AdMob units are per-platform, never shared. */
    val isIos: Boolean

    /** Safe to call repeatedly; the underlying SDK init runs at most once. */
    fun initialize()

    /**
     * Loads and presents a full-screen interstitial, suspending until the user dismisses it.
     * Returns false when no ad could be shown (no fill, no network, unsupported platform) —
     * the caller must then let the user continue rather than trapping them on a dead gate.
     */
    suspend fun showInterstitial(unitId: String): Boolean
}

/**
 * A 300x250 (MREC) banner rendered inside the deck's ad card. Draws nothing on platforms
 * without an SDK.
 */
@Composable
expect fun PlatformBannerAd(unitId: String, modifier: Modifier)
