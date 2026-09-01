package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenDto(
    val token: String = "",
    val platform: String = "",
    val updatedAt: Long = 0L
)

@Serializable
data class NotificationPrefsDto(
    val messages: Boolean = true,
    val likes: Boolean = true,
    val payments: Boolean = true,
    val support: Boolean = true
)
