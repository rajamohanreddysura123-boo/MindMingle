package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class DeleteMyAccountUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.deleteMyAccount()
}
