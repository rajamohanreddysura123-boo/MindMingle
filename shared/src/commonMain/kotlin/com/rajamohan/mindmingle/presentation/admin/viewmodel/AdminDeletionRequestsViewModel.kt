package com.rajamohan.mindmingle.presentation.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.DeletionRequest
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import com.rajamohan.mindmingle.domain.usecase.DeleteUserCascadeUseCase
import com.rajamohan.mindmingle.domain.usecase.ListDeletionRequestsUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AdminDeletionRequestsUiState(
    val isLoading: Boolean = true,
    val requests: List<DeletionRequest> = emptyList(),
    /** uid currently being purged — the row shows a spinner and the rest stay clickable. */
    val purgingUid: String = "",
    val error: String = "",
    val message: String = ""
) {
    val pending: List<DeletionRequest> get() = requests.filter { it.isPending }

    val completed: List<DeletionRequest> get() = requests.filterNot { it.isPending }
}

/**
 * Admin deletion queue. A user asking to be deleted only clears out what their own credentials
 * reach; the purge run from here removes the rest and drops the bannedUids tombstone that keeps
 * the leftover Firebase Auth record permanently locked out.
 */
internal class AdminDeletionRequestsViewModel(
    private val listDeletionRequestsUseCase: ListDeletionRequestsUseCase,
    private val deleteUserCascadeUseCase: DeleteUserCascadeUseCase,
    private val mindMingleRemoteRepository: MindMingleRemoteRepository
) : ViewModel() {

    private companion object {
        const val TAG = "AdminDeletionRequestsViewModel"
    }

    private val _uiState = MutableStateFlow(AdminDeletionRequestsUiState())
    val uiState: StateFlow<AdminDeletionRequestsUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            try {
                val requests = listDeletionRequestsUseCase()
                _uiState.update { it.copy(isLoading = false, requests = requests) }
            } catch (e: Exception) {
                Napier.e(throwable = e, tag = TAG) { "load failed" }
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Could not load deletion requests")
                }
            }
        }
    }

    fun purge(uid: String) {
        if (_uiState.value.purgingUid.isNotBlank()) return
        val adminUid = mindMingleRemoteRepository.getCurrentUid()
        if (adminUid == null) {
            _uiState.update { it.copy(error = "No admin session — sign in again") }
            return
        }
        _uiState.update { it.copy(purgingUid = uid, error = "", message = "") }
        viewModelScope.launch {
            try {
                deleteUserCascadeUseCase(uid, adminUid).fold(
                    onSuccess = {
                        _uiState.update { it.copy(purgingUid = "", message = "Account deleted") }
                        load()
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(purgingUid = "", error = error.message ?: "Delete failed")
                        }
                    }
                )
            } catch (e: Exception) {
                Napier.e(throwable = e, tag = TAG) { "purge failed for uid=$uid" }
                _uiState.update { it.copy(purgingUid = "", error = e.message ?: "Delete failed") }
            }
        }
    }
}
