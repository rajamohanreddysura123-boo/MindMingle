package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.data.remote.dto.PlanCatalogDto
import com.rajamohan.mindmingle.data.remote.dto.PlanPricingDto

data class CountryPricing(
    val countryCode: String = "",
    val currency: String = "",
    val symbol: String = "",
    val decimals: Int = 2,
    val dialCode: String = "",
    val monthly: Long = 0L,
    val annual: Long = 0L
) {
    fun amountFor(plan: PremiumPlan): Long = when (plan) {
        PremiumPlan.MONTHLY -> monthly
        PremiumPlan.ANNUAL -> annual
    }

    fun priceLabelFor(plan: PremiumPlan): String = formatMinorAmount(amountFor(plan), decimals, symbol)

    val annualSavingsPercent: Int
        get() {
            val fullYear = monthly * 12
            if (fullYear <= 0L || annual <= 0L || annual >= fullYear) return 0
            return (((fullYear - annual) * 100) / fullYear).toInt()
        }

    val isValid: Boolean get() = currency.length == 3 && monthly > 0L && annual > 0L
}

data class PlanCatalog(
    val enabled: Boolean = true,
    val defaultCountry: String = "US",
    val countries: Map<String, CountryPricing> = emptyMap(),
    val resolvedCountry: String = ""
) {
    /**
     * A country row only bills if it has actually been priced — every country ships with a
     * currency but only the tuned markets carry amounts, so an unpriced market falls back to
     * [defaultCountry] instead of quoting zero.
     */
    val pricing: CountryPricing?
        get() = countries[resolvedCountry]?.takeIf { it.isValid }
            ?: countries[defaultCountry]?.takeIf { it.isValid }

    val sortedCountries: List<CountryPricing>
        get() = countries.values.sortedBy { it.countryCode }
}

/**
 * Renders a smallest-unit amount the way the market writes it: 9900 paise as ₹99,
 * 299 cents as $2.99, 399 yen as ¥399 (JPY carries no decimals at all).
 */
fun formatMinorAmount(amount: Long, decimals: Int, symbol: String): String {
    if (decimals <= 0) return "$symbol$amount"

    var divisor = 1L
    repeat(decimals) { divisor *= 10 }

    val major = amount / divisor
    val minor = amount % divisor
    if (minor == 0L) return "$symbol$major"

    val padded = minor.toString().padStart(decimals, '0').trimEnd('0')
    return "$symbol$major.$padded"
}

fun PlanPricingDto.toDomain(countryCode: String): CountryPricing = CountryPricing(
    countryCode = countryCode,
    currency = currency,
    symbol = symbol.ifBlank { currency },
    decimals = decimals,
    dialCode = dialCode,
    monthly = monthly,
    annual = annual
)

fun CountryPricing.toDto(): PlanPricingDto = PlanPricingDto(
    currency = currency,
    symbol = symbol,
    decimals = decimals,
    dialCode = dialCode,
    monthly = monthly,
    annual = annual
)

fun PlanCatalogDto.toDomain(resolvedCountry: String = ""): PlanCatalog = PlanCatalog(
    enabled = enabled,
    defaultCountry = defaultCountry,
    countries = countries.mapValues { (code, dto) -> dto.toDomain(code) },
    resolvedCountry = resolvedCountry.ifBlank { defaultCountry }
)
