package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.Subscription
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

class AdminCancelSubscriptionUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(uid: String, immediate: Boolean): Result<Subscription> =
        repository.adminCancelSubscription(uid = uid, immediate = immediate)
}
