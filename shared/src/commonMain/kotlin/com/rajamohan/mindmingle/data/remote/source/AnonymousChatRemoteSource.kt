package com.rajamohan.mindmingle.data.remote.source

import com.rajamohan.mindmingle.data.remote.dto.AnonymousAuditDto
import com.rajamohan.mindmingle.data.remote.dto.AnonymousQueueDto
import com.rajamohan.mindmingle.data.remote.dto.AnonymousRelayDto
import com.rajamohan.mindmingle.data.remote.dto.AnonymousSessionDto
import com.rajamohan.mindmingle.domain.model.nowMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AnonymousChatRemoteSource {

    private companion object {
        const val QUEUE = "anonymousQueue"
        const val SESSIONS = "anonymousSessions"
        const val RELAY = "relay"
        const val AUDIT = "anonymousAuditLog"

        /**
         * Waiting slots to read when looking for a partner. Only one of them is needed, and the
         * rest are tried in order when a claim loses the race — reading the whole queue would make
         * every pairing attempt cost one read per person waiting.
         */
        const val QUEUE_SCAN_LIMIT = 20

        /** A waiting slot nobody claimed is swept after this long. */
        const val QUEUE_TTL_MILLIS = 60L * 60L * 1000L

        /** Backstop lifetime for a room, and for anything relayed inside it, if no client purges. */
        const val SESSION_TTL_MILLIS = 12L * 60L * 60L * 1000L
        const val RELAY_TTL_MILLIS = 60L * 60L * 1000L
    }

    private val firestore get() = Firebase.firestore

    private fun expiryAfter(millis: Long): Timestamp {
        val expiresAtMillis = nowMillis() + millis
        return Timestamp(seconds = expiresAtMillis / 1000L, nanoseconds = 0)
    }

    fun newSessionId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..20).map { chars.random() }.joinToString("")
    }

    suspend fun enqueue(uid: String) {
        firestore.collection(QUEUE).document(uid).set(
            AnonymousQueueDto(uid = uid, expiresAt = expiryAfter(QUEUE_TTL_MILLIS))
        )
    }

    /**
     * The oldest unclaimed slots, longest wait first. Ordered and capped server-side: the caller
     * only needs one partner, and walks the rest only when a claim loses the compare-and-set race
     * in firestore.rules.
     */
    suspend fun waitingUids(uid: String): List<String> {
        return firestore.collection(QUEUE)
            .where { "sessionId" equalTo "" }
            .orderBy("joinedAt")
            .limit(QUEUE_SCAN_LIMIT)
            .get()
            .documents
            .map { it.data<AnonymousQueueDto>() }
            .filter { it.uid.isNotEmpty() && it.uid != uid }
            .map { it.uid }
    }

    suspend fun createSession(sessionId: String, selfUid: String, peerUid: String) {
        firestore.collection(SESSIONS).document(sessionId).set(
            AnonymousSessionDto(
                participants = listOf(selfUid, peerUid),
                openedBy = selfUid,
                expiresAt = expiryAfter(SESSION_TTL_MILLIS)
            )
        )
    }

    suspend fun claimPeer(peerUid: String, sessionId: String): Boolean {
        return try {
            firestore.collection(QUEUE).document(peerUid).update("sessionId" to sessionId)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun bindOwnQueueEntry(uid: String, sessionId: String) {
        firestore.collection(QUEUE).document(uid).update("sessionId" to sessionId)
    }

    suspend fun readQueueEntry(uid: String): AnonymousQueueDto? {
        val document = firestore.collection(QUEUE).document(uid).get()
        return if (document.exists) document.data<AnonymousQueueDto>() else null
    }

    fun observeQueueEntry(uid: String): Flow<AnonymousQueueDto?> {
        return firestore.collection(QUEUE).document(uid).snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data<AnonymousQueueDto>() else null
        }
    }

    suspend fun readSession(sessionId: String): AnonymousSessionDto? {
        val document = firestore.collection(SESSIONS).document(sessionId).get()
        return if (document.exists) document.data<AnonymousSessionDto>() else null
    }

    fun observeSession(sessionId: String): Flow<AnonymousSessionDto?> {
        return firestore.collection(SESSIONS).document(sessionId).snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data<AnonymousSessionDto>() else null
        }
    }

    fun observeRelay(sessionId: String): Flow<List<Pair<String, AnonymousRelayDto>>> {
        return firestore.collection(SESSIONS).document(sessionId).collection(RELAY)
            .orderBy("sentAt")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { document ->
                    document.id to document.data<AnonymousRelayDto>(
                        serverTimestampBehavior = ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
    }

    suspend fun pushRelay(sessionId: String, senderId: String, text: String) {
        firestore.collection(SESSIONS).document(sessionId).collection(RELAY)
            .add(
                AnonymousRelayDto(
                    senderId = senderId,
                    text = text,
                    expiresAt = expiryAfter(RELAY_TTL_MILLIS)
                )
            )
    }

    suspend fun deleteRelay(sessionId: String, messageId: String) {
        firestore.collection(SESSIONS).document(sessionId).collection(RELAY)
            .document(messageId).delete()
    }

    suspend fun endSession(sessionId: String, uid: String) {
        firestore.collection(SESSIONS).document(sessionId).update("endedBy" to uid)
    }

    suspend fun purgeSession(sessionId: String) {
        val relayDocuments = firestore.collection(SESSIONS).document(sessionId)
            .collection(RELAY).get().documents
        for (document in relayDocuments) {
            firestore.collection(SESSIONS).document(sessionId).collection(RELAY)
                .document(document.id).delete()
        }
        firestore.collection(SESSIONS).document(sessionId).delete()
    }

    suspend fun leaveQueue(uid: String) {
        firestore.collection(QUEUE).document(uid).delete()
    }

    suspend fun recordAudit(
        sessionId: String,
        participants: List<String>,
        openedBy: String,
        closedBy: String,
        closeReason: String
    ) {
        firestore.collection(AUDIT).document("${sessionId}_$closedBy").set(
            AnonymousAuditDto(
                sessionId = sessionId,
                participants = participants,
                openedBy = openedBy,
                closedBy = closedBy,
                closeReason = closeReason
            )
        )
    }
}
