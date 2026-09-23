package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.PaymentLink
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

/**
 * A hosted payment page for the chosen plan.
 *
 * Used where there is no Razorpay checkout SDK — desktop. The plan is granted by the webhook when
 * the link is paid, so nothing is returned to verify and there is nothing for the caller to do
 * afterwards except watch the subscription document it is already streaming.
 */
class CreatePaymentLinkUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(plan: PremiumPlan, countryHint: String = ""): Result<PaymentLink> =
        repository.createPaymentLink(plan, countryHint)
}
