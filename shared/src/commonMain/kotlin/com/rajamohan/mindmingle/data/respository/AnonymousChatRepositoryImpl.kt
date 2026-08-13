package com.rajamohan.mindmingle.data.respository

import com.rajamohan.mindmingle.data.remote.source.AnonymousChatRemoteSource
import com.rajamohan.mindmingle.domain.model.AnonymousSession
import com.rajamohan.mindmingle.domain.model.anonymousAccentIndexFor
import com.rajamohan.mindmingle.domain.model.anonymousAliasFor
import com.rajamohan.mindmingle.domain.repository.AnonymousChatRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

internal class AnonymousChatRepositoryImpl(
    private val remoteSource: AnonymousChatRemoteSource
) : AnonymousChatRepository {

    private companion object {
        const val TAG = "AnonymousChatRepository"
        const val ACCENT_COUNT = 5
        const val CLAIM_SETTLE_MILLIS = 350L
        const val SEARCH_TIMEOUT_MILLIS = 20_000L
        const val REASON_SKIPPED = "skipped"
        const val REASON_LEFT = "left"
    }

    private val skippedUids = mutableSetOf<String>()

    private var activeSessionId: String? = null
    private var activePeerUid: String? = null
    private var activeOpenedBy: String = ""

    override suspend fun findPartner(uid: String): AnonymousSession? {
        return try {
            remoteSource.enqueue(uid)
            claimWaitingPartner(uid) ?: awaitIncomingClaim(uid)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "findPartner failed for uid=$uid" }
            runCatching { remoteSource.leaveQueue(uid) }
            null
        }
    }

    private suspend fun claimWaitingPartner(uid: String): AnonymousSession? {
        val candidates = remoteSource.waitingUids(uid).filterNot { skippedUids.contains(it) }

        for (peerUid in candidates) {
            val sessionId = remoteSource.newSessionId()
            remoteSource.createSession(sessionId = sessionId, selfUid = uid, peerUid = peerUid)

            val claimed = remoteSource.claimPeer(peerUid = peerUid, sessionId = sessionId)
            if (!claimed) {
                runCatching { remoteSource.purgeSession(sessionId) }
                continue
            }

            delay(CLAIM_SETTLE_MILLIS)
            val settledSessionId = remoteSource.readQueueEntry(peerUid)?.sessionId
            if (settledSessionId != sessionId) {
                runCatching { remoteSource.purgeSession(sessionId) }
                continue
            }

            val counterClaim = remoteSource.readQueueEntry(uid)?.sessionId.orEmpty()
            if (counterClaim.isNotEmpty() && counterClaim != sessionId) {
                if (uid > peerUid) {
                    runCatching { remoteSource.purgeSession(sessionId) }
                    val adopted = remoteSource.readSession(counterClaim) ?: continue
                    if (!adopted.participants.contains(peerUid)) continue
                    remoteSource.bindOwnQueueEntry(uid = uid, sessionId = counterClaim)
                    return rememberSession(
                        sessionId = counterClaim,
                        peerUid = peerUid,
                        openedBy = adopted.openedBy.ifEmpty { peerUid }
                    )
                }
                runCatching { remoteSource.purgeSession(counterClaim) }
            }

            remoteSource.bindOwnQueueEntry(uid = uid, sessionId = sessionId)
            return rememberSession(sessionId = sessionId, peerUid = peerUid, openedBy = uid)
        }
        return null
    }

    private suspend fun awaitIncomingClaim(uid: String): AnonymousSession? {
        val sessionId = withTimeoutOrNull(SEARCH_TIMEOUT_MILLIS) {
            remoteSource.observeQueueEntry(uid)
                .map { entry -> entry?.sessionId.orEmpty() }
                .first { it.isNotEmpty() }
        }

        if (sessionId.isNullOrEmpty()) {
            runCatching { remoteSource.leaveQueue(uid) }
            return null
        }

        val session = remoteSource.readSession(sessionId)
        val peerUid = session?.participants?.firstOrNull { it != uid }
        if (peerUid == null) {
            runCatching { remoteSource.leaveQueue(uid) }
            return null
        }

        if (skippedUids.contains(peerUid)) {
            runCatching {
                remoteSource.endSession(sessionId, uid)
                remoteSource.purgeSession(sessionId)
                remoteSource.leaveQueue(uid)
            }
            return null
        }

        return rememberSession(
            sessionId = sessionId,
            peerUid = peerUid,
            openedBy = session.openedBy.ifEmpty { peerUid }
        )
    }

    private fun rememberSession(sessionId: String, peerUid: String, openedBy: String): AnonymousSession {
        activeSessionId = sessionId
        activePeerUid = peerUid
        activeOpenedBy = openedBy
        return AnonymousSession(
            sessionId = sessionId,
            peerAlias = anonymousAliasFor(sessionId + peerUid),
            peerAccentIndex = anonymousAccentIndexFor(sessionId + peerUid, ACCENT_COUNT)
        )
    }

    override fun observeIncomingMessages(sessionId: String, selfUid: String): Flow<Pair<String, String>> {
        val delivered = mutableSetOf<String>()
        return flow {
            remoteSource.observeRelay(sessionId).collect { relayItems ->
                for ((messageId, relay) in relayItems) {
                    if (relay.senderId == selfUid || relay.senderId.isEmpty()) continue
                    if (!delivered.add(messageId)) continue
                    emit(messageId to relay.text)
                    runCatching { remoteSource.deleteRelay(sessionId, messageId) }
                }
            }
        }.catch { error ->
            Napier.e(throwable = error, tag = TAG) { "observeIncomingMessages failed for session=$sessionId" }
        }
    }

    override fun observePartnerPresence(sessionId: String, selfUid: String): Flow<Boolean> {
        return remoteSource.observeSession(sessionId)
            .map { session ->
                session != null && (session.endedBy.isEmpty() || session.endedBy == selfUid)
            }
            .catch { error ->
                Napier.e(throwable = error, tag = TAG) { "observePartnerPresence failed for session=$sessionId" }
                emit(false)
            }
    }

    override suspend fun sendMessage(sessionId: String, selfUid: String, text: String): Boolean {
        return try {
            remoteSource.pushRelay(sessionId = sessionId, senderId = selfUid, text = text)
            true
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "sendMessage failed for session=$sessionId" }
            false
        }
    }

    override suspend fun skipPartner(uid: String) {
        activePeerUid?.let { skippedUids.add(it) }
        closeActiveSession(uid = uid, reason = REASON_SKIPPED)
    }

    override suspend fun leave(uid: String) {
        closeActiveSession(uid = uid, reason = REASON_LEFT)
    }

    private suspend fun closeActiveSession(uid: String, reason: String) {
        val sessionId = activeSessionId
        val peerUid = activePeerUid
        val openedBy = activeOpenedBy

        activeSessionId = null
        activePeerUid = null
        activeOpenedBy = ""

        runCatching { remoteSource.leaveQueue(uid) }

        if (sessionId == null) return

        runCatching {
            remoteSource.recordAudit(
                sessionId = sessionId,
                participants = listOfNotNull(uid, peerUid),
                openedBy = openedBy,
                closedBy = uid,
                closeReason = reason
            )
        }
        runCatching { remoteSource.endSession(sessionId = sessionId, uid = uid) }
        runCatching { remoteSource.purgeSession(sessionId) }
    }
}
