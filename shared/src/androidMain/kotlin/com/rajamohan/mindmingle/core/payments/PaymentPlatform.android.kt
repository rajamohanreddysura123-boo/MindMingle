package com.rajamohan.mindmingle.core.payments

import android.app.Activity
import com.rajamohan.mindmingle.core.AppContext
import com.razorpay.Checkout
import io.github.aakira.napier.Napier
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume

private const val TAG = "PaymentPlatform"

actual object PaymentPlatform {

    actual val isSupported: Boolean = true

    actual suspend fun startCheckout(request: CheckoutRequest): CheckoutResult {
        val activity = AppContext.get() as? Activity
            ?: return CheckoutResult.Failed("Checkout needs an active screen")

        return suspendCancellableCoroutine { continuation ->
            RazorpayBridge.expectResult { result ->
                if (continuation.isActive) continuation.resume(result)
            }

            continuation.invokeOnCancellation { RazorpayBridge.cancelExpectation() }

            try {
                Checkout.preload(activity.applicationContext)
                Checkout().apply {
                    setKeyID(request.keyId)
                    open(activity, request.toCheckoutOptions())
                }
            } catch (e: Exception) {
                Napier.w(throwable = e, tag = TAG) { "could not open Razorpay checkout" }
                RazorpayBridge.cancelExpectation()
                if (continuation.isActive) {
                    continuation.resume(CheckoutResult.Failed(e.message ?: "Could not open checkout"))
                }
            }
        }
    }
}

private fun CheckoutRequest.toCheckoutOptions(): JSONObject = JSONObject().apply {
    put("name", "MindMingle")
    put("description", planLabel)
    put("order_id", orderId)
    put("currency", currency)
    put("amount", amountMinor)
    put("send_sms_hash", true)
    put("retry", JSONObject().put("enabled", true).put("max_count", 1))
    put(
        "prefill",
        JSONObject().apply {
            if (userName.isNotBlank()) put("name", userName)
            if (userEmail.isNotBlank()) put("email", userEmail)
            if (userPhone.isNotBlank()) put("contact", userPhone)
        }
    )
}
