package com.rajamohan.mindmingle.domain.model

import kotlinx.serialization.Serializable

/**
 * Discover-feed filter criteria sent to the `filterDiscoverProfiles` cloud function; matching happens server-side.
 * Serializable so the last-applied set survives a restart (MindMingleDatabaseProvider).
 */
@Serializable
data class DiscoverFilterCriteria(
    val minAge: Int = DiscoverFilterDefaults.MIN_AGE,
    val maxAge: Int = DiscoverFilterDefaults.MAX_AGE,
    val interests: Set<String> = emptySet(),
    val lookingFor: Set<String> = emptySet(),
    val occupations: Set<String> = emptySet(),
    /** Null means "any distance" — also what a user with no known coordinates always sends. */
    val maxDistanceKm: Int? = null,
    /** Matched against the `gender` answer in the candidate's `details` map. */
    val genders: Set<String> = emptySet(),
    val experienceLevels: Set<String> = emptySet(),
    /** Matched against the `languages` answer in the candidate's `selections` map. */
    val languages: Set<String> = emptySet(),
    /** ISO 3166-1 alpha-2 codes; empty means any country. MindMingle+ only, enforced server-side. */
    val countries: Set<String> = emptySet(),
    /** ADM2 names, matched against a profile's `district`. MindMingle+ only. */
    val districts: Set<String> = emptySet(),
    /**
     * Lifestyle/intent filters keyed by their profile_options.json field key — one entry per
     * field the user narrowed. Kept as a map so a new filterable field costs a key in
     * [DiscoverFilterOptions.detailFilterKeys] and nothing else.
     */
    val detailFilters: Map<String, Set<String>> = emptyMap()
)

/** Shared by the filter sheet, the criteria sent server-side and the "is this filter active" checks. */
object DiscoverFilterDefaults {
    const val MIN_AGE = 18
    const val MAX_AGE = 70

    /** Widest distance the sheet's slider offers; at this value the filter is off. */
    const val MAX_DISTANCE_KM = 500
}

/**
 * Strips the MindMingle+ filter groups — distance, languages and the lifestyle/values set.
 * Age, interests, "looking for", occupation, gender and experience level stay free: gender
 * decides whether the deck is usable at all, and experience level is the point of the product.
 *
 * Applied client-side so the deck matches what a free user can see, and again server-side in
 * `filterDiscoverProfiles`, because a modified client can send whatever it likes.
 */
fun DiscoverFilterCriteria.withoutPremiumFilters(): DiscoverFilterCriteria = copy(
    maxDistanceKm = null,
    languages = emptySet(),
    countries = emptySet(),
    districts = emptySet(),
    detailFilters = emptyMap()
)

/** The profile_options.json fields the Discover sheet offers as lifestyle/intent filters, in display order. */
object DiscoverFilterOptions {
    val detailFilterKeys = listOf(
        "relationshipIntention",
        "drinking",
        "smoking",
        "exercise",
        "diet",
        "children",
        "education",
        "personalityType"
    )
}

/**
 * Turns a user's own Dating Preferences answers (profile_options.json) into the Discover
 * filter they start with, so `preferredMinAge` / `preferredMaxAge` / `preferredDistanceKm` /
 * `interestedIn` actually steer the deck instead of sitting unused in Firestore.
 *
 * Only applies until the user applies a filter of their own — after that the stored set wins.
 */
object DiscoverFilterSeed {

    /** "Interested in" answers name audiences; the `gender` field names identities. */
    private val interestedInToGenders = mapOf(
        "Men" to setOf("Man"),
        "Women" to setOf("Woman"),
        "Non-binary people" to setOf("Non-binary"),
        // "Everyone" deliberately maps to no filter at all rather than to every option —
        // that keeps profiles whose gender answer is blank or "Prefer not to say" in the deck.
        "Everyone" to emptySet()
    )

    fun from(user: User): DiscoverFilterCriteria {
        val preferredMin = user.details["preferredMinAge"]?.toIntOrNull()
        val preferredMax = user.details["preferredMaxAge"]?.toIntOrNull()
        val minAge = preferredMin?.coerceIn(DiscoverFilterDefaults.MIN_AGE, DiscoverFilterDefaults.MAX_AGE)
            ?: DiscoverFilterDefaults.MIN_AGE
        val maxAge = preferredMax?.coerceIn(DiscoverFilterDefaults.MIN_AGE, DiscoverFilterDefaults.MAX_AGE)
            ?: DiscoverFilterDefaults.MAX_AGE

        return DiscoverFilterCriteria(
            // A reversed pair (max below min) would match nobody — fall back to the full span.
            minAge = if (minAge <= maxAge) minAge else DiscoverFilterDefaults.MIN_AGE,
            maxAge = if (minAge <= maxAge) maxAge else DiscoverFilterDefaults.MAX_AGE,
            maxDistanceKm = user.details["preferredDistanceKm"]?.toIntOrNull()
                ?.takeIf { it > 0 && it < DiscoverFilterDefaults.MAX_DISTANCE_KM },
            genders = interestedInToGenders[user.details["interestedIn"]].orEmpty()
        )
    }
}

/**
 * One page of the Discover feed. [cursor] resumes the server-side scan on the next call; blank
 * means the scan reached the end of the collection, so the next request starts over from a fresh
 * random position.
 */
data class DiscoverPage(
    val profiles: List<User> = emptyList(),
    val cursor: String = ""
)
