package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.AnonymousMessage
import com.rajamohan.mindmingle.domain.repository.AnonymousChatRepository
import com.rajamohan.mindmingle.domain.repository.EphemeralMessageStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveAnonymousMessagesUseCase(
    private val repository: AnonymousChatRepository,
    private val store: EphemeralMessageStore
) {
    operator fun invoke(sessionId: String, selfUid: String): Flow<AnonymousMessage> {
        return repository.observeIncomingMessages(sessionId, selfUid).map { (messageId, text) ->
            store.saveIncoming(sessionId = sessionId, messageId = messageId, text = text)
        }
    }
}
