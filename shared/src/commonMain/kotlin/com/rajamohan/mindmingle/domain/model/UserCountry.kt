package com.rajamohan.mindmingle.domain.model

/**
 * Which country a user belongs to.
 *
 * Profiles carry no country field — there was never a form that asked for one. Two things do
 * imply it, so this derives rather than migrates:
 *
 *   The phone number. A stored number is "+91 9876543210", and the longest matching dial code
 *   wins so +971 is never mistaken for +97 and +1 does not swallow the countries that share it.
 *   This is the same rule the billing country uses server-side, so an admin reading a profile
 *   and an invoice sees one answer, not two.
 *
 *   The location string, written by IP geolocation as "Bengaluru, Karnataka, India". Its last
 *   segment is a country name, which is only useful because we have the name-to-code list
 *   already; it is the fallback for email sign-ins with no phone on file.
 *
 * Neither is guaranteed, so the result is nullable and the UI has to cope with "unknown".
 */
object UserCountryResolver {

    /**
     * ISO 3166-1 alpha-2 for [user], or null when neither the phone number nor the location
     * says anything usable.
     */
    fun resolve(user: User, countries: List<CountryCode>): String? {
        if (countries.isEmpty()) return null

        fromPhone(user.phoneNumber, countries)?.let { return it }
        return fromLocation(user.location, countries)
    }

    private fun fromPhone(phoneNumber: String, countries: List<CountryCode>): String? {
        val digits = phoneNumber.filter { it.isDigit() || it == '+' }
        if (!digits.startsWith("+")) return null

        // Longest dial code first: +1 matches both US and Canada, and a two-character prefix
        // must never beat a three-character one that also fits.
        return countries
            .filter { it.dialCode.length > 1 && digits.startsWith(it.dialCode.filter { c -> c.isDigit() || c == '+' }) }
            .maxByOrNull { it.dialCode.length }
            ?.code
    }

    private fun fromLocation(location: String, countries: List<CountryCode>): String? {
        val tail = location.substringAfterLast(',').trim()
        if (tail.isBlank()) return null
        return countries.firstOrNull { it.name.equals(tail, ignoreCase = true) }?.code
    }

    /** "🇮🇳 IN" for a row, or a neutral marker when the country could not be worked out. */
    fun label(code: String?, countries: List<CountryCode>): String {
        if (code.isNullOrBlank()) return "🌐 Unknown"
        val country = countries.firstOrNull { it.code == code } ?: return "🌐 $code"
        return "${country.flagEmoji} $code"
    }
}
