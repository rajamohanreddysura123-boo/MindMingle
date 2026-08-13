package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.core.platform.AppPlatform
import com.rajamohan.mindmingle.core.platform.getCurrentPlatform
import com.rajamohan.mindmingle.data.remote.dto.AdConfigDto

/**
 * Google AdMob settings for the Discover deck, mirrored from Firestore `appConfig/ads`
 * so frequency and unit ids can be retuned without shipping a new build. Every field
 * falls back to the values here when the doc is missing or unreadable, which is what
 * keeps the deck working offline and on a fresh project with no config doc yet.
 *
 * The defaults ship Google's official *test* ad unit ids. Serving live ads against a
 * debug build is an AdMob policy violation ("invalid traffic") and can get the account
 * disabled, so real ids belong only in the Firestore doc / release manifest.
 */
data class AdConfig(
    val enabled: Boolean = true,
    /** An ad slot is inserted after this many swiped profiles. */
    val everyNProfiles: Int = 5,
    /** Every Nth ad slot is a full-screen interstitial instead of the in-deck banner card. */
    val interstitialEveryNAdSlots: Int = 3,
    /** How long the ad card holds the deck before "Continue" unlocks. */
    val minSecondsOnAdCard: Int = 5,
    val androidBannerUnitId: String = AdUnits.ANDROID_TEST_BANNER,
    val androidInterstitialUnitId: String = AdUnits.ANDROID_TEST_INTERSTITIAL,
    val iosBannerUnitId: String = AdUnits.IOS_TEST_BANNER,
    val iosInterstitialUnitId: String = AdUnits.IOS_TEST_INTERSTITIAL
) {
    /** Guards against a bad remote value (0 or negative) turning into an ad on every swipe. */
    val swipesPerAd: Int get() = everyNProfiles.coerceAtLeast(1)

    val adSlotsPerInterstitial: Int get() = interstitialEveryNAdSlots.coerceAtLeast(1)

    val gateSeconds: Int get() = minSecondsOnAdCard.coerceIn(0, 30)
}

/**
 * Google's public test ad unit ids. They always fill, never earn, and are the only ids
 * safe to click during development.
 * https://developers.google.com/admob/android/test-ads
 */
object AdUnits {
    const val ANDROID_TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val ANDROID_TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val IOS_TEST_BANNER = "ca-app-pub-3940256099942544/2934735716"
    const val IOS_TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/4411468910"
}

/** The banner unit for whichever platform is running; empty on desktop (no AdMob SDK there). */
fun AdConfig.bannerUnitIdForPlatform(isIos: Boolean): String =
    if (isIos) iosBannerUnitId else androidBannerUnitId

fun AdConfig.interstitialUnitIdForPlatform(isIos: Boolean): String =
    if (isIos) iosInterstitialUnitId else androidInterstitialUnitId

/** Desktop has no Mobile Ads SDK at all, so the deck must never arm an ad gate there. */
fun AdConfig.isServableHere(): Boolean = enabled && getCurrentPlatform() == AppPlatform.MOBILE

fun AdConfigDto.toDomain(): AdConfig = AdConfig(
    enabled = enabled,
    everyNProfiles = everyNProfiles,
    interstitialEveryNAdSlots = interstitialEveryNAdSlots,
    minSecondsOnAdCard = minSecondsOnAdCard,
    androidBannerUnitId = androidBannerUnitId.ifBlank { AdUnits.ANDROID_TEST_BANNER },
    androidInterstitialUnitId = androidInterstitialUnitId.ifBlank { AdUnits.ANDROID_TEST_INTERSTITIAL },
    iosBannerUnitId = iosBannerUnitId.ifBlank { AdUnits.IOS_TEST_BANNER },
    iosInterstitialUnitId = iosInterstitialUnitId.ifBlank { AdUnits.IOS_TEST_INTERSTITIAL }
)
