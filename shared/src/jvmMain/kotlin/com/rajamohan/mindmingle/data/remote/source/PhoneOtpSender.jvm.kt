package com.rajamohan.mindmingle.data.remote.source

actual object PhoneOtpSender {
    actual fun sendOtp(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        onCodeSent("MOCK_JVM_VERIFICATION_ID")
    }

    actual fun verifyOtp(
        verificationId: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (code.length == 6 || verificationId == "MOCK_JVM_VERIFICATION_ID") {
            onSuccess()
        } else {
            onError("Invalid OTP code")
        }
    }
}
