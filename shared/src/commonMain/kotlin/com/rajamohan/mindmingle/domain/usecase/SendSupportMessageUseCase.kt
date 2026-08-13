package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class SendSupportMessageUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(uid: String, userName: String, senderId: String, text: String): Boolean {
        return repository.sendSupportMessage(uid, userName, senderId, text)
    }
}
