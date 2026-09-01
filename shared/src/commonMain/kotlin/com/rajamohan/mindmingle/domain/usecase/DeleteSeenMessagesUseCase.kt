package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

/**
 * Removes messages the reader has now seen. Messages in MindMingle do not persist: this is the
 * client half of that, and the Firestore TTL policy sweeps anything the recipient never opened.
 */
class DeleteSeenMessagesUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(conversationId: String, messageIds: List<String>) {
        repository.deleteSeenMessages(conversationId = conversationId, messageIds = messageIds)
    }
}
