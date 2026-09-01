package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class RequestAccountDeletionUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.requestAccountDeletion()
}
