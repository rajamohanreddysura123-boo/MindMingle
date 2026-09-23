package com.rajamohan.mindmingle.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaymentRecordDto(
    val paymentId: String = "",
    val orderId: String = "",
    val planId: String = "",
    val amount: Long = 0L,
    val currency: String = "",
    val country: String = "",
    val source: String = "",
    val createdAt: Long = 0L,
    val status: String = "success",
    val reason: String = ""
)

/**
 * One issued invoice, as `getBillingHistory` returns it. Amounts are in the currency's
 * smallest unit, exactly like [PaymentRecordDto].
 */
/** One tax line on an invoice. [rate] is basis points: 1800 = 18%. */
@Serializable
data class TaxComponentDto(
    val label: String = "",
    val rate: Int = 0,
    val amount: Long = 0L
)

@Serializable
data class InvoiceDto(
    val invoiceNumber: String = "",
    val paymentId: String = "",
    val planId: String = "",
    val planLabel: String = "",
    val description: String = "",
    val subtotal: Long = 0L,
    val taxAmount: Long = 0L,
    val total: Long = 0L,
    val currency: String = "",
    val symbol: String = "",
    val decimals: Int = 2,
    val country: String = "",
    val issuedAt: Long = 0L,
    val periodEnd: Long = 0L,
    val status: String = "",
    val taxLabel: String = "",
    val taxRate: Int = 0,
    val taxComponents: List<TaxComponentDto> = emptyList(),
    val placeOfSupply: String = "",
    val isExport: Boolean = false,
    val taxNote: String = "",
    val sellerLegalName: String = "",
    val sellerAddress: String = "",
    val sellerTaxId: String = ""
)

@Serializable
data class BillingHistoryRequestDto(val uid: String = "")

@Serializable
data class BillingHistoryResponseDto(
    val uid: String = "",
    val planId: String = "",
    val status: String = "",
    val currentPeriodEnd: Long = 0L,
    val billingCountry: String = "",
    val billingCurrency: String = "",
    val payments: List<PaymentRecordDto> = emptyList(),
    val invoices: List<InvoiceDto> = emptyList()
)

/**
 * Tells the backend a checkout came back with an error. Sent so a decline the Razorpay
 * webhook never sees still leaves a record and reaches the user by email.
 */
@Serializable
data class RecordPaymentFailureRequestDto(
    val orderId: String,
    val planId: String = "",
    val reason: String = ""
)

@Serializable
data class PaymentDetailsRequestDto(val paymentId: String)


@Serializable
data class AdminSetSubscriptionRequestDto(
    val uid: String,
    val planId: String,
    val days: Int
)

@Serializable
data class AdminCancelSubscriptionRequestDto(
    val uid: String,
    val immediate: Boolean
)

@Serializable
data class AdminSubscriptionResponseDto(
    val planId: String = "",
    val status: String = "",
    val currentPeriodEnd: Long = 0L
)
