package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.Subscription
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

class GetSubscriptionUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(uid: String): Subscription? = repository.getSubscription(uid)
}
