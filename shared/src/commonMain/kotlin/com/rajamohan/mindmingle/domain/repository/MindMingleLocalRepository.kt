package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.User

interface MindMingleLocalRepository {
    suspend fun saveUserSession(user: User): Boolean
    suspend fun getUserSession(): User?
    suspend fun clearSession(): Boolean

    /** Last-applied Discover filters, so the deck opens where the user left it. */
    suspend fun saveDiscoverFilters(criteria: DiscoverFilterCriteria): Boolean

    /** Null when the user has never applied a filter on this device. */
    suspend fun getDiscoverFilters(): DiscoverFilterCriteria?
}