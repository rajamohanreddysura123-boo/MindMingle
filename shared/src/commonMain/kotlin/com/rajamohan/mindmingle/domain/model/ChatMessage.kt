package com.rajamohan.mindmingle.domain.model

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val sentAtSeconds: Long
)
