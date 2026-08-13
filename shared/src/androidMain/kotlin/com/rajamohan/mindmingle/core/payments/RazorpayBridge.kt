package com.rajamohan.mindmingle.core.payments

import com.razorpay.Checkout
import com.razorpay.PaymentData
import io.github.aakira.napier.Napier
import org.json.JSONObject

private const val TAG = "RazorpayBridge"

object RazorpayBridge {

    private var pending: ((CheckoutResult) -> Unit)? = null

    internal fun expectResult(onResult: (CheckoutResult) -> Unit) {
        pending = onResult
    }

    internal fun cancelExpectation() {
        pending = null
    }

    fun handlePaymentSuccess(paymentId: String?, paymentData: PaymentData?) {
        val id = paymentData?.paymentId ?: paymentId
        val orderId = paymentData?.orderId
        val signature = paymentData?.signature

        if (id.isNullOrBlank() || orderId.isNullOrBlank() || signature.isNullOrBlank()) {
            Napier.w(tag = TAG) { "payment succeeded without order id/signature" }
            deliver(CheckoutResult.Failed("Payment went through but could not be verified"))
            return
        }

        deliver(CheckoutResult.Success(paymentId = id, orderId = orderId, signature = signature))
    }

    fun handlePaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        if (code == Checkout.PAYMENT_CANCELED) {
            deliver(CheckoutResult.Cancelled)
            return
        }
        Napier.w(tag = TAG) { "payment failed ($code): $response" }
        deliver(CheckoutResult.Failed(describe(response)))
    }

    private fun deliver(result: CheckoutResult) {
        val callback = pending
        pending = null
        callback?.invoke(result)
    }

    private fun describe(response: String?): String {
        if (response.isNullOrBlank()) return "Payment failed"
        return runCatching {
            JSONObject(response).getJSONObject("error").getString("description")
        }.getOrNull() ?: "Payment failed"
    }
}
