package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class SendOtpUseCase(
    private val repository: MindMingleRemoteRepository
) {
    operator fun invoke(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        repository.sendOtp(phoneNumber, onCodeSent, onError)
    }
}
