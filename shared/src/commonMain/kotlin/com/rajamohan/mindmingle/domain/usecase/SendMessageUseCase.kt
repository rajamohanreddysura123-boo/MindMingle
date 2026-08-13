package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class SendMessageUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(matchId: String, senderId: String, text: String): Boolean {
        return repository.sendMessage(matchId, senderId, text)
    }
}
