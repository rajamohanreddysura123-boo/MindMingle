package com.rajamohan.mindmingle.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mindmingle.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * The region and district names for one country, as the filter and profile setup offer them.
 *
 * ## Where this comes from
 * `tools/extract_admin_areas.py` pulls them from geoBoundaries and throws the geometry away. That
 * matters: the ADM2 dataset is 550 MB of polygons worldwide, and about 9 KB of names per country.
 * Nothing here draws maps or does point-in-polygon — a profile's district is decided once, on the
 * device, by the OS geocoder — so names are all that ever needs to exist.
 *
 * ## Where it lives
 * `composeResources/files/admin_areas/{ISO}.json`, beside the country list that is already bundled
 * for the phone field. Read on demand for the one country in play, cached after the first read.
 *
 * It was briefly a Firestore document per country. Bundling is better here for the same reason the
 * country list is bundled: it is static reference data that never changes between releases, it
 * works offline, and it needs no seeding step before a country's districts appear.
 *
 * The lists are flat and independent rather than districts nested under regions: the per-country
 * geoBoundaries files carry no parent link, so any nesting would be invented. It also matches how
 * people search — nobody picks "Telangana" in order to find "Hyderabad", they type "Hyderabad".
 */
@Serializable
data class AdminAreas(
    val countryCode: String = "",
    val regions: List<String> = emptyList(),
    val districts: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = regions.isEmpty() && districts.isEmpty()
}

object AdminAreasRepository {
    private val cache = mutableMapOf<String, AdminAreas>()
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * The names for [countryCode], or empty for a country nobody has extracted yet — which is a
     * normal state, not an error: the picker simply offers nothing and every other filter works.
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun forCountry(countryCode: String): AdminAreas {
        val code = countryCode.trim().uppercase()
        if (code.isBlank()) return AdminAreas()
        cache[code]?.let { return it }

        return try {
            val bytes = Res.readBytes("files/admin_areas/$code.json")
            json.decodeFromString<AdminAreas>(bytes.decodeToString()).also { cache[code] = it }
        } catch (e: Exception) {
            // Missing file for that country; cached as empty so a search does not retry per keystroke.
            AdminAreas().also { cache[code] = it }
        }
    }
}
