package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.AccountStatus
import com.rajamohan.mindmingle.domain.model.AdConfig
import com.rajamohan.mindmingle.domain.model.AppUpdateConfig
import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.model.ChatMessage
import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.DiscoverPage
import com.rajamohan.mindmingle.domain.model.IncomingLike
import com.rajamohan.mindmingle.domain.model.MessageAlert
import com.rajamohan.mindmingle.domain.model.SupportThread
import com.rajamohan.mindmingle.domain.model.User
import kotlinx.coroutines.flow.Flow

interface MindMingleRemoteRepository {
    suspend fun saveUser(user: User): Result<Unit>

    /** Writes the signed-in account's own coordinates and the moment they were taken. */
    suspend fun updateLocation(
        uid: String,
        latitude: Double,
        longitude: Double,
        updatedAt: Long,
        countryCode: String = "",
        region: String = "",
        district: String = ""
    ): Result<Unit>

    /** Mirrors the signed-in account's MindMingle+ state onto its own profile doc for the badge. */
    suspend fun setPremiumFlag(uid: String, isPremium: Boolean): Result<Unit>

    suspend fun uploadUserPhoto(uid: String, bytes: ByteArray): Result<String>

    suspend fun deleteUserPhoto(downloadUrl: String): Result<Unit>

    /** The signed-in Firebase Auth uid, or null if no session exists yet. */
    fun getCurrentUid(): String?

    /**
     * One page of the server-filtered Discover feed — matching (age/interests/lookingFor/
     * occupation) runs in the cloud function, not the client. Pass the previous page's
     * [DiscoverPage.cursor] to continue the scan; blank starts a fresh one.
     */
    suspend fun getDiscoverProfiles(
        excludeUid: String,
        filters: DiscoverFilterCriteria = DiscoverFilterCriteria(),
        cursor: String = ""
    ): DiscoverPage

    /**
     * Records a like from [fromUid] to [toUid]. A like on its own is only a request — chat opens
     * once the other side likes back. Returns true when this call was the one that opened it.
     */
    suspend fun likeUser(fromUid: String, toUid: String): Boolean

    /** The uids [uid] has already liked, so the Likes screen can tell a request from a reply. */
    suspend fun getSentLikeUids(uid: String): Set<String>

    /** Removes an incoming like without liking back. The sender is not told. */
    suspend fun ignoreIncomingLike(uid: String, fromUid: String)

    suspend fun getIncomingLikes(uid: String): List<User>

    /** Live "who liked me" stream — updates instantly on every new incoming like. */
    fun observeIncomingLikes(uid: String): Flow<List<User>>

    /** The same mailbox with timestamps, for the alert watcher. */
    fun observeIncomingLikeAlerts(uid: String): Flow<List<IncomingLike>>

    /** Every conversation this user is in, live, for the message alert watcher. */
    fun observeMessageAlerts(uid: String): Flow<List<MessageAlert>>

    /** Totals for the Profile screen's stat row. One aggregation read each, never a page. */
    suspend fun countConversations(uid: String): Int

    suspend fun countIncomingLikes(uid: String): Int

    suspend fun getUser(uid: String): User?

    /**
     * One page of conversations, newest activity first. [startAfterConversationId] is the last conversationId of
     * the previous page; null starts from the top. A short page means there are no more.
     */
    suspend fun getConversations(
        uid: String,
        pageSize: Int = 30,
        startAfterConversationId: String? = null
    ): List<ChatConversation>

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>

    suspend fun sendMessage(conversationId: String, senderId: String, text: String): Boolean

    /**
     * Deletes messages this user has now seen — the client half of the disappearing-message model.
     * Anything never opened is swept by the Firestore TTL policy instead.
     */
    suspend fun deleteSeenMessages(conversationId: String, messageIds: List<String>)

    /** Remote AdMob settings for the Discover deck; falls back to [AdConfig] defaults on any failure. */
    suspend fun getAdConfig(): AdConfig

    /**
     * The backend-declared minimum supported version. Falls back to [AppUpdateConfig] defaults
     * ("no floor") on any failure, so a Firestore outage can never lock users out of the app.
     */
    suspend fun getAppUpdateConfig(): AppUpdateConfig

    /** Checked right after every sign-in: blocked if banned/deleted or disabled by an admin. */
    suspend fun checkAccountStatus(uid: String): AccountStatus

    /**
     * Files a deletion request: wipes everything the signed-in user owns and can reach, locks
     * every sign-in path for them from this moment, and queues the rest for an admin to purge.
     * Irreversible.
     */
    suspend fun requestAccountDeletion(): Result<Unit>

    /**
     * Hides the signed-in account for [days] and signs them out. There is no early undo — the
     * Firestore rules only allow the flags to be cleared once the window has run out, which the
     * next sign-in after that does automatically.
     */
    suspend fun deactivateMyAccount(days: Int): Result<Long>

    suspend fun signOutCurrentUser()

    /** Desktop's primary sign-in — emails a one-time verification code to [email]. */
    suspend fun sendEmailOtp(email: String): Result<Unit>

    /** Verifies the emailed code and signs the caller in. Returns the resulting Firebase uid. */
    suspend fun verifyEmailOtpAndSignIn(email: String, code: String): Result<String>

    /** Email + password sign-in. Returns the Firebase uid; fails when the credentials don't match. */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<String>

    /** Registers a new email + password account and leaves it signed in. Returns the Firebase uid. */
    suspend fun createAccountWithEmailPassword(email: String, password: String): Result<String>

    /** Live message stream for [uid]'s support thread with the MindMingle team. */
    fun observeSupportMessages(uid: String): Flow<List<ChatMessage>>

    /** [senderId] is [uid] itself when the user is writing, or "support" when an admin replies. */
    suspend fun sendSupportMessage(uid: String, userName: String, senderId: String, text: String): Boolean

    /** Admin-desktop-only: every user's support thread, most recently active first. */
    fun observeSupportThreads(): Flow<List<SupportThread>>
}
