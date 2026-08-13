package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class GetIncomingLikesUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(uid: String): List<User> {
        return repository.getIncomingLikes(uid)
    }
}
