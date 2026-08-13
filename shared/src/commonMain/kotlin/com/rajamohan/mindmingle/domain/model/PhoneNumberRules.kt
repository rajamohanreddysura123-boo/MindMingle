package com.rajamohan.mindmingle.domain.model

/** National significant number length per ISO 3166-1 alpha-2 country code. Unlisted countries fall back to [default]. */
object PhoneNumberRules {
    private val digitLengths: Map<String, IntRange> = mapOf(
        "IN" to 10..10,
        "US" to 10..10,
        "CA" to 10..10,
        "GB" to 10..10,
        "AU" to 9..9,
        "DE" to 10..11,
        "FR" to 9..9,
        "JP" to 10..10,
        "CN" to 11..11,
        "BR" to 10..11,
        "RU" to 10..10,
        "ZA" to 9..9,
        "SG" to 8..8,
        "AE" to 9..9,
        "PK" to 10..10,
        "BD" to 10..10,
        "NG" to 10..10,
        "IT" to 9..10,
        "ES" to 9..9,
        "MX" to 10..10,
        "ID" to 9..12,
        "PH" to 10..10,
        "KR" to 9..10,
        "NL" to 9..9,
        "SE" to 7..9,
        "CH" to 9..9,
        "NZ" to 8..9,
        "IE" to 9..9,
        "SA" to 9..9,
        "TR" to 10..10,
        "TH" to 9..9,
        "VN" to 9..10,
        "MY" to 9..10,
        "EG" to 10..10,
        "AR" to 10..11,
        "KE" to 9..9,
        "PL" to 9..9,
        "UA" to 9..9,
        "IL" to 9..9,
        "HK" to 8..8,
        "TW" to 9..9
    )

    private val default = 6..14

    fun expectedLength(isoCode: String): IntRange = digitLengths[isoCode.uppercase()] ?: default

    fun isValidLength(isoCode: String, digitCount: Int): Boolean = digitCount in expectedLength(isoCode)

    /** Splits a stored "+91 9876543210" style string into its country and local digits, defaulting to India. */
    fun parse(rawPhoneNumber: String, countries: List<CountryCode>): Pair<CountryCode, String> {
        val trimmed = rawPhoneNumber.trim()
        if (trimmed.startsWith("+")) {
            val match = countries.filter { trimmed.startsWith(it.dialCode) }.maxByOrNull { it.dialCode.length }
            if (match != null) {
                return match to trimmed.removePrefix(match.dialCode).filter { it.isDigit() }
            }
        }
        return CountryCode.defaultIndia to trimmed.filter { it.isDigit() }
    }
}
