package com.rajamohan.mindmingle.core.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.rajamohan.mindmingle.core.AppContext
import io.github.aakira.napier.Napier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "AdsPlatform"

actual object AdsPlatform {

    private var initialized = false

    actual val isSupported: Boolean = true

    actual val isIos: Boolean = false

    actual fun initialize() {
        if (initialized) return
        val context = AppContext.get() as? Context ?: return
        initialized = true
        // Runs its network handshake on a background thread inside the SDK.
        MobileAds.initialize(context) {
            Napier.i(tag = TAG) { "MobileAds initialized" }
        }
    }

    actual suspend fun showInterstitial(unitId: String): Boolean {
        val activity = AppContext.get() as? Activity ?: return false
        initialize()

        val ad = loadInterstitial(activity, unitId) ?: return false
        return presentInterstitial(activity, ad)
    }

    private suspend fun loadInterstitial(activity: Activity, unitId: String): InterstitialAd? =
        suspendCancellableCoroutine { continuation ->
            InterstitialAd.load(
                activity,
                unitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        if (continuation.isActive) continuation.resume(ad)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Napier.w(tag = TAG) { "interstitial load failed: ${error.message}" }
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            )
        }

    /** Resumes once the ad is dismissed (or immediately if it fails to present). */
    private suspend fun presentInterstitial(activity: Activity, ad: InterstitialAd): Boolean =
        suspendCancellableCoroutine { continuation ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    Napier.w(tag = TAG) { "interstitial show failed: ${error.message}" }
                    if (continuation.isActive) continuation.resume(false)
                }
            }
            ad.show(activity)
        }
}

@Composable
actual fun PlatformBannerAd(unitId: String, modifier: Modifier) {
    val context = AppContext.get() as? Context

    if (context == null || unitId.isBlank()) {
        Box(modifier = modifier)
        return
    }

    val adView = remember(unitId) {
        AdView(context).apply {
            setAdSize(AdSize.MEDIUM_RECTANGLE)
            adUnitId = unitId
            loadAd(AdRequest.Builder().build())
        }
    }

    // AdView holds a WebView; leaking it across recompositions leaks the whole activity.
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    AndroidView(factory = { adView }, modifier = modifier)
}
