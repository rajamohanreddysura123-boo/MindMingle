package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.AnonymousChatRepository
import com.rajamohan.mindmingle.domain.repository.EphemeralMessageStore

class SkipAnonymousPartnerUseCase(
    private val repository: AnonymousChatRepository,
    private val store: EphemeralMessageStore
) {
    suspend operator fun invoke(uid: String) {
        repository.skipPartner(uid)
        store.purgeSeen()
    }
}
