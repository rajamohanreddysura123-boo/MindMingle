package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.ChatMessage
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveSupportMessagesUseCase(
    private val repository: MindMingleRemoteRepository
) {
    operator fun invoke(uid: String): Flow<List<ChatMessage>> {
        return repository.observeSupportMessages(uid)
    }
}
