package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.AdminStats
import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository

class GetAdminStatsUseCase(
    private val repository: MindMingleAdminRepository
) {
    suspend operator fun invoke(): AdminStats {
        return repository.getStats()
    }
}
