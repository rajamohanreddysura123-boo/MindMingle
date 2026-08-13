package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class LikeUserUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(fromUid: String, toUid: String): Boolean {
        return repository.likeUser(fromUid, toUid)
    }
}
