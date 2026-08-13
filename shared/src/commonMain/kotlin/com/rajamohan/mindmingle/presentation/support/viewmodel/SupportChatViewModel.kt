package com.rajamohan.mindmingle.presentation.support.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.usecase.ObserveSupportMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.SendSupportMessageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SupportChatViewModel(
    private val observeSupportMessagesUseCase: ObserveSupportMessagesUseCase,
    private val sendSupportMessageUseCase: SendSupportMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportChatUiState())
    val uiState: StateFlow<SupportChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null

    fun onEvent(event: SupportChatEvent) {
        when (event) {
            is SupportChatEvent.Open -> open(event.uid)
            is SupportChatEvent.Send -> send(event.uid, event.userName, event.senderId, event.text)
        }
    }

    private fun open(uid: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            observeSupportMessagesUseCase(uid).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun send(uid: String, userName: String, senderId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { sendSupportMessageUseCase(uid, userName, senderId, trimmed) }
    }

    override fun onCleared() {
        messagesJob?.cancel()
        super.onCleared()
    }
}
