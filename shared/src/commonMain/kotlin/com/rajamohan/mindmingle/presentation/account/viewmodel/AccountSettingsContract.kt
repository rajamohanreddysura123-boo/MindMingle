package com.rajamohan.mindmingle.presentation.account.viewmodel

import com.rajamohan.mindmingle.domain.model.NotificationPrefs
import com.rajamohan.mindmingle.domain.model.formatUtcDate
import com.rajamohan.mindmingle.domain.model.nowMillis
import com.rajamohan.mindmingle.domain.usecase.DeactivateMyAccountUseCase

internal data class AccountSettingsUiState(
    /** Per-category push switches. Defaults to all-on, which is what a missing prefs doc means. */
    val notificationPrefs: NotificationPrefs = NotificationPrefs(),
    val isLoadingPrefs: Boolean = true,
    val prefsError: String = "",
    val selectedDays: Int = DeactivateMyAccountUseCase.ALLOWED_DAYS.first(),
    val isDeactivating: Boolean = false,
    val isDeactivated: Boolean = false,
    val deactivateError: String = "",
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: String = ""
) {
    val dayOptions: List<Int> get() = DeactivateMyAccountUseCase.ALLOWED_DAYS

    val isBusy: Boolean get() = isDeactivating || isDeleting

    /** The date the account comes back if the window were started right now. */
    val returnDateLabel: String
        get() = formatUtcDate(nowMillis() + selectedDays * MILLIS_PER_DAY)

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
