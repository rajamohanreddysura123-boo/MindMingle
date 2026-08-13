package com.rajamohan.mindmingle.data.respository

import com.rajamohan.mindmingle.data.local.source.MindMingleDatabaseProvider
import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.repository.MindMingleLocalRepository

internal class MindMingleLocalRepositoryImpl(
    private val mindMingleDatabaseProvider: MindMingleDatabaseProvider
) : MindMingleLocalRepository {

    override suspend fun saveUserSession(user: User): Boolean {
        return try {
            mindMingleDatabaseProvider.saveUserSession(user = user)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getUserSession(): User? {
        return try {
            mindMingleDatabaseProvider.getUserSession()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun clearSession(): Boolean {
        return try {
            mindMingleDatabaseProvider.clearSession()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun saveDiscoverFilters(criteria: DiscoverFilterCriteria): Boolean {
        return try {
            mindMingleDatabaseProvider.saveDiscoverFilters(criteria = criteria)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getDiscoverFilters(): DiscoverFilterCriteria? {
        return try {
            mindMingleDatabaseProvider.getDiscoverFilters()
        } catch (e: Exception) {
            // A stored blob written by an older build can fail to parse — fall back to seeding.
            e.printStackTrace()
            null
        }
    }
}