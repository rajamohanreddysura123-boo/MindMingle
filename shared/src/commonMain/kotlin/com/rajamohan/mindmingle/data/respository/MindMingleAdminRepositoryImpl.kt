package com.rajamohan.mindmingle.data.respository

import com.rajamohan.mindmingle.data.remote.source.MindMingleFirebaseProvider
import com.rajamohan.mindmingle.domain.model.AdminStats
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.model.toDomain
import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository
import io.github.aakira.napier.Napier

internal class MindMingleAdminRepositoryImpl(
    private val mindMingleFirebaseProvider: MindMingleFirebaseProvider
) : MindMingleAdminRepository {

    private companion object {
        const val TAG = "MindMingleAdminRepository"
    }

    override suspend fun isCurrentUserAdmin(): Boolean {
        val uid = mindMingleFirebaseProvider.getCurrentUid() ?: return false
        return try {
            mindMingleFirebaseProvider.isUidAdmin(uid)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "isCurrentUserAdmin check failed" }
            false
        }
    }

    override suspend fun listUsers(): List<User> {
        return try {
            mindMingleFirebaseProvider.listAllUsers().map { it.toDomain() }
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "listUsers failed" }
            emptyList()
        }
    }

    override suspend fun getStats(): AdminStats {
        return try {
            val (users, matches, messages) = mindMingleFirebaseProvider.getAdminStats()
            AdminStats(totalUsers = users, totalMatches = matches, totalMessages = messages)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getStats failed" }
            AdminStats(totalUsers = 0, totalMatches = 0, totalMessages = 0)
        }
    }

    override suspend fun setUserDisabled(uid: String, disabled: Boolean): Result<Unit> {
        return try {
            mindMingleFirebaseProvider.setUserDisabled(uid, disabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "setUserDisabled failed for uid=$uid" }
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(uid: String, adminUid: String): Result<Unit> {
        return try {
            mindMingleFirebaseProvider.adminDeleteUser(
                uid = uid,
                ban = true,
                reason = "account deleted by admin $adminUid"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "deleteUser failed for uid=$uid" }
            Result.failure(e)
        }
    }

    override suspend fun unbanUser(uid: String): Result<Unit> {
        return try {
            mindMingleFirebaseProvider.unbanUid(uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "unbanUser failed for uid=$uid" }
            Result.failure(e)
        }
    }
}
