package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.repository.PushRepository

class UnregisterPushDeviceUseCase(
    private val repository: PushRepository
) {
    suspend operator fun invoke(uid: String): Result<Unit> = repository.unregisterCurrentDevice(uid)
}
