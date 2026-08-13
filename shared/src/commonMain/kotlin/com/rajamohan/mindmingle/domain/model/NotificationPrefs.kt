package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.data.remote.dto.NotificationPrefsDto

enum class NotificationCategory(val key: String, val label: String, val description: String) {
    MESSAGES("messages", "Messages", "New chat messages from your matches"),
    LIKES("likes", "Likes", "When someone likes your profile"),
    MATCHES("matches", "Matches", "When a like turns into a match"),
    PAYMENTS("payments", "Billing", "Payment receipts and plan reminders"),
    SUPPORT("support", "Support", "Replies from the MindMingle team")
}

data class NotificationPrefs(
    val messages: Boolean = true,
    val likes: Boolean = true,
    val matches: Boolean = true,
    val payments: Boolean = true,
    val support: Boolean = true
) {
    fun isEnabled(category: NotificationCategory): Boolean = when (category) {
        NotificationCategory.MESSAGES -> messages
        NotificationCategory.LIKES -> likes
        NotificationCategory.MATCHES -> matches
        NotificationCategory.PAYMENTS -> payments
        NotificationCategory.SUPPORT -> support
    }

    fun with(category: NotificationCategory, enabled: Boolean): NotificationPrefs = when (category) {
        NotificationCategory.MESSAGES -> copy(messages = enabled)
        NotificationCategory.LIKES -> copy(likes = enabled)
        NotificationCategory.MATCHES -> copy(matches = enabled)
        NotificationCategory.PAYMENTS -> copy(payments = enabled)
        NotificationCategory.SUPPORT -> copy(support = enabled)
    }
}

fun NotificationPrefsDto.toDomain(): NotificationPrefs = NotificationPrefs(
    messages = messages,
    likes = likes,
    matches = matches,
    payments = payments,
    support = support
)

fun NotificationPrefs.toDto(): NotificationPrefsDto = NotificationPrefsDto(
    messages = messages,
    likes = likes,
    matches = matches,
    payments = payments,
    support = support
)
