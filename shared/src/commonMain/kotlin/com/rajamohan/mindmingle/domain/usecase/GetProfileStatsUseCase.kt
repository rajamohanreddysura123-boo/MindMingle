package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

/** The two counts on the Profile screen's stat row. */
data class ProfileStats(val likes: Int, val conversations: Int)

class GetProfileStatsUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(uid: String): ProfileStats = ProfileStats(
        likes = repository.countIncomingLikes(uid),
        conversations = repository.countConversations(uid)
    )
}
