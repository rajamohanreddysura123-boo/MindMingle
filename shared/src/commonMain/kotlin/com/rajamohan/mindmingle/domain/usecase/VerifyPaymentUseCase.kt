package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.Subscription
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

class VerifyPaymentUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(
        orderId: String,
        paymentId: String,
        signature: String
    ): Result<Subscription> = repository.verifyPayment(
        orderId = orderId,
        paymentId = paymentId,
        signature = signature
    )
}
