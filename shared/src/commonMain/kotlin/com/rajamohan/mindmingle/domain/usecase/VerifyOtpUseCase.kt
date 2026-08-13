package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class VerifyOtpUseCase(
    private val repository: MindMingleRemoteRepository
) {
    operator fun invoke(
        verificationId: String,
        code: String,
        onSuccess: (user: User) -> Unit,
        onError: (message: String) -> Unit
    ) {
        repository.verifyOtp(
            verificationId = verificationId,
            code = code,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
