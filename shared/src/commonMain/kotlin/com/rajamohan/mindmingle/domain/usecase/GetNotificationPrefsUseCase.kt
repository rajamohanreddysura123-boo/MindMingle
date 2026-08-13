package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.NotificationPrefs
import com.rajamohan.mindmingle.domain.repository.PushRepository

class GetNotificationPrefsUseCase(
    private val repository: PushRepository
) {
    suspend operator fun invoke(uid: String): NotificationPrefs = repository.getPrefs(uid)
}
