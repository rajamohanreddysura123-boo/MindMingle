package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.MessageAlert
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveMessageAlertsUseCase(
    private val repository: MindMingleRemoteRepository
) {
    operator fun invoke(uid: String): Flow<List<MessageAlert>> = repository.observeMessageAlerts(uid)
}
