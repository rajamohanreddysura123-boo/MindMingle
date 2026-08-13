package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.EphemeralMessageStore

class PurgeSeenAnonymousMessagesUseCase(
    private val store: EphemeralMessageStore
) {
    suspend operator fun invoke() {
        store.purgeSeen()
    }
}
