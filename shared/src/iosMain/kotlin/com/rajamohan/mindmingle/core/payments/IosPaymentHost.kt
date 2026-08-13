package com.rajamohan.mindmingle.core.payments

object IosPaymentHost {

    var isBridgeInstalled: Boolean = false
        private set

    private var presenter: ((Map<String, String>, (Map<String, String>) -> Unit) -> Unit)? = null

    fun install(presentCheckout: (Map<String, String>, (Map<String, String>) -> Unit) -> Unit) {
        presenter = presentCheckout
        isBridgeInstalled = true
    }

    internal fun present(options: Map<String, String>, onFinished: (Map<String, String>) -> Unit) {
        val present = presenter
        if (present == null) {
            onFinished(mapOf("status" to "failed", "message" to "Payments are not available yet"))
            return
        }
        present(options, onFinished)
    }
}
