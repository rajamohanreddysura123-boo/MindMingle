package com.rajamohan.mindmingle.presentation.premium.viewmodel

import com.rajamohan.mindmingle.domain.model.CountryPricing
import com.rajamohan.mindmingle.domain.model.PaymentLink
import com.rajamohan.mindmingle.domain.model.PlanCatalog
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.model.Subscription
import com.rajamohan.mindmingle.domain.model.isPremiumNow
import com.rajamohan.mindmingle.domain.model.nowMillis

internal sealed class PremiumEvent {
    data class Load(val uid: String) : PremiumEvent()
    data class SelectPlan(val plan: PremiumPlan) : PremiumEvent()
    data object Checkout : PremiumEvent()

    /** Closes the payment sheet. The link stays payable until it expires on Razorpay's side. */
    data object DismissPaymentLink : PremiumEvent()
    data object DismissError : PremiumEvent()
}

internal data class PremiumUiState(
    val selectedPlan: PremiumPlan = PremiumPlan.MONTHLY,
    val subscription: Subscription? = null,
    val catalog: PlanCatalog? = null,
    val isLoadingPricing: Boolean = false,
    val isProcessing: Boolean = false,
    /**
     * A hosted payment page, shown where there is no checkout SDK — desktop. Non-null means the
     * QR is on screen and the app is waiting for the webhook, not for the user to come back and
     * confirm anything.
     */
    val paymentLink: PaymentLink? = null,
    val justUpgraded: Boolean = false,
    val error: String = ""
) {
    val isPremium: Boolean get() = subscription.isPremiumNow()

    val activePlan: PremiumPlan? get() = if (isPremium) subscription?.plan else null

    val daysLeft: Int get() = subscription?.daysLeftAt(nowMillis()) ?: 0

    /** The row this user is billed by — resolved server-side from their phone number. */
    val pricing: CountryPricing? get() = catalog?.pricing

    val priceLabel: String get() = pricing?.priceLabelFor(selectedPlan).orEmpty()

    val periodLabel: String get() = selectedPlan.periodLabel

    val annualSavingsPercent: Int get() = pricing?.annualSavingsPercent ?: 0

    val annualToggleLabel: String
        get() = if (annualSavingsPercent > 0) "Annual · Save $annualSavingsPercent%" else "Annual"

    val billingCountry: String get() = catalog?.resolvedCountry.orEmpty()

    val currency: String get() = pricing?.currency.orEmpty()

    /**
     * Whether a purchase can be started at all.
     *
     * It no longer asks whether a checkout SDK exists. Desktop buys through a Razorpay-hosted page
     * instead of a native sheet, so the only questions left are whether upgrades are switched on
     * and whether this country has a price.
     */
    val isCheckoutAvailable: Boolean
        get() = catalog?.enabled == true && pricing?.isValid == true

    val canCheckout: Boolean get() = isCheckoutAvailable && !isProcessing && !isPremium
}
