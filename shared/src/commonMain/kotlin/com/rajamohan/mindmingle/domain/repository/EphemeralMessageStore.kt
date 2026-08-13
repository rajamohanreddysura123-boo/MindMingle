package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.AnonymousMessage

interface EphemeralMessageStore {
    suspend fun saveIncoming(sessionId: String, messageId: String, text: String): AnonymousMessage

    suspend fun markSeen(messageIds: List<String>)

    suspend fun purgeSeen()

    suspend fun pendingMessages(): List<AnonymousMessage>

    suspend fun clearAll()
}
