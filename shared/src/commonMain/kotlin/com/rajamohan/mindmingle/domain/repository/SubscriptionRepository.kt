package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.BillingHistory
import com.rajamohan.mindmingle.domain.model.PaymentLink
import com.rajamohan.mindmingle.domain.model.PaymentOrder
import com.rajamohan.mindmingle.domain.model.PlanCatalog
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {

    /** Per-country price list plus the market the signed-in user is billed in. */
    suspend fun getPlanCatalog(countryHint: String): Result<PlanCatalog>

    /** Admin-only: overwrites the whole price list in Firestore. */
    suspend fun savePlanCatalog(catalog: PlanCatalog): Result<Int>

    /** Admin-only: restores the seed price list shipped with the Cloud Functions. */
    suspend fun resetPlanCatalog(): Result<Int>

    suspend fun createOrder(plan: PremiumPlan, countryHint: String): Result<PaymentOrder>

    /** A hosted payment page, for platforms with no Razorpay checkout SDK. */
    suspend fun createPaymentLink(plan: PremiumPlan, countryHint: String): Result<PaymentLink>

    suspend fun verifyPayment(orderId: String, paymentId: String, signature: String): Result<Subscription>

    fun observeSubscription(uid: String): Flow<Subscription?>

    /** Order history; blank [uid] means the signed-in user. Another uid requires admin. */
    suspend fun getBillingHistory(uid: String = ""): Result<BillingHistory>

    /**
     * Reports a checkout that failed on the device. Nothing is granted; the attempt is recorded
     * and the user is emailed that no money was taken. Never throws.
     */
    suspend fun recordPaymentFailure(orderId: String, planId: String, reason: String)

    /** Admin-only: grants or extends a plan without a payment. */
    suspend fun adminSetSubscription(uid: String, plan: PremiumPlan, days: Int): Result<Subscription>

    /** Admin-only: [immediate] ends access now instead of at the end of the paid period. */
    suspend fun adminCancelSubscription(uid: String, immediate: Boolean): Result<Subscription>
}
