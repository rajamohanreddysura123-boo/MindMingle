package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class GetDiscoverProfilesUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(excludeUid: String, filters: DiscoverFilterCriteria = DiscoverFilterCriteria()): List<User> {
        return repository.getDiscoverProfiles(excludeUid, filters)
    }
}
