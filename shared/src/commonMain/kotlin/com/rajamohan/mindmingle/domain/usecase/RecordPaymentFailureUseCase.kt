package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

/**
 * Tells the backend a checkout failed on this device.
 *
 * The Razorpay webhook only reports failures Razorpay itself saw; a sheet that died on a dead
 * network never produces one, and that is exactly the case where the user is least sure whether
 * they were charged.
 */
class RecordPaymentFailureUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(orderId: String, planId: String, reason: String) =
        repository.recordPaymentFailure(orderId, planId, reason)
}
