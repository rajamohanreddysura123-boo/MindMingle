package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * One doc per user who asked for their account to be removed (deletionRequests/{uid}).
 *
 * The user's own client writes this and locks itself out straight away; the account data itself
 * survives until an admin works the queue and runs the purge, because a client SDK cannot reach
 * another person's records.
 */
@Serializable
data class DeletionRequestDto(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val requestedAt: Long = 0L,
    val status: String = STATUS_PENDING,
    val deletedAt: Long = 0L,
    val deletedBy: String = ""
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_DELETED = "deleted"
    }
}
