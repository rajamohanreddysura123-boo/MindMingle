package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository

class SetUserDisabledUseCase(
    private val repository: MindMingleAdminRepository
) {
    suspend operator fun invoke(uid: String, disabled: Boolean): Result<Unit> {
        return repository.setUserDisabled(uid, disabled)
    }
}
