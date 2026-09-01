package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

/**
 * One page of conversations. [startAfterConversationId] is the last conversationId of the previous page; null
 * starts from the most recent.
 */
class GetConversationsUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(
        uid: String,
        pageSize: Int = 30,
        startAfterConversationId: String? = null
    ): List<ChatConversation> {
        return repository.getConversations(
            uid = uid,
            pageSize = pageSize,
            startAfterConversationId = startAfterConversationId
        )
    }
}
