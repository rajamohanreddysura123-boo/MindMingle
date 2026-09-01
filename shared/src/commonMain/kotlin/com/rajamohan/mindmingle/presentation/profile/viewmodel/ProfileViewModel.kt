package com.rajamohan.mindmingle.presentation.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.nowMillis
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import com.rajamohan.mindmingle.domain.usecase.GetBillingHistoryUseCase
import com.rajamohan.mindmingle.domain.usecase.GetProfileStatsUseCase
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
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val observeSubscriptionUseCase: ObserveSubscriptionUseCase,
    private val getBillingHistoryUseCase: GetBillingHistoryUseCase,
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
            // Catches an account that stopped being usable mid-session: an admin disabling it, a
            // deletion filed from another device, or a deactivation window started elsewhere.
            val isLockedOut = user != null && (
                user.isDisabled ||
                    user.isDeletionRequested ||
                    user.isDeactivatedAt(nowMillis())
                )
            if (isLockedOut) {
                mindMingleRemoteRepository.signOutCurrentUser()
                _uiState.update { it.copy(isLoading = false, isAccountBlocked = true) }
                return@launch
            }
            val stats = getProfileStatsUseCase(uid)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    user = user,
                    conversationsCount = stats.conversations,
                    likesCount = stats.likes
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
