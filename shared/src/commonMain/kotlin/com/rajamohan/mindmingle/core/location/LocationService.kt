package com.rajamohan.mindmingle.core.location

import io.github.aakira.napier.Napier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class LocationResult(
    val city: String,
    val region: String,
    val country: String,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val displayString: String
        get() = listOf(city, region, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

@Serializable
private data class IpApiCoResponse(
    val city: String = "",
    val region: String = "",
    @SerialName("country_name") val countryName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Present and true when ipapi.co's free-tier rate limit was hit instead of a real lookup. */
    val error: Boolean = false
)

@Serializable
private data class IpWhoIsResponse(
    val city: String = "",
    val region: String = "",
    val country: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val success: Boolean = true
)

/**
 * geojs.io's shape, oddly: lat/long come back as strings ("17.3843"), not numbers — the one thing
 * that makes this provider not a drop-in copy of the other two.
 */
@Serializable
private data class GeoJsResponse(
    val city: String = "",
    val region: String = "",
    val country: String = "",
    val latitude: String = "",
    val longitude: String = ""
)

/** Platform HTTP fetch only — raw JSON body from the given URL, or null on any failure. */
internal expect suspend fun fetchLocationJson(url: String): String?

/**
 * Approximate "current location" via IP geolocation — no OS location permission dialog needed,
 * works identically on Android/Desktop/iOS. City/region-level accuracy, plus a coarse lat/long
 * (accurate enough for a "nearby" distance filter, not turn-by-turn navigation).
 *
 * Three providers, tried in order — ipapi.co, then ipwho.is, then geojs.io. All three are free,
 * keyless, and rate-limited per IP rather than per account, so on a shared dev machine (or just a
 * busy day) any single one of them can run dry; two silently return an error body rather than an
 * HTTP error, which is what the fallback chain exists to route around. Three rather than two is
 * what makes desktop specifically resilient: it is the one platform with no OS location API at
 * all (see DeviceLocation.jvm.kt), so this is not a fallback for it — it is the only path there
 * is, and one rate-limited provider used to mean no location at all.
 *
 * This only decides *which* provider answers, once an attempt is already under way. How often an
 * attempt happens at all — and backing off cleanly when every provider is down — is
 * RefreshMyLocationUseCase's job, not this object's.
 */
object LocationService {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getCurrentLocation(): LocationResult? {
        fetchLocationJson("https://ipapi.co/json/")?.let { body ->
            parseIpApiCo(body)?.let { return it }
        }
        fetchLocationJson("https://ipwho.is/")?.let { body ->
            parseIpWhoIs(body)?.let { return it }
        }
        fetchLocationJson("https://get.geojs.io/v1/ip/geo.json")?.let { body ->
            parseGeoJs(body)?.let { return it }
        }
        return null
    }

    private fun parseIpApiCo(body: String): LocationResult? = try {
        val parsed = json.decodeFromString<IpApiCoResponse>(body)
        if (parsed.error || (parsed.city.isBlank() && parsed.countryName.isBlank())) {
            null
        } else {
            LocationResult(
                city = parsed.city,
                region = parsed.region,
                country = parsed.countryName,
                latitude = parsed.latitude,
                longitude = parsed.longitude
            )
        }
    } catch (e: Exception) {
        Napier.d(throwable = e, tag = TAG) { "ipapi.co body did not parse" }
        null
    }

    private fun parseIpWhoIs(body: String): LocationResult? = try {
        val parsed = json.decodeFromString<IpWhoIsResponse>(body)
        if (!parsed.success || (parsed.city.isBlank() && parsed.country.isBlank())) {
            null
        } else {
            LocationResult(
                city = parsed.city,
                region = parsed.region,
                country = parsed.country,
                latitude = parsed.latitude,
                longitude = parsed.longitude
            )
        }
    } catch (e: Exception) {
        Napier.d(throwable = e, tag = TAG) { "ipwho.is body did not parse" }
        null
    }

    private fun parseGeoJs(body: String): LocationResult? = try {
        val parsed = json.decodeFromString<GeoJsResponse>(body)
        if (parsed.city.isBlank() && parsed.country.isBlank()) {
            null
        } else {
            LocationResult(
                city = parsed.city,
                region = parsed.region,
                country = parsed.country,
                latitude = parsed.latitude.toDoubleOrNull(),
                longitude = parsed.longitude.toDoubleOrNull()
            )
        }
    } catch (e: Exception) {
        Napier.d(throwable = e, tag = TAG) { "geojs.io body did not parse" }
        null
    }

    private const val TAG = "LocationService"
}
