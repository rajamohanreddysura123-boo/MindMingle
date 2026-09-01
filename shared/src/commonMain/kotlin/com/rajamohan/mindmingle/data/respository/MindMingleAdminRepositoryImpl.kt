package com.rajamohan.mindmingle.data.respository

import com.rajamohan.mindmingle.data.remote.dto.SubscriberListRequestDto
import com.rajamohan.mindmingle.data.remote.source.MindMingleFirebaseProvider
import com.rajamohan.mindmingle.domain.model.AdminStats
import com.rajamohan.mindmingle.domain.model.DeletionRequest
import com.rajamohan.mindmingle.domain.model.SubscriberPage
import com.rajamohan.mindmingle.domain.model.SubscriberStats
import com.rajamohan.mindmingle.domain.model.SubscriberStatusFilter
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

    override suspend fun listUsers(pageSize: Int, startAfterUid: String?): List<User> {
        return try {
            mindMingleFirebaseProvider.listUsers(pageSize = pageSize, startAfterUid = startAfterUid)
                .map { it.toDomain() }
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "listUsers failed" }
            emptyList()
        }
    }

    override suspend fun listSubscribers(
        status: SubscriberStatusFilter,
        planId: String,
        country: String,
        query: String,
        pageSize: Int,
        cursor: String
    ): Result<SubscriberPage> {
        return try {
            val response = mindMingleFirebaseProvider.adminListSubscribers(
                SubscriberListRequestDto(
                    status = status.value,
                    planId = planId,
                    country = country,
                    query = query,
                    pageSize = pageSize,
                    cursor = cursor
                )
            )
            Result.success(
                SubscriberPage(
                    subscribers = response.subscribers.map { it.toDomain() },
                    cursor = response.cursor
                )
            )
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "listSubscribers failed" }
            Result.failure(e)
        }
    }

    override suspend fun getSubscriberStats(): Result<SubscriberStats> {
        return try {
            Result.success(mindMingleFirebaseProvider.adminSubscriberStats().toDomain())
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getSubscriberStats failed" }
            Result.failure(e)
        }
    }

    override suspend fun getStats(): AdminStats {
        return try {
            val (users, conversations, messages) = mindMingleFirebaseProvider.getAdminStats()
            AdminStats(totalUsers = users, totalConversations = conversations, totalMessages = messages)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getStats failed" }
            AdminStats(totalUsers = 0, totalConversations = 0, totalMessages = 0)
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
            mindMingleFirebaseProvider.adminPurgeUser(
                uid = uid,
                adminUid = adminUid,
                reason = "account deleted by admin $adminUid"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "deleteUser failed for uid=$uid" }
            Result.failure(e)
        }
    }

    override suspend fun listDeletionRequests(): List<DeletionRequest> {
        return try {
            mindMingleFirebaseProvider.listDeletionRequests().map { it.toDomain() }
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "listDeletionRequests failed" }
            emptyList()
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
