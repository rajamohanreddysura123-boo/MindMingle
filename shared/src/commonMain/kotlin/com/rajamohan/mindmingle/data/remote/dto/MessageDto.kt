package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val senderId: String = "",
    val text: String = "",
    val sentAt: BaseTimestamp = Timestamp.ServerTimestamp
)
