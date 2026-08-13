package com.rajamohan.mindmingle.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Google ships no Mobile Ads SDK for desktop JVM, so the desktop build never serves an ad
 * and never arms the swipe gate (see AdConfig.isServableHere).
 */
actual object AdsPlatform {

    actual val isSupported: Boolean = false

    actual val isIos: Boolean = false

    actual fun initialize() = Unit

    actual suspend fun showInterstitial(unitId: String): Boolean = false
}

@Composable
actual fun PlatformBannerAd(unitId: String, modifier: Modifier) {
    Box(modifier = modifier)
}
