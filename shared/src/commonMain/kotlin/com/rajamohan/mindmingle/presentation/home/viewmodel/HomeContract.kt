package com.rajamohan.mindmingle.presentation.home.viewmodel

import com.rajamohan.mindmingle.domain.model.AdConfig
import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.DiscoverFilterDefaults
import com.rajamohan.mindmingle.domain.model.User
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal sealed class HomeEvent {
    data class LoadProfiles(val uid: String) : HomeEvent()
    data class Connect(val fromUid: String, val toUid: String) : HomeEvent()
    data object Pass : HomeEvent()
    data object DismissMatch : HomeEvent()
    data class ApplyFilters(val filters: DiscoverFilters) : HomeEvent()
    data object ResetFilters : HomeEvent()

    /** User tapped "Continue" on the in-deck ad card once its countdown finished. */
    data object DismissAd : HomeEvent()
}

internal enum class AdSlotKind {
    /** A 300x250 banner rendered in the profile card's own slot, blocking the deck. */
    BANNER,

    /** A full-screen AdMob interstitial; the deck shows a blocking placeholder behind it. */
    INTERSTITIAL
}

/**
 * An armed ad slot. While this is non-null the deck shows the ad in place of a profile and
 * Pass/Connect stay disabled — that is the "can't swipe while the ad plays" rule.
 * [secondsLeft] counts a banner slot down; [canContinue] is what unlocks the deck again.
 */
internal data class AdGate(
    val kind: AdSlotKind,
    val unitId: String,
    val secondsLeft: Int = 0,
    val failed: Boolean = false
) {
    /** Interstitials unlock when the SDK hands control back; a failed load must never trap the user. */
    val canContinue: Boolean
        get() = failed || (kind == AdSlotKind.BANNER && secondsLeft <= 0)
}

internal data class DiscoverFilters(
    val minAge: Int = DiscoverFilterDefaults.MIN_AGE,
    val maxAge: Int = DiscoverFilterDefaults.MAX_AGE,
    val interests: Set<String> = emptySet(),
    val lookingFor: Set<String> = emptySet(),
    val occupations: Set<String> = emptySet(),
    /** Null means "any distance" — the slider sits at its maximum and the filter is off. */
    val maxDistanceKm: Int? = null,
    val genders: Set<String> = emptySet(),
    val experienceLevels: Set<String> = emptySet(),
    val languages: Set<String> = emptySet(),
    /** Lifestyle/intent selections keyed by profile_options.json field key; empty values count as unset. */
    val detailFilters: Map<String, Set<String>> = emptyMap()
) {
    private val isAgeDefault: Boolean
        get() = minAge == DiscoverFilterDefaults.MIN_AGE && maxAge == DiscoverFilterDefaults.MAX_AGE

    /** Only the keys the user actually narrowed — a key mapped to an empty set is not a filter. */
    private val activeDetailFilters: Map<String, Set<String>>
        get() = detailFilters.filterValues { it.isNotEmpty() }

    val isDefault: Boolean
        get() = isAgeDefault && maxDistanceKm == null &&
            interests.isEmpty() && lookingFor.isEmpty() && occupations.isEmpty() &&
            genders.isEmpty() && experienceLevels.isEmpty() && languages.isEmpty() &&
            activeDetailFilters.isEmpty()

    /** Counts filter *groups* in use, not individual selections — it drives the badge on the filter button. */
    val activeCount: Int
        get() = (if (isAgeDefault) 0 else 1) +
            (if (maxDistanceKm != null) 1 else 0) +
            (if (interests.isNotEmpty()) 1 else 0) +
            (if (lookingFor.isNotEmpty()) 1 else 0) +
            (if (occupations.isNotEmpty()) 1 else 0) +
            (if (genders.isNotEmpty()) 1 else 0) +
            (if (experienceLevels.isNotEmpty()) 1 else 0) +
            (if (languages.isNotEmpty()) 1 else 0) +
            activeDetailFilters.size

    /** Sent to the `filterDiscoverProfiles` cloud function — actual matching happens server-side. */
    fun toCriteria(): DiscoverFilterCriteria = DiscoverFilterCriteria(
        minAge = minAge,
        maxAge = maxAge,
        interests = interests,
        lookingFor = lookingFor,
        occupations = occupations,
        maxDistanceKm = maxDistanceKm,
        genders = genders,
        experienceLevels = experienceLevels,
        languages = languages,
        detailFilters = activeDetailFilters
    )

    /** Drops the MindMingle+ groups — used when a subscription lapses while filters are still set. */
    fun withoutPremiumFilters(): DiscoverFilters = copy(
        maxDistanceKm = null,
        languages = emptySet(),
        detailFilters = emptyMap()
    )
}

/** Inverse of [DiscoverFilters.toCriteria] — rebuilds the sheet's state from a stored or seeded set. */
internal fun DiscoverFilterCriteria.toFilters(): DiscoverFilters = DiscoverFilters(
    minAge = minAge,
    maxAge = maxAge,
    interests = interests,
    lookingFor = lookingFor,
    occupations = occupations,
    maxDistanceKm = maxDistanceKm,
    genders = genders,
    experienceLevels = experienceLevels,
    languages = languages,
    detailFilters = detailFilters
)

/** Great-circle distance between two lat/long points, in kilometers. */
private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLng = (lng2 - lng1) * PI / 180.0
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusKm * c
}

internal data class HomeUiState(
    val isLoading: Boolean = false,
    val allProfiles: List<User> = emptyList(),
    val filters: DiscoverFilters = DiscoverFilters(),
    val myLatitude: Double? = null,
    val myLongitude: Double? = null,
    val currentIndex: Int = 0,
    val matchedUser: User? = null,
    val adConfig: AdConfig = AdConfig(),
    /** True while an MindMingle+ plan is active — the deck serves no ads at all then. */
    val isPremium: Boolean = false,
    val adGate: AdGate? = null,
    val error: String = ""
) {
    // allProfiles already reflects the server-side filtered result of `filters` (see HomeViewModel.fetchDiscoverProfiles).
    val profiles: List<User> get() = allProfiles
    val currentProfile: User? get() = profiles.getOrNull(currentIndex)
    val hasMoreProfiles: Boolean get() = currentIndex < profiles.size

    /** Swipe actions stay locked for as long as an ad occupies the deck. */
    val isSwipeLocked: Boolean get() = adGate != null
    /** True once we know the signed-in user's own coordinates — distance filter only makes sense then. */
    val hasMyLocation: Boolean get() = myLatitude != null && myLongitude != null

    fun distanceToKm(user: User): Double? {
        val lat = myLatitude ?: return null
        val lng = myLongitude ?: return null
        val userLat = user.latitude ?: return null
        val userLng = user.longitude ?: return null
        return haversineKm(lat, lng, userLat, userLng)
    }
}
