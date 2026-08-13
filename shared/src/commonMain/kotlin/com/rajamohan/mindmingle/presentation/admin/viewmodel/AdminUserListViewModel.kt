package com.rajamohan.mindmingle.presentation.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.usecase.ListAllUsersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUserListUiState(
    val isLoading: Boolean = false,
    val allUsers: List<User> = emptyList(),
    val query: String = ""
) {
    val filteredUsers: List<User>
        get() {
            if (query.isBlank()) return allUsers
            val q = query.trim().lowercase()
            return allUsers.filter { user ->
                user.name.lowercase().contains(q) ||
                    user.email.lowercase().contains(q) ||
                    user.occupation.lowercase().contains(q)
            }
        }
}

class AdminUserListViewModel(
    private val listAllUsersUseCase: ListAllUsersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUserListUiState())
    val uiState: StateFlow<AdminUserListUiState> = _uiState.asStateFlow()

    fun loadUsers() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val users = listAllUsersUseCase()
            _uiState.update { it.copy(isLoading = false, allUsers = users) }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }
}
