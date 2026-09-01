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

/**
 * `appConfig/plans` as stored in Firestore. Read by every signed-in client, written only by an
 * admin (firestore.rules) — pricing does not go through a Cloud Function.
 */
@Serializable
data class PlanCatalogDto(
    val enabled: Boolean = true,
    val defaultCountry: String = "US",
    val countries: Map<String, PlanPricingDto> = emptyMap()
)
