package com.rajamohan.mindmingle.presentation.profile.viewmodel

import com.rajamohan.mindmingle.domain.model.BillingHistory
import com.rajamohan.mindmingle.domain.model.PaymentRecord
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.model.Subscription
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.model.isPremiumNow
import com.rajamohan.mindmingle.domain.model.nowMillis

internal data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val conversationsCount: Int = 0,
    val likesCount: Int = 0,
    val isAccountBlocked: Boolean = false,
    val subscription: Subscription? = null,
    val billing: BillingHistory? = null,
    val isLoadingBilling: Boolean = false,
    val billingError: String = ""
) {
    val payments: List<PaymentRecord> get() = billing?.payments.orEmpty()

    val hasOrders: Boolean get() = payments.isNotEmpty()

    val planRenewsLabel: String get() = billing?.renewsLabel ?: "—"

    val isPremium: Boolean get() = subscription.isPremiumNow()

    val activePlan: PremiumPlan? get() = if (isPremium) subscription?.plan else null

    val planDaysLeft: Int get() = subscription?.daysLeftAt(nowMillis()) ?: 0

    val profileCompletionPercent: Int
        get() {
            val u = user ?: return 0
            var filled = 1 // name always present once profile exists
            val total = 5
            if (u.bio.isNotBlank()) filled++
            if (u.occupation.isNotBlank()) filled++
            if (u.experienceLevel.isNotBlank()) filled++
            if (u.githubUrl.isNotBlank()) filled++
            return (filled * 100) / total
        }
}
