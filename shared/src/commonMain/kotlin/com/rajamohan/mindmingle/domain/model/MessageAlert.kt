package com.rajamohan.mindmingle.domain.model

/**
 * Someone else's last message in one conversation, as much as the alert watcher can know about it.
 *
 * There is no text here and there cannot be: a conversation document stores only who wrote last
 * and when, because messages disappear once seen and a stored preview would outlive the message
 * it copied. So a notification says that someone wrote, never what.
 */
data class MessageAlert(
    val conversationId: String,
    val fromUid: String,
    val fromName: String,
    val sentAtSeconds: Long
)
