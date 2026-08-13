package com.rajamohan.mindmingle.data.local.source

import com.rajamohan.mindmingle.core.LMPreferences
import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class EphemeralRecord(
    val id: String,
    val sessionId: String,
    val text: String,
    val sequence: Long,
    val seen: Boolean
)

internal class EphemeralMessageLocalSource(
    private val preferences: LMPreferences
) {

    private companion object {
        const val TAG = "EphemeralMessageStore"
        const val RECORDS_KEY = "anonymous_pending_messages"
        const val SEQUENCE_KEY = "anonymous_message_sequence"
        const val EMPTY_RECORDS = "[]"
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun readAll(): List<EphemeralRecord> {
        val raw = preferences.get(RECORDS_KEY, EMPTY_RECORDS, String::class)
        return try {
            json.decodeFromString<List<EphemeralRecord>>(raw)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "readAll failed, resetting local store" }
            emptyList()
        }
    }

    suspend fun writeAll(records: List<EphemeralRecord>) {
        preferences.update(RECORDS_KEY, json.encodeToString(records), String::class)
    }

    suspend fun nextSequence(): Long {
        val current = preferences.get(SEQUENCE_KEY, 0, Int::class)
        val next = current + 1
        preferences.update(SEQUENCE_KEY, next, Int::class)
        return next.toLong()
    }

    suspend fun clear() {
        preferences.update(RECORDS_KEY, EMPTY_RECORDS, String::class)
    }
}
