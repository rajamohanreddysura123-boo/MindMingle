package com.rajamohan.mindmingle.presentation.likes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.usecase.ObserveIncomingLikesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class LikesUiState(
    val isLoading: Boolean = false,
    val likedByUsers: List<User> = emptyList()
)

internal class LikesViewModel(
    private val observeIncomingLikesUseCase: ObserveIncomingLikesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LikesUiState())
    val uiState: StateFlow<LikesUiState> = _uiState.asStateFlow()

    private var likesJob: Job? = null

    fun loadLikes(uid: String) {
        _uiState.update { it.copy(isLoading = true) }
        likesJob?.cancel()
        likesJob = viewModelScope.launch {
            observeIncomingLikesUseCase(uid).collect { users ->
                _uiState.update { it.copy(isLoading = false, likedByUsers = users) }
            }
        }
    }

    override fun onCleared() {
        likesJob?.cancel()
        super.onCleared()
    }
}
