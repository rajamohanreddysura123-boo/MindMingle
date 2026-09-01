package com.rajamohan.mindmingle.presentation.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.core.platform.AppPlatform
import com.rajamohan.mindmingle.core.platform.getCurrentPlatform
import com.rajamohan.mindmingle.domain.repository.MindMingleLocalRepository
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import com.rajamohan.mindmingle.domain.usecase.UnregisterPushDeviceUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AuthViewModel(
    private val mindMingleLocalRepository: MindMingleLocalRepository,
    private val mindMingleRemoteRepository: MindMingleRemoteRepository,
    private val unregisterPushDeviceUseCase: UnregisterPushDeviceUseCase
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
            is AuthEvent.GoogleSignInVerified -> {
                googleSignInVerified(uid = event.uid, email = event.email, name = event.name)
            }
            is AuthEvent.RequestEmailOtp -> {
                requestEmailOtp(email = event.email)
            }
            is AuthEvent.VerifyEmailOtp -> {
                verifyEmailOtp(code = event.code)
            }
            is AuthEvent.EmailPasswordSignIn -> {
                emailPasswordAuth(email = event.email, password = event.password, isNewAccount = false)
            }
            is AuthEvent.EmailPasswordSignUp -> {
                emailPasswordAuth(email = event.email, password = event.password, isNewAccount = true)
            }
            is AuthEvent.ResetState -> {
                viewModelScope.launch {
                    signOut()
                    _authUiState.value = AuthUiState(isCheckingSession = false)
                }
            }
        }
    }

    /**
     * Signs out, dropping this device's push token first.
     *
     * Order matters: the token row lives under `users/{uid}/devices` and only its owner may delete
     * it, so once Firebase Auth has let go there is no longer any credential to delete it with. A
     * phone that skipped this keeps receiving that account's notifications after the next person
     * signs in on it.
     */
    private suspend fun signOut() {
        mindMingleRemoteRepository.getCurrentUid()?.let { uid ->
            unregisterPushDeviceUseCase(uid)
        }
        mindMingleRemoteRepository.signOutCurrentUser()
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

    /**
     * Email + password, both directions: [isNewAccount] registers, otherwise it signs in. Either
     * way the result is an ordinary Firebase Auth session, so everything downstream is the normal
     * flow — [admitIfAllowed] applies the ban/deactivation gate, resolves userType for admin
     * routing, and decides Profile Setup vs Home.
     */
    private fun emailPasswordAuth(email: String, password: String, isNewAccount: Boolean) {
        val normalized = email.trim().lowercase()
        if (!EMAIL_REGEX.matches(normalized)) {
            _authUiState.update { it.copy(isLoading = false, error = "Enter a valid email address") }
            return
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            _authUiState.update {
                it.copy(isLoading = false, error = "Password must be at least $MIN_PASSWORD_LENGTH characters")
            }
            return
        }

        val prefillName = normalized.substringBefore("@")
            .replace(".", " ")
            .split(" ")
            .joinToString(" ") { word -> word.replaceFirstChar { char -> char.uppercase() } }

        _authUiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            val result = if (isNewAccount) {
                mindMingleRemoteRepository.createAccountWithEmailPassword(normalized, password)
            } else {
                mindMingleRemoteRepository.signInWithEmailPassword(normalized, password)
            }

            result
                .onSuccess { uid -> admitIfAllowed(uid = uid, email = normalized, prefillName = prefillName) }
                .onFailure { e ->
                    Napier.e(throwable = e, tag = TAG) { "emailPasswordAuth failed for email=$normalized" }
                    _authUiState.update {
                        it.copy(isLoading = false, error = authErrorMessage(e, isNewAccount))
                    }
                }
        }
    }

    /**
     * Firebase's own messages are readable but leak which half of the pair was wrong, so the
     * sign-in cases collapse into one — same reason the Console's own forms do it.
     */
    private fun authErrorMessage(error: Throwable, isNewAccount: Boolean): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("EMAIL_EXISTS", ignoreCase = true) ||
                raw.contains("already in use", ignoreCase = true) ->
                "That email already has an account — sign in instead"
            raw.contains("WEAK_PASSWORD", ignoreCase = true) ->
                "Pick a stronger password"
            raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
                "Email sign-in is switched off for this project"
            raw.contains("NETWORK", ignoreCase = true) ->
                "No connection — check your network and try again"
            isNewAccount -> raw.ifBlank { "Could not create your account" }
            else -> "Email or password is incorrect"
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
            signOut()
            _authUiState.update { it.copy(isLoading = false, isCheckingSession = false, error = status.message) }
            return
        }

        // Skip Profile Setup when this uid already has a completed profile doc.
        val existingUser = mindMingleRemoteRepository.getUser(uid)
        val profileComplete = existingUser?.isProfileComplete == true

        // Admins sign in through the same flow as everyone else — what separates them is
        // userType on their own profile doc, which only the Firebase Console can set. The panel
        // is desktop-only, so an admin signing in on a phone lands on Home like any other user.
        if (existingUser?.isAdmin == true && getCurrentPlatform() == AppPlatform.DESKTOP) {
            _authUiState.update {
                it.copy(
                    isLoading = false,
                    isCheckingSession = false,
                    isSuccess = true,
                    uid = uid,
                    email = email ?: existingUser.email,
                    prefillName = prefillName ?: existingUser.name,
                    isAdmin = true
                )
            }
            return
        }

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

    private companion object {
        const val TAG = "AuthViewModel"

        /** Firebase Auth's own floor — anything shorter is rejected server-side anyway. */
        const val MIN_PASSWORD_LENGTH = 6

        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
