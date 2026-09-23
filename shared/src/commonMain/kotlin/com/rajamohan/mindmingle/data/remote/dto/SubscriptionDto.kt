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

/**
 * A Razorpay-hosted payment page, for platforms with no checkout SDK — desktop.
 *
 * [url] is short by design: it is rendered as a QR to be scanned with a phone, and a long URL
 * makes a denser code that is harder to scan across a desk.
 */
@Serializable
data class CreatePaymentLinkResponseDto(
    val linkId: String = "",
    val url: String = "",
    val amount: Long = 0L,
    val currency: String = "",
    val planId: String = "",
    val country: String = "",
    val symbol: String = "",
    val decimals: Int = 2,
    val expiresAt: Long = 0L
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
