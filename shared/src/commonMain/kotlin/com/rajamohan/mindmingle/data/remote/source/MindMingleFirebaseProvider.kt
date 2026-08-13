package com.rajamohan.mindmingle.data.remote.source

import com.rajamohan.mindmingle.core.media.toStorageData
import com.rajamohan.mindmingle.data.remote.dto.AdConfigDto
import com.rajamohan.mindmingle.data.remote.dto.BannedUidDto
import com.rajamohan.mindmingle.data.remote.dto.CreateOrderRequestDto
import com.rajamohan.mindmingle.data.remote.dto.AdminCancelSubscriptionRequestDto
import com.rajamohan.mindmingle.data.remote.dto.AdminDeleteUserRequestDto
import com.rajamohan.mindmingle.data.remote.dto.AdminSetSubscriptionRequestDto
import com.rajamohan.mindmingle.data.remote.dto.AdminSubscriptionResponseDto
import com.rajamohan.mindmingle.data.remote.dto.BillingHistoryRequestDto
import com.rajamohan.mindmingle.data.remote.dto.BillingHistoryResponseDto
import com.rajamohan.mindmingle.data.remote.dto.DeleteAccountResponseDto
import com.rajamohan.mindmingle.data.remote.dto.DeviceTokenDto
import com.rajamohan.mindmingle.data.remote.dto.NotificationPrefsDto
import com.rajamohan.mindmingle.data.remote.dto.EmptyRequestDto
import com.rajamohan.mindmingle.data.remote.dto.PaymentDetailsRequestDto
import com.rajamohan.mindmingle.data.remote.dto.PaymentDetailsResponseDto
import com.rajamohan.mindmingle.data.remote.dto.PlanPricingRequestDto
import com.rajamohan.mindmingle.data.remote.dto.PlanPricingResponseDto
import com.rajamohan.mindmingle.data.remote.dto.SavePlanPricingRequestDto
import com.rajamohan.mindmingle.data.remote.dto.SavePlanPricingResponseDto
import com.rajamohan.mindmingle.data.remote.dto.CreateOrderResponseDto
import com.rajamohan.mindmingle.data.remote.dto.LikeDto
import com.rajamohan.mindmingle.data.remote.dto.MatchDto
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
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions
import dev.gitlive.firebase.storage.FirebaseStorageMetadata
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

internal class MindMingleFirebaseProvider {

    private val firestore get() = Firebase.firestore
    private val storage get() = Firebase.storage

    /** The signed-in Firebase Auth uid, or null if no session exists yet. */
    fun getCurrentUid(): String? = Firebase.auth.currentUser?.uid

    fun sendPhoneOtp(
        phoneNumber: String,
        onCodeSent: (verificationId: String) -> Unit,
        onError: (message: String) -> Unit
    ) {
        PhoneOtpSender.sendOtp(phoneNumber, onCodeSent, onError)
    }

    fun verifyPhoneOtp(
        verificationId: String,
        code: String,
        onSuccess: () -> Unit,
        onError: (message: String) -> Unit
    ) {
        PhoneOtpSender.verifyOtp(verificationId, code, onSuccess, onError)
    }

    suspend fun saveUser(user: UserDto) {
        firestore.collection("users")
            .document(user.uid)
            .set(user)
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
        val detailFilters: Map<String, List<String>> = emptyMap()
    )

    @Serializable
    private data class DiscoverProfilesResponse(val profiles: List<UserDto> = emptyList())

