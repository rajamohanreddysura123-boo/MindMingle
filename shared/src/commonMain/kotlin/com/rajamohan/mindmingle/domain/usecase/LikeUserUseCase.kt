package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

/**
 * Likes [toUid]. A like is a request until it is returned; only the returning like opens the
 * conversation, and that is what this returns true for.
 */
class LikeUserUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(fromUid: String, toUid: String): Boolean {
        return repository.likeUser(fromUid, toUid)
    }
}
