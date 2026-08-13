package com.rajamohan.mindmingle.presentation.anonymous.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.AnonymousMessage
import com.rajamohan.mindmingle.domain.usecase.GetPendingAnonymousMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.LeaveAnonymousChatUseCase
import com.rajamohan.mindmingle.domain.usecase.MarkAnonymousMessagesSeenUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveAnonymousMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveAnonymousPartnerPresenceUseCase
import com.rajamohan.mindmingle.domain.usecase.PurgeSeenAnonymousMessagesUseCase
import com.rajamohan.mindmingle.domain.usecase.SendAnonymousMessageUseCase
import com.rajamohan.mindmingle.domain.usecase.SkipAnonymousPartnerUseCase
import com.rajamohan.mindmingle.domain.usecase.StartAnonymousChatUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AnonymousChatViewModel(
    private val startAnonymousChatUseCase: StartAnonymousChatUseCase,
    private val observeAnonymousMessagesUseCase: ObserveAnonymousMessagesUseCase,
    private val observeAnonymousPartnerPresenceUseCase: ObserveAnonymousPartnerPresenceUseCase,
    private val sendAnonymousMessageUseCase: SendAnonymousMessageUseCase,
    private val skipAnonymousPartnerUseCase: SkipAnonymousPartnerUseCase,
    private val leaveAnonymousChatUseCase: LeaveAnonymousChatUseCase,
    private val markAnonymousMessagesSeenUseCase: MarkAnonymousMessagesSeenUseCase,
    private val getPendingAnonymousMessagesUseCase: GetPendingAnonymousMessagesUseCase,
    private val purgeSeenAnonymousMessagesUseCase: PurgeSeenAnonymousMessagesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnonymousChatUiState())
    val uiState: StateFlow<AnonymousChatUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var messagesJob: Job? = null
    private var presenceJob: Job? = null

    private var isScreenVisible = true
    private var outgoingSequence = 0L

    fun onEvent(event: AnonymousChatEvent) {
        when (event) {
            is AnonymousChatEvent.Start -> startSearch(event.uid)
            is AnonymousChatEvent.Skip -> skip(event.uid)
            is AnonymousChatEvent.Leave -> leave(event.uid)
            is AnonymousChatEvent.Send -> send(event.uid, event.text)
            is AnonymousChatEvent.VisibilityChanged -> onVisibilityChanged(event.isVisible)
            is AnonymousChatEvent.LoadPending -> loadPending()
            is AnonymousChatEvent.RevealPending -> revealPending()
            is AnonymousChatEvent.DismissPending -> dismissPending()
        }
    }

    private fun startSearch(uid: String) {
        if (_uiState.value.phase == AnonymousPhase.Searching) return

        cancelStreams()
        _uiState.update {
            it.copy(
                phase = AnonymousPhase.Searching,
                session = null,
                messages = emptyList()
            )
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val session = startAnonymousChatUseCase(uid)
            if (session == null) {
                _uiState.update { it.copy(phase = AnonymousPhase.NoOneFound, session = null) }
                return@launch
            }

            outgoingSequence = 0L
            _uiState.update { it.copy(phase = AnonymousPhase.Chatting, session = session) }
            observeMessages(sessionId = session.sessionId, uid = uid)
            observePresence(sessionId = session.sessionId, uid = uid)
        }
    }

    private fun observeMessages(sessionId: String, uid: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            observeAnonymousMessagesUseCase(sessionId, uid).collect { message ->
                val delivered = if (isScreenVisible) message.copy(isSeen = true) else message
                _uiState.update { it.copy(messages = it.messages + delivered) }
                if (isScreenVisible) {
                    markAnonymousMessagesSeenUseCase(listOf(message.id))
                }
            }
        }
    }

    private fun observePresence(sessionId: String, uid: String) {
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            observeAnonymousPartnerPresenceUseCase(sessionId, uid).collect { isPresent ->
                if (!isPresent && _uiState.value.phase == AnonymousPhase.Chatting) {
                    messagesJob?.cancel()
                    _uiState.update { it.copy(phase = AnonymousPhase.PartnerLeft) }
                }
            }
        }
    }

    private fun send(uid: String, text: String) {
        val trimmed = text.trim()
        val session = _uiState.value.session
        if (trimmed.isEmpty() || session == null) return

        outgoingSequence += 1
        val localMessage = AnonymousMessage(
            id = "self_${session.sessionId}_$outgoingSequence",
            sessionId = session.sessionId,
            text = trimmed,
            isMine = true,
            sequence = outgoingSequence,
            isSeen = true
        )
        _uiState.update { it.copy(messages = it.messages + localMessage, isSending = true) }

        viewModelScope.launch {
            sendAnonymousMessageUseCase(session.sessionId, uid, trimmed)
            _uiState.update { it.copy(isSending = false) }
        }
    }

    private fun skip(uid: String) {
        cancelStreams()
        _uiState.update {
            it.copy(
                phase = AnonymousPhase.Searching,
                session = null,
                messages = emptyList(),
                skippedCount = it.skippedCount + 1
            )
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            skipAnonymousPartnerUseCase(uid)
            val session = startAnonymousChatUseCase(uid)
            if (session == null) {
                _uiState.update { it.copy(phase = AnonymousPhase.NoOneFound, session = null) }
                return@launch
            }

            outgoingSequence = 0L
            _uiState.update { it.copy(phase = AnonymousPhase.Chatting, session = session) }
            observeMessages(sessionId = session.sessionId, uid = uid)
            observePresence(sessionId = session.sessionId, uid = uid)
        }
    }

    private fun leave(uid: String) {
        cancelStreams()
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                phase = AnonymousPhase.Idle,
                session = null,
                messages = emptyList(),
                isPendingRevealed = false
            )
        }
        viewModelScope.launch {
            leaveAnonymousChatUseCase(uid)
            _uiState.update { it.copy(pendingMessages = getPendingAnonymousMessagesUseCase()) }
        }
    }

    private fun onVisibilityChanged(isVisible: Boolean) {
        isScreenVisible = isVisible
        if (!isVisible) return

        val unseen = _uiState.value.messages.filter { !it.isMine && !it.isSeen }
        if (unseen.isEmpty()) return

        _uiState.update { state ->
            state.copy(messages = state.messages.map { if (it.isMine) it else it.copy(isSeen = true) })
        }
        viewModelScope.launch {
            markAnonymousMessagesSeenUseCase(unseen.map { it.id })
        }
    }

    private fun loadPending() {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingMessages = getPendingAnonymousMessagesUseCase()) }
        }
    }

    private fun revealPending() {
        val pending = _uiState.value.pendingMessages
        _uiState.update { it.copy(isPendingRevealed = true) }
        viewModelScope.launch {
            markAnonymousMessagesSeenUseCase(pending.map { it.id })
        }
    }

    private fun dismissPending() {
        _uiState.update { it.copy(isPendingRevealed = false, pendingMessages = emptyList()) }
        viewModelScope.launch {
            purgeSeenAnonymousMessagesUseCase()
        }
    }

    private fun cancelStreams() {
        messagesJob?.cancel()
        presenceJob?.cancel()
        messagesJob = null
        presenceJob = null
    }

    override fun onCleared() {
        cancelStreams()
        searchJob?.cancel()
        super.onCleared()
    }
}
