package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.AdminStats
import com.rajamohan.mindmingle.domain.model.DeletionRequest
import com.rajamohan.mindmingle.domain.model.SubscriberPage
import com.rajamohan.mindmingle.domain.model.SubscriberStats
import com.rajamohan.mindmingle.domain.model.SubscriberStatusFilter
import com.rajamohan.mindmingle.domain.model.User

/**
 * Admin-only operations. Kept separate from [MindMingleRemoteRepository] so the admin
 * surface stays small and auditable. Admins sign in through the same phone-OTP/
 * Google flow as regular users (see AuthViewModel) — every mutating call here is
 * additionally gated server-side by the admins/{uid} Firestore rule, so this
 * interface is inert for non-admins.
 */
interface MindMingleAdminRepository {
    suspend fun isCurrentUserAdmin(): Boolean
    /**
     * One page of users, ordered by uid. [startAfterUid] is the last uid of the previous page;
     * null starts from the beginning. A short page means the end of the collection.
     */
    suspend fun listUsers(pageSize: Int = 50, startAfterUid: String? = null): List<User>
    suspend fun getStats(): AdminStats
    suspend fun setUserDisabled(uid: String, disabled: Boolean): Result<Unit>

    /**
     * Full purge of [uid]: profile, likes both sides, conversations and their messages, support thread,
     * subscription, anonymous traces and photos, then a bannedUids tombstone.
     *
     * The one thing a client SDK cannot reach is their Firebase Auth account, which survives the
     * purge — the tombstone is what keeps it permanently locked out.
     */
    suspend fun deleteUser(uid: String, adminUid: String): Result<Unit>
    suspend fun unbanUser(uid: String): Result<Unit>

    /** Users who asked to be deleted from Account Settings, newest first. */
    suspend fun listDeletionRequests(): List<DeletionRequest>

    /**
     * One page of subscribers — current and past — filtered server-side.
     *
     * [cursor] is the value returned by the previous page; blank starts over, and a blank
     * cursor coming back means the end of the collection. [query] is a substring match on
     * name, email and uid.
     */
    suspend fun listSubscribers(
        status: SubscriberStatusFilter = SubscriberStatusFilter.ALL,
        planId: String = "",
        country: String = "",
        query: String = "",
        pageSize: Int = 25,
        cursor: String = ""
    ): Result<SubscriberPage>

    /** Headline subscriber counts. Reads the whole collection, so call it once per screen. */
    suspend fun getSubscriberStats(): Result<SubscriberStats>
}
