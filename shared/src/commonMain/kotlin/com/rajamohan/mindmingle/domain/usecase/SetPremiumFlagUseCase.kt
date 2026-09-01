package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

/**
 * Copies the signed-in account's MindMingle+ state onto its own profile doc so other people can
 * see the badge. Never called for anyone else's uid — the rules would reject it anyway.
 */
class SetPremiumFlagUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(uid: String, isPremium: Boolean): Result<Unit> {
        return repository.setPremiumFlag(uid, isPremium)
    }
}
