package com.rajamohan.mindmingle.presentation.preview

import androidx.compose.runtime.Composable
import com.rajamohan.mindmingle.domain.model.AccountStatus
import com.rajamohan.mindmingle.domain.model.AdConfig
import com.rajamohan.mindmingle.domain.model.AdminStats
import com.rajamohan.mindmingle.domain.model.AnonymousMessage
import com.rajamohan.mindmingle.domain.model.AnonymousSession
import com.rajamohan.mindmingle.domain.model.AppUpdateConfig
import com.rajamohan.mindmingle.domain.model.BillingHistory
import com.rajamohan.mindmingle.domain.model.ChatConversation
import com.rajamohan.mindmingle.domain.model.ChatMessage
import com.rajamohan.mindmingle.domain.model.DeletionRequest
import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.DiscoverPage
import com.rajamohan.mindmingle.domain.model.IncomingLike
import com.rajamohan.mindmingle.domain.model.MessageAlert
import com.rajamohan.mindmingle.domain.model.PaymentOrder
import com.rajamohan.mindmingle.domain.model.PlanCatalog
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.model.Subscriber
import com.rajamohan.mindmingle.domain.model.SubscriberPage
import com.rajamohan.mindmingle.domain.model.SubscriberStatusFilter
import com.rajamohan.mindmingle.domain.model.SubscriberStats
import com.rajamohan.mindmingle.domain.model.Subscription
import com.rajamohan.mindmingle.domain.model.SupportThread
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.repository.AnonymousChatRepository
import com.rajamohan.mindmingle.domain.repository.EphemeralMessageStore
import com.rajamohan.mindmingle.domain.repository.MindMingleAdminRepository
import com.rajamohan.mindmingle.domain.repository.MindMingleLocalRepository
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository
import com.rajamohan.mindmingle.presentation.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.KoinApplication
import org.koin.dsl.module

/**
 * Everything the @Preview functions in this package need to render a real screen.
 *
 * ## Why fakes at all
 * Every screen resolves its ViewModel with `koinViewModel()`, and those ViewModels reach a
 * repository on first composition. In the IDE preview renderer there is no Firebase app, no
 * Android context worth speaking of and no network, so the real repositories either throw or
 * hang — a preview would render an error box or a spinner forever.
 *
 * [PreviewHost] therefore starts a Koin graph that is *the real one* with only the repository
 * layer swapped out. Use cases, ViewModels and every screen below them are the production
 * classes, so a preview breaks when the real wiring breaks, which is the whole point.
 *
 * ## Reading a preview
 * The fakes return small, fixed sample sets. A screen with data shows [previewUsers]; the empty
 * and loading states are best seen by pointing a preview at the relevant fake returning nothing.
 */

// ---------------------------------------------------------------------------------------------
// Sample data
// ---------------------------------------------------------------------------------------------

internal val previewUsers = listOf(
    User(
        uid = "u1",
        name = "Irene Fox",
        email = "irene@example.com",
        bio = "Backend engineer, mostly Kotlin and Go. Long walks and longer stack traces.",
        occupation = "Backend Engineer",
        interests = listOf("Street Food", "Books", "Travel", "Digital Art", "Beach Time"),
        experienceLevel = "Senior",
        lookingFor = "Collaboration",
        age = 27,
        location = "Washington, D.C."
    ),
    User(
        uid = "u2",
        name = "Lay M",
        email = "lay@example.com",
        bio = "Into coffee, travel and late-night talks. Always open to new people and good vibes.",
        occupation = "Product Designer",
        interests = listOf("Coffee", "Travel", "Music"),
        experienceLevel = "Mid",
        lookingFor = "Friendship",
        age = 25,
        location = "Washington, USA"
    ),
    User(
        uid = "u3",
        name = "Sam Okafor",
        email = "sam@example.com",
        bio = "Android dev. Compose all the way down.",
        occupation = "Android Engineer",
        interests = listOf("Running", "Photography"),
        experienceLevel = "Lead",
        lookingFor = "Mentorship",
        age = 31,
        location = "Lagos, Nigeria"
    )
)

internal val previewConversations = listOf(
    ChatConversation(
        conversationId = "u0_u1",
        otherUid = "u1",
        otherUserName = "Irene Fox",
        otherUserAvatarUrl = "",
        lastMessageFrom = "u1",
        lastMessageAtSeconds = 1_760_000_000L
    ),
    ChatConversation(
        conversationId = "u0_u2",
        otherUid = "u2",
        otherUserName = "Lay M",
        otherUserAvatarUrl = "",
        lastMessageFrom = "u0",
        lastMessageAtSeconds = 1_759_900_000L
    )
)

internal val previewMessages = listOf(
    ChatMessage(id = "m1", senderId = "u1", text = "Hey — saw you work with Compose too.", sentAtSeconds = 1_760_000_000L),
    ChatMessage(id = "m2", senderId = "u0", text = "I do! Multiplatform, mostly.", sentAtSeconds = 1_760_000_060L),
    ChatMessage(id = "m3", senderId = "u1", text = "Same. What are you building?", sentAtSeconds = 1_760_000_120L)
)

