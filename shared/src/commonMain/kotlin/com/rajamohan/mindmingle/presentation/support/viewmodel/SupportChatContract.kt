package com.rajamohan.mindmingle.presentation.support.viewmodel

import com.rajamohan.mindmingle.domain.model.ChatMessage

/** Shared by the user-facing Help & Support screen and the admin-desktop reply screen — both are just "observe + send" on one uid's thread. */
internal sealed class SupportChatEvent {
    data class Open(val uid: String) : SupportChatEvent()
    data class Send(val uid: String, val userName: String, val senderId: String, val text: String) : SupportChatEvent()
}

internal data class SupportChatUiState(
    val messages: List<ChatMessage> = emptyList()
)
