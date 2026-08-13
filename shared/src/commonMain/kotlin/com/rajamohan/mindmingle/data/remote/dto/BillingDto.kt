package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaymentRecordDto(
    val paymentId: String = "",
    val orderId: String = "",
    val planId: String = "",
    val amount: Long = 0L,
    val currency: String = "",
    val country: String = "",
    val source: String = "",
    val createdAt: Long = 0L
)

@Serializable
data class BillingHistoryRequestDto(val uid: String = "")

@Serializable
data class BillingHistoryResponseDto(
    val uid: String = "",
    val planId: String = "",
    val status: String = "",
    val currentPeriodEnd: Long = 0L,
    val billingCountry: String = "",
    val billingCurrency: String = "",
    val payments: List<PaymentRecordDto> = emptyList()
)

@Serializable
data class PaymentDetailsRequestDto(val paymentId: String)

@Serializable
data class PaymentDetailsResponseDto(
    val paymentId: String = "",
    val orderId: String = "",
    val planId: String = "",
    val country: String = "",
    val status: String = "",
    val amount: Long = 0L,
    val currency: String = "",
    val method: String = "",
    val email: String = "",
    val contact: String = "",
    val createdAt: Long = 0L,
    val description: String = "",
    val grantedNow: Boolean = false
)

@Serializable
data class AdminSetSubscriptionRequestDto(
    val uid: String,
    val planId: String,
    val days: Int
)

@Serializable
data class AdminCancelSubscriptionRequestDto(
    val uid: String,
    val immediate: Boolean
)

@Serializable
data class AdminSubscriptionResponseDto(
    val planId: String = "",
    val status: String = "",
    val currentPeriodEnd: Long = 0L
)

@Serializable
data class AdminDeleteUserRequestDto(
    val uid: String,
    val ban: Boolean,
    val reason: String = ""
)

@Serializable
data class DeleteAccountResponseDto(
    val deleted: Boolean = false,
    val banned: Boolean = false
)
