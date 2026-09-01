package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.ChatMessage
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveMessagesUseCase(
    private val repository: MindMingleRemoteRepository
) {
    operator fun invoke(conversationId: String): Flow<List<ChatMessage>> {
        return repository.observeMessages(conversationId)
    }
}
