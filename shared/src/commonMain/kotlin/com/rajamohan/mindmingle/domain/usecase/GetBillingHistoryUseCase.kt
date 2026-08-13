package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.BillingHistory
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

class GetBillingHistoryUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(uid: String = ""): Result<BillingHistory> =
        repository.getBillingHistory(uid)
}
