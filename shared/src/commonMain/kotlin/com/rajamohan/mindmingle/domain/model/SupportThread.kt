package com.rajamohan.mindmingle.domain.model

/** One user's support conversation with the MindMingle team — admin-desktop-only inbox row. */
data class SupportThread(
    val uid: String,
    val userName: String,
    val lastMessage: String,
    val lastMessageAtSeconds: Long
)
