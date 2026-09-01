package com.rajamohan.mindmingle.data.remote.source

import com.rajamohan.mindmingle.core.media.toStorageData
import com.rajamohan.mindmingle.data.remote.dto.AdConfigDto
import com.rajamohan.mindmingle.data.remote.dto.BannedUidDto
import com.rajamohan.mindmingle.data.remote.dto.CreateOrderRequestDto
import com.rajamohan.mindmingle.data.remote.dto.AdminCancelSubscriptionRequestDto
import com.rajamohan.mindmingle.data.remote.dto.AdminSetSubscriptionRequestDto
import com.rajamohan.mindmingle.data.remote.dto.AdminSubscriptionResponseDto
import com.rajamohan.mindmingle.data.remote.dto.AppUpdateConfigDto
import com.rajamohan.mindmingle.data.remote.dto.BillingHistoryRequestDto
import com.rajamohan.mindmingle.data.remote.dto.BillingHistoryResponseDto
import com.rajamohan.mindmingle.data.remote.dto.RecordPaymentFailureRequestDto
import com.rajamohan.mindmingle.data.remote.dto.SubscriberListRequestDto
import com.rajamohan.mindmingle.data.remote.dto.SubscriberListResponseDto
import com.rajamohan.mindmingle.data.remote.dto.SubscriberStatsDto
import com.rajamohan.mindmingle.data.remote.dto.SubscriberStatsRequestDto
import com.rajamohan.mindmingle.data.remote.dto.DeletionRequestDto
import com.rajamohan.mindmingle.data.remote.dto.DeviceTokenDto
import com.rajamohan.mindmingle.data.remote.dto.NotificationPrefsDto
import com.rajamohan.mindmingle.data.remote.dto.PaymentDetailsRequestDto
import com.rajamohan.mindmingle.data.remote.dto.PlanCatalogDto
import com.rajamohan.mindmingle.data.remote.dto.CreateOrderResponseDto
import com.rajamohan.mindmingle.data.remote.dto.LikeDto
import com.rajamohan.mindmingle.data.remote.dto.ConversationDto
import com.rajamohan.mindmingle.data.remote.dto.MessageDto
import com.rajamohan.mindmingle.data.remote.dto.SubscriptionDto
import com.rajamohan.mindmingle.data.remote.dto.SupportThreadDto
import com.rajamohan.mindmingle.data.remote.dto.UserDto
import com.rajamohan.mindmingle.data.remote.dto.VerifyPaymentRequestDto
import com.rajamohan.mindmingle.data.remote.dto.VerifyPaymentResponseDto
import com.rajamohan.mindmingle.domain.model.DiscoverFilterCriteria
import com.rajamohan.mindmingle.domain.model.nowMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.storage.FirebaseStorageMetadata
import dev.gitlive.firebase.storage.storage
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

internal class MindMingleFirebaseProvider {

    private companion object {
        const val TAG = "MindMingleFirebaseProvider"
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        const val MILLIS_PER_HOUR = 60L * 60L * 1000L

        /**
         * Where the Cloud Functions run. They sit next to Firestore rather than in the
         * us-central1 default, and a callable is addressed by name *and* region — calling
         * without this looks for a function that does not exist there and fails as NOT_FOUND.
         */
        const val FUNCTIONS_REGION = "asia-southeast1"
    }

    private val firestore get() = Firebase.firestore
    private val storage get() = Firebase.storage
    private val functions get() = Firebase.functions(FUNCTIONS_REGION)

    /** The signed-in Firebase Auth uid, or null if no session exists yet. */
    fun getCurrentUid(): String? = Firebase.auth.currentUser?.uid

    suspend fun saveUser(user: UserDto) {
        firestore.collection("users")
            .document(user.uid)
            .set(user)
    }

    /**
     * Writes this account's own coordinates. Three fields together so a reader can never see a
     * position without knowing how old it is.
     */
    suspend fun updateLocation(
        uid: String,
        latitude: Double,
        longitude: Double,
        updatedAt: Long,
        countryCode: String,
        region: String,
        district: String
    ) {
        // Names are written only when the geocoder produced them: an empty answer must not wipe
        // the country a profile already had, or a user in a tunnel would drop out of every country
        // filter until the next refresh.
        val fields = buildList {
            add("latitude" to latitude)
            add("longitude" to longitude)
            add("locationUpdatedAt" to updatedAt)
            if (countryCode.isNotBlank()) add("countryCode" to countryCode)
            if (region.isNotBlank()) add("region" to region)
            if (district.isNotBlank()) add("district" to district)
        }.toTypedArray()

        firestore.collection("users").document(uid).update(*fields)
    }

    /**
     * Copies this account's own MindMingle+ state onto its public profile doc, which is what the
     * badge beside a name reads — subscriptions/{uid} itself is owner-only, so no viewer can
     * check someone else's plan. Only ever called for the signed-in uid.
     */
    suspend fun setPremiumFlag(uid: String, isPremium: Boolean) {
        firestore.collection("users").document(uid).update("isPremium" to isPremium)
    }

