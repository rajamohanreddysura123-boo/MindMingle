package com.rajamohan.mindmingle.core.payments

data class CheckoutRequest(
    val keyId: String,
    val orderId: String,
    val amountMinor: Long,
    val currency: String,
    val planLabel: String,
    val userName: String,
    val userEmail: String,
    val userPhone: String
)

sealed interface CheckoutResult {
    data class Success(
        val paymentId: String,
        val orderId: String,
        val signature: String
    ) : CheckoutResult

    data object Cancelled : CheckoutResult

    data class Failed(val message: String) : CheckoutResult
}

expect object PaymentPlatform {

    val isSupported: Boolean

    suspend fun startCheckout(request: CheckoutRequest): CheckoutResult
}
