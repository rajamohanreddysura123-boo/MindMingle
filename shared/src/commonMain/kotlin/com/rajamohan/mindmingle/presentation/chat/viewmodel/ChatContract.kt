package com.rajamohan.mindmingle.presentation.chat.viewmodel

import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.model.ChatMessage

internal sealed class ChatEvent {
    data class LoadConversations(val uid: String) : ChatEvent()
    /** Next page of conversations — the list is paged, not fetched whole. */
    data object LoadMoreConversations : ChatEvent()
    data class SearchChanged(val query: String) : ChatEvent()
    data class OpenConversation(val conversationId: String) : ChatEvent()
    data object CloseConversation : ChatEvent()
    data class SendMessage(val conversationId: String, val senderId: String, val text: String) : ChatEvent()
}

internal data class ChatUiState(
    val uid: String = "",
    val isLoadingConversations: Boolean = false,
    val isLoadingMore: Boolean = false,
    val conversations: List<ChatConversation> = emptyList(),
    /** False once a short page comes back — nothing left to fetch. */
    val hasMore: Boolean = true,
    val query: String = "",
    val activeConversation: ChatConversation? = null,
    val messages: List<ChatMessage> = emptyList()
) {
    /** Searches the pages loaded so far, by the name copied onto the match. */
    val visibleConversations: List<ChatConversation>
        get() {
            if (query.isBlank()) return conversations
            val needle = query.trim().lowercase()
            return conversations.filter { it.displayName.lowercase().contains(needle) }
        }
}
