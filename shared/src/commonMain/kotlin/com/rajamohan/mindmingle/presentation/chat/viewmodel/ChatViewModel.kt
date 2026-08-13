package com.rajamohan.mindmingle.presentation.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.LoadConversations -> loadConversations(event.uid)
            is ChatEvent.OpenConversation -> openConversation(event.matchId)
            is ChatEvent.CloseConversation -> closeConversation()
            is ChatEvent.SendMessage -> sendMessage(event.matchId, event.senderId, event.text)
        }
    }

    private fun loadConversations(uid: String) {
        _uiState.update { it.copy(isLoadingConversations = true) }
        viewModelScope.launch {
            val conversations = getConversationsUseCase(uid)
            _uiState.update { it.copy(isLoadingConversations = false, conversations = conversations) }
        }
    }

    private fun openConversation(matchId: String) {
        val conversation = _uiState.value.conversations.firstOrNull { it.matchId == matchId }
        _uiState.update { it.copy(activeConversation = conversation, messages = emptyList()) }

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            observeMessagesUseCase(matchId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun closeConversation() {
        messagesJob?.cancel()
        messagesJob = null
        _uiState.update { it.copy(activeConversation = null, messages = emptyList()) }
    }

    private fun sendMessage(matchId: String, senderId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            sendMessageUseCase(matchId, senderId, trimmed)
        }
    }

    override fun onCleared() {
        messagesJob?.cancel()
        super.onCleared()
    }
}
