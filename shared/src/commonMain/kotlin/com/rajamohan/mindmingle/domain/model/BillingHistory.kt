package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.data.remote.dto.BillingHistoryResponseDto
import com.rajamohan.mindmingle.data.remote.dto.PaymentDetailsResponseDto
import com.rajamohan.mindmingle.data.remote.dto.PaymentRecordDto

data class PaymentRecord(
    val paymentId: String,
    val orderId: String,
    val planId: String,
    val amount: Long,
    val currency: String,
    val country: String,
    val source: String,
    val createdAt: Long
) {
    val plan: PremiumPlan? get() = PremiumPlan.fromId(planId)

    val isAdminGrant: Boolean get() = source == "admin-grant"

    val dateLabel: String get() = formatUtcDate(createdAt)

    fun amountLabel(decimals: Int = 2): String = when {
        isAdminGrant -> "Granted"
        currency.isBlank() -> "—"
        else -> "${formatMinorAmount(amount, decimals, symbol = "")} $currency"
    }
}

data class BillingHistory(
    val uid: String = "",
    val planId: String = "",
    val status: String = "",
    val currentPeriodEnd: Long = 0L,
    val billingCountry: String = "",
    val billingCurrency: String = "",
    val payments: List<PaymentRecord> = emptyList()
) {
    val subscription: Subscription
        get() = Subscription(planId = planId, status = status, currentPeriodEnd = currentPeriodEnd)

    val isActive: Boolean get() = subscription.isActiveAt(nowMillis())

    val renewsLabel: String
        get() = if (currentPeriodEnd > 0L) formatUtcDate(currentPeriodEnd) else "—"
}

data class PaymentDetails(
    val paymentId: String,
    val orderId: String,
    val planId: String,
    val country: String,
    val status: String,
    val amount: Long,
    val currency: String,
    val method: String,
    val createdAt: Long,
    val grantedNow: Boolean
) {
    val isCaptured: Boolean get() = status == "captured"
}

private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/**
 * "12 Aug 2026" from epoch millis, in UTC. Days-from-civil in reverse (Howard Hinnant's
 * algorithm) — the project has no date-time library, and billing rows only need a date.
 */
fun formatUtcDate(millis: Long): String {
    if (millis <= 0L) return "—"

    var days = millis / 86_400_000L
    if (millis % 86_400_000L < 0L) days -= 1

    var z = days + 719_468L
    val era = (if (z >= 0) z else z - 146_096L) / 146_097L
    val dayOfEra = z - era * 146_097L
    val yearOfEra = (dayOfEra - dayOfEra / 1460L + dayOfEra / 36_524L - dayOfEra / 146_096L) / 365L
    var year = yearOfEra + era * 400L
    val dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPrime = (5L * dayOfYear + 2L) / 153L
    val day = dayOfYear - (153L * monthPrime + 2L) / 5L + 1L
    val month = if (monthPrime < 10L) monthPrime + 3L else monthPrime - 9L
    if (month <= 2L) year += 1L

    val monthName = MONTH_NAMES.getOrElse((month - 1L).toInt()) { "" }
    return "$day $monthName $year"
}

fun PaymentRecordDto.toDomain(): PaymentRecord = PaymentRecord(
    paymentId = paymentId,
    orderId = orderId,
    planId = planId,
    amount = amount,
    currency = currency,
    country = country,
    source = source,
    createdAt = createdAt
)

fun BillingHistoryResponseDto.toDomain(): BillingHistory = BillingHistory(
    uid = uid,
    planId = planId,
    status = status,
    currentPeriodEnd = currentPeriodEnd,
    billingCountry = billingCountry,
    billingCurrency = billingCurrency,
    payments = payments.map { it.toDomain() }
)

fun PaymentDetailsResponseDto.toDomain(): PaymentDetails = PaymentDetails(
    paymentId = paymentId,
    orderId = orderId,
    planId = planId,
    country = country,
    status = status,
    amount = amount,
    currency = currency,
    method = method,
    createdAt = createdAt,
    grantedNow = grantedNow
)
