package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.Subscription
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow

class ObserveSubscriptionUseCase(
    private val repository: SubscriptionRepository
) {
    operator fun invoke(uid: String): Flow<Subscription?> = repository.observeSubscription(uid)
}
