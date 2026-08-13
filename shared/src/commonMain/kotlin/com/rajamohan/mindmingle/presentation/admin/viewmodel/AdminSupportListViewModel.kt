package com.rajamohan.mindmingle.presentation.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.SupportThread
import com.rajamohan.mindmingle.domain.usecase.ObserveSupportThreadsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AdminSupportListUiState(
    val isLoading: Boolean = true,
    val threads: List<SupportThread> = emptyList()
)

internal class AdminSupportListViewModel(
    private val observeSupportThreadsUseCase: ObserveSupportThreadsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminSupportListUiState())
    val uiState: StateFlow<AdminSupportListUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            observeSupportThreadsUseCase().collect { threads ->
                _uiState.update {
                    it.copy(isLoading = false, threads = threads.sortedByDescending { thread -> thread.lastMessageAtSeconds })
                }
            }
        }
    }
}
