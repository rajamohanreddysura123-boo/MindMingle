package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlanPricingDto(
    val currency: String = "",
    val symbol: String = "",
    val decimals: Int = 2,
    val dialCode: String = "",
    val monthly: Long = 0L,
    val annual: Long = 0L
)

@Serializable
data class PlanCatalogDto(
    val enabled: Boolean = true,
    val defaultCountry: String = "US",
    val countries: Map<String, PlanPricingDto> = emptyMap()
)

@Serializable
data class PlanPricingRequestDto(val countryHint: String = "")

@Serializable
data class PlanPricingResponseDto(
    val country: String = "",
    val enabled: Boolean = true,
    val defaultCountry: String = "US",
    val pricing: PlanPricingDto? = null,
    val countries: Map<String, PlanPricingDto> = emptyMap()
)

@Serializable
data class SavePlanPricingRequestDto(
    val enabled: Boolean,
    val defaultCountry: String,
    val countries: Map<String, PlanPricingDto>
)

@Serializable
data class SavePlanPricingResponseDto(val saved: Int = 0)
