package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.model.Subscription
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

class AdminSetSubscriptionUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(uid: String, plan: PremiumPlan, days: Int): Result<Subscription> =
        repository.adminSetSubscription(uid = uid, plan = plan, days = days)
}
