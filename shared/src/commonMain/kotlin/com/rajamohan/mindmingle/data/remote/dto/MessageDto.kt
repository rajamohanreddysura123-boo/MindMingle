package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/**
 * One chat message. Messages do not persist: the recipient's client deletes them once it has shown
 * them, and anything never opened is swept by the Firestore TTL policy on [expiresAt].
 *
 * The TTL policy is configured once per project against the `messages` collection group; Firestore
 * does the deleting server-side, so no Cloud Function or scheduler is involved. It fires within
 * roughly a day *after* the timestamp rather than on the second, which is why the client also hides
 * anything already past its expiry — the visible behaviour stays exact even when the sweep lags.
 */
@Serializable
data class MessageDto(
    val senderId: String = "",
    val text: String = "",
    val sentAt: BaseTimestamp = Timestamp.ServerTimestamp,
    /** When this becomes eligible for the TTL sweep. Null on messages written before TTL existed. */
    val expiresAt: BaseTimestamp? = null
)
