package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository

class DeleteUserCascadeUseCase(
    private val repository: MindMingleAdminRepository
) {
    suspend operator fun invoke(uid: String, adminUid: String): Result<Unit> {
        return repository.deleteUser(uid, adminUid)
    }
}
