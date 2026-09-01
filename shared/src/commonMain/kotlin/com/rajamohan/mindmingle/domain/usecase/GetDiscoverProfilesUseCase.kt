package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.DiscoverPage
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

/**
 * One page of the Discover deck. [cursor] is the previous page's cursor — blank starts a fresh
 * scan from a random point in the collection.
 */
class GetDiscoverProfilesUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(
        excludeUid: String,
        filters: DiscoverFilterCriteria = DiscoverFilterCriteria(),
        cursor: String = ""
    ): DiscoverPage {
        return repository.getDiscoverProfiles(excludeUid = excludeUid, filters = filters, cursor = cursor)
    }
}
