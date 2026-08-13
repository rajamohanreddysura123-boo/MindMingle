package com.rajamohan.mindmingle.core.payments

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual object PaymentPlatform {

    actual val isSupported: Boolean
        get() = IosPaymentHost.isBridgeInstalled

    actual suspend fun startCheckout(request: CheckoutRequest): CheckoutResult {
        if (!IosPaymentHost.isBridgeInstalled) {
            return CheckoutResult.Failed("Payments are not available yet")
        }

        return suspendCancellableCoroutine { continuation ->
            IosPaymentHost.present(request.toOptions()) { result ->
                if (continuation.isActive) continuation.resume(result.toCheckoutResult())
            }
        }
    }
}

private fun CheckoutRequest.toOptions(): Map<String, String> = mapOf(
    "keyId" to keyId,
    "orderId" to orderId,
    "amount" to amountMinor.toString(),
    "currency" to currency,
    "description" to planLabel,
    "name" to userName,
    "email" to userEmail,
    "contact" to userPhone
)

private fun Map<String, String>.toCheckoutResult(): CheckoutResult = when (this["status"]) {
    "success" -> {
        val paymentId = this["paymentId"].orEmpty()
        val orderId = this["orderId"].orEmpty()
        val signature = this["signature"].orEmpty()
        if (paymentId.isBlank() || orderId.isBlank() || signature.isBlank()) {
            CheckoutResult.Failed("Payment went through but could not be verified")
        } else {
            CheckoutResult.Success(paymentId = paymentId, orderId = orderId, signature = signature)
        }
    }
    "cancelled" -> CheckoutResult.Cancelled
    else -> CheckoutResult.Failed(this["message"].orEmpty().ifBlank { "Payment failed" })
}
