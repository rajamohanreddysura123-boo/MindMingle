package com.rajamohan.mindmingle.domain.usecase

import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository

/** One page of users for the admin list — see [MindMingleAdminRepository.listUsers]. */
class ListAllUsersUseCase(
    private val repository: MindMingleAdminRepository
) {
    suspend operator fun invoke(pageSize: Int = 50, startAfterUid: String? = null): List<User> {
        return repository.listUsers(pageSize = pageSize, startAfterUid = startAfterUid)
    }
}
