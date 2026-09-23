package com.rajamohan.mindmingle.domain.model

import com.rajamohan.mindmingle.data.remote.dto.BillingHistoryResponseDto
import com.rajamohan.mindmingle.data.remote.dto.InvoiceDto
import com.rajamohan.mindmingle.data.remote.dto.PaymentRecordDto

data class PaymentRecord(
    val paymentId: String,
    val orderId: String,
    val planId: String,
    val amount: Long,
    val currency: String,
    val country: String,
    val source: String,
    val createdAt: Long,
    /** "success" for a captured `payments` row, "failed" for a `paymentAttempts` decline. A
     *  `paymentAttempts` doc that is merely "link_created" (a QR generated, not yet paid or
     *  declined) is deliberately left out of this — that status never flips to "paid" on success,
     *  so surfacing it would show every successful desktop payment twice. */
    val status: String = "success",
    /** Razorpay's decline reason, only ever set when [status] is "failed". */
    val reason: String = ""
) {
    val plan: PremiumPlan? get() = PremiumPlan.fromId(planId)

    val isAdminGrant: Boolean get() = source == "admin-grant"

    val isFailed: Boolean get() = status == "failed"

    val dateLabel: String get() = formatUtcDate(createdAt)

    fun amountLabel(decimals: Int = 2): String = when {
        isFailed -> "—"
        isAdminGrant -> "Granted"
        currency.isBlank() -> "—"
        else -> "${formatMinorAmount(amount, decimals, symbol = "")} $currency"
    }
}

/**
 * A receipt the user can be shown or shown to an accountant. Issued server-side, one per
 * captured payment; the number is what support should be quoted.
 */
/** One tax line. [rate] is basis points, so 1800 reads as 18%. */
data class TaxLine(
    val label: String,
    val rate: Int,
    val amount: Long
) {
    fun rateLabel(): String {
        val percent = rate / 100.0
        return if (rate % 100 == 0) "${rate / 100}%" else "$percent%"
    }
}

data class Invoice(
    val invoiceNumber: String,
    val paymentId: String,
    val planId: String,
    val planLabel: String,
    val description: String,
    val subtotal: Long,
    val taxAmount: Long,
    val total: Long,
    val currency: String,
    val symbol: String,
    val decimals: Int,
    val country: String,
    val issuedAt: Long,
    val periodEnd: Long,
    val status: String,
    val taxLabel: String = "",
    val taxRate: Int = 0,
    val taxLines: List<TaxLine> = emptyList(),
    val placeOfSupply: String = "",
    val isExport: Boolean = false,
    val taxNote: String = "",
    val sellerLegalName: String = "",
    val sellerAddress: String = "",
    val sellerTaxId: String = ""
) {
    val isPaid: Boolean get() = status == "paid"

    val issuedLabel: String get() = formatUtcDate(issuedAt)

    val activeUntilLabel: String get() = formatUtcDate(periodEnd)

    val totalLabel: String get() = formatMinorAmount(total, decimals, symbol)

    val subtotalLabel: String get() = formatMinorAmount(subtotal, decimals, symbol)

    /** Each tax line as "CGST (9%)" to "₹8.95", ready to render as rows. */
    fun taxRows(): List<Pair<String, String>> = taxLines.map { line ->
        "${line.label} (${line.rateLabel()})" to formatMinorAmount(line.amount, decimals, symbol)
    }
}

data class BillingHistory(
    val uid: String = "",
    val planId: String = "",
    val status: String = "",
    val currentPeriodEnd: Long = 0L,
    val billingCountry: String = "",
    val billingCurrency: String = "",
    val payments: List<PaymentRecord> = emptyList(),
    val invoices: List<Invoice> = emptyList()
) {
    /** The invoice for a payment row, when one was issued (admin grants have none). */
    fun invoiceFor(paymentId: String): Invoice? = invoices.firstOrNull { it.paymentId == paymentId }

    val subscription: Subscription
        get() = Subscription(planId = planId, status = status, currentPeriodEnd = currentPeriodEnd)

    val isActive: Boolean get() = subscription.isActiveAt(nowMillis())

    val renewsLabel: String
        get() = if (currentPeriodEnd > 0L) formatUtcDate(currentPeriodEnd) else "—"
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
    createdAt = createdAt,
    status = status,
    reason = reason
)

fun InvoiceDto.toDomain(): Invoice = Invoice(
    invoiceNumber = invoiceNumber,
    paymentId = paymentId,
    planId = planId,
    planLabel = planLabel,
    description = description,
    subtotal = subtotal,
    taxAmount = taxAmount,
    total = total,
    currency = currency,
    symbol = symbol,
    decimals = decimals,
    country = country,
    issuedAt = issuedAt,
    periodEnd = periodEnd,
    status = status,
    taxLabel = taxLabel,
    taxRate = taxRate,
    taxLines = taxComponents.map { TaxLine(label = it.label, rate = it.rate, amount = it.amount) },
    placeOfSupply = placeOfSupply,
    isExport = isExport,
    taxNote = taxNote,
    sellerLegalName = sellerLegalName,
    sellerAddress = sellerAddress,
    sellerTaxId = sellerTaxId
)

fun BillingHistoryResponseDto.toDomain(): BillingHistory = BillingHistory(
    uid = uid,
    planId = planId,
    status = status,
    currentPeriodEnd = currentPeriodEnd,
    billingCountry = billingCountry,
    billingCurrency = billingCurrency,
    payments = payments.map { it.toDomain() },
    invoices = invoices.map { it.toDomain() }
)

