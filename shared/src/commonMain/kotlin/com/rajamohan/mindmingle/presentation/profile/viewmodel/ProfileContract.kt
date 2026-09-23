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

    /**
     * How much of the real profile-setup flow this person has actually finished.
     *
     * The previous version only checked bio, occupation, experience level and a GitHub link —
     * four optional text fields nobody is required to fill during setup, and none of the ten-odd
     * questions setup actually asks (a photo, age, location, what you're looking for, interests,
     * lifestyle answers) counted at all. Someone who filled in every real step could sit stuck at
     * whatever number they started at; the bar read as static because it mostly was — a change
     * a user makes in Edit Profile almost never touched one of those four fields.
     *
     * Ten factors now, each worth an equal tenth, covering everything the setup flow collects.
     * A photo counts once regardless of how many are uploaded — the deck only ever needed one to
     * stop showing the gradient placeholder, so a second or third photo is not "more complete."
     * Lifestyle answers (`details`/`selections`, driven by profile_options.json) count as one
     * factor rather than one per question, so this stays stable as that question set changes.
     */
    val profileCompletionPercent: Int
        get() {
            val u = user ?: return 0
            var filled = 0
            val total = 10

            if (u.name.isNotBlank()) filled++
            if (u.photoUrls.isNotEmpty()) filled++
            if (u.age > 0) filled++
            if (u.location.isNotBlank()) filled++
            if (u.bio.isNotBlank()) filled++
            if (u.occupation.isNotBlank()) filled++
            if (u.experienceLevel.isNotBlank()) filled++
            if (u.lookingFor.isNotBlank()) filled++
            if (u.interests.isNotEmpty()) filled++
            if (u.details.isNotEmpty() || u.selections.isNotEmpty()) filled++

            return (filled * 100) / total
        }
}