// ---------------------------------------------------------------------------------------------
// Fake repositories
// ---------------------------------------------------------------------------------------------

private class PreviewRemoteRepository : MindMingleRemoteRepository {
    override suspend fun saveUser(user: User): Result<Unit> = Result.success(Unit)
    override suspend fun setPremiumFlag(uid: String, isPremium: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun updateLocation(
        uid: String,
        latitude: Double,
        longitude: Double,
        updatedAt: Long,
        countryCode: String,
        region: String,
        district: String
    ): Result<Unit> = Result.success(Unit)
    override suspend fun uploadUserPhoto(uid: String, bytes: ByteArray): Result<String> = Result.success("")
    override suspend fun deleteUserPhoto(downloadUrl: String): Result<Unit> = Result.success(Unit)
    override fun getCurrentUid(): String = "u0"
    override suspend fun getDiscoverProfiles(excludeUid: String, filters: DiscoverFilterCriteria, cursor: String): DiscoverPage =
        DiscoverPage(profiles = previewUsers, cursor = "")

    override suspend fun likeUser(fromUid: String, toUid: String): Boolean = false
    override suspend fun getSentLikeUids(uid: String): Set<String> = setOf("u2")
    override suspend fun ignoreIncomingLike(uid: String, fromUid: String) = Unit
    override suspend fun getIncomingLikes(uid: String): List<User> = previewUsers
    override fun observeIncomingLikes(uid: String): Flow<List<User>> = flowOf(previewUsers)
    override fun observeIncomingLikeAlerts(uid: String): Flow<List<IncomingLike>> = flowOf(emptyList())
    override fun observeMessageAlerts(uid: String): Flow<List<MessageAlert>> = flowOf(emptyList())
    override suspend fun countConversations(uid: String): Int = previewConversations.size
    override suspend fun countIncomingLikes(uid: String): Int = previewUsers.size
    override suspend fun getUser(uid: String): User = previewUsers.first()
    override suspend fun getConversations(uid: String, pageSize: Int, startAfterConversationId: String?): List<ChatConversation> =
        if (startAfterConversationId == null) previewConversations else emptyList()

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> = flowOf(previewMessages)
    override suspend fun sendMessage(conversationId: String, senderId: String, text: String): Boolean = true
    override suspend fun deleteSeenMessages(conversationId: String, messageIds: List<String>) = Unit
    override suspend fun getAdConfig(): AdConfig = AdConfig(enabled = false)
    override suspend fun getAppUpdateConfig(): AppUpdateConfig = AppUpdateConfig()
    override suspend fun checkAccountStatus(uid: String): AccountStatus = AccountStatus(isBlocked = false)
    override suspend fun requestAccountDeletion(): Result<Unit> = Result.success(Unit)
    override suspend fun deactivateMyAccount(days: Int): Result<Long> = Result.success(0L)
    override suspend fun signOutCurrentUser() = Unit
    override suspend fun sendEmailOtp(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun verifyEmailOtpAndSignIn(email: String, code: String): Result<String> = Result.success("u0")
    override suspend fun signInWithEmailPassword(email: String, password: String): Result<String> = Result.success("u0")
    override suspend fun createAccountWithEmailPassword(email: String, password: String): Result<String> = Result.success("u0")
    override fun observeSupportMessages(uid: String): Flow<List<ChatMessage>> = flowOf(previewMessages)
    override suspend fun sendSupportMessage(uid: String, userName: String, senderId: String, text: String): Boolean = true
    override fun observeSupportThreads(): Flow<List<SupportThread>> = flowOf(
        listOf(SupportThread(uid = "u1", userName = "Irene Fox", lastMessage = "Thanks!", lastMessageAtSeconds = 1_760_000_000L))
    )
}

private class PreviewLocalRepository : MindMingleLocalRepository {
    override suspend fun saveUserSession(user: User): Boolean = true
    override suspend fun getUserSession(): User = previewUsers.first()
    override suspend fun clearSession(): Boolean = true
    override suspend fun saveDiscoverFilters(criteria: DiscoverFilterCriteria): Boolean = true
    override suspend fun getDiscoverFilters(): DiscoverFilterCriteria? = null
    override suspend fun getLastLikeAlertAt(uid: String): Long = 0L
    override suspend fun saveLastLikeAlertAt(uid: String, millis: Long) = Unit
    override suspend fun getLastMessageAlertAt(uid: String): Long = 0L
    override suspend fun saveLastMessageAlertAt(uid: String, seconds: Long) = Unit
}

private class PreviewAdminRepository : MindMingleAdminRepository {
    override suspend fun isCurrentUserAdmin(): Boolean = true
    override suspend fun listUsers(pageSize: Int, startAfterUid: String?): List<User> =
        if (startAfterUid == null) previewUsers else emptyList()

    override suspend fun getStats(): AdminStats = AdminStats(totalUsers = 128, totalConversations = 42, totalMessages = 917)
    override suspend fun setUserDisabled(uid: String, disabled: Boolean): Result<Unit> = Result.success(Unit)
    override suspend fun deleteUser(uid: String, adminUid: String): Result<Unit> = Result.success(Unit)
    override suspend fun unbanUser(uid: String): Result<Unit> = Result.success(Unit)
    override suspend fun listDeletionRequests(): List<DeletionRequest> =
        listOf(DeletionRequest(uid = "u3", name = "Sam Okafor", email = "sam@example.com"))

    override suspend fun listSubscribers(
        status: SubscriberStatusFilter,
        planId: String,
        country: String,
        query: String,
        pageSize: Int,
        cursor: String
    ): Result<SubscriberPage> = Result.success(SubscriberPage(subscribers = emptyList<Subscriber>(), cursor = ""))

    override suspend fun getSubscriberStats(): Result<SubscriberStats> =
        Result.success(SubscriberStats(total = 24, active = 18, expired = 4, cancelled = 2))
}

private class PreviewSubscriptionRepository : SubscriptionRepository {
    override suspend fun getPlanCatalog(countryHint: String): Result<PlanCatalog> = Result.success(PlanCatalog())
    override suspend fun savePlanCatalog(catalog: PlanCatalog): Result<Int> = Result.success(0)
    override suspend fun resetPlanCatalog(): Result<Int> = Result.success(0)
    override suspend fun createOrder(plan: PremiumPlan, countryHint: String): Result<PaymentOrder> =
        Result.failure(IllegalStateException("previews do not open checkout"))

    override suspend fun verifyPayment(orderId: String, paymentId: String, signature: String): Result<Subscription> =
        Result.failure(IllegalStateException("previews do not verify payments"))

    override fun observeSubscription(uid: String): Flow<Subscription?> = flowOf(null)
    override suspend fun getBillingHistory(uid: String): Result<BillingHistory> = Result.success(BillingHistory())
    override suspend fun recordPaymentFailure(orderId: String, planId: String, reason: String) = Unit
    override suspend fun adminSetSubscription(uid: String, plan: PremiumPlan, days: Int): Result<Subscription> =
        Result.success(Subscription())

    override suspend fun adminCancelSubscription(uid: String, immediate: Boolean): Result<Subscription> =
        Result.success(Subscription())
}

private class PreviewAnonymousChatRepository : AnonymousChatRepository {
    override suspend fun findPartner(uid: String): AnonymousSession =
        AnonymousSession(sessionId = "s1", peerAlias = "Quiet Fox", peerAccentIndex = 1)

    override fun observeIncomingMessages(sessionId: String, selfUid: String): Flow<Pair<String, String>> = flowOf()
    override fun observePartnerPresence(sessionId: String, selfUid: String): Flow<Boolean> = flowOf(true)
    override suspend fun sendMessage(sessionId: String, selfUid: String, text: String): Boolean = true
    override suspend fun skipPartner(uid: String) = Unit
    override suspend fun leave(uid: String) = Unit
}

private class PreviewEphemeralMessageStore : EphemeralMessageStore {
    override suspend fun saveIncoming(sessionId: String, messageId: String, text: String): AnonymousMessage =
        AnonymousMessage(id = messageId, sessionId = sessionId, text = text, isMine = false, sequence = 0L, isSeen = false)

    override suspend fun markSeen(messageIds: List<String>) = Unit
    override suspend fun purgeSeen() = Unit
    override suspend fun pendingMessages(): List<AnonymousMessage> = emptyList()
    override suspend fun clearAll() = Unit
}

// ---------------------------------------------------------------------------------------------
// Host
// ---------------------------------------------------------------------------------------------

/**
 * Overrides only the repository bindings. Koin's later definition wins, so this module is added
 * after the real ones and everything above the repository layer stays production code.
 */
private val previewOverrides = module {
    single<MindMingleRemoteRepository> { PreviewRemoteRepository() }
    single<MindMingleLocalRepository> { PreviewLocalRepository() }
    single<MindMingleAdminRepository> { PreviewAdminRepository() }
    single<SubscriptionRepository> { PreviewSubscriptionRepository() }
    single<AnonymousChatRepository> { PreviewAnonymousChatRepository() }
    single<EphemeralMessageStore> { PreviewEphemeralMessageStore() }
}

/**
 * Wraps preview content in the app theme and a Koin graph backed by the fakes above.
 *
 * `KoinApplication` creates a scope local to this composition rather than touching the global
 * one, so a preview can never collide with a running app or with another preview on the page.
 */
@Composable
internal fun PreviewHost(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    KoinApplication(application = {
        modules(com.rajamohan.mindmingle.di.getCommonModules() + previewOverrides)
    }) {
        AppTheme(darkTheme = darkTheme) {
            content()
        }
    }
}