    /** Server-side filtered Discover feed — see oo_backend_services/src/index.ts (filterDiscoverProfiles). */
    suspend fun getDiscoverProfiles(excludeUid: String, filters: DiscoverFilterCriteria): List<UserDto> {
        return Firebase.functions.httpsCallable("filterDiscoverProfiles")
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
                    detailFilters = filters.detailFilters.mapValues { (_, values) -> values.toList() }
                )
            )
            .data<DiscoverProfilesResponse>()
            .profiles
            .filter { it.uid != excludeUid }
    }

    /** Returns true when the like is mutual and a match was created. */
    suspend fun likeUser(fromUid: String, toUid: String): Boolean {
        firestore.collection("likes").document(fromUid).collection("sentTo")
            .document(toUid).set(LikeDto())
        firestore.collection("incomingLikes").document(toUid).collection("from")
            .document(fromUid).set(LikeDto())

        val reverseLike = firestore.collection("likes").document(toUid).collection("sentTo")
            .document(fromUid).get()

        if (reverseLike.exists) {
            val matchId = listOf(fromUid, toUid).sorted().joinToString("_")
            firestore.collection("matches").document(matchId)
                .set(MatchDto(users = listOf(fromUid, toUid)))
            return true
        }
        return false
    }

    suspend fun getIncomingLikes(uid: String): List<UserDto> {
        val likerUids = firestore.collection("incomingLikes").document(uid).collection("from")
            .get()
            .documents
            .map { it.id }

        return likerUids.mapNotNull { likerUid ->
            val doc = firestore.collection("users").document(likerUid).get()
            if (doc.exists) doc.data<UserDto>() else null
        }
    }

    /** Live "who liked me" stream — recomputed on every new incoming like. */
    fun observeIncomingLikes(uid: String): Flow<List<UserDto>> {
        return firestore.collection("incomingLikes").document(uid).collection("from")
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { likeDoc ->
                    val doc = firestore.collection("users").document(likeDoc.id).get()
                    if (doc.exists) doc.data<UserDto>() else null
                }
            }
    }

    suspend fun getMatches(uid: String): List<Pair<String, MatchDto>> {
        return firestore.collection("matches")
            .where { "users" contains uid }
            .get()
            .documents
            .map { it.id to it.data<MatchDto>() }
    }

    suspend fun getUserById(uid: String): UserDto? {
        val doc = firestore.collection("users").document(uid).get()
        return if (doc.exists) doc.data<UserDto>() else null
    }

    /** Live message stream for a match, oldest first, each paired with its Firestore document id. */
    fun observeMessages(matchId: String): Flow<List<Pair<String, MessageDto>>> {
        return firestore.collection("matches").document(matchId).collection("messages")
            .orderBy("sentAt")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    doc.id to doc.data<MessageDto>(serverTimestampBehavior = ServerTimestampBehavior.ESTIMATE)
                }
            }
    }

    suspend fun sendMessage(matchId: String, senderId: String, text: String) {
        firestore.collection("matches").document(matchId).collection("messages")
            .add(MessageDto(senderId = senderId, text = text))
        firestore.collection("matches").document(matchId)
            .update(
                "lastMessage" to text,
                "lastMessageAt" to FieldValue.serverTimestamp
            )
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

    // ---------------------------------------------------------------------
    // MindMingle+ subscriptions (Razorpay). The client never sees a price or a key
    // secret: it asks a Cloud Function for an order, opens the Razorpay SDK
    // with it, and hands the signed result back for server-side verification
    // (functions/src/razorpay.ts). The resulting subscriptions/{uid} doc is
    // read-only to every client, which is what makes "premium hides ads"
    // impossible to fake locally.
    // ---------------------------------------------------------------------

    suspend fun createRazorpayOrder(planId: String, countryHint: String): CreateOrderResponseDto {
        return Firebase.functions.httpsCallable("createRazorpayOrder")
            .invoke(CreateOrderRequestDto(planId = planId, countryHint = countryHint))
            .data<CreateOrderResponseDto>()
    }

    /**
     * The per-country price list plus the market this caller is billed in. The country comes
     * back from the server (resolved from the profile's phone number), so a patched client
     * cannot shop for a cheaper market.
     */
    suspend fun getPlanPricing(countryHint: String): PlanPricingResponseDto {
        return Firebase.functions.httpsCallable("getPlanPricing")
            .invoke(PlanPricingRequestDto(countryHint))
            .data<PlanPricingResponseDto>()
    }

    /** Admin-only; also gated server-side by admins/{uid}. */
    suspend fun savePlanPricing(request: SavePlanPricingRequestDto): Int {
        return Firebase.functions.httpsCallable("savePlanPricing")
            .invoke(request)
            .data<SavePlanPricingResponseDto>()
            .saved
    }

    /**
     * Order history for [uid] (blank = the caller). Passing someone else's uid only works for
     * an admin — the function checks admins/{uid} before answering.
     */
    suspend fun getBillingHistory(uid: String): BillingHistoryResponseDto {
        return Firebase.functions.httpsCallable("getBillingHistory")
            .invoke(BillingHistoryRequestDto(uid))
            .data<BillingHistoryResponseDto>()
    }

    /**
     * Re-reads one payment straight from Razorpay. Doubles as a repair path: a captured
     * payment that never got recorded is granted server-side while answering.
     */
    suspend fun getPaymentDetails(paymentId: String): PaymentDetailsResponseDto {
        return Firebase.functions.httpsCallable("getPaymentDetails")
            .invoke(PaymentDetailsRequestDto(paymentId))
            .data<PaymentDetailsResponseDto>()
    }

    suspend fun adminSetSubscription(uid: String, planId: String, days: Int): AdminSubscriptionResponseDto {
        return Firebase.functions.httpsCallable("adminSetSubscription")
            .invoke(AdminSetSubscriptionRequestDto(uid = uid, planId = planId, days = days))
            .data<AdminSubscriptionResponseDto>()
    }

    suspend fun adminCancelSubscription(uid: String, immediate: Boolean): AdminSubscriptionResponseDto {
        return Firebase.functions.httpsCallable("adminCancelSubscription")
            .invoke(AdminCancelSubscriptionRequestDto(uid = uid, immediate = immediate))
            .data<AdminSubscriptionResponseDto>()
    }

    /**
     * Server-side account purge — profile, likes, matches, messages, anonymous rooms,
     * subscription, photos and the Firebase Auth account itself. A client SDK cannot do most
     * of that, which is why both delete paths go through Cloud Functions.
     */
    suspend fun deleteMyAccount(): Boolean {
        return Firebase.functions.httpsCallable("deleteMyAccount")
            .invoke(EmptyRequestDto())
            .data<DeleteAccountResponseDto>()
            .deleted
    }

    suspend fun adminDeleteUser(uid: String, ban: Boolean, reason: String): Boolean {
        return Firebase.functions.httpsCallable("adminDeleteUser")
            .invoke(AdminDeleteUserRequestDto(uid = uid, ban = ban, reason = reason))
            .data<DeleteAccountResponseDto>()
            .deleted
    }

    /** Admin-only; restores every row to the seed table shipped with the functions. */
    suspend fun resetPlanPricing(): Int {
        return Firebase.functions.httpsCallable("resetPlanPricing")
            .invoke(EmptyRequestDto())
            .data<SavePlanPricingResponseDto>()
            .saved
    }

    suspend fun verifyRazorpayPayment(
        orderId: String,
        paymentId: String,
        signature: String
    ): VerifyPaymentResponseDto {
        return Firebase.functions.httpsCallable("verifyRazorpayPayment")
            .invoke(VerifyPaymentRequestDto(orderId = orderId, paymentId = paymentId, signature = signature))
            .data<VerifyPaymentResponseDto>()
    }

    suspend fun getSubscription(uid: String): SubscriptionDto? {
        val doc = firestore.collection("subscriptions").document(uid).get()
        return if (doc.exists) doc.data<SubscriptionDto>() else null
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
    // same phone-OTP/Google/email-OTP flow as regular users (see AuthViewModel).
    // Every write here also requires an admins/{uid} doc per Firestore rules;
    // these methods are inert for a non-admin caller regardless.
    // ---------------------------------------------------------------------

    suspend fun signOutCurrentUser() {
        Firebase.auth.signOut()
    }

    suspend fun isUidAdmin(uid: String): Boolean {
        return firestore.collection("admins").document(uid).get().exists
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
        Firebase.functions.httpsCallable("requestEmailOtp").invoke(EmailOtpRequest(email))
    }

    /** Verifies the mailed code, signs the caller in via the returned custom token, and returns the uid. */
    suspend fun verifyEmailOtpAndSignIn(email: String, code: String): String {
        val response = Firebase.functions.httpsCallable("verifyEmailOtp")
            .invoke(VerifyEmailOtpRequest(email, code))
            .data<VerifyEmailOtpResponse>()
        Firebase.auth.signInWithCustomToken(response.customToken)
        return response.uid
    }

    suspend fun listAllUsers(): List<UserDto> {
        return firestore.collection("users")
            .get()
            .documents
            .map { it.data<UserDto>() }
    }

    suspend fun getAdminStats(): Triple<Int, Int, Int> {
        val userCount = firestore.collection("users").get().documents.size
        val matchDocs = firestore.collection("matches").get().documents
        val messageCount = matchDocs.sumOf { matchDoc ->
            firestore.collection("matches").document(matchDoc.id).collection("messages")
                .get().documents.size
        }
        return Triple(userCount, matchDocs.size, messageCount)
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

    /**
     * Full account purge now lives in the deleteMyAccount / adminDeleteUser Cloud Functions
     * (functions/src/account.ts) — a client SDK cannot delete a Firebase Auth user, Storage
     * objects, or the other half of someone else's like, so a client-side cascade always left
     * records behind.
     */

}