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
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.rajamohan.mindmingle.core.AppContext
import io.github.aakira.napier.Napier
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "AdsPlatform"

/**
 * Hashed advertising ids for development hardware. Emulators are already treated as test devices
 * by the SDK; physical phones have to be listed here by hand — see [AdsPlatform.registerTestDevices].
 */
private val TEST_DEVICE_IDS = emptyList<String>()

actual object AdsPlatform {

    private var initialized = false

    actual val isSupported: Boolean = true

    actual val isIos: Boolean = false

    actual fun initialize() {
        if (initialized) return
        val context = AppContext.get() as? Context ?: return
        initialized = true

        registerTestDevices()

        // Runs its network handshake on a background thread inside the SDK.
        MobileAds.initialize(context) {
            Napier.i(tag = TAG) { "MobileAds initialized" }
        }
    }

    /**
     * Marks development hardware as test devices, so a debug build loading a live ad unit serves a
     * test ad instead of a real one.
     *
     * This matters more than it looks. The manifest carries the live AdMob application id, so
     * without this a developer tapping their own ad generates invalid traffic against the real
     * account — which is how AdMob accounts get suspended, not merely warned.
     *
     * Emulators are test devices to the SDK automatically. A physical phone is not: run the app
     * once, find the line the SDK logs — "Use RequestConfiguration.Builder.setTestDeviceIds(...)"
     * — and add that hashed id here. It is a hash of the advertising id, not an identifier of the
     * person, and it is safe to commit.
     */
    private fun registerTestDevices() {
        if (TEST_DEVICE_IDS.isEmpty()) return
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(TEST_DEVICE_IDS)
                .build()
        )
        Napier.i(tag = TAG) { "registered ${TEST_DEVICE_IDS.size} ad test device(s)" }
    }

    /**
     * The consent flow Google requires before serving a personalised ad in the EEA or the UK.
     *
     * Two calls, in this order every time: ask the SDK whether this user needs a form, and show one
     * if they do. Which users those are is decided by the SDK from their location — the app never
     * guesses, and a user outside the requirement never sees anything. The answer is remembered
     * across launches, so this is silent for everyone who has already decided.
     *
     * Failures are swallowed on purpose. A consent check that cannot reach the network must not
     * block Discover from loading; the SDK will simply serve non-personalised ads or none, which
     * is the correct conservative outcome.
     */
    actual suspend fun requestConsent() {
        val activity = AppContext.get() as? Activity ?: return

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        val ready = suspendCancellableCoroutine { continuation ->
            consentInformation.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                { if (continuation.isActive) continuation.resume(true) },
                { error ->
                    Napier.w(tag = TAG) { "consent info update failed: ${error.message}" }
                    if (continuation.isActive) continuation.resume(false)
                }
            )
        }
        if (!ready) return

        suspendCancellableCoroutine { continuation ->
            // Shows the form only when one is required and not yet answered; otherwise returns
            // immediately, which is why this needs no separate "is a form available" branch.
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (error != null) {
                    Napier.w(tag = TAG) { "consent form failed: ${error.message}" }
                }
                if (continuation.isActive) continuation.resume(Unit)
            }
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
