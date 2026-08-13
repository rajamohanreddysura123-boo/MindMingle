package com.rajamohan.mindmingle.presentation.anonymous.viewmodel

import com.rajamohan.mindmingle.domain.model.AnonymousMessage
import com.rajamohan.mindmingle.domain.model.AnonymousSession

internal enum class AnonymousPhase {
    Idle,
    Searching,
    NoOneFound,
    Chatting,
    PartnerLeft
}

internal sealed class AnonymousChatEvent {
    data class Start(val uid: String) : AnonymousChatEvent()
    data class Skip(val uid: String) : AnonymousChatEvent()
    data class Leave(val uid: String) : AnonymousChatEvent()
    data class Send(val uid: String, val text: String) : AnonymousChatEvent()
    data class VisibilityChanged(val isVisible: Boolean) : AnonymousChatEvent()
    data object LoadPending : AnonymousChatEvent()
    data object RevealPending : AnonymousChatEvent()
    data object DismissPending : AnonymousChatEvent()
}

internal data class AnonymousChatUiState(
    val phase: AnonymousPhase = AnonymousPhase.Idle,
    val session: AnonymousSession? = null,
    val messages: List<AnonymousMessage> = emptyList(),
    val pendingMessages: List<AnonymousMessage> = emptyList(),
    val isPendingRevealed: Boolean = false,
    val isSending: Boolean = false,
    val skippedCount: Int = 0
)
