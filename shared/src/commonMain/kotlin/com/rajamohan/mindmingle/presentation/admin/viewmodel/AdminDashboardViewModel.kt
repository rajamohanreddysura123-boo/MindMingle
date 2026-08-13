package com.rajamohan.mindmingle.presentation.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.AdminStats
import com.rajamohan.mindmingle.domain.usecase.GetAdminStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val stats: AdminStats = AdminStats(0, 0, 0)
)

class AdminDashboardViewModel(
    private val getAdminStatsUseCase: GetAdminStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    fun loadStats() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val stats = getAdminStatsUseCase()
            _uiState.update { it.copy(isLoading = false, stats = stats) }
        }
    }
}
