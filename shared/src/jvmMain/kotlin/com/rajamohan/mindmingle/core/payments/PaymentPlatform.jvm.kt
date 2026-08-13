package com.rajamohan.mindmingle.core.payments

actual object PaymentPlatform {

    actual val isSupported: Boolean = false

    actual suspend fun startCheckout(request: CheckoutRequest): CheckoutResult =
        CheckoutResult.Failed("Upgrade to MindMingle+ from the Android or iOS app")
}
