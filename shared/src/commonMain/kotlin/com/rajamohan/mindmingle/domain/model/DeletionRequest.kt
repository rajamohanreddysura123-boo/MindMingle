package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.data.remote.dto.DeletionRequestDto

data class DeletionRequest(
    val uid: String,
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val requestedAt: Long = 0L,
    val status: String = DeletionRequestDto.STATUS_PENDING,
    val deletedAt: Long = 0L,
    val deletedBy: String = ""
) {
    val isPending: Boolean get() = status == DeletionRequestDto.STATUS_PENDING

    val requestedLabel: String get() = if (requestedAt > 0L) formatUtcDate(requestedAt) else "—"

    /** Whole days the request has been sitting in the queue — the admin list sorts on this. */
    fun waitingDaysAt(nowMillis: Long): Int {
        if (requestedAt <= 0L) return 0
        val elapsed = nowMillis - requestedAt
        return if (elapsed <= 0L) 0 else (elapsed / MILLIS_PER_DAY).toInt()
    }

    private companion object {
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}

fun DeletionRequestDto.toDomain(): DeletionRequest = DeletionRequest(
    uid = uid,
    name = name,
    email = email,
    phoneNumber = phoneNumber,
    requestedAt = requestedAt,
    status = status,
    deletedAt = deletedAt,
    deletedBy = deletedBy
)
