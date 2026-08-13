package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.PlanCatalog
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

class SavePlanCatalogUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(catalog: PlanCatalog): Result<Int> = repository.savePlanCatalog(catalog)
}
