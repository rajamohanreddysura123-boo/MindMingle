package com.rajamohan.mindmingle.presentation.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import com.rajamohan.mindmingle.domain.usecase.DeleteMyAccountUseCase
import com.rajamohan.mindmingle.domain.usecase.GetBillingHistoryUseCase
import com.rajamohan.mindmingle.domain.usecase.GetConversationsUseCase
import com.rajamohan.mindmingle.domain.usecase.GetIncomingLikesUseCase
import com.rajamohan.mindmingle.domain.usecase.GetUserProfileUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveSubscriptionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProfileViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getConversationsUseCase: GetConversationsUseCase,
    private val getIncomingLikesUseCase: GetIncomingLikesUseCase,
    private val observeSubscriptionUseCase: ObserveSubscriptionUseCase,
    private val getBillingHistoryUseCase: GetBillingHistoryUseCase,
    private val deleteMyAccountUseCase: DeleteMyAccountUseCase,
    private val mindMingleRemoteRepository: MindMingleRemoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var subscriptionJob: Job? = null

    fun loadProfile(uid: String) {
        observeSubscription(uid)
        loadBillingHistory()
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val user = getUserProfileUseCase(uid)
            if (user?.isDisabled == true) {
                mindMingleRemoteRepository.signOutCurrentUser()
                _uiState.update { it.copy(isLoading = false, isAccountBlocked = true) }
                return@launch
            }
            val matches = getConversationsUseCase(uid)
            val likes = getIncomingLikesUseCase(uid)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    user = user,
                    matchesCount = matches.size,
                    likesCount = likes.size
                )
            }
        }
    }

    fun loadBillingHistory() {
        _uiState.update { it.copy(isLoadingBilling = true) }
        viewModelScope.launch {
            getBillingHistoryUseCase().fold(
                onSuccess = { history ->
                    _uiState.update { it.copy(isLoadingBilling = false, billing = history) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingBilling = false,
                            billingError = error.message ?: "Could not load your orders"
                        )
                    }
                }
            )
        }
    }

    /**
     * Point of no return: the server erases every record tied to this account, including the
     * Firebase Auth user, so the session is dead either way once this succeeds.
     */
    fun deleteAccount() {
        if (_uiState.value.isDeletingAccount) return
        _uiState.update { it.copy(isDeletingAccount = true, deleteError = "") }
        viewModelScope.launch {
            deleteMyAccountUseCase().fold(
                onSuccess = {
                    mindMingleRemoteRepository.signOutCurrentUser()
                    _uiState.update { it.copy(isDeletingAccount = false, isAccountDeleted = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            deleteError = error.message ?: "Could not delete your account"
                        )
                    }
                }
            )
        }
    }

    private fun observeSubscription(uid: String) {
        subscriptionJob?.cancel()
        subscriptionJob = viewModelScope.launch {
            observeSubscriptionUseCase(uid).collect { subscription ->
                _uiState.update { it.copy(subscription = subscription) }
            }
        }
    }

    override fun onCleared() {
        subscriptionJob?.cancel()
        super.onCleared()
    }
}
