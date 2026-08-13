package com.rajamohan.mindmingle.data.respository

import com.rajamohan.mindmingle.data.remote.source.MindMingleFirebaseProvider
import com.rajamohan.mindmingle.domain.model.AccountStatus
import com.rajamohan.mindmingle.domain.model.AdConfig
import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.model.ChatMessage
import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.SupportThread
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.model.toDomain
import com.rajamohan.mindmingle.domain.model.toDto
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import dev.gitlive.firebase.firestore.Timestamp
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class MindMingleRemoteRepositoryImpl(
    private val mindMingleFirebaseProvider: MindMingleFirebaseProvider
) : MindMingleRemoteRepository {

    private companion object {
        const val TAG = "MindMingleRemoteRepository"
    }

    override fun sendOtp(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            mindMingleFirebaseProvider.sendPhoneOtp(
                phoneNumber = phoneNumber,
                onCodeSent = onCodeSent,
                onError = onError
            )
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "sendOtp failed" }
            onError(e.message ?: "Failed to send OTP")
        }
    }

    override fun verifyOtp(
        verificationId: String,
        code: String,
        onSuccess: (user: User) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            mindMingleFirebaseProvider.verifyPhoneOtp(
                verificationId = verificationId,
                code = code,
                onSuccess = {
                    val uid = mindMingleFirebaseProvider.getCurrentUid() ?: verificationId
                    val user = User(uid = uid, phoneNumber = "")
                    onSuccess(user)
                },
                onError = onError
            )
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "verifyOtp failed" }
            onError(e.message ?: "Verification failed")
        }
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

    override suspend fun getDiscoverProfiles(excludeUid: String, filters: DiscoverFilterCriteria): List<User> {
        return try {
            mindMingleFirebaseProvider.getDiscoverProfiles(excludeUid, filters).map { it.toDomain() }
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getDiscoverProfiles failed" }
            emptyList()
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

    override suspend fun getUser(uid: String): User? {
        return try {
            mindMingleFirebaseProvider.getUserById(uid)?.toDomain()
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getUser failed for uid=$uid" }
            null
        }
    }

    override suspend fun getConversations(uid: String): List<ChatConversation> {
        return try {
            mindMingleFirebaseProvider.getMatches(uid).mapNotNull { (matchId, matchDto) ->
                val otherUid = matchDto.users.firstOrNull { it != uid } ?: return@mapNotNull null
                val otherUserDto = mindMingleFirebaseProvider.getUserById(otherUid) ?: return@mapNotNull null
                ChatConversation(
                    matchId = matchId,
                    otherUser = otherUserDto.toDomain(),
                    lastMessage = matchDto.lastMessage,
                    lastMessageAtSeconds = (matchDto.lastMessageAt as? Timestamp)?.seconds ?: 0L
                )
            }.sortedByDescending { it.lastMessageAtSeconds }
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getConversations failed" }
            emptyList()
        }
    }

    override fun observeMessages(matchId: String): Flow<List<ChatMessage>> {
        return mindMingleFirebaseProvider.observeMessages(matchId).map { messages ->
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

    override suspend fun sendMessage(matchId: String, senderId: String, text: String): Boolean {
        return try {
            mindMingleFirebaseProvider.sendMessage(matchId, senderId, text)
            true
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "sendMessage failed for match=$matchId" }
            false
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

    override suspend fun checkAccountStatus(uid: String): AccountStatus {
        return try {
            if (mindMingleFirebaseProvider.isUidBanned(uid)) {
                return AccountStatus(
                    isBlocked = true,
                    message = "This account has been removed. Contact support if you believe this is a mistake."
                )
            }
            val user = mindMingleFirebaseProvider.getUserById(uid)
            if (user?.isDisabled == true) {
                return AccountStatus(
                    isBlocked = true,
                    message = "Your account has been disabled. Contact support if you believe this is a mistake."
                )
            }
            AccountStatus(isBlocked = false)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "checkAccountStatus failed for uid=$uid" }
            AccountStatus(isBlocked = false)
        }
    }

    override suspend fun deleteMyAccount(): Result<Unit> {
        return try {
            mindMingleFirebaseProvider.deleteMyAccount()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "deleteMyAccount failed" }
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