package com.rajamohan.mindmingle.data.respository

import com.rajamohan.mindmingle.data.remote.dto.SavePlanPricingRequestDto
import com.rajamohan.mindmingle.data.remote.source.MindMingleFirebaseProvider
import com.rajamohan.mindmingle.domain.model.BillingHistory
import com.rajamohan.mindmingle.domain.model.PaymentDetails
import com.rajamohan.mindmingle.domain.model.PaymentOrder
import com.rajamohan.mindmingle.domain.model.PlanCatalog
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.model.Subscription
import com.rajamohan.mindmingle.domain.model.toDomain
import com.rajamohan.mindmingle.domain.model.toDto
import com.rajamohan.mindmingle.domain.repository.SubscriptionRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal class SubscriptionRepositoryImpl(
    private val mindMingleFirebaseProvider: MindMingleFirebaseProvider
) : SubscriptionRepository {

    private companion object {
        const val TAG = "SubscriptionRepository"
    }

    override suspend fun getPlanCatalog(countryHint: String): Result<PlanCatalog> {
        return try {
            Result.success(mindMingleFirebaseProvider.getPlanPricing(countryHint).toDomain())
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getPlanCatalog failed" }
            Result.failure(e)
        }
    }

    override suspend fun savePlanCatalog(catalog: PlanCatalog): Result<Int> {
        return try {
            val saved = mindMingleFirebaseProvider.savePlanPricing(
                SavePlanPricingRequestDto(
                    enabled = catalog.enabled,
                    defaultCountry = catalog.defaultCountry,
                    countries = catalog.countries.mapValues { (_, pricing) -> pricing.toDto() }
                )
            )
            Result.success(saved)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "savePlanCatalog failed" }
            Result.failure(e)
        }
    }

    override suspend fun resetPlanCatalog(): Result<Int> {
        return try {
            Result.success(mindMingleFirebaseProvider.resetPlanPricing())
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "resetPlanCatalog failed" }
            Result.failure(e)
        }
    }

    override suspend fun createOrder(plan: PremiumPlan, countryHint: String): Result<PaymentOrder> {
        return try {
            Result.success(
                mindMingleFirebaseProvider.createRazorpayOrder(planId = plan.id, countryHint = countryHint).toDomain()
            )
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "createOrder failed" }
            Result.failure(e)
        }
    }

    override suspend fun verifyPayment(
        orderId: String,
        paymentId: String,
        signature: String
    ): Result<Subscription> {
        return try {
            val response = mindMingleFirebaseProvider.verifyRazorpayPayment(
                orderId = orderId,
                paymentId = paymentId,
                signature = signature
            )
            Result.success(
                Subscription(
                    planId = response.planId,
                    status = response.status,
                    currentPeriodEnd = response.currentPeriodEnd,
                    lastPaymentId = paymentId
                )
            )
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "verifyPayment failed" }
            Result.failure(e)
        }
    }

    override suspend fun getSubscription(uid: String): Subscription? {
        return try {
            mindMingleFirebaseProvider.getSubscription(uid)?.toDomain()
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "getSubscription failed" }
            null
        }
    }

    override suspend fun getBillingHistory(uid: String): Result<BillingHistory> {
        return try {
            Result.success(mindMingleFirebaseProvider.getBillingHistory(uid).toDomain())
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getBillingHistory failed" }
            Result.failure(e)
        }
    }

    override suspend fun getPaymentDetails(paymentId: String): Result<PaymentDetails> {
        return try {
            Result.success(mindMingleFirebaseProvider.getPaymentDetails(paymentId).toDomain())
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getPaymentDetails failed" }
            Result.failure(e)
        }
    }

    override suspend fun adminSetSubscription(
        uid: String,
        plan: PremiumPlan,
        days: Int
    ): Result<Subscription> {
        return try {
            val response = mindMingleFirebaseProvider.adminSetSubscription(uid = uid, planId = plan.id, days = days)
            Result.success(
                Subscription(
                    planId = response.planId,
                    status = response.status,
                    currentPeriodEnd = response.currentPeriodEnd
                )
            )
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "adminSetSubscription failed" }
            Result.failure(e)
        }
    }

    override suspend fun adminCancelSubscription(uid: String, immediate: Boolean): Result<Subscription> {
        return try {
            val response = mindMingleFirebaseProvider.adminCancelSubscription(uid = uid, immediate = immediate)
            Result.success(
                Subscription(
                    planId = response.planId,
                    status = response.status,
                    currentPeriodEnd = response.currentPeriodEnd
                )
            )
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "adminCancelSubscription failed" }
            Result.failure(e)
        }
    }

    override fun observeSubscription(uid: String): Flow<Subscription?> {
        return mindMingleFirebaseProvider.observeSubscription(uid)
            .map { dto -> dto?.toDomain() }
            .catch { error ->
                Napier.w(throwable = error, tag = TAG) { "observeSubscription failed" }
                emit(null)
            }
    }
}
