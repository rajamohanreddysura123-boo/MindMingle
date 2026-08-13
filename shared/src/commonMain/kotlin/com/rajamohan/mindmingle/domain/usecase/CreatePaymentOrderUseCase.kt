package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.PaymentOrder
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

class CreatePaymentOrderUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(plan: PremiumPlan, countryHint: String = ""): Result<PaymentOrder> =
        repository.createOrder(plan = plan, countryHint = countryHint)
}
