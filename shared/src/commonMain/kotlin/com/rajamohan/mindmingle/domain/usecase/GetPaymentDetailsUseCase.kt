package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.PaymentDetails
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

class GetPaymentDetailsUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(paymentId: String): Result<PaymentDetails> =
        repository.getPaymentDetails(paymentId)
}
