package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val uid: String = "",
    val phoneNumber: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val photoUrls: List<String> = emptyList(),
    val bio: String = "",
    val occupation: String = "",
    val interests: List<String> = emptyList(),
    val experienceLevel: String = "",
    val lookingFor: String = "",
    val githubUrl: String = "",
    val portfolioLinks: List<String> = emptyList(),
    val age: Int = 0,
    val location: String = "",
    /**
     * ISO 3166-1 alpha-2, from the same lookup that fills [location] — "IN", "US", "GB".
     *
     * The code rather than the name because a name has spellings and translations and a code does
     * not, and the country filter compares exactly. Blank on profiles written before this existed;
     * `filterDiscoverProfiles` falls back to reading the tail of [location] for those, so no
     * backfill is needed.
     */
    val countryCode: String = "",
    /** ADM1 — state or province, from the device geocoder. Blank when it could not say. */
    val region: String = "",
    /** ADM2 — district or county. What the district filter compares against. */
    val district: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    /**
     * When [latitude]/[longitude] were last written, so a stale fix can be told from a fresh one.
     * 0 means "never, or written before this field existed", which reads as stale and triggers a
     * refresh on the next Home open.
     */
    val locationUpdatedAt: Long = 0L,
    /**
     * How far this profile is from the caller, in kilometres — attached by
     * `filterDiscoverProfiles`, which strips [latitude]/[longitude] in the same breath. It is never
     * stored on the user document; it only exists on a discovery response, and it is null on a
     * profile read any other way.
     */
    val distanceKm: Double? = null,
    /**
     * "user" or "admin". Owner-immutable in firestore.rules — a client may only ever create its
     * own doc as "user" and can never change the field, so an admin is promoted from the Firebase
     * Console alone. This is what `isAdmin()` in the rules reads.
     */
    val userType: String = "user",
    /**
     * Public mirror of MindMingle+ state, used for the badge next to a name. subscriptions/{uid}
     * is readable by its owner alone, so a viewer can never check someone else's plan directly;
     * each client copies its own state here (see HomeViewModel.mirrorPremiumFlag). Cosmetic only —
     * every paid feature still gates on the real subscription doc, server-side.
     */
    val isPremium: Boolean = false,
    val isProfileComplete: Boolean = false,
    val isDisabled: Boolean = false,
    val isDeactivated: Boolean = false,
    val deactivatedAt: Long = 0L,
    val reactivateAt: Long = 0L,
    val isDeletionRequested: Boolean = false,
    val deletionRequestedAt: Long = 0L,
    val createdAt: Long = 0L,
    val details: Map<String, String> = emptyMap(),
    val selections: Map<String, List<String>> = emptyMap()
)