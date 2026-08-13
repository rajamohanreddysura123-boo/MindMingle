package com.rajamohan.mindmingle.data.remote.source

expect object PhoneOtpSender {
    fun sendOtp(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (message: String) -> Unit
    )

    fun verifyOtp(
        verificationId: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (message: String) -> Unit
    )
}
