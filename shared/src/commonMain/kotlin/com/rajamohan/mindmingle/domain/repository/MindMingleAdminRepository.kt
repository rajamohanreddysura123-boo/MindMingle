package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.AdminStats
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
    suspend fun listUsers(): List<User>
    suspend fun getStats(): AdminStats
    suspend fun setUserDisabled(uid: String, disabled: Boolean): Result<Unit>
    suspend fun deleteUser(uid: String, adminUid: String): Result<Unit>
    suspend fun unbanUser(uid: String): Result<Unit>
}
