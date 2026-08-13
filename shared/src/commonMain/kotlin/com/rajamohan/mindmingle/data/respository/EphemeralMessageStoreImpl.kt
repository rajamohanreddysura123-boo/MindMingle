package com.rajamohan.mindmingle.data.respository

import com.rajamohan.mindmingle.data.local.source.EphemeralMessageLocalSource
import com.rajamohan.mindmingle.data.local.source.EphemeralRecord
import com.rajamohan.mindmingle.domain.model.AnonymousMessage
import com.rajamohan.mindmingle.domain.repository.EphemeralMessageStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EphemeralMessageStoreImpl(
    private val localSource: EphemeralMessageLocalSource
) : EphemeralMessageStore {

    private val mutex = Mutex()

    override suspend fun saveIncoming(sessionId: String, messageId: String, text: String): AnonymousMessage {
        return mutex.withLock {
            val sequence = localSource.nextSequence()
            val record = EphemeralRecord(
                id = messageId,
                sessionId = sessionId,
                text = text,
                sequence = sequence,
                seen = false
            )
            val records = localSource.readAll().filterNot { it.id == messageId } + record
            localSource.writeAll(records)
            record.toDomain()
        }
    }

    override suspend fun markSeen(messageIds: List<String>) {
        mutex.withLock {
            val ids = messageIds.toSet()
            val records = localSource.readAll().map { record ->
                if (ids.contains(record.id)) record.copy(seen = true) else record
            }
            localSource.writeAll(records)
        }
    }

    override suspend fun purgeSeen() {
        mutex.withLock {
            val remaining = localSource.readAll().filterNot { it.seen }
            localSource.writeAll(remaining)
        }
    }

    override suspend fun pendingMessages(): List<AnonymousMessage> {
        return mutex.withLock {
            localSource.readAll()
                .filterNot { it.seen }
                .sortedBy { it.sequence }
                .map { it.toDomain() }
        }
    }

    override suspend fun clearAll() {
        mutex.withLock {
            localSource.clear()
        }
    }
}

private fun EphemeralRecord.toDomain(): AnonymousMessage = AnonymousMessage(
    id = id,
    sessionId = sessionId,
    text = text,
    isMine = false,
    sequence = sequence,
    isSeen = seen
)
