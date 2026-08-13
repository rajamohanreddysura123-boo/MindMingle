package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository

class UnbanUserUseCase(
    private val repository: MindMingleAdminRepository
) {
    suspend operator fun invoke(uid: String): Result<Unit> {
        return repository.unbanUser(uid)
    }
}
