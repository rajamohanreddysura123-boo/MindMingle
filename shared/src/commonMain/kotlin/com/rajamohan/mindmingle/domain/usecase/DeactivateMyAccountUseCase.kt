package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class DeactivateMyAccountUseCase(
    private val repository: MindMingleRemoteRepository
) {
    /** [days] must be one of [ALLOWED_DAYS]; returns the instant the account comes back. */
    suspend operator fun invoke(days: Int): Result<Long> {
        if (days !in ALLOWED_DAYS) {
            return Result.failure(IllegalArgumentException("Pick 7, 15 or 30 days"))
        }
        return repository.deactivateMyAccount(days)
    }

    companion object {
        val ALLOWED_DAYS = listOf(7, 15, 30)
    }
}
