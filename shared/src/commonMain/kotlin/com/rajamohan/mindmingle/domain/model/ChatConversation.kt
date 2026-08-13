package com.rajamohan.mindmingle.domain.model

data class ChatConversation(
    val matchId: String,
    val otherUser: User,
    val lastMessage: String,
    val lastMessageAtSeconds: Long
)
