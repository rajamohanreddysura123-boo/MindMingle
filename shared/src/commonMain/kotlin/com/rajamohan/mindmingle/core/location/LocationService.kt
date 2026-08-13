package com.rajamohan.mindmingle.core.location

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

    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null
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

/** Platform HTTP fetch only — raw JSON body from the given URL, or null on any failure. */
internal expect suspend fun fetchLocationJson(url: String): String?

/**
 * Approximate "current location" via IP geolocation — no OS location permission dialog needed,
 * works identically on Android/Desktop/iOS. City/region-level accuracy, plus a coarse lat/long
 * (accurate enough for a "nearby" distance filter, not turn-by-turn navigation).
 *
 * Tries ipapi.co first, falls back to ipwho.is — both are free, keyless, and rate-limited per IP,
 * so a single provider silently returning an empty/error body (no exception, just blank fields)
 * was the actual cause of "Detect Location" silently doing nothing before this fallback existed.
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
        e.printStackTrace()
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
        e.printStackTrace()
        null
    }
}
