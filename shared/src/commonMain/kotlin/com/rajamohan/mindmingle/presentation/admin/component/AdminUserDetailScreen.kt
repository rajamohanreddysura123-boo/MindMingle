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

    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        viewModel.loadUser(uid)
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onUserDeleted()
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
                            // An admin deciding whether to disable or delete an account should be
                            // looking at the same photo everyone else in the app sees.
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
                    onGrantMonth = { viewModel.grantPlan(PremiumPlan.MONTHLY, 30) },
                    onGrantYear = { viewModel.grantPlan(PremiumPlan.ANNUAL, 365) },
                    onCancelAtPeriodEnd = { viewModel.cancelPlan(immediate = false) },
                    onEndNow = { viewModel.cancelPlan(immediate = true) }
                )

                if (uiState.subscriptionMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = uiState.subscriptionMessage, style = typography.bodySmall, color = colors.tertiary)
                }

                if (uiState.error.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = uiState.error, style = typography.bodySmall, color = colors.error)
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.banMessage.isNotBlank()) {
                    Text(text = uiState.banMessage, style = typography.bodySmall, color = colors.primary)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        onClick = { viewModel.toggleDisabled() },
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
                        onClick = { showDeleteConfirm = true },
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

                    // Deleting an account leaves a bannedUids tombstone, because a client SDK
                    // cannot remove somebody else's Firebase Auth record. This is the only way
                    // back from that short of the Firebase Console — the repository call is a
                    // no-op when no tombstone exists, so it is safe on any account.
                    Surface(
                        onClick = { viewModel.unbanUser() },
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

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            userName = uiState.user?.name.orEmpty(),
            isDeleting = uiState.isMutating,
            onConfirm = {
                viewModel.deleteUser()
            },
            onDismiss = { showDeleteConfirm = false }
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
private fun DeleteConfirmDialog(
    userName: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            shadowElevation = 12.dp,
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Text(
                    text = "Delete this account?",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "This permanently erases ${userName.ifBlank { "this user" }}'s profile, likes, matches, and messages, and blocks the account from ever signing in again. This cannot be undone.",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = onDismiss,
                        enabled = !isDeleting,
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(text = "Cancel", style = typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        }
                    }
                    Surface(
                        onClick = onConfirm,
                        enabled = !isDeleting,
                        shape = RoundedCornerShape(14.dp),
                        color = colors.error,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (isDeleting) {
                                CircularProgressIndicator(color = colors.onError, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Text(text = "Delete", style = typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onError)
                            }
                        }
                    }
                }
            }
        }
    }
}
