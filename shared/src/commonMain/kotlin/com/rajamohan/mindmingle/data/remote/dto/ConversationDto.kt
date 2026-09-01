package com.rajamohan.mindmingle.data.remote.dto

import dev.gitlive.firebase.firestore.BaseTimestamp
import kotlinx.serialization.Serializable

/**
 * A one-to-one conversation. Chat lives underneath it (`conversations/{conversationId}/messages`),
 * and firestore.rules only lets the two people named in [users] near any of it.
 *
 * The document id is the two uids sorted and joined with "_", so a pair of users always resolves
 * to the same conversation no matter who opens it first. A conversation is created the moment one
 * user likes another — there is no mutual-like requirement.
 *
 * [names] and [avatarUrls] are copies of each participant's card, keyed by uid. They are here for
 * two reasons: the chat list draws a row without reading either profile document, and a profile
 * that has since been deleted leaves nothing to read at all — with the copy, that conversation
 * renders as "Unknown user" instead of silently vanishing.
 *
 * Message text is deliberately *not* stored here. A preview field would keep a copy of a message
 * that the recipient has already made disappear, which would defeat the whole model — see
 * [MessageDto]. [lastMessageFrom] plus [lastMessageAt] are enough to draw "New message".
 */
@Serializable
data class ConversationDto(
    val users: List<String> = emptyList(),
    val names: Map<String, String> = emptyMap(),
    val avatarUrls: Map<String, String> = emptyMap(),
    /** Who sent the most recent message; blank before anyone has written. */
    val lastMessageFrom: String = "",
    val lastMessageAt: BaseTimestamp? = null,
    /** Hours a message survives once sent. Fixed when the conversation is created. */
    val retentionHours: Int = DEFAULT_RETENTION_HOURS,
    val createdAt: Long = 0L
) {
    companion object {
        /** Unopened messages disappear after a day, matching the "24 hour chat" option. */
        const val DEFAULT_RETENTION_HOURS = 24
    }
}
