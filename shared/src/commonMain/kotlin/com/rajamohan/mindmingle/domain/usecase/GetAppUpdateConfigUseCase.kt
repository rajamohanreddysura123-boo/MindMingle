package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.AppUpdateConfig
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository

/**
 * Reads the backend-declared minimum supported version. Called once per launch by the Android
 * in-app update gate; never throws — the repository turns every failure into "no floor".
 */
class GetAppUpdateConfigUseCase(
    private val repository: MindMingleRemoteRepository
) {
    suspend operator fun invoke(): AppUpdateConfig = repository.getAppUpdateConfig()
}
