package com.rajamohan.mindmingle.domain.model

data class AccountStatus(
    val isBlocked: Boolean,
    val message: String = "",
    /** True when a pending deletion or deletion-ban was cleared — the caller should route to ProfileSetup so the user rebuilds from scratch. */
    val wasReset: Boolean = false
)
