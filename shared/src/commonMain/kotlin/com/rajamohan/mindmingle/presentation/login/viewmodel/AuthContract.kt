package com.rajamohan.mindmingle.presentation.login.viewmodel

internal sealed class AuthEvent {
    data class GoogleSignInVerified(val uid: String, val email: String, val name: String) : AuthEvent()
    /** Retired alternative to email + password — kept for when the mailed-code flow is wanted again. */
    data class RequestEmailOtp(val email: String) : AuthEvent()
    data class VerifyEmailOtp(val code: String) : AuthEvent()
    /** Desktop's sign-in, and mobile's alternative to Google. Plain Firebase Auth. */
    data class EmailPasswordSignIn(val email: String, val password: String) : AuthEvent()
    /** Same screen in "Create account" mode — registers, then signs the new account straight in. */
    data class EmailPasswordSignUp(val email: String, val password: String) : AuthEvent()
    data object ResetState : AuthEvent()
}

internal data class AuthUiState(
    val isLoading: Boolean = false,
    val isCheckingSession: Boolean = true,
    val uid: String? = null,
    val email: String? = null,
    val prefillName: String? = null,
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