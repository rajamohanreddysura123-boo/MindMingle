package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class AnonymousSessionDto(
    val participants: List<String> = emptyList(),
    val openedBy: String = "",
    val endedBy: String = "",
    val createdAt: BaseTimestamp = Timestamp.ServerTimestamp
)
