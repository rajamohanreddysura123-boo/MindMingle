package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.serialization.Serializable

@Serializable
data class MatchDto(
    val users: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageAt: BaseTimestamp? = null
)
