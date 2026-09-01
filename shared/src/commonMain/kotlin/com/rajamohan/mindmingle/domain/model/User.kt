package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.data.remote.dto.UserDto

/**
 * What a signed-in account is allowed to be. Stored on the user doc as a plain string so an
 * unknown value from a newer build degrades to [USER] instead of failing to parse.
 */
enum class UserType(val raw: String) {
    USER("user"),
    ADMIN("admin");

    companion object {
        fun fromRaw(raw: String): UserType =
            entries.firstOrNull { it.raw.equals(raw.trim(), ignoreCase = true) } ?: USER
    }
}

data class User(
    val uid: String,
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
    /** ISO 3166-1 alpha-2 for the country in [location]; blank on older profiles. */
    val countryCode: String = "",
    /** ADM1 — state or province, from the device geocoder. Blank when it could not say. */
    val region: String = "",
    /** ADM2 — district or county. What the district filter compares against. */
    val district: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** When the coordinates were last written; 0 means never, which counts as stale. */
    val locationUpdatedAt: Long = 0L,
    /** Distance from the signed-in user, measured server-side. Only ever set on a deck profile. */
    val distanceKm: Double? = null,
    val userType: UserType = UserType.USER,
    /** True while this person holds MindMingle+. Drives the badge beside their name. */
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
) {
    val isAdmin: Boolean get() = userType == UserType.ADMIN

    /**
     * Every photo worth showing, in profile order. [avatarUrl] stands in for accounts that
     * predate the photo grid and never uploaded to it; an empty list means the gradient
     * placeholder, which is a legitimate look and not an error.
     */
    val displayPhotoUrls: List<String>
        get() = photoUrls.filter { it.isNotBlank() }
            .ifEmpty { listOfNotNull(avatarUrl.takeIf { it.isNotBlank() }) }

    /** True while a deactivation window is still running at [nowMillis]. */
    fun isDeactivatedAt(nowMillis: Long): Boolean = isDeactivated && nowMillis < reactivateAt

    /** True once a deactivation window has run out — the next sign-in clears the flags. */
    fun isDeactivationExpiredAt(nowMillis: Long): Boolean = isDeactivated && nowMillis >= reactivateAt
}

fun UserDto.toDomain(): User = User(
    uid = uid,
    phoneNumber = phoneNumber,
    name = name,
    email = email,
    avatarUrl = avatarUrl,
    photoUrls = photoUrls,
    bio = bio,
    occupation = occupation,
    interests = interests,
    experienceLevel = experienceLevel,
    lookingFor = lookingFor,
    githubUrl = githubUrl,
    portfolioLinks = portfolioLinks,
    age = age,
    location = location,
    countryCode = countryCode,
    region = region,
    district = district,
    latitude = latitude,
    longitude = longitude,
    locationUpdatedAt = locationUpdatedAt,
    distanceKm = distanceKm,
    userType = UserType.fromRaw(userType),
    isPremium = isPremium,
    isProfileComplete = isProfileComplete,
    isDisabled = isDisabled,
    isDeactivated = isDeactivated,
    deactivatedAt = deactivatedAt,
    reactivateAt = reactivateAt,
    isDeletionRequested = isDeletionRequested,
    deletionRequestedAt = deletionRequestedAt,
    createdAt = createdAt,
    details = details,
    selections = selections
)

fun User.toDto(createdAt: Long = this.createdAt): UserDto = UserDto(
    uid = uid,
    phoneNumber = phoneNumber,
    name = name,
    email = email,
    avatarUrl = avatarUrl,
    photoUrls = photoUrls,
    bio = bio,
    occupation = occupation,
    interests = interests,
    experienceLevel = experienceLevel,
    lookingFor = lookingFor,
    githubUrl = githubUrl,
    portfolioLinks = portfolioLinks,
    age = age,
    location = location,
    countryCode = countryCode,
    region = region,
    district = district,
    latitude = latitude,
    longitude = longitude,
    locationUpdatedAt = locationUpdatedAt,
    // Written back as-is: rules reject any update that changes it, so a client round-trip of an
    // admin's own profile must carry the same value it read.
    userType = userType.raw,
    isPremium = isPremium,
    isProfileComplete = isProfileComplete,
    isDisabled = isDisabled,
    isDeactivated = isDeactivated,
    deactivatedAt = deactivatedAt,
    reactivateAt = reactivateAt,
    isDeletionRequested = isDeletionRequested,
    deletionRequestedAt = deletionRequestedAt,
    createdAt = createdAt,
    details = details,
    selections = selections
)
