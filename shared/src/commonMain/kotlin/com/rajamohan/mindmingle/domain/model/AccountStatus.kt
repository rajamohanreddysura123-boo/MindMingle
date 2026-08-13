package com.rajamohan.mindmingle.domain.model

data class AccountStatus(
    val isBlocked: Boolean,
    val message: String = ""
)
