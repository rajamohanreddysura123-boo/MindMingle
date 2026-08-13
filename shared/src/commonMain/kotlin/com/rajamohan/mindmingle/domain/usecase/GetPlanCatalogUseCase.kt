package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.PlanCatalog
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository

class GetPlanCatalogUseCase(
    private val repository: SubscriptionRepository
) {
    suspend operator fun invoke(countryHint: String = ""): Result<PlanCatalog> =
        repository.getPlanCatalog(countryHint)
}
