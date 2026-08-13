package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.AccountStatus
import com.rajamohan.mindmingle.domain.model.AdConfig
import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.model.ChatMessage
import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.SupportThread
import com.rajamohan.mindmingle.domain.model.User
import kotlinx.coroutines.flow.Flow

interface MindMingleRemoteRepository {
    fun sendOtp(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (message: String) -> Unit
    )

    fun verifyOtp(
        verificationId: String,
        code: String,
        onSuccess: (user: User) -> Unit,
        onError: (message: String) -> Unit
    )

    suspend fun saveUser(user: User): Result<Unit>

    /** Uploads one profile photo and returns its public download URL. */
    suspend fun uploadUserPhoto(uid: String, bytes: ByteArray): Result<String>

    suspend fun deleteUserPhoto(downloadUrl: String): Result<Unit>

    /** The signed-in Firebase Auth uid, or null if no session exists yet. */
    fun getCurrentUid(): String?

    /** Server-side filtered Discover feed — matching (age/interests/lookingFor/occupation) runs in the cloud function, not the client. */
    suspend fun getDiscoverProfiles(excludeUid: String, filters: DiscoverFilterCriteria = DiscoverFilterCriteria()): List<User>

    /** Records a like from [fromUid] to [toUid]. Returns true if it created a mutual match. */
    suspend fun likeUser(fromUid: String, toUid: String): Boolean

    suspend fun getIncomingLikes(uid: String): List<User>

    /** Live "who liked me" stream — updates instantly on every new incoming like. */
    fun observeIncomingLikes(uid: String): Flow<List<User>>

    suspend fun getUser(uid: String): User?

    suspend fun getConversations(uid: String): List<ChatConversation>

    fun observeMessages(matchId: String): Flow<List<ChatMessage>>

    suspend fun sendMessage(matchId: String, senderId: String, text: String): Boolean

    /** Remote AdMob settings for the Discover deck; falls back to [AdConfig] defaults on any failure. */
    suspend fun getAdConfig(): AdConfig

    /** Checked right after every sign-in: blocked if banned/deleted or disabled by an admin. */
    suspend fun checkAccountStatus(uid: String): AccountStatus

    /**
     * Erases the signed-in account everywhere — profile, likes, matches, messages, anonymous
     * rooms, subscription, photos and the Firebase Auth user (functions/src/account.ts).
     * Irreversible, and leaves no tombstone: signing up again later starts clean.
     */
    suspend fun deleteMyAccount(): Result<Unit>

    suspend fun signOutCurrentUser()

    /** Desktop's primary sign-in — emails a one-time verification code to [email]. */
    suspend fun sendEmailOtp(email: String): Result<Unit>

    /** Verifies the emailed code and signs the caller in. Returns the resulting Firebase uid. */
    suspend fun verifyEmailOtpAndSignIn(email: String, code: String): Result<String>

    /** Live message stream for [uid]'s support thread with the MindMingle team. */
    fun observeSupportMessages(uid: String): Flow<List<ChatMessage>>

    /** [senderId] is [uid] itself when the user is writing, or "support" when an admin replies. */
    suspend fun sendSupportMessage(uid: String, userName: String, senderId: String, text: String): Boolean

    /** Admin-desktop-only: every user's support thread, most recently active first. */
    fun observeSupportThreads(): Flow<List<SupportThread>>
}