    /** Uploads one profile photo and returns its public download URL. */
    suspend fun uploadUserPhoto(uid: String, bytes: ByteArray): String {
        val ref = storage.reference.child("users/$uid/photos/${randomPhotoId()}.jpg")
        ref.putData(bytes.toStorageData(), FirebaseStorageMetadata(contentType = "image/jpeg"))
        return ref.getDownloadUrl()
    }

    suspend fun deleteUserPhoto(downloadUrl: String) {
        storage.getReferenceFromUrl(downloadUrl).delete()
    }

    private fun randomPhotoId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..16).map { chars.random() }.joinToString("")
    }

    @Serializable
    private data class DiscoverFilterRequest(
        val minAge: Int,
        val maxAge: Int,
        val interests: List<String>,
        val lookingFor: List<String>,
        val occupations: List<String>,
        val maxDistanceKm: Int? = null,
        val genders: List<String> = emptyList(),
        val experienceLevels: List<String> = emptyList(),
        val languages: List<String> = emptyList(),
        /** ISO 3166-1 alpha-2 codes. Ignored server-side for a caller without MindMingle+. */
        val countries: List<String> = emptyList(),
        /** ADM2 district names. Also MindMingle+ only. */
        val districts: List<String> = emptyList(),
        val detailFilters: Map<String, List<String>> = emptyMap(),
        val cursor: String = "",
        val limit: Int = 0
    )

    @Serializable
    private data class DiscoverProfilesResponse(
        val profiles: List<UserDto> = emptyList(),
        val cursor: String = "",
        /**
         * The function reports whether it had to look past the caller's filters to fill the deck.
         * Declared so the response still decodes; deliberately not surfaced — a widened deck is
         * meant to look like an ordinary one.
         */
        val relaxed: Boolean = false
    )

    /**
     * One deck of server-filtered Discover profiles — see
     * mindmingle_backend_services/src/index.ts (filterDiscoverProfiles).
     *
     * The feed is paged: the function scans a bounded slice of `users` and returns a cursor to
     * resume from, so a deck load costs the same whether the app has 100 users or 100k. A blank
     * cursor back means the scan reached the end and the next call should start over.
     */
    suspend fun getDiscoverProfiles(
        excludeUid: String,
        filters: DiscoverFilterCriteria,
        cursor: String
    ): Pair<List<UserDto>, String> {
        val response = functions.httpsCallable("filterDiscoverProfiles")
            .invoke(
                DiscoverFilterRequest(
                    minAge = filters.minAge,
                    maxAge = filters.maxAge,
                    interests = filters.interests.toList(),
                    lookingFor = filters.lookingFor.toList(),
                    occupations = filters.occupations.toList(),
                    maxDistanceKm = filters.maxDistanceKm,
                    genders = filters.genders.toList(),
                    experienceLevels = filters.experienceLevels.toList(),
                    languages = filters.languages.toList(),
                    countries = filters.countries.toList(),
                    districts = filters.districts.toList(),
                    detailFilters = filters.detailFilters.mapValues { (_, values) -> values.toList() },
                    cursor = cursor
                )
            )
            .data<DiscoverProfilesResponse>()

        return response.profiles.filter { it.uid != excludeUid } to response.cursor
    }

    /**
     * Records a like from [fromUid] to [toUid] and, when that like is a reply to one already
     * pointing the other way, opens the pair's conversation.
     *
     * A like on its own is a request: the recipient sees it in their Likes screen and can return
     * it or ignore it. Only a returned like unlocks chat, which is what keeps the inbox free of
     * messages from people the user never chose. Returns true when this call was the one that
     * opened the conversation.
     */
    suspend fun likeUser(fromUid: String, toUid: String): Boolean {
        // One read of the liker's own profile, copied into both markers so the recipient's
        // "who liked me" list never has to fetch profiles at all (see [LikeDto]).
        val liker = getUserById(fromUid)
        val like = LikeDto(
            fromUid = fromUid,
            name = liker?.name.orEmpty(),
            occupation = liker?.occupation.orEmpty(),
            experienceLevel = liker?.experienceLevel.orEmpty(),
            avatarUrl = liker?.avatarUrl.orEmpty(),
            createdAt = nowMillis()
        )

        firestore.collection("likes").document(fromUid).collection("sentTo")
            .document(toUid).set(like)
        firestore.collection("incomingLikes").document(toUid).collection("from")
            .document(fromUid).set(like)

        // The other direction has to already exist for chat to open — this is the whole gate.
        val likeBack = firestore.collection("likes").document(toUid).collection("sentTo")
            .document(fromUid).get()
        if (!likeBack.exists) return false

        val conversationId = conversationIdFor(fromUid, toUid)
        val existing = firestore.collection("conversations").document(conversationId).get()

        // Already open — leave it alone. Rewriting would clobber lastMessageFrom/lastMessageAt
        // and reset the "new message" state of a live conversation.
        if (existing.exists) return false

        // Both cards are copied onto the conversation so the chat list can draw a row without
        // reading either profile, and so a conversation survives the other person deleting their
        // account (it renders as "Unknown user" rather than disappearing).
        val other = getUserById(toUid)
        firestore.collection("conversations").document(conversationId)
            .set(
                ConversationDto(
                    users = listOf(fromUid, toUid).sorted(),
                    names = mapOf(
                        fromUid to liker?.name.orEmpty(),
                        toUid to other?.name.orEmpty()
                    ),
                    avatarUrls = mapOf(
                        fromUid to liker?.avatarUrl.orEmpty(),
                        toUid to other?.avatarUrl.orEmpty()
                    ),
                    createdAt = nowMillis()
                )
            )
        return true
    }

    /** The uids this user has already liked — drives the "liked back" state in the Likes screen. */
    suspend fun getSentLikeUids(uid: String): Set<String> {
        return firestore.collection("likes").document(uid).collection("sentTo")
            .get()
            .documents
            .map { it.id }
            .toSet()
    }

    /**
     * Drops an incoming like without liking back. Only the recipient's copy is removed: the sender
     * keeps their own "sentTo" marker, so they are never told they were turned down and the same
     * profile does not come back around in their deck.
     */
    suspend fun ignoreIncomingLike(uid: String, fromUid: String) {
        firestore.collection("incomingLikes").document(uid).collection("from")
            .document(fromUid).delete()
    }

    /** Deterministic id for a pair: the same two uids always resolve to the same conversation. */
    private fun conversationIdFor(uidA: String, uidB: String): String =
        listOf(uidA, uidB).sorted().joinToString("_")

    /** Built entirely from the like documents — no profile lookups, one read per liker. */
    suspend fun getIncomingLikes(uid: String): List<UserDto> {
        return firestore.collection("incomingLikes").document(uid).collection("from")
            .get()
            .documents
            .map { it.data<LikeDto>().toLikerProfile(it.id) }
    }

    /**
     * Live "who liked me" stream. Each snapshot maps straight off the like documents, so a change
     * costs nothing beyond the snapshot itself — the old version re-read every liker's profile
     * on every emission.
     */
    fun observeIncomingLikes(uid: String): Flow<List<UserDto>> {
        return firestore.collection("incomingLikes").document(uid).collection("from")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { likeDoc -> likeDoc.data<LikeDto>().toLikerProfile(likeDoc.id) }
            }
    }

    /**
     * The same mailbox as [observeIncomingLikes], but keeping `createdAt` — the alert watcher
     * needs it to tell a like that just arrived from the hundred that were already there.
     */
    fun observeIncomingLikeMarkers(uid: String): Flow<List<LikeDto>> {
        return firestore.collection("incomingLikes").document(uid).collection("from")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { likeDoc ->
                    val like = likeDoc.data<LikeDto>()
                    if (like.fromUid.isBlank()) like.copy(fromUid = likeDoc.id) else like
                }
            }
    }

    /**
     * The card-sized profile carried on a like. Only the fields the likes list renders are here;
     * anything more (bio, photos, interests) comes from the real profile when it is opened.
     */
    private fun LikeDto.toLikerProfile(documentId: String): UserDto = UserDto(
        uid = fromUid.ifBlank { documentId },
        name = name,
        occupation = occupation,
        experienceLevel = experienceLevel,
        avatarUrl = avatarUrl
    )

    suspend fun getConversationsFor(uid: String): List<Pair<String, ConversationDto>> {
        return firestore.collection("conversations")
            .where { "users" contains uid }
            .get()
            .documents
            .map { it.id to it.data<ConversationDto>() }
    }

    /**
     * Live view of every conversation this user is in, for the message alert watcher.
     *
     * No orderBy, so it needs no composite index — the watcher compares timestamps itself. The
     * conversation row carries no message text by design (see [ConversationDto]), which is also
     * why an alert can only ever say that someone wrote, never what they wrote.
     */
    fun observeConversationsFor(uid: String): Flow<List<Pair<String, ConversationDto>>> {
        return firestore.collection("conversations")
            .where { "users" contains uid }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc -> doc.id to doc.data<ConversationDto>() }
            }
    }

    /**
     * One page of this user's conversations, newest activity first. Paged for the same reason the
     * admin user list is: someone with hundreds of conversations should not pay to read all of them to
     * look at the top of the list.
     *
     * Ordering by lastMessageAt alongside an array-contains filter needs the composite index in
     * firestore.indexes.json.
     */
    suspend fun getConversationsPage(
        uid: String,
        pageSize: Int,
        startAfterConversationId: String?
    ): List<Pair<String, ConversationDto>> {
        val base = firestore.collection("conversations")
            .where { "users" contains uid }
            .orderBy("lastMessageAt", Direction.DESCENDING)
            .limit(pageSize)

        val page = if (startAfterConversationId.isNullOrBlank()) {
            base
        } else {
            val cursorDoc = firestore.collection("conversations").document(startAfterConversationId).get()
            if (cursorDoc.exists) base.startAfter(cursorDoc) else base
        }

        return page.get().documents.map { it.id to it.data<ConversationDto>() }
    }

    /**
     * How many conversations and incoming likes this user has, as aggregations.
     *
     * The Profile screen showed `getConversations(uid).size` and `getIncomingLikes(uid).size` — a
     * page of thirty conversation documents and every like document, fetched in full so that two
     * integers could be drawn. Worse, the conversations call is paged, so the number silently
     * stopped at thirty: someone with forty-five matches was shown "30".
     *
     * count() is one read each and has no page size.
     */
    suspend fun countConversationsFor(uid: String): Int {
        return firestore.collection("conversations")
            .where { "users" contains uid }
            .count()
            .toInt()
    }

    suspend fun countIncomingLikes(uid: String): Int {
        return firestore.collection("incomingLikes").document(uid).collection("from")
            .count()
            .toInt()
    }

    suspend fun getUserById(uid: String): UserDto? {
        val doc = firestore.collection("users").document(uid).get()
        return if (doc.exists) doc.data<UserDto>() else null
    }

    /** Live message stream for a conversation, oldest first, each paired with its Firestore document id. */
    fun observeMessages(conversationId: String): Flow<List<Pair<String, MessageDto>>> {
        return firestore.collection("conversations").document(conversationId).collection("messages")
            .orderBy("sentAt")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.id to doc.data<MessageDto>(serverTimestampBehavior = ServerTimestampBehavior.ESTIMATE)
                }
            }
    }

    /**
     * Writes a message that is already scheduled to disappear: [retentionHours] from now, the TTL
     * policy on `expiresAt` sweeps it if the recipient never opened it. The conversation row records only
     * who wrote last and when — never the text, which would outlive the message it copied.
     */
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        retentionHours: Int = ConversationDto.DEFAULT_RETENTION_HOURS
    ) {
        val expiresAtMillis = nowMillis() + retentionHours * MILLIS_PER_HOUR
        firestore.collection("conversations").document(conversationId).collection("messages")
            .add(
                MessageDto(
                    senderId = senderId,
                    text = text,
                    expiresAt = Timestamp(
                        seconds = expiresAtMillis / 1000L,
                        nanoseconds = 0
                    )
                )
            )
        firestore.collection("conversations").document(conversationId)
            .update(
                "lastMessageFrom" to senderId,
                "lastMessageAt" to FieldValue.serverTimestamp
            )
    }

    /**
     * Deletes messages the recipient has now seen. This is the client half of the disappearing
     * model — the TTL policy only catches what nobody ever opened.
     */
    suspend fun deleteSeenMessages(conversationId: String, messageIds: List<String>) {
        messageIds.forEach { messageId ->
            deleteQuietly {
                firestore.collection("conversations").document(conversationId)
                    .collection("messages").document(messageId).delete()
            }
        }
    }

    /** Live message stream for a user's support thread with the MindMingle team, oldest first. */
    fun observeSupportMessages(uid: String): Flow<List<Pair<String, MessageDto>>> {
        return firestore.collection("supportChats").document(uid).collection("messages")
            .orderBy("sentAt")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.id to doc.data<MessageDto>(serverTimestampBehavior = ServerTimestampBehavior.ESTIMATE)
                }
            }
    }

    /**
     * Appends a message to [uid]'s support thread and upserts the thread doc's preview fields.
     * [senderId] is either [uid] itself (the user wrote it) or "support" (an admin replied).
     */
    suspend fun sendSupportMessage(uid: String, userName: String, senderId: String, text: String) {
        firestore.collection("supportChats").document(uid).collection("messages")
            .add(MessageDto(senderId = senderId, text = text))
        firestore.collection("supportChats").document(uid)
            .set(
                SupportThreadDto(uid = uid, userName = userName, lastMessage = text),
                merge = true
            )
    }

    /** Admin-only: every user's support thread, most recently active first. */
    fun observeSupportThreads(): Flow<List<SupportThreadDto>> {
        return firestore.collection("supportChats")
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.data<SupportThreadDto>() } }
    }

    /**
     * AdMob settings for the Discover deck. Readable by any signed-in client, writable only by
     * an admin (firestore.rules) — the point is retuning ad frequency or swapping an ad unit id
     * without a store release. Returns null when the doc has not been seeded yet.
     */
    suspend fun getAdConfig(): AdConfigDto? {
        val doc = firestore.collection("appConfig").document("ads").get()
        return if (doc.exists) doc.data<AdConfigDto>() else null
    }

    /**
     * The minimum supported app version, read at launch by the Android in-app update gate.
     *
     * Unlike every other appConfig doc this one is readable *without* being signed in
     * (firestore.rules), because a forced update has to be enforceable on the login screen
     * too — a user stuck on an unsupported build often cannot sign in in the first place.
     * It holds nothing but version numbers. Returns null when the doc has not been seeded.
     */
    suspend fun getAppUpdateConfig(): AppUpdateConfigDto? {
        val doc = firestore.collection("appConfig").document("appUpdate").get()
        return if (doc.exists) doc.data<AppUpdateConfigDto>() else null
    }

    // ---------------------------------------------------------------------
    // MindMingle+ subscriptions (Razorpay). The client never sees a price or a key
    // secret: it asks a Cloud Function for an order, opens the Razorpay SDK
    // with it, and hands the signed result back for server-side verification
    // (functions/src/razorpay.ts). The resulting subscriptions/{uid} doc is
    // read-only to every client, which is what makes "premium hides ads"
    // impossible to fake locally.
    // ---------------------------------------------------------------------

    suspend fun createRazorpayOrder(planId: String, countryHint: String): CreateOrderResponseDto {
        return functions.httpsCallable("createRazorpayOrder")
            .invoke(CreateOrderRequestDto(planId = planId, countryHint = countryHint))
            .data<CreateOrderResponseDto>()
    }

    /**
     * The per-country price list, read straight from `appConfig/plans` — no Cloud Function is
     * involved. firestore.rules lets any signed-in client read the doc and only an admin write
     * it, so the price list is public-to-users and tamper-proof without a callable. Returns null
     * when the doc has not been seeded yet, which is when the client falls back to
     * [defaultPlanCatalog].
     */
    suspend fun getPlanCatalog(): PlanCatalogDto? {
        val doc = firestore.collection("appConfig").document("plans").get()
        return if (doc.exists) doc.data<PlanCatalogDto>() else null
    }

    /** Admin-only write of the whole price list; gated by firestore.rules (admins/{uid}). */
    suspend fun savePlanCatalog(catalog: PlanCatalogDto) {
        firestore.collection("appConfig").document("plans").set(catalog)
    }

    /**
     * Order history for [uid] (blank = the caller). Passing someone else's uid only works for
     * an admin — the function checks admins/{uid} before answering.
     */
    suspend fun getBillingHistory(uid: String): BillingHistoryResponseDto {
        return functions.httpsCallable("getBillingHistory")
            .invoke(BillingHistoryRequestDto(uid))
            .data<BillingHistoryResponseDto>()
    }

    /**
     * Admin-only: one page of subscribers, already joined with their profiles and filtered
     * server-side. See functions/src/subscribers.ts for why this is not a client query.
     */
    suspend fun adminListSubscribers(request: SubscriberListRequestDto): SubscriberListResponseDto {
        return functions.httpsCallable("adminListSubscribers")
            .invoke(request)
            .data<SubscriberListResponseDto>()
    }

    /** Admin-only: headline counts. Reads the whole collection, so it is fetched once per screen. */
    suspend fun adminSubscriberStats(): SubscriberStatsDto {
        return functions.httpsCallable("adminSubscriberStats")
            .invoke(SubscriberStatsRequestDto())
            .data<SubscriberStatsDto>()
    }

    /**
     * Reports a checkout that came back with an error. The server grants nothing from this —
     * it only records the attempt and emails the user that no money was taken.
     */
    suspend fun recordPaymentFailure(orderId: String, planId: String, reason: String) {
        functions.httpsCallable("recordPaymentFailure")
            .invoke(RecordPaymentFailureRequestDto(orderId = orderId, planId = planId, reason = reason))
    }


    suspend fun adminSetSubscription(uid: String, planId: String, days: Int): AdminSubscriptionResponseDto {
        return functions.httpsCallable("adminSetSubscription")
            .invoke(AdminSetSubscriptionRequestDto(uid = uid, planId = planId, days = days))
            .data<AdminSubscriptionResponseDto>()
    }

    suspend fun adminCancelSubscription(uid: String, immediate: Boolean): AdminSubscriptionResponseDto {
        return functions.httpsCallable("adminCancelSubscription")
            .invoke(AdminCancelSubscriptionRequestDto(uid = uid, immediate = immediate))
            .data<AdminSubscriptionResponseDto>()
    }

    // ---------------------------------------------------------------------
    // Account state the user controls themselves — deactivation and the
    // deletion request. All plain Firestore writes: firestore.rules is what
    // stops a client from clearing a deactivation early or un-requesting a
    // deletion, not this code.
    // ---------------------------------------------------------------------

    /** Starts a [days]-long deactivation window and returns the instant it ends. */
    suspend fun deactivateAccount(uid: String, days: Int): Long {
        val now = nowMillis()
        val reactivateAt = now + days * MILLIS_PER_DAY
        firestore.collection("users").document(uid).update(
            "isDeactivated" to true,
            "deactivatedAt" to now,
            "reactivateAt" to reactivateAt
        )
        return reactivateAt
    }

    /** Clears a finished deactivation. Rejected by the rules while the window is still running. */
    suspend fun clearDeactivation(uid: String) {
        firestore.collection("users").document(uid).update(
            "isDeactivated" to false,
            "deactivatedAt" to 0L,
            "reactivateAt" to 0L
        )
    }

    /**
     * Self-service deletion, client-side half. Removes everything this user owns and can reach
     * with their own credentials, files the deletionRequests/{uid} row an admin works from, and
     * flags the profile so every sign-in path turns them away from here on.
     *
     * What is deliberately left for the admin purge: conversations and their messages, the mirror rows
     * sitting in other people's mailboxes, the support thread, the subscription, and the Firebase
     * Auth account itself — none of which a client SDK is allowed to touch.
     */
    suspend fun requestAccountDeletion(user: UserDto) {
        val uid = user.uid
        deleteOwnPhotos(user.photoUrls)
        deleteOwnLikes(uid)
        deleteQuietly { firestore.collection("anonymousQueue").document(uid).delete() }
        deleteQuietly { firestore.collection("notificationPrefs").document(uid).delete() }
        deleteOwnDevices(uid)

        firestore.collection("deletionRequests").document(uid).set(
            DeletionRequestDto(
                uid = uid,
                name = user.name,
                email = user.email,
                phoneNumber = user.phoneNumber,
                requestedAt = nowMillis(),
                status = DeletionRequestDto.STATUS_PENDING
            )
        )

        // Keeps the doc (the admin queue needs something to show) but empties it of anything
        // another user could still see, and drops it out of Discover via isProfileComplete.
        firestore.collection("users").document(uid).update(
            "isDeletionRequested" to true,
            "deletionRequestedAt" to nowMillis(),
            "isProfileComplete" to false,
            "photoUrls" to emptyList<String>(),
            "avatarUrl" to ""
        )
    }

    private suspend fun deleteOwnPhotos(photoUrls: List<String>) {
        photoUrls.forEach { url ->
            deleteQuietly { storage.getReferenceFromUrl(url).delete() }
        }
    }

    /** Both directions of this user's likes, including the mirror row in the other mailbox. */
    private suspend fun deleteOwnLikes(uid: String) {
        val sentTo = firestore.collection("likes").document(uid).collection("sentTo").get().documents
        sentTo.forEach { doc ->
            deleteQuietly {
                firestore.collection("incomingLikes").document(doc.id).collection("from")
                    .document(uid).delete()
            }
            deleteQuietly {
                firestore.collection("likes").document(uid).collection("sentTo")
                    .document(doc.id).delete()
            }
        }

        val receivedFrom = firestore.collection("incomingLikes").document(uid).collection("from")
            .get().documents
        receivedFrom.forEach { doc ->
            deleteQuietly {
                firestore.collection("likes").document(doc.id).collection("sentTo")
                    .document(uid).delete()
            }
            deleteQuietly {
                firestore.collection("incomingLikes").document(uid).collection("from")
                    .document(doc.id).delete()
            }
        }
    }

    private suspend fun deleteOwnDevices(uid: String) {
        val devices = firestore.collection("users").document(uid).collection("devices")
            .get().documents
        devices.forEach { doc ->
            deleteQuietly {
                firestore.collection("users").document(uid).collection("devices")
                    .document(doc.id).delete()
            }
        }
    }

    // ---------------------------------------------------------------------
    // Admin purge — the other half of a deletion request, run from the admin
    // deletion queue. Every path below is admin-writable per firestore.rules.
    //
    // One thing no client SDK can do: delete somebody else's Firebase Auth
    // account. The bannedUids/{uid} tombstone written at the end is what keeps
    // that leftover Auth record permanently locked out (checkAccountStatus
    // rejects a banned uid on every sign-in path).
    // ---------------------------------------------------------------------

    suspend fun adminPurgeUser(uid: String, adminUid: String, reason: String) {
        val user = getUserById(uid)

        // Best-effort and expected to fail: storage.rules allows a photo delete only from its
        // own owner, so an admin's attempt is denied and the files stay behind as orphans. The
        // profile doc holding their URLs is deleted below, which is what makes them unreachable.
        deleteOwnPhotos(user?.photoUrls.orEmpty())
        purgeLikesBothSides(uid)
        purgeConversations(uid)
        purgeAnonymous(uid)
        purgeSubscription(uid)
        purgeSupportThread(uid)
        deleteOwnDevices(uid)
        deleteQuietly { firestore.collection("notificationPrefs").document(uid).delete() }
        deleteQuietly { firestore.collection("users").document(uid).delete() }

        firestore.collection("bannedUids").document(uid)
            .set(BannedUidDto(reason = reason, bannedBy = adminUid))

        markDeletionRequestDone(uid = uid, adminUid = adminUid)
    }

    private suspend fun purgeLikesBothSides(uid: String) {
        deleteOwnLikes(uid)
        deleteQuietly { firestore.collection("likes").document(uid).delete() }
        deleteQuietly { firestore.collection("incomingLikes").document(uid).delete() }
    }

    private suspend fun purgeConversations(uid: String) {
        val conversations = firestore.collection("conversations").where { "users" contains uid }.get().documents
        conversations.forEach { conversationDoc ->
            val messages = firestore.collection("conversations").document(conversationDoc.id)
                .collection("messages").get().documents
            messages.forEach { message ->
                deleteQuietly {
                    firestore.collection("conversations").document(conversationDoc.id)
                        .collection("messages").document(message.id).delete()
                }
            }
            deleteQuietly { firestore.collection("conversations").document(conversationDoc.id).delete() }
        }
    }

    private suspend fun purgeAnonymous(uid: String) {
        deleteQuietly { firestore.collection("anonymousQueue").document(uid).delete() }

        val auditEntries = firestore.collection("anonymousAuditLog")
            .where { "participants" contains uid }
            .get().documents
        auditEntries.forEach { doc ->
            deleteQuietly { firestore.collection("anonymousAuditLog").document(doc.id).delete() }
        }
    }

    private suspend fun purgeSubscription(uid: String) {
        val payments = firestore.collection("subscriptions").document(uid)
            .collection("payments").get().documents
        payments.forEach { payment ->
            deleteQuietly {
                firestore.collection("subscriptions").document(uid)
                    .collection("payments").document(payment.id).delete()
            }
        }
        deleteQuietly { firestore.collection("subscriptions").document(uid).delete() }
    }

    private suspend fun purgeSupportThread(uid: String) {
        val messages = firestore.collection("supportChats").document(uid)
            .collection("messages").get().documents
        messages.forEach { message ->
            deleteQuietly {
                firestore.collection("supportChats").document(uid)
                    .collection("messages").document(message.id).delete()
            }
        }
        deleteQuietly { firestore.collection("supportChats").document(uid).delete() }
    }

    private suspend fun markDeletionRequestDone(uid: String, adminUid: String) {
        val request = firestore.collection("deletionRequests").document(uid).get()
        if (!request.exists) return
        deleteQuietly {
            firestore.collection("deletionRequests").document(uid).update(
                "status" to DeletionRequestDto.STATUS_DELETED,
                "deletedAt" to nowMillis(),
                "deletedBy" to adminUid
            )
        }
    }

    /** Admin-only: the deletion queue, newest request first. */
    suspend fun listDeletionRequests(): List<DeletionRequestDto> {
        return firestore.collection("deletionRequests")
            .get()
            .documents
            .map { it.data<DeletionRequestDto>() }
            .sortedByDescending { it.requestedAt }
    }

    /**
     * A purge is a long chain of deletes with no transaction around it. One row that is already
     * gone, or one path the caller happens not to own, must not strand the rest of the cascade —
     * so every individual delete is best-effort and logged rather than thrown.
     */
    private suspend fun deleteQuietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "purge step failed, continuing" }
        }
    }

    suspend fun verifyRazorpayPayment(
        orderId: String,
        paymentId: String,
        signature: String
    ): VerifyPaymentResponseDto {
        return functions.httpsCallable("verifyRazorpayPayment")
            .invoke(VerifyPaymentRequestDto(orderId = orderId, paymentId = paymentId, signature = signature))
            .data<VerifyPaymentResponseDto>()
    }


    fun observeSubscription(uid: String): Flow<SubscriptionDto?> {
        return firestore.collection("subscriptions").document(uid)
            .snapshots
            .map { snapshot -> if (snapshot.exists) snapshot.data<SubscriptionDto>() else null }
    }

    // ---------------------------------------------------------------------
    // Push notifications. Tokens live under the user's own doc and are read by the
    // notification Cloud Functions with the Admin SDK; preferences sit in their own
    // doc so a profile save can never wipe them (profiles are written with set()).
    // ---------------------------------------------------------------------

    suspend fun registerDeviceToken(uid: String, token: String, platform: String) {
        firestore.collection("users").document(uid).collection("devices").document(token)
            .set(DeviceTokenDto(token = token, platform = platform, updatedAt = nowMillis()))
    }

    suspend fun unregisterDeviceToken(uid: String, token: String) {
        firestore.collection("users").document(uid).collection("devices").document(token).delete()
    }

    suspend fun getNotificationPrefs(uid: String): NotificationPrefsDto? {
        val doc = firestore.collection("notificationPrefs").document(uid).get()
        return if (doc.exists) doc.data<NotificationPrefsDto>() else null
    }

    suspend fun saveNotificationPrefs(uid: String, prefs: NotificationPrefsDto) {
        firestore.collection("notificationPrefs").document(uid).set(prefs)
    }

    // ---------------------------------------------------------------------
    // Ban / disable enforcement (checked by every consumer client on sign-in)
    // ---------------------------------------------------------------------

    suspend fun isUidBanned(uid: String): Boolean {
        return firestore.collection("bannedUids").document(uid).get().exists
    }

    // ---------------------------------------------------------------------
    // Admin — dashboard and account moderation. Admins sign in through the
    // same email-OTP/Google flow as regular users (see AuthViewModel); what makes
    // them an admin is userType on their own profile doc. Every write here also
    // requires that same userType per Firestore rules, so these methods are inert
    // for a non-admin caller regardless.
    // ---------------------------------------------------------------------

    suspend fun signOutCurrentUser() {
        Firebase.auth.signOut()
    }

    /**
     * Admin rights live on the profile doc itself: `users/{uid}.userType == "admin"`. The field is
     * immutable to its owner in firestore.rules, so it can only ever have been set from the
     * Firebase Console — reading it here matches exactly what the rules will enforce.
     */
    suspend fun isUidAdmin(uid: String): Boolean {
        val doc = firestore.collection("users").document(uid).get()
        return doc.exists && doc.data<UserDto>().userType.equals("admin", ignoreCase = true)
    }

    // ---------------------------------------------------------------------
    // Email OTP — desktop's primary sign-in (no working Google OAuth on JVM).
    // Both calls hit Cloud Functions (functions/src/index.ts); the code itself
    // never touches the client, it's generated/checked server-side and mailed
    // via the "Trigger Email" Firestore extension. verifyEmailOtp additionally
    // flips admins/{uid} server-side when the email matches the reserved admin
    // address, so a later Google sign-in with that same email is recognized as
    // admin automatically (Firebase Auth resolves both to the same uid by email).
    // ---------------------------------------------------------------------

    @Serializable
    private data class EmailOtpRequest(val email: String)

    @Serializable
    private data class VerifyEmailOtpRequest(val email: String, val code: String)

    @Serializable
    private data class VerifyEmailOtpResponse(val customToken: String, val uid: String)

    suspend fun requestEmailOtp(email: String) {
        functions.httpsCallable("requestEmailOtp").invoke(EmailOtpRequest(email))
    }

    // ---------------------------------------------------------------------
    // Email + password — desktop's sign-in (no working Google OAuth on JVM) and
    // mobile's alternative to Google. Pure Firebase Auth: no Cloud Function, no
    // mailed code. Needs the Email/Password provider enabled in Firebase Console
    // → Authentication → Sign-in method.
    // ---------------------------------------------------------------------

    /** Signs an existing account in. Throws when the address is unknown or the password is wrong. */
    suspend fun signInWithEmailPassword(email: String, password: String): String {
        return Firebase.auth.signInWithEmailAndPassword(email, password).user?.uid
            ?: throw IllegalStateException("Firebase returned no user for $email")
    }

    /** Registers a new account and leaves it signed in. Throws when the address is already taken. */
    suspend fun createAccountWithEmailPassword(email: String, password: String): String {
        return Firebase.auth.createUserWithEmailAndPassword(email, password).user?.uid
            ?: throw IllegalStateException("Firebase returned no user for $email")
    }

    /** Verifies the mailed code, signs the caller in via the returned custom token, and returns the uid. */
    suspend fun verifyEmailOtpAndSignIn(email: String, code: String): String {
        val response = functions.httpsCallable("verifyEmailOtp")
            .invoke(VerifyEmailOtpRequest(email, code))
            .data<VerifyEmailOtpResponse>()
        Firebase.auth.signInWithCustomToken(response.customToken)
        return response.uid
    }

    /**
     * One page of the admin user list, ordered by uid so the cursor is stable and unique (every
     * profile doc stores its own uid, and doc ids never collide). Paging keeps the read count
     * proportional to what the admin actually looks at instead of to the size of the table.
     */
    suspend fun listUsers(pageSize: Int, startAfterUid: String?): List<UserDto> {
        val base = firestore.collection("users").orderBy("uid").limit(pageSize)
        val page = if (startAfterUid.isNullOrBlank()) {
            base
        } else {
            base.startAfterFieldValues { add(startAfterUid) }
        }
        return page.get().documents.map { it.data<UserDto>() }
    }

    /**
     * Dashboard totals as three aggregation queries — Firestore counts server-side and bills
     * roughly one read per 1000 index entries, so this costs the same at 200 users as at 200k.
     * It used to fetch every user, every conversation and then every message of every one, which grew
     * without bound: one dashboard load eventually meant a million document reads.
     *
     * The message total spans the `messages` collection group, so it counts support-thread
     * messages alongside one-to-one chat. Admins can already read both (firestore.rules), and counting
     * never exposes content.
     */
    suspend fun getAdminStats(): Triple<Int, Int, Int> {
        val userCount = firestore.collection("users").count().toInt()
        val conversationCount = firestore.collection("conversations").count().toInt()
        val messageCount = firestore.collectionGroup("messages").count().toInt()
        return Triple(userCount, conversationCount, messageCount)
    }

    suspend fun setUserDisabled(uid: String, disabled: Boolean) {
        firestore.collection("users").document(uid).update("isDisabled" to disabled)
    }

    suspend fun banUid(uid: String, reason: String, adminUid: String) {
        firestore.collection("bannedUids").document(uid)
            .set(BannedUidDto(reason = reason, bannedBy = adminUid))
    }

    suspend fun unbanUid(uid: String) {
        firestore.collection("bannedUids").document(uid).delete()
    }

    /** Account purge lives further up this file — see requestAccountDeletion / adminPurgeUser. */

}