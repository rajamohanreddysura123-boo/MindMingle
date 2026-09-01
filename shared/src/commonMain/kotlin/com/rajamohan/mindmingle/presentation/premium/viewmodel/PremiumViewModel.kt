package com.rajamohan.mindmingle.presentation.premium.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.core.payments.CheckoutRequest
import com.rajamohan.mindmingle.core.payments.CheckoutResult
import com.rajamohan.mindmingle.core.payments.PaymentPlatform
import com.rajamohan.mindmingle.domain.model.PaymentOrder
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.usecase.CreatePaymentOrderUseCase
import com.rajamohan.mindmingle.domain.usecase.GetPlanCatalogUseCase
import com.rajamohan.mindmingle.domain.usecase.GetUserProfileUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveSubscriptionUseCase
import com.rajamohan.mindmingle.domain.usecase.RecordPaymentFailureUseCase
import com.rajamohan.mindmingle.domain.usecase.VerifyPaymentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PremiumViewModel(
    private val createPaymentOrderUseCase: CreatePaymentOrderUseCase,
    private val verifyPaymentUseCase: VerifyPaymentUseCase,
    private val recordPaymentFailureUseCase: RecordPaymentFailureUseCase,
    private val observeSubscriptionUseCase: ObserveSubscriptionUseCase,
    private val getPlanCatalogUseCase: GetPlanCatalogUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private var uid: String = ""
    private var user: User? = null
    private var subscriptionJob: Job? = null

    fun onEvent(event: PremiumEvent) {
        when (event) {
            is PremiumEvent.Load -> load(event.uid)
            is PremiumEvent.SelectPlan -> _uiState.update { it.copy(selectedPlan = event.plan) }
            is PremiumEvent.Checkout -> checkout()
            is PremiumEvent.DismissError -> _uiState.update { it.copy(error = "") }
        }
    }

    private fun load(uid: String) {
        if (uid.isBlank() || this.uid == uid) return
        this.uid = uid

        subscriptionJob?.cancel()
        subscriptionJob = viewModelScope.launch {
            observeSubscriptionUseCase(uid).collect { subscription ->
                _uiState.update { it.copy(subscription = subscription) }
            }
        }

        viewModelScope.launch {
            user = getUserProfileUseCase(uid)
        }

        loadPricing()
    }

    private fun loadPricing() {
        _uiState.update { it.copy(isLoadingPricing = true) }
        viewModelScope.launch {
            getPlanCatalogUseCase().fold(
                onSuccess = { catalog ->
                    _uiState.update { it.copy(isLoadingPricing = false, catalog = catalog) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingPricing = false,
                            error = error.message ?: "Could not load pricing"
                        )
                    }
                }
            )
        }
    }

    private fun checkout() {
        val state = _uiState.value
        if (!state.canCheckout) return

        if (!PaymentPlatform.isSupported) {
            _uiState.update { it.copy(error = "Upgrade to MindMingle+ from the Android or iOS app") }
            return
        }

        if (state.pricing?.isValid != true) {
            _uiState.update { it.copy(error = "No price is configured for your country yet") }
            return
        }

        _uiState.update { it.copy(isProcessing = true, error = "") }

        viewModelScope.launch {
            val order = createPaymentOrderUseCase(state.selectedPlan).getOrElse { error ->
                _uiState.update {
                    it.copy(isProcessing = false, error = error.message ?: "Could not start checkout")
                }
                return@launch
            }

            when (val result = PaymentPlatform.startCheckout(order.toCheckoutRequest())) {
                is CheckoutResult.Success -> verify(result)
                // A cancel is a decision, not a failure — nothing was attempted, so nothing is
                // recorded and the user is not emailed about it.
                is CheckoutResult.Cancelled -> _uiState.update { it.copy(isProcessing = false) }
                is CheckoutResult.Failed -> {
                    _uiState.update { it.copy(isProcessing = false, error = result.message) }
                    recordPaymentFailureUseCase(
                        orderId = order.orderId,
                        planId = state.selectedPlan.id,
                        reason = result.message
                    )
                }
            }
        }
    }

    private suspend fun verify(result: CheckoutResult.Success) {
        verifyPaymentUseCase(
            orderId = result.orderId,
            paymentId = result.paymentId,
            signature = result.signature
        ).fold(
            onSuccess = { subscription ->
                _uiState.update {
                    it.copy(isProcessing = false, justUpgraded = true, subscription = subscription)
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = error.message
                            ?: "Payment received — activation is taking a moment. Reopen this screen shortly."
                    )
                }
            }
        )
    }

    private fun PaymentOrder.toCheckoutRequest(): CheckoutRequest {
        val profile = user
        return CheckoutRequest(
            keyId = keyId,
            orderId = orderId,
            amountMinor = amountMinor,
            currency = currency,
            planLabel = _uiState.value.selectedPlan.label,
            userName = profile?.name.orEmpty(),
            userEmail = profile?.email.orEmpty(),
            userPhone = profile?.phoneNumber.orEmpty()
        )
    }

    override fun onCleared() {
        subscriptionJob?.cancel()
        super.onCleared()
    }
}
