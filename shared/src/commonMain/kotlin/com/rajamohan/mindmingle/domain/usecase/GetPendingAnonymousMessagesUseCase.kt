package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.AnonymousMessage
import com.rajamohan.mindmingle.domain.repository.EphemeralMessageStore

class GetPendingAnonymousMessagesUseCase(
    private val store: EphemeralMessageStore
) {
    suspend operator fun invoke(): List<AnonymousMessage> {
        return store.pendingMessages()
    }
}
