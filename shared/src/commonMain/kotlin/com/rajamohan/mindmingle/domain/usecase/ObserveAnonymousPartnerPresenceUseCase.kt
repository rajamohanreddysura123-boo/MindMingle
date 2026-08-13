package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.AnonymousChatRepository
import kotlinx.coroutines.flow.Flow

class ObserveAnonymousPartnerPresenceUseCase(
    private val repository: AnonymousChatRepository
) {
    operator fun invoke(sessionId: String, selfUid: String): Flow<Boolean> {
        return repository.observePartnerPresence(sessionId, selfUid)
    }
}
