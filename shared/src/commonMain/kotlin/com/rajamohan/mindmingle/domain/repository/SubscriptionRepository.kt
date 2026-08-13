package com.rajamohan.mindmingle.domain.repository

import com.rajamohan.mindmingle.domain.model.BillingHistory
import com.rajamohan.mindmingle.domain.model.PaymentDetails
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

    suspend fun verifyPayment(orderId: String, paymentId: String, signature: String): Result<Subscription>

    suspend fun getSubscription(uid: String): Subscription?

    fun observeSubscription(uid: String): Flow<Subscription?>

    /** Order history; blank [uid] means the signed-in user. Another uid requires admin. */
    suspend fun getBillingHistory(uid: String = ""): Result<BillingHistory>

    /** Re-reads a payment from Razorpay, granting it if it was captured but never recorded. */
    suspend fun getPaymentDetails(paymentId: String): Result<PaymentDetails>

    /** Admin-only: grants or extends a plan without a payment. */
    suspend fun adminSetSubscription(uid: String, plan: PremiumPlan, days: Int): Result<Subscription>

    /** Admin-only: [immediate] ends access now instead of at the end of the paid period. */
    suspend fun adminCancelSubscription(uid: String, immediate: Boolean): Result<Subscription>
}
