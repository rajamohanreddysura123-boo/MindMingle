package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionDto(
    val uid: String = "",
    val provider: String = "",
    val planId: String = "",
    val status: String = "",
    val currentPeriodEnd: Long = 0L,
    val lastPaymentId: String = "",
    val lastOrderId: String = "",
    val updatedAt: Long = 0L
)

@Serializable
data class CreateOrderRequestDto(val planId: String, val countryHint: String = "")

@Serializable
data class CreateOrderResponseDto(
    val orderId: String,
    val amount: Long,
    val currency: String,
    val keyId: String,
    val planId: String,
    val country: String = "",
    val symbol: String = "",
    val decimals: Int = 2
)

@Serializable
data class VerifyPaymentRequestDto(
    val orderId: String,
    val paymentId: String,
    val signature: String
)

@Serializable
data class VerifyPaymentResponseDto(
    val planId: String,
    val currentPeriodEnd: Long,
    val status: String
)
