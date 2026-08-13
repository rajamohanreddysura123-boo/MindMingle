package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class AnonymousQueueDto(
    val uid: String = "",
    val sessionId: String = "",
    val joinedAt: BaseTimestamp = Timestamp.ServerTimestamp
)
