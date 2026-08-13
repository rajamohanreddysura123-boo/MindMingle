package com.rajamohan.mindmingle.presentation.chat.viewmodel

import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.model.ChatMessage

internal sealed class ChatEvent {
    data class LoadConversations(val uid: String) : ChatEvent()
    data class OpenConversation(val matchId: String) : ChatEvent()
    data object CloseConversation : ChatEvent()
    data class SendMessage(val matchId: String, val senderId: String, val text: String) : ChatEvent()
}

internal data class ChatUiState(
    val isLoadingConversations: Boolean = false,
    val conversations: List<ChatConversation> = emptyList(),
    val activeConversation: ChatConversation? = null,
    val messages: List<ChatMessage> = emptyList()
)
