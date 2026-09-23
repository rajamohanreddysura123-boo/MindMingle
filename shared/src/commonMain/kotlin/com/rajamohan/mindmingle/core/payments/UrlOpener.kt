package com.rajamohan.mindmingle.core.payments

/**
 * Hands a URL to whatever the platform uses to open one.
 *
 * It exists for the desktop payment page: the purchase happens on a Razorpay-hosted page, and the
 * app's job ends at getting the user to it. Returns false when nothing could be opened, which the
 * caller shows as "copy the link instead" rather than as a failure — the QR beside it is still a
 * perfectly good way to pay.
 */
expect object UrlOpener {
    fun open(url: String): Boolean
}
