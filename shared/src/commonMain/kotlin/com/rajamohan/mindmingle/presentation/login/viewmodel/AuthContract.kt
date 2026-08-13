package com.rajamohan.mindmingle.presentation.login.viewmodel

internal sealed class AuthEvent {
    data class SendOtp(val phone: String) : AuthEvent()
    data class VerifyOtp(val verificationId: String, val code: String) : AuthEvent()
    data class GoogleSignInVerified(val uid: String, val email: String, val name: String) : AuthEvent()
    data class PhoneChanged(val phone: String) : AuthEvent()
    data class OtpCodeChanged(val code: String) : AuthEvent()
    /** Desktop's primary sign-in — no working Google OAuth on JVM, so email + a mailed code instead. */
    data class RequestEmailOtp(val email: String) : AuthEvent()
    data class VerifyEmailOtp(val code: String) : AuthEvent()
    data object ResetState : AuthEvent()
}

internal data class AuthUiState(
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val phone: String = "",
    val otpCode: String = "",
    val verificationId: String? = null,
    val uid: String? = null,
    val email: String? = null,
    val prefillName: String? = null,
    val isCodeSent: Boolean = false,
    val isSuccess: Boolean = false,
    /** True once [isSuccess] fires and the signed-in uid already has a completed profile doc. */
    val profileComplete: Boolean = false,
    /** True once [isSuccess] fires and the signed-in uid has an admins/{uid} Firestore doc. */
    val isAdmin: Boolean = false,
    /** True once a mailed verification code has been requested — desktop's primary sign-in flow. */
    val isEmailOtpSent: Boolean = false,
    val emailOtpEmail: String = "",
    val error: String = ""
)