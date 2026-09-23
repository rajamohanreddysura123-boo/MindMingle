package com.rajamohan.mindmingle.data.respository

import com.rajamohan.mindmingle.data.remote.dto.PlanCatalogDto
import com.rajamohan.mindmingle.data.remote.source.MindMingleFirebaseProvider
import com.rajamohan.mindmingle.domain.model.BillingHistory
import com.rajamohan.mindmingle.domain.model.CountryCodeRepository
import com.rajamohan.mindmingle.domain.model.PaymentLink
import com.rajamohan.mindmingle.domain.model.PaymentOrder
import com.rajamohan.mindmingle.domain.model.PlanCatalog
import com.rajamohan.mindmingle.domain.model.countryFromPhone
import com.rajamohan.mindmingle.domain.model.defaultPlanCatalog
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

    /**
     * Pricing comes straight out of `appConfig/plans` — no Cloud Function. Every user reads it,
     * only an admin writes it (firestore.rules). The stored rows win; the shipped
     * [defaultPlanCatalog] fills in every country the doc has not been given a row for yet, so the
     * admin screen always lists all 242 markets and a fresh project still shows prices before an
     * admin has saved anything.
     *
     * A failed read falls back to those shipped prices rather than failing the call: the price
     * list is something every user is meant to be able to see, and the app already carries a copy
     * of it. Only what an admin has since changed is lost, and just until the read works again.
     */
    override suspend fun getPlanCatalog(countryHint: String): Result<PlanCatalog> {
        return try {
            val dialCodes = dialCodes()
            val fallback = defaultPlanCatalog(dialCodes)

            val stored = try {
                mindMingleFirebaseProvider.getPlanCatalog()?.toDomain()
            } catch (e: Exception) {
                Napier.w(throwable = e, tag = TAG) { "appConfig/plans unreadable, showing shipped prices" }
                null
            }

            val countries = (fallback.countries + stored?.countries.orEmpty())
                .mapValues { (code, pricing) ->
                    if (pricing.dialCode.isBlank()) pricing.copy(dialCode = dialCodes[code].orEmpty()) else pricing
                }

            val catalog = PlanCatalog(
                enabled = stored?.enabled ?: fallback.enabled,
                defaultCountry = stored?.defaultCountry?.ifBlank { fallback.defaultCountry } ?: fallback.defaultCountry,
                countries = countries
            )

            Result.success(catalog.copy(resolvedCountry = resolveCountry(countryHint, catalog, dialCodes)))
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getPlanCatalog failed" }
            Result.failure(e)
        }
    }

    override suspend fun savePlanCatalog(catalog: PlanCatalog): Result<Int> {
        return try {
            mindMingleFirebaseProvider.savePlanCatalog(
                PlanCatalogDto(
                    enabled = catalog.enabled,
                    defaultCountry = catalog.defaultCountry,
                    countries = catalog.countries.mapValues { (_, pricing) -> pricing.toDto() }
                )
            )
            Result.success(catalog.countries.size)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "savePlanCatalog failed" }
            Result.failure(e)
        }
    }

    override suspend fun resetPlanCatalog(): Result<Int> {
        return try {
            val catalog = defaultPlanCatalog(dialCodes())
            mindMingleFirebaseProvider.savePlanCatalog(
                PlanCatalogDto(
                    enabled = catalog.enabled,
                    defaultCountry = catalog.defaultCountry,
                    countries = catalog.countries.mapValues { (_, pricing) -> pricing.toDto() }
                )
            )
            Result.success(catalog.countries.size)
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "resetPlanCatalog failed" }
            Result.failure(e)
        }
    }

    private suspend fun dialCodes(): Map<String, String> =
        CountryCodeRepository.getCountryCodes().associate { it.code to it.dialCode }

    /**
     * Which market the caller is billed in: an explicit ISO code from the caller, otherwise the
     * country behind the profile's phone number, otherwise the catalog default.
     */
    private suspend fun resolveCountry(
        countryHint: String,
        catalog: PlanCatalog,
        dialCodes: Map<String, String>
    ): String {
        val hint = countryHint.trim().uppercase()
        if (hint.length == 2 && catalog.countries.containsKey(hint)) return hint

        val uid = mindMingleFirebaseProvider.getCurrentUid() ?: return catalog.defaultCountry
        val phone = try {
            mindMingleFirebaseProvider.getUserById(uid)?.phoneNumber.orEmpty()
        } catch (e: Exception) {
            Napier.w(throwable = e, tag = TAG) { "country lookup failed, using default market" }
            ""
        }

        return countryFromPhone(phone, dialCodes)?.takeIf { catalog.countries.containsKey(it) }
            ?: catalog.defaultCountry
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

    override suspend fun createPaymentLink(plan: PremiumPlan, countryHint: String): Result<PaymentLink> {
        return try {
            Result.success(
                mindMingleFirebaseProvider.createPaymentLink(planId = plan.id, countryHint = countryHint).toDomain()
            )
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "createPaymentLink failed" }
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

    override suspend fun getBillingHistory(uid: String): Result<BillingHistory> {
        return try {
            Result.success(mindMingleFirebaseProvider.getBillingHistory(uid).toDomain())
        } catch (e: Exception) {
            Napier.e(throwable = e, tag = TAG) { "getBillingHistory failed" }
            Result.failure(e)
        }
    }

    override suspend fun recordPaymentFailure(orderId: String, planId: String, reason: String) {
        try {
            mindMingleFirebaseProvider.recordPaymentFailure(orderId, planId, reason)
        } catch (e: Exception) {
            // Best effort only: the user has already been shown the decline, and losing the
            // record must never turn into a second error on top of the first.
            Napier.w(throwable = e, tag = TAG) { "recordPaymentFailure failed" }
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
