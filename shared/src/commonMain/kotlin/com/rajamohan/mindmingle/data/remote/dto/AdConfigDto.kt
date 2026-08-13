package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Firestore `appConfig/ads` — world-readable, admin-writable (see firestore.rules).
 * Every field defaults, so a partially filled doc is still valid.
 */
@Serializable
data class AdConfigDto(
    val enabled: Boolean = true,
    val everyNProfiles: Int = 5,
    val interstitialEveryNAdSlots: Int = 3,
    val minSecondsOnAdCard: Int = 5,
    val androidBannerUnitId: String = "",
    val androidInterstitialUnitId: String = "",
    val iosBannerUnitId: String = "",
    val iosInterstitialUnitId: String = ""
)
