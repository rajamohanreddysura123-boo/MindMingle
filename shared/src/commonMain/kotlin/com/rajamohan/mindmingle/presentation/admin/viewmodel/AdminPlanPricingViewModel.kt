package com.rajamohan.mindmingle.presentation.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.CountryPricing
import com.rajamohan.mindmingle.domain.model.PlanCatalog
import com.rajamohan.mindmingle.domain.model.formatMinorAmount
import com.rajamohan.mindmingle.domain.usecase.GetPlanCatalogUseCase
import com.rajamohan.mindmingle.domain.usecase.ResetPlanCatalogUseCase
import com.rajamohan.mindmingle.domain.usecase.SavePlanCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PricingRow(
    val countryCode: String,
    val currency: String,
    val symbol: String,
    val decimals: Int,
    val dialCode: String,
    val monthlyInput: String,
    val annualInput: String
) {
    val monthlyMinor: Long? get() = parseMajorToMinor(monthlyInput, decimals)
    val annualMinor: Long? get() = parseMajorToMinor(annualInput, decimals)
    val isValid: Boolean get() = (monthlyMinor ?: 0L) > 0L && (annualMinor ?: 0L) > 0L

    fun toPricing(): CountryPricing = CountryPricing(
        countryCode = countryCode,
        currency = currency,
        symbol = symbol,
        decimals = decimals,
        dialCode = dialCode,
        monthly = monthlyMinor ?: 0L,
        annual = annualMinor ?: 0L
    )
}

internal data class AdminPlanPricingUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val rows: List<PricingRow> = emptyList(),
    val query: String = "",
    val enabled: Boolean = true,
    val defaultCountry: String = "US",
    val message: String = "",
    val error: String = ""
) {
    val filteredRows: List<PricingRow>
        get() {
            if (query.isBlank()) return rows
            val needle = query.trim().uppercase()
            return rows.filter {
                it.countryCode.contains(needle) || it.currency.contains(needle)
            }
        }

    val hasInvalidRow: Boolean get() = rows.any { !it.isValid }

    val canSave: Boolean get() = !isSaving && rows.isNotEmpty() && !hasInvalidRow
}

/**
 * Prices are edited in whole currency units (99, 2.99) and stored in the smallest unit
 * (9900, 299), which is what Razorpay charges in.
 */
internal fun parseMajorToMinor(input: String, decimals: Int): Long? {
    val trimmed = input.trim().replace(",", "")
    if (trimmed.isEmpty()) return null

    val parts = trimmed.split(".")
    if (parts.size > 2) return null

    val whole = parts[0].ifEmpty { "0" }
    if (!whole.all { it.isDigit() }) return null

    val fractionInput = parts.getOrNull(1).orEmpty()
    if (!fractionInput.all { it.isDigit() }) return null
    if (fractionInput.length > decimals) return null

    var multiplier = 1L
    repeat(decimals) { multiplier *= 10 }

    val fraction = fractionInput.padEnd(decimals, '0').ifEmpty { "0" }
    val wholeValue = whole.toLongOrNull() ?: return null
    val fractionValue = if (decimals == 0) 0L else fraction.toLongOrNull() ?: return null

    return wholeValue * multiplier + fractionValue
}

internal fun minorToMajorInput(amount: Long, decimals: Int): String {
    val formatted = formatMinorAmount(amount, decimals, symbol = "")
    return formatted
}

internal class AdminPlanPricingViewModel(
    private val getPlanCatalogUseCase: GetPlanCatalogUseCase,
    private val savePlanCatalogUseCase: SavePlanCatalogUseCase,
    private val resetPlanCatalogUseCase: ResetPlanCatalogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminPlanPricingUiState())
    val uiState: StateFlow<AdminPlanPricingUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = "", message = "") }
        viewModelScope.launch {
            getPlanCatalogUseCase().fold(
                onSuccess = { catalog -> applyCatalog(catalog) },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Could not load pricing")
                    }
                }
            )
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onMonthlyChanged(countryCode: String, value: String) {
        updateRow(countryCode) { it.copy(monthlyInput = value) }
    }

    fun onAnnualChanged(countryCode: String, value: String) {
        updateRow(countryCode) { it.copy(annualInput = value) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) {
            _uiState.update { it.copy(error = "Fix the highlighted prices first") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = "", message = "") }
        viewModelScope.launch {
            val catalog = PlanCatalog(
                enabled = state.enabled,
                defaultCountry = state.defaultCountry,
                countries = state.rows.associate { row -> row.countryCode to row.toPricing() },
                resolvedCountry = state.defaultCountry
            )

            savePlanCatalogUseCase(catalog).fold(
                onSuccess = { saved ->
                    _uiState.update {
                        it.copy(isSaving = false, message = "Saved $saved countries")
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isSaving = false, error = error.message ?: "Could not save pricing")
                    }
                }
            )
        }
    }

    fun restoreDefaults() {
        _uiState.update { it.copy(isSaving = true, error = "", message = "") }
        viewModelScope.launch {
            resetPlanCatalogUseCase().fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, message = "Restored default pricing") }
                    load()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isSaving = false, error = error.message ?: "Could not restore pricing")
                    }
                }
            )
        }
    }

    private fun applyCatalog(catalog: PlanCatalog) {
        val rows = catalog.sortedCountries.map { pricing ->
            PricingRow(
                countryCode = pricing.countryCode,
                currency = pricing.currency,
                symbol = pricing.symbol,
                decimals = pricing.decimals,
                dialCode = pricing.dialCode,
                monthlyInput = minorToMajorInput(pricing.monthly, pricing.decimals),
                annualInput = minorToMajorInput(pricing.annual, pricing.decimals)
            )
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                rows = rows,
                enabled = catalog.enabled,
                defaultCountry = catalog.defaultCountry
            )
        }
    }

    private fun updateRow(countryCode: String, transform: (PricingRow) -> PricingRow) {
        _uiState.update { state ->
            state.copy(
                message = "",
                rows = state.rows.map { row ->
                    if (row.countryCode == countryCode) transform(row) else row
                }
            )
        }
    }
}
