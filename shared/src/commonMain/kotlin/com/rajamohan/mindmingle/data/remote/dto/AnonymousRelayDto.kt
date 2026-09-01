package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/**
 * Transport only: the recipient deletes each of these the moment it has been read. [expiresAt] is
 * the backstop for a message nobody ever collected, so undelivered text cannot sit in Firestore
 * indefinitely.
 */
@Serializable
data class AnonymousRelayDto(
    val senderId: String = "",
    val text: String = "",
    val sentAt: BaseTimestamp = Timestamp.ServerTimestamp,
    val expiresAt: BaseTimestamp? = null
)
