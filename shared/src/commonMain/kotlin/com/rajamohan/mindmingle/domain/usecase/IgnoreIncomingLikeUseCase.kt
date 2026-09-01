package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

/**
 * Turns down an incoming like. Only the recipient's copy is removed — the sender is never told,
 * which is deliberate: a rejection notice is worse for both sides than silence.
 */
class IgnoreIncomingLikeUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(uid: String, fromUid: String) = repository.ignoreIncomingLike(uid, fromUid)
}
