package com.rajamohan.mindmingle.presentation.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.model.Subscriber
import com.rajamohan.mindmingle.domain.model.SubscriberStats
import com.rajamohan.mindmingle.domain.model.SubscriberStatusFilter
import com.rajamohan.mindmingle.domain.usecase.GetSubscriberStatsUseCase
import com.rajamohan.mindmingle.domain.usecase.ListSubscribersUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminSubscriberListUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val subscribers: List<Subscriber> = emptyList(),
    val stats: SubscriberStats = SubscriberStats(),
    val status: SubscriberStatusFilter = SubscriberStatusFilter.ALL,
    val planId: String = "",
    val country: String = "",
    val query: String = "",
    /** Blank once the scan reaches the end of the collection. */
    val cursor: String = "",
    val error: String = ""
) {
    val hasMore: Boolean get() = cursor.isNotBlank()

    val isEmpty: Boolean get() = !isLoading && subscribers.isEmpty()

    /** Countries present in what has loaded so far — enough to drive a filter chip row. */
    val loadedCountries: List<String>
        get() = subscribers.map { it.billingCountry }.filter { it.isNotBlank() }.distinct().sorted()

    val hasFilters: Boolean
        get() = status != SubscriberStatusFilter.ALL ||
            planId.isNotBlank() ||
            country.isNotBlank() ||
            query.isNotBlank()
}

/**
 * Current and past subscribers, paged from the server.
 *
 * Every filter is applied by the Cloud Function rather than here. That matters for search: the
 * admin user list can only search what it has already loaded, because it pages a raw collection
 * client-side. This screen does not have that limitation — a name typed here is matched against
 * the whole collection as the server scans it, so a subscriber on page nine is still findable
 * without loading pages one through eight.
 *
 * Changing any filter throws away the loaded pages and starts a fresh scan, because the cursor
 * only means anything for the filter set it was produced with.
 */
class AdminSubscriberListViewModel(
    private val listSubscribersUseCase: ListSubscribersUseCase,
    private val getSubscriberStatsUseCase: GetSubscriberStatsUseCase
) : ViewModel() {

    private companion object {
        const val PAGE_SIZE = 25

        /** Typing a name should not fire a call per keystroke. */
        const val SEARCH_DEBOUNCE_MS = 350L
    }

    private val _uiState = MutableStateFlow(AdminSubscriberListUiState())
    val uiState: StateFlow<AdminSubscriberListUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun load() {
        loadStats()
        reload()
    }

    private fun loadStats() {
        viewModelScope.launch {
            getSubscriberStatsUseCase().onSuccess { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
    }

    /** Fresh scan from the top with the current filters. */
    fun reload() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, error = "", subscribers = emptyList(), cursor = "") }

        viewModelScope.launch {
            listSubscribersUseCase(
                status = state.status,
                planId = state.planId,
                country = state.country,
                query = state.query,
                pageSize = PAGE_SIZE
            ).fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(isLoading = false, subscribers = page.subscribers, cursor = page.cursor)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Could not load subscribers")
                    }
                }
            )
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            listSubscribersUseCase(
                status = state.status,
                planId = state.planId,
                country = state.country,
                query = state.query,
                pageSize = PAGE_SIZE,
                cursor = state.cursor
            ).fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            // The server may return a short page and still have more to scan —
                            // the filters can reject an entire slice — so the cursor decides
                            // whether there is more, never the page size.
                            subscribers = it.subscribers + page.subscribers,
                            cursor = page.cursor
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoadingMore = false, error = error.message ?: "Could not load more")
                    }
                }
            )
        }
    }

    fun onStatusChanged(status: SubscriberStatusFilter) {
        if (_uiState.value.status == status) return
        _uiState.update { it.copy(status = status) }
        reload()
    }

    fun onPlanChanged(plan: PremiumPlan?) {
        val planId = plan?.id.orEmpty()
        if (_uiState.value.planId == planId) return
        _uiState.update { it.copy(planId = planId) }
        reload()
    }

    fun onCountryChanged(country: String) {
        if (_uiState.value.country == country) return
        _uiState.update { it.copy(country = country) }
        reload()
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            reload()
        }
    }

    fun clearFilters() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                status = SubscriberStatusFilter.ALL,
                planId = "",
                country = "",
                query = ""
            )
        }
        reload()
    }
}
