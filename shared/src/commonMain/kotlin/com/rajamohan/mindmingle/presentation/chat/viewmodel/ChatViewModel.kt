package com.rajamohan.mindmingle.presentation.chat.viewmodel

import androidx.lifecycle.ViewModel
import com.rajamohan.mindmingle.core.push.OpenConversation
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.usecase.DeleteSeenMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.GetConversationsUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ChatViewModel(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val deleteSeenMessagesUseCase: DeleteSeenMessagesUseCase
) : ViewModel() {

    private companion object {
        const val PAGE_SIZE = 30
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    /**
     * Ids of the other person's messages shown in the open conversation. They are deleted when the
     * conversation closes rather than the instant they render, so a message is not destroyed out
     * from under someone who is still reading it.
     */
    private var seenMessageIds: MutableSet<String> = mutableSetOf()

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.LoadConversations -> loadConversations(event.uid)
            is ChatEvent.LoadMoreConversations -> loadMoreConversations()
            is ChatEvent.SearchChanged -> _uiState.update { it.copy(query = event.query) }
            is ChatEvent.OpenConversation -> openConversation(event.conversationId)
            is ChatEvent.CloseConversation -> closeConversation()
            is ChatEvent.SendMessage -> sendMessage(event.conversationId, event.senderId, event.text)
        }
    }

    private fun loadConversations(uid: String) {
        _uiState.update { it.copy(uid = uid, isLoadingConversations = true) }
        viewModelScope.launch {
            val page = getConversationsUseCase(uid = uid, pageSize = PAGE_SIZE)
            _uiState.update {
                it.copy(
                    isLoadingConversations = false,
                    conversations = page,
                    hasMore = page.size == PAGE_SIZE
                )
            }
        }
    }

    private fun loadMoreConversations() {
        val state = _uiState.value
        if (state.isLoadingConversations || state.isLoadingMore || !state.hasMore) return
        val cursor = state.conversations.lastOrNull()?.conversationId ?: return
        if (state.uid.isBlank()) return

        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            val page = getConversationsUseCase(
                uid = state.uid,
                pageSize = PAGE_SIZE,
                startAfterConversationId = cursor
            )
            _uiState.update {
                it.copy(
                    isLoadingMore = false,
                    conversations = it.conversations + page,
                    hasMore = page.size == PAGE_SIZE
                )
            }
        }
    }

    private fun openConversation(conversationId: String) {
        // Desktop keeps both panes on screen and switches conversations without ever "closing"
        // one, so the previous conversation's seen messages have to be flushed here too —
        // otherwise they would only ever disappear on mobile.
        flushSeenMessages()

        val conversation = _uiState.value.conversations.firstOrNull { it.conversationId == conversationId }
        _uiState.update { it.copy(activeConversation = conversation, messages = emptyList()) }
        // The alert watcher reads this to stay quiet about the chat already on screen.
        OpenConversation.opened(conversationId)
        seenMessageIds = mutableSetOf()

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            observeMessagesUseCase(conversationId).collect { messages ->
                val myUid = _uiState.value.uid
                messages.filter { it.senderId != myUid }.forEach { seenMessageIds.add(it.id) }
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun closeConversation() {
        flushSeenMessages()

        messagesJob?.cancel()
        messagesJob = null
        OpenConversation.closed()
        _uiState.update { it.copy(activeConversation = null, messages = emptyList()) }
    }

    /**
     * Deletes what the reader has seen in the conversation currently open, and clears the tally.
     * Called whenever that conversation stops being the one on screen — closed on mobile, switched
     * away from on desktop.
     */
    private fun flushSeenMessages() {
        val conversationId = _uiState.value.activeConversation?.conversationId ?: return
        val toDelete = seenMessageIds.toList()
        seenMessageIds = mutableSetOf()
        if (toDelete.isEmpty()) return

        viewModelScope.launch {
            deleteSeenMessagesUseCase(conversationId = conversationId, messageIds = toDelete)
            // The row's "New message" flag is derived from the match, which the delete does not
            // touch, so re-read the page the list is showing.
            refreshConversations()
        }
    }

    private fun refreshConversations() {
        val uid = _uiState.value.uid
        if (uid.isBlank()) return
        viewModelScope.launch {
            val page = getConversationsUseCase(uid = uid, pageSize = PAGE_SIZE)
            _uiState.update { it.copy(conversations = page, hasMore = page.size == PAGE_SIZE) }
        }
    }

    private fun sendMessage(conversationId: String, senderId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            sendMessageUseCase(conversationId, senderId, trimmed)
        }
    }

    override fun onCleared() {
        messagesJob?.cancel()
        super.onCleared()
    }
}
