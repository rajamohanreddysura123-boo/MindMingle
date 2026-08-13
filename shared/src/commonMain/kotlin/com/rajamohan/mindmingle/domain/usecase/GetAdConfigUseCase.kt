package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.AdConfig
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

class GetAdConfigUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(): AdConfig = repository.getAdConfig()
}
