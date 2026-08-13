package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class BannedUidDto(
    val bannedAt: BaseTimestamp = Timestamp.ServerTimestamp,
    val reason: String = "",
    val bannedBy: String = ""
)
