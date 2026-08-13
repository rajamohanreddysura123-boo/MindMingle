package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.serialization.Serializable

@Serializable
data class SupportThreadDto(
    val uid: String = "",
    val userName: String = "",
    val lastMessage: String = "",
    val lastMessageAt: BaseTimestamp? = null
)
