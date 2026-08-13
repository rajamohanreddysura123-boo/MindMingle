package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.EphemeralMessageStore

class MarkAnonymousMessagesSeenUseCase(
    private val store: EphemeralMessageStore
) {
    suspend operator fun invoke(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        store.markSeen(messageIds)
    }
}
