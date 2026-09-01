package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

/**
 * Who this user has already liked. The Likes screen intersects it with the incoming likes to tell
 * a request still waiting on an answer from one that has already been returned.
 */
class GetSentLikeUidsUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(uid: String): Set<String> = repository.getSentLikeUids(uid)
}
