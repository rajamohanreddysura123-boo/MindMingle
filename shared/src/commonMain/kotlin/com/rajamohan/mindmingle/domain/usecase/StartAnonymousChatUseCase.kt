package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.AnonymousSession
import com.rajamohan.mindmingle.domain.repository.AnonymousChatRepository

class StartAnonymousChatUseCase(
    private val repository: AnonymousChatRepository
) {
    suspend operator fun invoke(uid: String): AnonymousSession? {
        return repository.findPartner(uid)
    }
}
