package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.IncomingLike
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveIncomingLikeAlertsUseCase(
    private val repository: MindMingleRemoteRepository
) {
    operator fun invoke(uid: String): Flow<List<IncomingLike>> = repository.observeIncomingLikeAlerts(uid)
}
