package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.DeletionRequest
import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository

class ListDeletionRequestsUseCase(
    private val repository: MindMingleAdminRepository
) {
    suspend operator fun invoke(): List<DeletionRequest> = repository.listDeletionRequests()
}
