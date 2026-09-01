package com.rajamohan.mindmingle.presentation.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.CountryCode
import com.rajamohan.mindmingle.domain.model.CountryCodeRepository
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.model.UserCountryResolver
import com.rajamohan.mindmingle.domain.usecase.ListAllUsersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUserListUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val allUsers: List<User> = emptyList(),
    /** False once a short page comes back — there is nothing left to fetch. */
    val hasMore: Boolean = true,
    val query: String = "",
    /** ISO code to filter by; blank shows every country. */
    val country: String = "",
    /** The dial-code and name table country resolution needs. Empty until it loads. */
    val countries: List<CountryCode> = emptyList(),
    /** uid to ISO code, worked out once per page rather than per recomposition. */
    val countryByUid: Map<String, String> = emptyMap()
) {
    /** Searches the pages loaded so far, not the whole collection — see [AdminUserListViewModel]. */
    val filteredUsers: List<User>
        get() {
            val q = query.trim().lowercase()
            return allUsers.filter { user ->
                val matchesQuery = q.isBlank() ||
                    user.name.lowercase().contains(q) ||
                    user.email.lowercase().contains(q) ||
                    user.occupation.lowercase().contains(q)

                val matchesCountry = country.isBlank() || countryByUid[user.uid] == country

                matchesQuery && matchesCountry
            }
        }

    /** Countries actually present in the loaded pages, so the filter row has no dead options. */
    val availableCountries: List<String>
        get() = countryByUid.values.filter { it.isNotBlank() }.distinct().sorted()

    fun countryLabelFor(uid: String): String =
        UserCountryResolver.label(countryByUid[uid], countries)
}

/**
 * Users arrive a page at a time. The old version read the entire `users` collection on every
 * open, so the screen cost one document read per user in the app whether or not anyone scrolled.
 *
 * The trade-off is that search and the country filter only cover what has been loaded; a
 * full-collection search needs a server-side index rather than a client scan. The subscriber
 * screen does not share this limitation because its filtering happens in a Cloud Function.
 *
 * Country is derived rather than stored — see [UserCountryResolver] for why, and from what.
 */
class AdminUserListViewModel(
    private val listAllUsersUseCase: ListAllUsersUseCase
) : ViewModel() {

    private companion object {
        const val PAGE_SIZE = 50
    }

    private val _uiState = MutableStateFlow(AdminUserListUiState())
    val uiState: StateFlow<AdminUserListUiState> = _uiState.asStateFlow()

    fun loadUsers() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // The country table is bundled with the app, so this is a resource read rather than
            // a network call — but it still has to finish before uids can be mapped to codes.
            val countries = _uiState.value.countries.ifEmpty { CountryCodeRepository.getCountryCodes() }
            val users = listAllUsersUseCase(pageSize = PAGE_SIZE)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    allUsers = users,
                    hasMore = users.size == PAGE_SIZE,
                    countries = countries,
                    countryByUid = resolveCountries(users, countries)
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        val cursor = state.allUsers.lastOrNull()?.uid ?: return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val page = listAllUsersUseCase(pageSize = PAGE_SIZE, startAfterUid = cursor)
            _uiState.update {
                it.copy(
                    isLoadingMore = false,
                    allUsers = it.allUsers + page,
                    hasMore = page.size == PAGE_SIZE,
                    countryByUid = it.countryByUid + resolveCountries(page, it.countries)
                )
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onCountryChanged(country: String) {
        _uiState.update { it.copy(country = country) }
    }

    private fun resolveCountries(users: List<User>, countries: List<CountryCode>): Map<String, String> {
        if (countries.isEmpty()) return emptyMap()
        return users.mapNotNull { user ->
            UserCountryResolver.resolve(user, countries)?.let { code -> user.uid to code }
        }.toMap()
    }
}
