package com.rajamohan.mindmingle.presentation.likes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.usecase.GetSentLikeUidsUseCase
import com.rajamohan.mindmingle.domain.usecase.IgnoreIncomingLikeUseCase
import com.rajamohan.mindmingle.domain.usecase.LikeUserUseCase
import com.rajamohan.mindmingle.domain.usecase.ObserveIncomingLikesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One row of the Likes screen.
 *
 * [isConnected] is what splits the screen in two: someone whose like has already been returned
 * opens straight into chat, while everyone else is still a request waiting on an answer.
 */
internal data class LikeEntry(
    val user: User,
    val isConnected: Boolean
)

internal data class LikesUiState(
    val isLoading: Boolean = false,
    val entries: List<LikeEntry> = emptyList()
) {
    /** Requests still waiting on an answer — the top half of the screen. */
    val pending: List<LikeEntry> get() = entries.filterNot { it.isConnected }

    /** Likes this user returned; these open a conversation. */
    val connected: List<LikeEntry> get() = entries.filter { it.isConnected }

    val totalCount: Int get() = entries.size
}

internal sealed class LikesEvent {
    /** Returns a like, which opens the conversation for both sides. */
    data class LikeBack(val fromUid: String) : LikesEvent()

    /** Turns a like down. The sender is never told. */
    data class Ignore(val fromUid: String) : LikesEvent()
}

internal class LikesViewModel(
    private val observeIncomingLikesUseCase: ObserveIncomingLikesUseCase,
    private val getSentLikeUidsUseCase: GetSentLikeUidsUseCase,
    private val likeUserUseCase: LikeUserUseCase,
    private val ignoreIncomingLikeUseCase: IgnoreIncomingLikeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LikesUiState())
    val uiState: StateFlow<LikesUiState> = _uiState.asStateFlow()

    private var likesJob: Job? = null
    private var currentUid: String = ""

    /** Who this user has liked. Kept here so a like-back updates the row without a re-read. */
    private var sentLikeUids: Set<String> = emptySet()

    fun loadLikes(uid: String) {
        currentUid = uid
        _uiState.update { it.copy(isLoading = true) }

        likesJob?.cancel()
        likesJob = viewModelScope.launch {
            // Read once up front: the incoming stream below re-derives rows from this set, and a
            // like-back updates it locally rather than paying for another collection read.
            sentLikeUids = getSentLikeUidsUseCase(uid)

            observeIncomingLikesUseCase(uid).collect { users ->
                _uiState.update { it.copy(isLoading = false, entries = toEntries(users)) }
            }
        }
    }

    fun onEvent(event: LikesEvent) {
        when (event) {
            is LikesEvent.LikeBack -> likeBack(event.fromUid)
            is LikesEvent.Ignore -> ignore(event.fromUid)
        }
    }

    private fun likeBack(fromUid: String) {
        val uid = currentUid
        if (uid.isBlank()) return

        // Flip the row immediately — the write is a round trip and the incoming-likes stream does
        // not carry this user's own sent likes, so nothing else would tell the UI it changed.
        sentLikeUids = sentLikeUids + fromUid
        _uiState.update { state ->
            state.copy(entries = state.entries.map { if (it.user.uid == fromUid) it.copy(isConnected = true) else it })
        }

        viewModelScope.launch {
            likeUserUseCase(uid, fromUid)
        }
    }

    private fun ignore(fromUid: String) {
        val uid = currentUid
        if (uid.isBlank()) return

        // The incoming-likes stream will drop the row on its own once the delete lands; removing
        // it here as well keeps the tap from feeling unanswered on a slow connection.
        _uiState.update { state ->
            state.copy(entries = state.entries.filterNot { it.user.uid == fromUid })
        }

        viewModelScope.launch {
            ignoreIncomingLikeUseCase(uid, fromUid)
        }
    }

    private fun toEntries(users: List<User>): List<LikeEntry> =
        users.map { user -> LikeEntry(user = user, isConnected = user.uid in sentLikeUids) }

    override fun onCleared() {
        likesJob?.cancel()
        super.onCleared()
    }
}
