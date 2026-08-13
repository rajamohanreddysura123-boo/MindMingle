package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.AnonymousChatRepository

class SendAnonymousMessageUseCase(
    private val repository: AnonymousChatRepository
) {
    suspend operator fun invoke(sessionId: String, selfUid: String, text: String): Boolean {
        return repository.sendMessage(sessionId, selfUid, text)
    }
}
