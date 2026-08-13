package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class AnonymousAuditDto(
    val sessionId: String = "",
    val participants: List<String> = emptyList(),
    val openedBy: String = "",
    val closedBy: String = "",
    val closeReason: String = "",
    val recordedAt: BaseTimestamp = Timestamp.ServerTimestamp
)
