package com.rajamohan.mindmingle.presentation.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.repository.MindMingleLocalRepository
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import com.rajamohan.mindmingle.domain.usecase.CheckIsAdminUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AuthViewModel(
    private val mindMingleLocalRepository: MindMingleLocalRepository,
    private val mindMingleRemoteRepository: MindMingleRemoteRepository,
    private val checkIsAdminUseCase: CheckIsAdminUseCase
) : ViewModel() {

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    init {
        viewModelScope.launch {
            restoreSession()
        }
    }

    /** Firebase Auth persists the session across process death — resume straight into the app instead of Onboarding when one exists. */
    private suspend fun restoreSession() {
        val uid = mindMingleRemoteRepository.getCurrentUid()
        if (uid == null) {
            _authUiState.update { it.copy(isCheckingSession = false) }
            return
        }
        admitIfAllowed(uid = uid, restoringSession = true)
    }

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.SendOtp -> {
                sendOtp(phone = event.phone)
            }
            is AuthEvent.VerifyOtp -> {
                verifyOtp(verificationId = event.verificationId, code = event.code)
            }
            is AuthEvent.GoogleSignInVerified -> {
                googleSignInVerified(uid = event.uid, email = event.email, name = event.name)
            }
            is AuthEvent.PhoneChanged -> {
                _authUiState.update { it.copy(phone = event.phone, error = "") }
            }
            is AuthEvent.OtpCodeChanged -> {
                _authUiState.update { it.copy(otpCode = event.code, error = "") }
            }
            is AuthEvent.RequestEmailOtp -> {
                requestEmailOtp(email = event.email)
            }
            is AuthEvent.VerifyEmailOtp -> {
                verifyEmailOtp(code = event.code)
            }
            is AuthEvent.ResetState -> {
                viewModelScope.launch {
                    mindMingleRemoteRepository.signOutCurrentUser()
                    _authUiState.value = AuthUiState(isCheckingSession = false)
                }
            }
        }
    }

    private fun sendOtp(phone: String) {
        _authUiState.update { it.copy(isLoading = true, error = "") }
        mindMingleRemoteRepository.sendOtp(
            phoneNumber = phone,
            onCodeSent = { verificationId ->
                _authUiState.update {
                    it.copy(
                        isLoading = false,
                        isCodeSent = true,
                        verificationId = verificationId,
                        phone = phone
                    )
                }
            },
            onError = { errorMessage ->
                _authUiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            }
        )
    }

    private fun verifyOtp(verificationId: String, code: String) {
        _authUiState.update { it.copy(isLoading = true, error = "") }
        mindMingleRemoteRepository.verifyOtp(
            verificationId = verificationId,
            code = code,
            onSuccess = { user ->
                viewModelScope.launch {
                    admitIfAllowed(uid = user.uid)
                }
            },
            onError = { errorMessage ->
                _authUiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            }
        )
    }

    private fun googleSignInVerified(uid: String, email: String, name: String) {
        _authUiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            admitIfAllowed(uid = uid, email = email, prefillName = name)
        }
    }

    /** Desktop's primary sign-in — mails a one-time code to [email] via a Cloud Function. */
    private fun requestEmailOtp(email: String) {
        _authUiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            mindMingleRemoteRepository.sendEmailOtp(email)
                .onSuccess {
                    _authUiState.update {
                        it.copy(isLoading = false, isEmailOtpSent = true, emailOtpEmail = email, error = "")
                    }
                }
                .onFailure { e ->
                    _authUiState.update { it.copy(isLoading = false, error = e.message ?: "Could not send verification code") }
                }
        }
    }

    private fun verifyEmailOtp(code: String) {
        val email = _authUiState.value.emailOtpEmail
        if (email.isBlank()) return
        _authUiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            mindMingleRemoteRepository.verifyEmailOtpAndSignIn(email, code)
                .onSuccess { uid -> admitIfAllowed(uid = uid, email = email) }
                .onFailure { e -> _authUiState.update { it.copy(isLoading = false, error = e.message ?: "Invalid code") } }
        }
    }

    /** Shared ban/disable gate for every sign-in path — blocks and signs back out if not allowed in. */
    private suspend fun admitIfAllowed(
        uid: String,
        email: String? = null,
        prefillName: String? = null,
        restoringSession: Boolean = false
    ) {
        val status = mindMingleRemoteRepository.checkAccountStatus(uid)
        if (status.isBlocked) {
            mindMingleRemoteRepository.signOutCurrentUser()
            _authUiState.update { it.copy(isLoading = false, isCheckingSession = false, error = status.message) }
            return
        }

        // Admins sign in through this same phone-OTP/Google flow — no separate admin login.
        // They don't need a consumer profile doc, so this check short-circuits before that lookup.
        if (checkIsAdminUseCase()) {
            _authUiState.update {
                it.copy(
                    isLoading = false,
                    isCheckingSession = false,
                    isSuccess = true,
                    uid = uid,
                    email = email,
                    prefillName = prefillName,
                    isAdmin = true
                )
            }
            return
        }

        // Skip Profile Setup when this uid already has a completed profile doc.
        val existingUser = mindMingleRemoteRepository.getUser(uid)
        val profileComplete = existingUser?.isProfileComplete == true

        if (restoringSession && existingUser == null) {
            // Firebase session present but no profile doc yet (e.g. abandoned setup) — don't force Onboarding.
            _authUiState.update { it.copy(isCheckingSession = false) }
            return
        }

        _authUiState.update {
            it.copy(
                isLoading = false,
                isCheckingSession = false,
                isSuccess = true,
                uid = uid,
                email = email ?: existingUser?.email,
                prefillName = prefillName ?: existingUser?.name,
                profileComplete = profileComplete
            )
        }
    }

    /** The signed-in Firebase Auth uid, or null if no session exists yet. */
    fun currentUid(): String? = mindMingleRemoteRepository.getCurrentUid()
}
