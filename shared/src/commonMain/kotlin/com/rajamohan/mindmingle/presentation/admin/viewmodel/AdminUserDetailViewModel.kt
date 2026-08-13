package com.rajamohan.mindmingle.presentation.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.BillingHistory
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import com.rajamohan.mindmingle.domain.usecase.AdminCancelSubscriptionUseCase
import com.rajamohan.mindmingle.domain.usecase.AdminSetSubscriptionUseCase
import com.rajamohan.mindmingle.domain.usecase.DeleteUserCascadeUseCase
import com.rajamohan.mindmingle.domain.usecase.GetBillingHistoryUseCase
import com.rajamohan.mindmingle.domain.usecase.GetUserProfileUseCase
import com.rajamohan.mindmingle.domain.usecase.SetUserDisabledUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUserDetailUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val isMutating: Boolean = false,
    val error: String = "",
    val isDeleted: Boolean = false,
    val billing: BillingHistory? = null,
    val isLoadingBilling: Boolean = false,
    val isSubscriptionMutating: Boolean = false,
    val subscriptionMessage: String = ""
) {
    val hasActivePlan: Boolean get() = billing?.isActive == true

    val planLabel: String
        get() = billing?.planId?.let { PremiumPlan.fromId(it)?.label ?: it }.orEmpty().ifBlank { "No plan" }

    val planStatus: String get() = billing?.status.orEmpty().ifBlank { "none" }

    val planEndsLabel: String get() = billing?.renewsLabel ?: "—"
}

class AdminUserDetailViewModel(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val setUserDisabledUseCase: SetUserDisabledUseCase,
    private val deleteUserCascadeUseCase: DeleteUserCascadeUseCase,
    private val getBillingHistoryUseCase: GetBillingHistoryUseCase,
    private val adminSetSubscriptionUseCase: AdminSetSubscriptionUseCase,
    private val adminCancelSubscriptionUseCase: AdminCancelSubscriptionUseCase,
    private val mindMingleRemoteRepository: MindMingleRemoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUserDetailUiState())
    val uiState: StateFlow<AdminUserDetailUiState> = _uiState.asStateFlow()

    fun loadUser(uid: String) {
        _uiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            val user = getUserProfileUseCase(uid)
            _uiState.update { it.copy(isLoading = false, user = user) }
        }
        loadBilling(uid)
    }

    fun loadBilling(uid: String) {
        _uiState.update { it.copy(isLoadingBilling = true) }
        viewModelScope.launch {
            getBillingHistoryUseCase(uid).fold(
                onSuccess = { history ->
                    _uiState.update { it.copy(isLoadingBilling = false, billing = history) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoadingBilling = false, error = e.message ?: "Could not load billing")
                    }
                }
            )
        }
    }

    /** Comps a plan without a payment — the grant shows up in the user's own order history. */
    fun grantPlan(plan: PremiumPlan, days: Int) {
        val uid = _uiState.value.user?.uid ?: return
        _uiState.update { it.copy(isSubscriptionMutating = true, error = "", subscriptionMessage = "") }
        viewModelScope.launch {
            adminSetSubscriptionUseCase(uid = uid, plan = plan, days = days).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isSubscriptionMutating = false, subscriptionMessage = "Granted ${plan.label}")
                    }
                    loadBilling(uid)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isSubscriptionMutating = false, error = e.message ?: "Grant failed")
                    }
                }
            )
        }
    }

    /** [immediate] ends access now (ads come back straight away) instead of at period end. */
    fun cancelPlan(immediate: Boolean) {
        val uid = _uiState.value.user?.uid ?: return
        _uiState.update { it.copy(isSubscriptionMutating = true, error = "", subscriptionMessage = "") }
        viewModelScope.launch {
            adminCancelSubscriptionUseCase(uid = uid, immediate = immediate).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubscriptionMutating = false,
                            subscriptionMessage = if (immediate) "Plan ended now" else "Plan ends at period end"
                        )
                    }
                    loadBilling(uid)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isSubscriptionMutating = false, error = e.message ?: "Cancel failed")
                    }
                }
            )
        }
    }

    fun toggleDisabled() {
        val user = _uiState.value.user ?: return
        val nextDisabled = !user.isDisabled
        _uiState.update { it.copy(isMutating = true, error = "") }
        viewModelScope.launch {
            setUserDisabledUseCase(user.uid, nextDisabled)
                .onSuccess {
                    _uiState.update { it.copy(isMutating = false, user = user.copy(isDisabled = nextDisabled)) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isMutating = false, error = e.message ?: "Update failed") }
                }
        }
    }

    fun deleteUser() {
        val user = _uiState.value.user ?: return
        val adminUid = mindMingleRemoteRepository.getCurrentUid()
        if (adminUid == null) {
            _uiState.update { it.copy(error = "No admin session — sign in again") }
            return
        }
        _uiState.update { it.copy(isMutating = true, error = "") }
        viewModelScope.launch {
            deleteUserCascadeUseCase(user.uid, adminUid)
                .onSuccess {
                    _uiState.update { it.copy(isMutating = false, isDeleted = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isMutating = false, error = e.message ?: "Delete failed") }
                }
        }
    }
}
