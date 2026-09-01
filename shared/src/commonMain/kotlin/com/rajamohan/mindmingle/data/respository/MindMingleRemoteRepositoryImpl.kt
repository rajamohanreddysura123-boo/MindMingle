package com.rajamohan.mindmingle.data.respository

import com.rajamohan.mindmingle.data.remote.dto.UserDto
import com.rajamohan.mindmingle.data.remote.source.MindMingleFirebaseProvider
import com.rajamohan.mindmingle.domain.model.AccountStatus
import com.rajamohan.mindmingle.domain.model.AdConfig
import com.rajamohan.mindmingle.domain.model.AppUpdateConfig
import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.model.ChatMessage
import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.DiscoverPage
import com.rajamohan.mindmingle.domain.model.SupportThread
import com.rajamohan.mindmingle.domain.model.IncomingLike
import com.rajamohan.mindmingle.domain.model.MessageAlert
import com.rajamohan.mindmingle.domain.model.toIncomingLike
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.model.formatUtcDate
import com.rajamohan.mindmingle.domain.model.nowMillis
import com.rajamohan.mindmingle.domain.model.toDomain
import com.rajamohan.mindmingle.domain.model.toDto
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import dev.gitlive.firebase.firestore.Timestamp
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal class MindMingleRemoteRepositoryImpl(
    private val mindMingleFirebaseProvider: MindMingleFirebaseProvider
) : MindMingleRemoteRepository {

    private companion object {
        const val TAG = "MindMingleRemoteRepository"
    }

    override suspend fun saveUser(user: User): Result<Unit> {
        return try {
            mindMingleFirebaseProvider.saveUser(
                user = user.toDto()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "saveUser failed for uid=${user.uid}" }
            Result.failure(e)
        }
    }

    override suspend fun updateLocation(
        uid: String,
        latitude: Double,
        longitude: Double,
        updatedAt: Long,
        countryCode: String,
        region: String,
        district: String
    ): Result<Unit> {
        return try {
            mindMingleFirebaseProvider.updateLocation(
                uid = uid,
                latitude = latitude,
                longitude = longitude,
                updatedAt = updatedAt,
                countryCode = countryCode,
                region = region,
                district = district
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "updateLocation failed for uid=$uid" }
            Result.failure(e)
        }
    }

    override suspend fun setPremiumFlag(uid: String, isPremium: Boolean): Result<Unit> {
        return try {
            mindMingleFirebaseProvider.setPremiumFlag(uid, isPremium)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "setPremiumFlag failed for uid=$uid" }
            Result.failure(e)
        }
    }

    override suspend fun uploadUserPhoto(uid: String, bytes: ByteArray): Result<String> {
        return try {
            Result.success(mindMingleFirebaseProvider.uploadUserPhoto(uid, bytes))
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "uploadUserPhoto failed for uid=$uid" }
            Result.failure(e)
        }
    }

    override suspend fun deleteUserPhoto(downloadUrl: String): Result<Unit> {
        return try {
            mindMingleFirebaseProvider.deleteUserPhoto(downloadUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "deleteUserPhoto failed" }
            Result.failure(e)
        }
    }

    override suspend fun getDiscoverProfiles(
        excludeUid: String,
        filters: DiscoverFilterCriteria,
        cursor: String
    ): DiscoverPage {
        return try {
            val (profiles, nextCursor) = mindMingleFirebaseProvider.getDiscoverProfiles(
                excludeUid = excludeUid,
                filters = filters,
                cursor = cursor
            )
            DiscoverPage(profiles = profiles.map { it.toDomain() }, cursor = nextCursor)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getDiscoverProfiles failed" }
            DiscoverPage()
        }
    }

    override suspend fun likeUser(fromUid: String, toUid: String): Boolean {
        return try {
            mindMingleFirebaseProvider.likeUser(fromUid, toUid)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "likeUser failed ($fromUid -> $toUid)" }
            false
        }
    }

    override suspend fun getSentLikeUids(uid: String): Set<String> {
        return try {
            mindMingleFirebaseProvider.getSentLikeUids(uid)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getSentLikeUids failed for uid=$uid" }
            emptySet()
        }
    }

    override suspend fun ignoreIncomingLike(uid: String, fromUid: String) {
        try {
            mindMingleFirebaseProvider.ignoreIncomingLike(uid, fromUid)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "ignoreIncomingLike failed ($fromUid -> $uid)" }
        }
    }

    override fun getCurrentUid(): String? = mindMingleFirebaseProvider.getCurrentUid()

    override suspend fun getIncomingLikes(uid: String): List<User> {
        return try {
            mindMingleFirebaseProvider.getIncomingLikes(uid).map { it.toDomain() }
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getIncomingLikes failed" }
            emptyList()
        }
    }

    override fun observeIncomingLikes(uid: String): Flow<List<User>> {
        return mindMingleFirebaseProvider.observeIncomingLikes(uid).map { users -> users.map { it.toDomain() } }
    }

    override fun observeIncomingLikeAlerts(uid: String): Flow<List<IncomingLike>> {
        return mindMingleFirebaseProvider.observeIncomingLikeMarkers(uid)
            .map { markers -> markers.map { it.toIncomingLike() } }
            .catch { error ->
                // A dropped stream must not take the screen with it — the watcher is a background
                // courtesy, and the likes list itself has its own stream and its own error path.
                Napier.w(throwable = error, tag = TAG) { "observeIncomingLikeAlerts failed" }
                emit(emptyList())
            }
    }

    override fun observeMessageAlerts(uid: String): Flow<List<MessageAlert>> {
        return mindMingleFirebaseProvider.observeConversationsFor(uid)
            .map { rows ->
                rows.mapNotNull { (conversationId, conversation) ->
                    val fromUid = conversation.lastMessageFrom
                    // Nothing written yet, or the last word was this user's own.
                    if (fromUid.isBlank() || fromUid == uid) return@mapNotNull null
                    MessageAlert(
                        conversationId = conversationId,
                        fromUid = fromUid,
                        fromName = conversation.names[fromUid].orEmpty(),
                        sentAtSeconds = (conversation.lastMessageAt as? Timestamp)?.seconds ?: 0L
                    )
                }
            }
            .catch { error ->
                Napier.w(throwable = error, tag = TAG) { "observeMessageAlerts failed" }
                emit(emptyList())
            }
    }

    override suspend fun countConversations(uid: String): Int {
        return try {
            mindMingleFirebaseProvider.countConversationsFor(uid)
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "countConversations failed for uid=$uid" }
            0
        }
    }

    override suspend fun countIncomingLikes(uid: String): Int {
        return try {
            mindMingleFirebaseProvider.countIncomingLikes(uid)
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "countIncomingLikes failed for uid=$uid" }
            0
        }
    }

    override suspend fun getUser(uid: String): User? {
        return try {
            mindMingleFirebaseProvider.getUserById(uid)?.toDomain()
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getUser failed for uid=$uid" }
            null
        }
    }

    /**
     * One page of conversations, newest first. Each row is drawn from the conversation document alone —
     * the old version read the other person's profile once per conversation, which is a read per row on
     * every open. A conversation whose copied name is blank belongs to someone who deleted their account
     * before the copy existed; it still shows, as "Unknown user".
     */
    override suspend fun getConversations(
        uid: String,
        pageSize: Int,
        startAfterConversationId: String?
    ): List<ChatConversation> {
        return try {
            mindMingleFirebaseProvider.getConversationsPage(
                uid = uid,
                pageSize = pageSize,
                startAfterConversationId = startAfterConversationId
            ).mapNotNull { (conversationId, conversationDto) ->
                val otherUid = conversationDto.users.firstOrNull { it != uid } ?: return@mapNotNull null
                ChatConversation(
                    conversationId = conversationId,
                    otherUid = otherUid,
                    otherUserName = conversationDto.names[otherUid].orEmpty(),
                    otherUserAvatarUrl = conversationDto.avatarUrls[otherUid].orEmpty(),
                    lastMessageFrom = conversationDto.lastMessageFrom,
                    lastMessageAtSeconds = (conversationDto.lastMessageAt as? Timestamp)?.seconds ?: 0L
                )
            }
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getConversations failed" }
            emptyList()
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> {
        return mindMingleFirebaseProvider.observeMessages(conversationId).map { messages ->
            messages.map { (id, dto) ->
                ChatMessage(
                    id = id,
                    senderId = dto.senderId,
                    text = dto.text,
                    sentAtSeconds = (dto.sentAt as? Timestamp)?.seconds ?: 0L
                )
            }
        }
    }

    override suspend fun sendMessage(conversationId: String, senderId: String, text: String): Boolean {
        return try {
            mindMingleFirebaseProvider.sendMessage(conversationId, senderId, text)
            true
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "sendMessage failed for conversation=$conversationId" }
            false
        }
    }

    override suspend fun deleteSeenMessages(conversationId: String, messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        try {
            mindMingleFirebaseProvider.deleteSeenMessages(conversationId, messageIds)
        } catch (e: Exception) {
            // The TTL sweep is the backstop, so a failure here delays disappearance rather than
            // preventing it.
            Napier.w(throwable = e, tag = TAG) { "deleteSeenMessages failed for conversation=$conversationId" }
        }
    }

    override suspend fun getAdConfig(): AdConfig {
        return try {
            mindMingleFirebaseProvider.getAdConfig()?.toDomain() ?: AdConfig()
        } catch (e: Exception) {
            // Ads are never worth blocking Discover for — fall back to the built-in defaults.
            Napier.w(throwable = e, tag = TAG) { "getAdConfig failed, using defaults" }
            AdConfig()
        }
    }

    override suspend fun getAppUpdateConfig(): AppUpdateConfig {
        return try {
            mindMingleFirebaseProvider.getAppUpdateConfig()?.toDomain() ?: AppUpdateConfig()
        } catch (e: Exception) {
            // Fail open, always. An unreachable Firestore must never be able to present a
            // "update required" wall the user has no way past — Play's own update priority
            // still applies in that case.
            Napier.w(throwable = e, tag = TAG) { "getAppUpdateConfig failed, no version floor applied" }
            AppUpdateConfig()
        }
    }

    override suspend fun checkAccountStatus(uid: String): AccountStatus {
        return try {
            if (mindMingleFirebaseProvider.isUidBanned(uid)) {
                return AccountStatus(
                    isBlocked = true,
                    message = "This account has been removed. Contact support if you believe this is a mistake."
                )
            }
            val user = mindMingleFirebaseProvider.getUserById(uid)?.toDomain()
            if (user?.isDisabled == true) {
                return AccountStatus(
                    isBlocked = true,
                    message = "Your account has been disabled. Contact support if you believe this is a mistake."
                )
            }
            if (user?.isDeletionRequested == true) {
                return AccountStatus(
                    isBlocked = true,
                    message = "This account has been deleted."
                )
            }
            if (user != null) {
                val now = nowMillis()
                if (user.isDeactivatedAt(now)) {
                    return AccountStatus(
                        isBlocked = true,
                        message = "Your account is deactivated until ${formatUtcDate(user.reactivateAt)}. " +
                            "You can sign in again after that date."
                    )
                }
                if (user.isDeactivationExpiredAt(now)) {
                    // Window is over — clearing the flags here is the reactivation. The rules only
                    // accept this write once reactivateAt has passed, so it cannot be rushed.
                    mindMingleFirebaseProvider.clearDeactivation(uid)
                }
            }
            AccountStatus(isBlocked = false)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "checkAccountStatus failed for uid=$uid" }
            AccountStatus(isBlocked = false)
        }
    }

    override suspend fun requestAccountDeletion(): Result<Unit> {
        return try {
            val uid = mindMingleFirebaseProvider.getCurrentUid()
                ?: return Result.failure(IllegalStateException("No signed-in account"))
            val user = mindMingleFirebaseProvider.getUserById(uid)
                ?: UserDto(uid = uid)
            mindMingleFirebaseProvider.requestAccountDeletion(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "requestAccountDeletion failed" }
            Result.failure(e)
        }
    }

    override suspend fun deactivateMyAccount(days: Int): Result<Long> {
        return try {
            val uid = mindMingleFirebaseProvider.getCurrentUid()
                ?: return Result.failure(IllegalStateException("No signed-in account"))
            Result.success(mindMingleFirebaseProvider.deactivateAccount(uid = uid, days = days))
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "deactivateMyAccount failed for days=$days" }
            Result.failure(e)
        }
    }

    override suspend fun signOutCurrentUser() {
        mindMingleFirebaseProvider.signOutCurrentUser()
    }

    override suspend fun sendEmailOtp(email: String): Result<Unit> {
        return try {
            mindMingleFirebaseProvider.requestEmailOtp(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "sendEmailOtp failed for email=$email" }
            Result.failure(e)
        }
    }

    override suspend fun verifyEmailOtpAndSignIn(email: String, code: String): Result<String> {
        return try {
            Result.success(mindMingleFirebaseProvider.verifyEmailOtpAndSignIn(email, code))
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "verifyEmailOtpAndSignIn failed for email=$email" }
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmailPassword(email: String, password: String): Result<String> {
        return try {
            Result.success(mindMingleFirebaseProvider.signInWithEmailPassword(email, password))
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "signInWithEmailPassword failed for email=$email" }
            Result.failure(e)
        }
    }

    override suspend fun createAccountWithEmailPassword(email: String, password: String): Result<String> {
        return try {
            Result.success(mindMingleFirebaseProvider.createAccountWithEmailPassword(email, password))
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "createAccountWithEmailPassword failed for email=$email" }
            Result.failure(e)
        }
    }

    override fun observeSupportMessages(uid: String): Flow<List<ChatMessage>> {
        return mindMingleFirebaseProvider.observeSupportMessages(uid).map { messages ->
            messages.map { (id, dto) ->
                ChatMessage(
                    id = id,
                    senderId = dto.senderId,
                    text = dto.text,
                    sentAtSeconds = (dto.sentAt as? Timestamp)?.seconds ?: 0L
                )
            }
        }
    }

    override suspend fun sendSupportMessage(uid: String, userName: String, senderId: String, text: String): Boolean {
        return try {
            mindMingleFirebaseProvider.sendSupportMessage(uid, userName, senderId, text)
            true
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "sendSupportMessage failed for uid=$uid" }
            false
        }
    }

    override fun observeSupportThreads(): Flow<List<SupportThread>> {
        return mindMingleFirebaseProvider.observeSupportThreads().map { threads ->
            threads.map { dto ->
                SupportThread(
                    uid = dto.uid,
                    userName = dto.userName,
                    lastMessage = dto.lastMessage,
                    lastMessageAtSeconds = (dto.lastMessageAt as? Timestamp)?.seconds ?: 0L
                )
            }
        }
    }
}