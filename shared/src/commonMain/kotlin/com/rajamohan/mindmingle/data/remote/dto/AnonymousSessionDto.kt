package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

/**
 * A live anonymous room. The clients purge it when either side leaves; [expiresAt] is the backstop
 * for when neither client got the chance — an app killed mid-conversation would otherwise leave the
 * room and its relayed messages in Firestore for good.
 */
@Serializable
data class AnonymousSessionDto(
    val participants: List<String> = emptyList(),
    val openedBy: String = "",
    val endedBy: String = "",
    val createdAt: BaseTimestamp = Timestamp.ServerTimestamp,
    val expiresAt: BaseTimestamp? = null
)
