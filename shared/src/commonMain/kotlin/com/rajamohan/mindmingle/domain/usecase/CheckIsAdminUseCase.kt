package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository

class CheckIsAdminUseCase(
    private val repository: MindMingleAdminRepository
) {
    suspend operator fun invoke(): Boolean {
        return repository.isCurrentUserAdmin()
    }
}
