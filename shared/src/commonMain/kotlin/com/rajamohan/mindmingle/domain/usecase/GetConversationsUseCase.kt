package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class GetConversationsUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(uid: String): List<ChatConversation> {
        return repository.getConversations(uid)
    }
}
