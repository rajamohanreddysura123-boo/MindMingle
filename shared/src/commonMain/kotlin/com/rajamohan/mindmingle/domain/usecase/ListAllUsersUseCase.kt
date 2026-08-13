package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository

class ListAllUsersUseCase(
    private val repository: MindMingleAdminRepository
) {
    suspend operator fun invoke(): List<User> {
        return repository.listUsers()
    }
}
