package com.rajamohan.mindmingle.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mindmingle.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Serializable
data class CountryCode(
    @SerialName("name") val name: String,
    @SerialName("dial_code") val dialCode: String,
    @SerialName("code") val code: String
) {
    /**
     * Converts 2-letter ISO 3166-1 alpha-2 country code into a regional indicator flag emoji.
     */
    val flagEmoji: String get() = getFlagEmoji(code)

    val displayString: String get() = "$flagEmoji  $name ($dialCode)"

    companion object {
        private fun getFlagEmoji(isoCode: String): String {
            if (isoCode.length != 2) return "🌐"
            val upper = isoCode.uppercase()
            val firstOffset = upper[0].code - 'A'.code
            val secondOffset = upper[1].code - 'A'.code
            if (firstOffset !in 0..25 || secondOffset !in 0..25) return "🌐"
            val firstHigh = '\uD83C'
            val firstLow = (0xDDE6 + firstOffset).toChar()
            val secondHigh = '\uD83C'
            val secondLow = (0xDDE6 + secondOffset).toChar()
            return "$firstHigh$firstLow$secondHigh$secondLow"
        }

        val defaultIndia = CountryCode(
            name = "India",
            dialCode = "+91",
            code = "IN"
        )
    }
}

object CountryCodeRepository {

    /**
     * The ISO code for a country *name*, or blank when it is not one of the 242 in the list.
     *
     * The IP lookup that fills a profile's location reports a name ("India"), and the country
     * filter compares codes — a name has spellings, translations and punctuation, a code does not.
     * Matching is case- and space-insensitive for the same reason.
     */
    suspend fun codeForName(name: String): String {
        if (name.isBlank()) return ""
        val normalised = name.trim().lowercase()
        return getCountryCodes().firstOrNull { it.name.trim().lowercase() == normalised }?.code.orEmpty()
    }

    private var cachedList: List<CountryCode>? = null
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun getCountryCodes(): List<CountryCode> {
        cachedList?.let { return it }

        return try {
            val bytes = Res.readBytes("files/CountryCodes.json")
            val jsonString = bytes.decodeToString()
            val parsed = json.decodeFromString<List<CountryCode>>(jsonString)
            cachedList = parsed
            parsed
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackList()
        }
    }

    fun fallbackList(): List<CountryCode> = listOf(
        CountryCode("India", "+91", "IN"),
        CountryCode("United States", "+1", "US"),
        CountryCode("United Kingdom", "+44", "GB"),
        CountryCode("Australia", "+61", "AU"),
        CountryCode("Germany", "+49", "DE"),
        CountryCode("Japan", "+81", "JP"),
        CountryCode("Canada", "+1", "CA")
    )
}
