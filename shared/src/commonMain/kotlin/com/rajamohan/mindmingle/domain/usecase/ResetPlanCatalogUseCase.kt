package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

class ResetPlanCatalogUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(): Result<Int> = repository.resetPlanCatalog()
}
