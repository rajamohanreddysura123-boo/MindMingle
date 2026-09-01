package com.rajamohan.mindmingle.domain.model

/**
 * A row in the chat list, built entirely from the conversation document — no profile read per row.
 *
 * [otherUserName] is the copy taken when the conversation was opened, so a conversation whose other side
 * has since deleted their account still has something to show. There is no message preview by
 * design: messages disappear once seen, and a preview would be a copy that outlived them.
 */
data class ChatConversation(
    val conversationId: String,
    val otherUid: String,
    val otherUserName: String,
    val otherUserAvatarUrl: String,
    val lastMessageFrom: String,
    val lastMessageAtSeconds: Long,
    /** True when the other side's profile is gone — the row still opens, the person is unnamed. */
    val isOtherUserDeleted: Boolean = false
) {
    /** What the list shows in place of message text. */
    val displayName: String
        get() = when {
            isOtherUserDeleted || otherUserName.isBlank() -> "Unknown user"
            else -> otherUserName
        }

    fun hasUnreadFor(uid: String): Boolean =
        lastMessageAtSeconds > 0L && lastMessageFrom.isNotBlank() && lastMessageFrom != uid
}
