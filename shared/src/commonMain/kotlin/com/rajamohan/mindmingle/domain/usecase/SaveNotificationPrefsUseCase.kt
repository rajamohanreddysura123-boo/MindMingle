package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.NotificationPrefs
import com.rajamohan.mindmingle.domain.repository.PushRepository

class SaveNotificationPrefsUseCase(
    private val repository: PushRepository
) {
    suspend operator fun invoke(uid: String, prefs: NotificationPrefs): Result<Unit> =
        repository.savePrefs(uid, prefs)
}
