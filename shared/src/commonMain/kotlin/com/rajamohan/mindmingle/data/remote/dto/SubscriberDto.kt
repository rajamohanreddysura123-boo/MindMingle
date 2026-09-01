package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * One row of the admin subscriber list, as `adminListSubscribers` returns it — plan state from
 * `subscriptions/{uid}` already joined with the name and email from `users/{uid}`.
 */
@Serializable
data class SubscriberDto(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val planId: String = "",
    val status: String = "",
    val currentPeriodEnd: Long = 0L,
    val billingCountry: String = "",
    val billingCurrency: String = "",
    val lastPaymentId: String = "",
    val lastInvoiceNumber: String = "",
    val updatedAt: Long = 0L,
    val isActive: Boolean = false,
    val isPaid: Boolean = false
)

@Serializable
data class SubscriberListRequestDto(
    val status: String = "all",
    val planId: String = "",
    val country: String = "",
    val query: String = "",
    val pageSize: Int = 25,
    /** Blank starts from the beginning; otherwise the `cursor` from the previous response. */
    val cursor: String = ""
)

@Serializable
data class SubscriberListResponseDto(
    val subscribers: List<SubscriberDto> = emptyList(),
    /** Blank means the scan reached the end of the collection. */
    val cursor: String = "",
    val scanned: Int = 0
)

/** `adminSubscriberStats` takes no arguments; the callable API still wants a payload to encode. */
@Serializable
class SubscriberStatsRequestDto

@Serializable
data class SubscriberStatsDto(
    val total: Int = 0,
    val active: Int = 0,
    val expired: Int = 0,
    val cancelled: Int = 0,
    val revoked: Int = 0
)
