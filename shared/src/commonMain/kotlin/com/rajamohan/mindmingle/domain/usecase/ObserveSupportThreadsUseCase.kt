package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.SupportThread
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import kotlinx.coroutines.flow.Flow

/** Admin-desktop-only: every user's support thread. */
class ObserveSupportThreadsUseCase(
    private val repository: MindMingleRemoteRepository
) {
    operator fun invoke(): Flow<List<SupportThread>> {
        return repository.observeSupportThreads()
    }
}
