package com.rajamohan.mindmingle.core.location

/**
 * The outcome of asking the device where it is.
 *
 * Every failure is named rather than folded into a null, because the caller's response differs:
 * a refusal is permanent until the user changes their mind, a disabled GPS is fixable in a
 * heartbeat, and a timeout is worth retrying later. Today they all fall back to the IP lookup —
 * naming them is what makes it possible to say something useful about it later.
 */
sealed class DeviceLocationResult {

    data class Located(
        val latitude: Double,
        val longitude: Double,
        /** Administrative names for the fix, when the platform's geocoder could supply them. */
        val place: PlaceNames = PlaceNames()
    ) : DeviceLocationResult()

    /** Asked and refused. Asking again is allowed. */
    data object PermissionDenied : DeviceLocationResult()

    /** Refused with "don't ask again", or blocked by policy. Only Settings can undo it. */
    data object PermissionPermanentlyDenied : DeviceLocationResult()

    /** Permission is held but location services are switched off device-wide. */
    data object LocationDisabled : DeviceLocationResult()

    /** Permission and services are fine, but no fix arrived — indoors, airplane mode, timeout. */
    data object Unavailable : DeviceLocationResult()

    /** This platform has no location surface wired up. Desktop. */
    data object NotSupported : DeviceLocationResult()
}

/**
 * The device's own coordinates, from the OS rather than from an IP address.
 *
 * This exists because [LocationService] — the IP lookup — is not accurate enough for the thing the
 * deck claims. An IP resolves to the ISP's egress point: the right city and the wrong suburb on
 * Wi-Fi, and on mobile data frequently the wrong city, since carriers route whole regions through
 * one gateway. Two people in the same room on different carriers can read as hundreds of
 * kilometres apart, which makes both "2.4 km away" and the 5 km filter fiction.
 *
 * The IP lookup stays as the fallback. A refused permission should leave the deck working with a
 * worse number, which is exactly what it has today, rather than with no number at all.
 */
/**
 * What a coordinate is called, in the terms the country filter and the district filter compare.
 *
 * Filled by the platform's own reverse geocoder — Android's `Geocoder`, Apple's `CLGeocoder`. Both
 * are part of the OS and need no key, no billing account and no Google Maps dependency, which is
 * the whole reason the district filter can exist at all here.
 *
 * Every field is optional because geocoding is best-effort: it needs a network, it fails silently
 * in the middle of an ocean, and plenty of the world has no district-level answer to give. A blank
 * simply means that profile does not match a district filter, which is the honest outcome.
 */
data class PlaceNames(
    /** ISO 3166-1 alpha-2, e.g. "IN". */
    val countryCode: String = "",
    /** ADM1 — state or province, e.g. "Telangana". */
    val region: String = "",
    /** ADM2 — district or county, e.g. "Hyderabad". */
    val district: String = "",
    /** The locality itself, e.g. "Hyderabad". Shown, never filtered on. */
    val city: String = ""
) {
    val isEmpty: Boolean get() = countryCode.isBlank() && region.isBlank() && district.isBlank()

    /** "Hyderabad, Telangana" — what the profile's free-text location field shows. */
    val displayString: String
        get() = listOf(city, region).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

expect object DeviceLocation {

    /** False on desktop, where [current] always returns [DeviceLocationResult.NotSupported]. */
    val isSupported: Boolean

    /**
     * One fix, now. Never starts a subscription — a dating app needs to know roughly where someone
     * is when they open the deck, and continuous tracking would be both a battery cost and a
     * promise this app has no reason to make.
     */
    suspend fun current(): DeviceLocationResult
}
