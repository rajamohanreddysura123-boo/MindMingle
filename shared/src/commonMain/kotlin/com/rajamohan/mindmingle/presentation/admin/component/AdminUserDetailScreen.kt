package com.rajamohan.mindmingle.presentation.admin.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rajamohan.mindmingle.domain.model.Invoice
import com.rajamohan.mindmingle.domain.model.PaymentRecord
import com.rajamohan.mindmingle.presentation.billing.InvoiceDetailDialog
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminUserDetailViewModel
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.CrossIcon
import com.rajamohan.mindmingle.presentation.common.component.RemoteProfileImage
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

sealed interface AdminPendingAction {
    data class GrantSubscription(val plan: PremiumPlan, val days: Int) : AdminPendingAction
    data class CancelSubscription(val immediate: Boolean) : AdminPendingAction
    data object ToggleDisabled : AdminPendingAction
    data object DeleteAccount : AdminPendingAction
    data object LiftBan : AdminPendingAction
}

@Composable
fun AdminUserDetailScreen(
    uid: String,
    onBack: () -> Unit,
    onUserDeleted: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: AdminUserDetailViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var pendingAction by remember { mutableStateOf<AdminPendingAction?>(null) }

    LaunchedEffect(uid) {
        viewModel.loadUser(uid)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .widthIn(max = 700.dp)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.desktopScreenPadding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "User Details",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading || uiState.user == null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else {
                val user = uiState.user!!

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = colors.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RemoteProfileImage(
                                url = user.displayPhotoUrls.firstOrNull().orEmpty(),
                                uid = user.uid,
                                contentDescription = user.name.ifBlank { "Profile photo" },
                                placeholderIconSize = 32.dp,
                                modifier = Modifier.size(64.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = user.name.ifBlank { "(no name)" },
                                    style = typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = user.email.ifBlank { "no email" },
                                    style = typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                                if (user.isDisabled) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.errorContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = "Disabled",
                                            style = typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.error,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        DetailRow("Phone", user.phoneNumber.ifBlank { "—" })
                        DetailRow("Experience", user.experienceLevel.ifBlank { "—" })
                        DetailRow("Looking For", user.lookingFor.ifBlank { "—" })
                        DetailRow("GitHub", user.githubUrl.ifBlank { "—" })
                        DetailRow("Bio", user.bio.ifBlank { "—" })
                        DetailRow("Occupation", user.occupation.ifBlank { "—" })
                        DetailRow("Profile Complete", if (user.isProfileComplete) "Yes" else "No")
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                SubscriptionPanel(
                    planLabel = uiState.planLabel,
                    planStatus = uiState.planStatus,
                    planEndsLabel = uiState.planEndsLabel,
                    hasActivePlan = uiState.hasActivePlan,
                    isLoading = uiState.isLoadingBilling,
                    isMutating = uiState.isSubscriptionMutating,
                    payments = uiState.billing?.payments.orEmpty(),
                    invoiceFor = { paymentId -> uiState.billing?.invoiceFor(paymentId) },
                    onGrantMonth = {
                        pendingAction = AdminPendingAction.GrantSubscription(PremiumPlan.MONTHLY, 30)
                    },
                    onGrantYear = {
                        pendingAction = AdminPendingAction.GrantSubscription(PremiumPlan.ANNUAL, 365)
                    },
                    onCancelAtPeriodEnd = {
                        pendingAction = AdminPendingAction.CancelSubscription(immediate = false)
                    },
                    onEndNow = {
                        pendingAction = AdminPendingAction.CancelSubscription(immediate = true)
                    }
                )

                // On-screen small status message for subscription actions
                if (uiState.subscriptionMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.tertiary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.subscriptionMessage,
                                style = typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onTertiaryContainer
                            )
                        }
                    }
                }

                // On-screen message for ban status
                if (uiState.banMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.banMessage,
                                style = typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onPrimaryContainer
                            )
                        }
                    }
                }

                if (uiState.error.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.errorContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            CrossIcon(color = colors.error, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.error,
                                style = typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        onClick = { pendingAction = AdminPendingAction.ToggleDisabled },
                        enabled = !uiState.isMutating,
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 22.dp)) {
                            Text(
                                text = if (user.isDisabled) "Re-enable Account" else "Disable Account",
                                style = typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = { pendingAction = AdminPendingAction.DeleteAccount },
                        enabled = !uiState.isMutating,
                        shape = RoundedCornerShape(16.dp),
                        color = colors.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 22.dp)
                        ) {
                            CrossIcon(color = colors.error, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delete Account",
                                style = typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.error
                            )
                        }
                    }

                    Surface(
                        onClick = { pendingAction = AdminPendingAction.LiftBan },
                        enabled = !uiState.isMutating,
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 22.dp)) {
                            Text(
                                text = "Lift Ban",
                                style = typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before any admin action
    pendingAction?.let { action ->
        val userName = uiState.user?.name.orEmpty().ifBlank { "this user" }
        when (action) {
            is AdminPendingAction.GrantSubscription -> {
                val isMonth = action.days == 30
                AdminConfirmActionDialog(
                    title = if (isMonth) "Grant 1 Month Subscription?" else "Grant 1 Year Subscription?",
                    message = "Are you sure you want to grant ${if (isMonth) "30 days" else "1 year"} of ${action.plan.label} to $userName? This will be recorded in their order history as an admin grant.",
                    confirmLabel = "Grant Plan",
                    isDestructive = false,
                    isLoading = uiState.isSubscriptionMutating,
                    onConfirm = {
                        val plan = action.plan
                        val days = action.days
                        pendingAction = null
                        viewModel.grantPlan(plan, days)
                    },
                    onDismiss = { pendingAction = null }
                )
            }
            is AdminPendingAction.CancelSubscription -> {
                AdminConfirmActionDialog(
                    title = if (action.immediate) "End Subscription Immediately?" else "Cancel at Period End?",
                    message = if (action.immediate) {
                        "Are you sure you want to end this subscription immediately? Premium features will be revoked and ads restored right away for $userName."
                    } else {
                        "Are you sure you want to cancel the subscription at period end? $userName will retain active benefits until ${uiState.planEndsLabel}."
                    },
                    confirmLabel = if (action.immediate) "End Now" else "Confirm Cancel",
                    isDestructive = action.immediate,
                    isLoading = uiState.isSubscriptionMutating,
                    onConfirm = {
                        val imm = action.immediate
                        pendingAction = null
                        viewModel.cancelPlan(imm)
                    },
                    onDismiss = { pendingAction = null }
                )
            }
            is AdminPendingAction.ToggleDisabled -> {
                val isDisabled = uiState.user?.isDisabled == true
                AdminConfirmActionDialog(
                    title = if (isDisabled) "Re-enable Account?" else "Disable Account?",
                    message = if (isDisabled) {
                        "Are you sure you want to re-enable $userName's account? They will be allowed to log in and use MindMingle."
                    } else {
                        "Are you sure you want to disable $userName's account? They will be prevented from logging in."
                    },
                    confirmLabel = if (isDisabled) "Re-enable" else "Disable",
                    isDestructive = !isDisabled,
                    isLoading = uiState.isMutating,
                    onConfirm = {
                        pendingAction = null
                        viewModel.toggleDisabled()
                    },
                    onDismiss = { pendingAction = null }
                )
            }
            is AdminPendingAction.DeleteAccount -> {
                AdminConfirmActionDialog(
                    title = "Delete this account?",
                    message = "This permanently erases $userName's profile, likes, matches, and messages, and blocks the account from ever signing in again. This cannot be undone.",
                    confirmLabel = "Delete",
                    isDestructive = true,
                    isLoading = uiState.isMutating,
                    onConfirm = {
                        pendingAction = null
                        viewModel.deleteUser()
                    },
                    onDismiss = { pendingAction = null }
                )
            }
            is AdminPendingAction.LiftBan -> {
                AdminConfirmActionDialog(
                    title = "Lift Ban on Account?",
                    message = "Are you sure you want to lift the ban on $userName (${uiState.user?.uid.orEmpty()})? The banned tombstone will be removed and this user will be permitted to sign in again.",
                    confirmLabel = "Lift Ban",
                    isDestructive = false,
                    isLoading = uiState.isMutating,
                    onConfirm = {
                        pendingAction = null
                        viewModel.unbanUser()
                    },
                    onDismiss = { pendingAction = null }
                )
            }
        }
    }

    // Success dialog once any admin action succeeds
    if (uiState.successMessage.isNotBlank()) {
        AdminSuccessDialog(
            title = uiState.successTitle,
            message = uiState.successMessage,
            onDismiss = {
                val wasDeleted = uiState.isDeleted
                viewModel.dismissSuccessDialog()
                if (wasDeleted) {
                    onUserDeleted()
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 130.dp)
        )
        Text(
            text = value,
            style = typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
private fun SubscriptionPanel(
    planLabel: String,
    planStatus: String,
    planEndsLabel: String,
    hasActivePlan: Boolean,
    isLoading: Boolean,
    isMutating: Boolean,
    payments: List<PaymentRecord>,
    invoiceFor: (String) -> Invoice?,
    onGrantMonth: () -> Unit,
    onGrantYear: () -> Unit,
    onCancelAtPeriodEnd: () -> Unit,
    onEndNow: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    // Support's most common question is "what exactly did this person pay?" — the invoice is
    // the answer, so it opens from the same row rather than from a separate screen.
    var openInvoice by remember { mutableStateOf<Invoice?>(null) }

    openInvoice?.let { invoice ->
        InvoiceDetailDialog(invoice = invoice, onDismiss = { openInvoice = null })
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = "Subscription",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = if (hasActivePlan) "$planLabel · $planStatus · until $planEndsLabel" else "No active plan",
                        style = typography.bodySmall,
                        color = if (hasActivePlan) colors.tertiary else colors.onSurfaceVariant
                    )
                }

                if (isLoading || isMutating) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminActionChip(label = "Grant 1 month", enabled = !isMutating, onClick = onGrantMonth)
                AdminActionChip(label = "Grant 1 year", enabled = !isMutating, onClick = onGrantYear)
            }

            if (hasActivePlan) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminActionChip(label = "Cancel at period end", enabled = !isMutating, onClick = onCancelAtPeriodEnd)
                    AdminActionChip(label = "End now", enabled = !isMutating, isDestructive = true, onClick = onEndNow)
                }
            }

            if (payments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Order history",
                    style = typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    payments.forEach { payment ->
                        val invoice = invoiceFor(payment.paymentId)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (invoice != null) {
                                        Modifier.clickable { openInvoice = invoice }
                                    } else {
                                        Modifier
                                    }
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = payment.plan?.label ?: payment.planId,
                                        style = typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface
                                    )
                                    if (payment.isFailed) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = colors.errorContainer
                                        ) {
                                            Text(
                                                text = "Failed",
                                                style = typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.onErrorContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (payment.isFailed) {
                                        "${payment.dateLabel}${if (payment.reason.isNotBlank()) " · ${payment.reason}" else ""}"
                                    } else {
                                        "${payment.dateLabel} · ${payment.source} · ${payment.paymentId}"
                                    },
                                    style = typography.bodySmall,
                                    color = if (payment.isFailed) colors.error else colors.onSurfaceVariant
                                )
                                if (invoice != null) {
                                    Text(
                                        text = "Invoice ${invoice.invoiceNumber} · View",
                                        style = typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.primary
                                    )
                                }
                            }
                            Text(
                                text = payment.amountLabel(),
                                style = typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (payment.isFailed) colors.onSurfaceVariant else colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminActionChip(
    label: String,
    enabled: Boolean,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (isDestructive) colors.errorContainer.copy(alpha = 0.3f) else colors.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.height(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = label,
                style = typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) colors.error else colors.onSurface
            )
        }
    }
}

@Composable
private fun AdminConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.widthIn(max = 440.dp).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(26.dp)) {
                Text(
                    text = title,
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive) colors.error else colors.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message,
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Cancel",
                                style = typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                        }
                    }
                    Surface(
                        onClick = onConfirm,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDestructive) colors.error else colors.primary,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = if (isDestructive) colors.onError else colors.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = confirmLabel,
                                    style = typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDestructive) colors.onError else colors.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSuccessDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.widthIn(max = 420.dp).padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(28.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(w * 0.22f, h * 0.52f)
                                lineTo(w * 0.42f, h * 0.72f)
                                lineTo(w * 0.78f, h * 0.28f)
                            }
                            drawPath(
                                path = path,
                                color = colors.primary,
                                style = Stroke(
                                    width = w * 0.13f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title.ifBlank { "Success" },
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    color = colors.primary,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "OK",
                            style = typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary
                        )
                    }
                }
            }
        }
    }
}
