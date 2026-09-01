package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/**
 * One person waiting to be paired. [expiresAt] is swept by the Firestore TTL policy: a client that
 * is killed while waiting leaves its slot behind otherwise, and every other waiter then pays to
 * read a ghost that will never answer.
 */
@Serializable
data class AnonymousQueueDto(
    val uid: String = "",
    val sessionId: String = "",
    val joinedAt: BaseTimestamp = Timestamp.ServerTimestamp,
    val expiresAt: BaseTimestamp? = null
)
