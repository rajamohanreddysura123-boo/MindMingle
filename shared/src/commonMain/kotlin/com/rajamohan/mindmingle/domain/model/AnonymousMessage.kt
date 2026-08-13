package com.rajamohan.mindmingle.domain.model

data class AnonymousMessage(
    val id: String,
    val sessionId: String,
    val text: String,
    val isMine: Boolean,
    val sequence: Long,
    val isSeen: Boolean
)
