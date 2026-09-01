package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.data.remote.dto.SubscriberDto
import com.rajamohan.mindmingle.data.remote.dto.SubscriberStatsDto

/**
 * A paying (or previously paying) user as the admin subscriber screen shows them.
 *
 * `isActive` is decided by the Cloud Function rather than recomputed here, so the list, the
 * detail screen and the ads gate can never disagree about who is currently a subscriber.
 */
data class Subscriber(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String,
    val planId: String,
    val status: String,
    val currentPeriodEnd: Long,
    val billingCountry: String,
    val billingCurrency: String,
    val lastPaymentId: String,
    val lastInvoiceNumber: String,
    val updatedAt: Long,
    val isActive: Boolean,
    val isPaid: Boolean
) {
    val plan: PremiumPlan? get() = PremiumPlan.fromId(planId)

    val displayName: String get() = name.ifBlank { email.ifBlank { uid } }

    val planLabel: String get() = plan?.label ?: planId.ifBlank { "—" }

    /** "Active until 12 Sep 2026" / "Expired 4 Aug 2026" — the whole state in one line. */
    val periodLabel: String
        get() = when {
            currentPeriodEnd <= 0L -> "No period on record"
            isActive -> "Active until ${formatUtcDate(currentPeriodEnd)}"
            else -> "Expired ${formatUtcDate(currentPeriodEnd)}"
        }

    /** What to show as the row's state chip. */
    val stateLabel: String
        get() = when {
            status == "revoked" -> "Revoked"
            status == "cancelled" && isActive -> "Cancelling"
            isActive -> "Active"
            else -> "Expired"
        }

    /** A comped plan has no payment behind it, and support should be able to see that at a glance. */
    val isComped: Boolean get() = !isPaid
}

/** Filters offered by the subscriber screen. The value is what the Cloud Function expects. */
enum class SubscriberStatusFilter(val value: String, val label: String) {
    ALL("all", "All"),
    ACTIVE("active", "Active"),
    EXPIRED("expired", "Expired"),
    CANCELLED("cancelled", "Cancelling"),
    REVOKED("revoked", "Revoked")
}

data class SubscriberPage(
    val subscribers: List<Subscriber>,
    /** Blank means there is nothing left to load. */
    val cursor: String
)

data class SubscriberStats(
    val total: Int = 0,
    val active: Int = 0,
    val expired: Int = 0,
    val cancelled: Int = 0,
    val revoked: Int = 0
)

fun SubscriberDto.toDomain(): Subscriber = Subscriber(
    uid = uid,
    name = name,
    email = email,
    photoUrl = photoUrl,
    planId = planId,
    status = status,
    currentPeriodEnd = currentPeriodEnd,
    billingCountry = billingCountry,
    billingCurrency = billingCurrency,
    lastPaymentId = lastPaymentId,
    lastInvoiceNumber = lastInvoiceNumber,
    updatedAt = updatedAt,
    isActive = isActive,
    isPaid = isPaid
)

fun SubscriberStatsDto.toDomain(): SubscriberStats = SubscriberStats(
    total = total,
    active = active,
    expired = expired,
    cancelled = cancelled,
    revoked = revoked
)
