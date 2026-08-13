package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveIncomingLikesUseCase(
    private val repository: MindMingleRemoteRepository
) {
    operator fun invoke(uid: String): Flow<List<User>> {
        return repository.observeIncomingLikes(uid)
    }
}
