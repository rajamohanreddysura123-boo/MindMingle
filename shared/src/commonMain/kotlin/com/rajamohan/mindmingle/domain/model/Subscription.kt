package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.data.remote.dto.CreateOrderResponseDto
import com.rajamohan.mindmingle.data.remote.dto.CreatePaymentLinkResponseDto
import com.rajamohan.mindmingle.data.remote.dto.SubscriptionDto
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val MILLIS_PER_DAY = 86_400_000L

data class Subscription(
    val planId: String = "",
    val status: String = "",
    val currentPeriodEnd: Long = 0L,
    val lastPaymentId: String = ""
) {
    val plan: PremiumPlan? get() = PremiumPlan.fromId(planId)

    /**
     * "cancelled" still counts until the period already paid for runs out; "revoked" is an
     * admin ending it on the spot, which also switches ads back on.
     */
    fun isActiveAt(nowMillis: Long): Boolean = currentPeriodEnd > nowMillis && status != "revoked"

    fun daysLeftAt(nowMillis: Long): Int {
        if (!isActiveAt(nowMillis)) return 0
        val remaining = currentPeriodEnd - nowMillis
        return ((remaining + MILLIS_PER_DAY - 1) / MILLIS_PER_DAY).toInt()
    }
}

/**
 * A payment link and how long it stays payable.
 *
 * There is no result to hand back when it is paid: the payment happens in a browser or on a phone,
 * and the app learns about it the same way it learns about an admin grant — the subscription
 * document changes underneath it.
 */
data class PaymentLink(
    val linkId: String,
    val url: String,
    val amountMinor: Long,
    val currency: String,
    val planId: String,
    val expiresAt: Long
)

data class PaymentOrder(
    val orderId: String,
    val amountMinor: Long,
    val currency: String,
    val keyId: String,
    val planId: String
)

@OptIn(ExperimentalTime::class)
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

fun Subscription?.isPremiumNow(): Boolean = this?.isActiveAt(nowMillis()) == true

fun SubscriptionDto.toDomain(): Subscription = Subscription(
    planId = planId,
    status = status,
    currentPeriodEnd = currentPeriodEnd,
    lastPaymentId = lastPaymentId
)

fun CreatePaymentLinkResponseDto.toDomain(): PaymentLink = PaymentLink(
    linkId = linkId,
    url = url,
    amountMinor = amount,
    currency = currency,
    planId = planId,
    expiresAt = expiresAt
)

fun CreateOrderResponseDto.toDomain(): PaymentOrder = PaymentOrder(
    orderId = orderId,
    amountMinor = amount,
    currency = currency,
    keyId = keyId,
    planId = planId
)
