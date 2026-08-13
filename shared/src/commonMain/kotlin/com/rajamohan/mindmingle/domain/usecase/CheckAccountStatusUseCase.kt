package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.AccountStatus
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class CheckAccountStatusUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(uid: String): AccountStatus {
        return repository.checkAccountStatus(uid)
    }
}
