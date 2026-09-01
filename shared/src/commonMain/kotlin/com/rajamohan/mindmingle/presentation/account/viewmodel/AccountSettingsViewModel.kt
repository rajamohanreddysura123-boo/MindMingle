package com.rajamohan.mindmingle.presentation.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import com.rajamohan.mindmingle.domain.model.NotificationCategory
import com.rajamohan.mindmingle.domain.usecase.DeactivateMyAccountUseCase
import com.rajamohan.mindmingle.domain.usecase.GetNotificationPrefsUseCase
import com.rajamohan.mindmingle.domain.usecase.SaveNotificationPrefsUseCase
import com.rajamohan.mindmingle.domain.usecase.RequestAccountDeletionUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AccountSettingsViewModel(
    private val deactivateMyAccountUseCase: DeactivateMyAccountUseCase,
    private val requestAccountDeletionUseCase: RequestAccountDeletionUseCase,
    private val getNotificationPrefsUseCase: GetNotificationPrefsUseCase,
    private val saveNotificationPrefsUseCase: SaveNotificationPrefsUseCase,
    private val mindMingleRemoteRepository: MindMingleRemoteRepository
) : ViewModel() {

    private companion object {
        const val TAG = "AccountSettingsViewModel"
    }

    private val _uiState = MutableStateFlow(AccountSettingsUiState())
    val uiState: StateFlow<AccountSettingsUiState> = _uiState.asStateFlow()

    init {
        loadNotificationPrefs()
    }

    private fun loadNotificationPrefs() {
        val uid = mindMingleRemoteRepository.getCurrentUid()
        if (uid.isNullOrBlank()) {
            _uiState.update { it.copy(isLoadingPrefs = false) }
            return
        }
        viewModelScope.launch {
            val prefs = getNotificationPrefsUseCase(uid)
            _uiState.update { it.copy(notificationPrefs = prefs, isLoadingPrefs = false) }
        }
    }

    /**
     * Flips one category and writes the whole preferences document.
     *
     * The switch moves first and is put back only if the write fails: a toggle that waits on a
     * round trip feels broken, and the cost of being briefly wrong is one notification.
     */
    fun setNotificationCategory(category: NotificationCategory, enabled: Boolean) {
        val uid = mindMingleRemoteRepository.getCurrentUid()
        if (uid.isNullOrBlank()) return

        val previous = _uiState.value.notificationPrefs
        val updated = previous.with(category, enabled)
        _uiState.update { it.copy(notificationPrefs = updated, prefsError = "") }

        viewModelScope.launch {
            saveNotificationPrefsUseCase(uid, updated).onFailure { error ->
                Napier.w(throwable = error, tag = TAG) { "saveNotificationPrefs failed" }
                _uiState.update {
                    it.copy(
                        notificationPrefs = previous,
                        prefsError = "Couldn't save that setting — check your connection"
                    )
                }
            }
        }
    }

    fun selectDays(days: Int) {
        _uiState.update { it.copy(selectedDays = days, deactivateError = "") }
    }

    /**
     * Starts the deactivation window and drops the session. Nothing here can undo it — the window
     * is only cleared by a later sign-in, once the date has passed (see checkAccountStatus).
     */
    fun deactivate() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isDeactivating = true, deactivateError = "") }
        viewModelScope.launch {
            try {
                deactivateMyAccountUseCase(_uiState.value.selectedDays).fold(
                    onSuccess = {
                        mindMingleRemoteRepository.signOutCurrentUser()
                        _uiState.update { it.copy(isDeactivating = false, isDeactivated = true) }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isDeactivating = false,
                                deactivateError = error.message ?: "Could not deactivate your account"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Napier.e(throwable = e, tag = TAG) { "deactivate failed" }
                _uiState.update {
                    it.copy(isDeactivating = false, deactivateError = e.message ?: "Could not deactivate your account")
                }
            }
        }
    }

    /**
     * Files the deletion request. Everything the user owns goes immediately and they can never
     * sign in again; an admin clears out what a client cannot reach from the deletion queue.
     */
    fun deleteAccount() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isDeleting = true, deleteError = "") }
        viewModelScope.launch {
            try {
                requestAccountDeletionUseCase().fold(
                    onSuccess = {
                        mindMingleRemoteRepository.signOutCurrentUser()
                        _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isDeleting = false,
                                deleteError = error.message ?: "Could not delete your account"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Napier.e(throwable = e, tag = TAG) { "deleteAccount failed" }
                _uiState.update {
                    it.copy(isDeleting = false, deleteError = e.message ?: "Could not delete your account")
                }
            }
        }
    }
}
