package com.rajamohan.mindmingle.presentation.profile.component.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rajamohan.mindmingle.domain.model.Invoice
import com.rajamohan.mindmingle.presentation.billing.InvoiceDetailDialog
import com.rajamohan.mindmingle.presentation.common.icon.CheckBadgeIcon
import com.rajamohan.mindmingle.presentation.common.icon.CrownIcon
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
import com.rajamohan.mindmingle.presentation.common.icon.FlameIcon
import com.rajamohan.mindmingle.presentation.common.icon.HeartIcon
import com.rajamohan.mindmingle.presentation.common.icon.HelpCircleIcon
import com.rajamohan.mindmingle.presentation.common.component.LogoutConfirmDialog
import com.rajamohan.mindmingle.presentation.common.icon.GearIcon
import com.rajamohan.mindmingle.presentation.common.icon.LogoutIcon
import com.rajamohan.mindmingle.presentation.common.icon.PencilIcon
import com.rajamohan.mindmingle.presentation.common.icon.BoltIcon
import com.rajamohan.mindmingle.presentation.profile.viewmodel.ProfileViewModel
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/** Two-column desktop layout: identity + stats card on the left, settings list on the right. */
@Composable
fun DesktopProfileScreen(
    uid: String,
    userName: String = "",
    userEmail: String = "",
    onLogout: () -> Unit = {},
    onUpgradeClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onOpenSupportClick: () -> Unit = {},
    onOpenAccountSettingsClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: ProfileViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uid) {
        viewModel.loadProfile(uid)
    }

    LaunchedEffect(uiState.isAccountBlocked) {
        if (uiState.isAccountBlocked) onLogout()
    }

    var showLogoutConfirm by remember { mutableStateOf(false) }

    // The invoice currently being read, if any. View state only — it dies with the screen.
    var openInvoice by remember { mutableStateOf<Invoice?>(null) }

    openInvoice?.let { invoice ->
        InvoiceDetailDialog(invoice = invoice, onDismiss = { openInvoice = null })
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.desktopScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Left column: identity, stats, upgrade banner
            Column(modifier = Modifier.width(340.dp)) {
                Surface(shape = RoundedCornerShape(28.dp), color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(colors.primary, colors.secondary)))
                                    .border(3.dp, colors.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                DeveloperAvatarIcon(color = Color.White, modifier = Modifier.size(46.dp))
                            }
                            Surface(
                                onClick = onEditProfileClick,
                                shape = CircleShape,
                                color = colors.primary,
                                modifier = Modifier.size(30.dp).offset(x = 2.dp, y = 2.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    PencilIcon(color = colors.onPrimary, modifier = Modifier.size(13.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = userName.ifBlank { uiState.user?.name.orEmpty().ifBlank { "MindMingle user" } }, style = typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
                            if (uiState.user?.githubUrl?.isNotBlank() == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                CheckBadgeIcon(color = colors.tertiary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(text = userEmail.ifBlank { uiState.user?.email.orEmpty() }, style = typography.bodyMedium, color = colors.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(shape = RoundedCornerShape(18.dp), color = colors.surfaceVariant.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Profile Completeness", style = typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                                    Text(text = "${uiState.profileCompletionPercent}%", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.primary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { uiState.profileCompletionPercent / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = colors.primary,
                                    trackColor = colors.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopStatTile(icon = { HeartIcon(color = colors.primary, modifier = Modifier.size(18.dp)) }, value = "${uiState.likesCount}", label = "Likes", modifier = Modifier.weight(1f))
                    DesktopStatTile(icon = { FlameIcon(color = colors.primary, modifier = Modifier.size(18.dp)) }, value = "${uiState.conversationsCount}", label = "Chats", modifier = Modifier.weight(1f))
                    DesktopStatTile(icon = { BoltIcon(color = colors.primary, modifier = Modifier.size(18.dp)) }, value = "${uiState.user?.interests?.size ?: 0}", label = "Interests", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isPremium) {
                    Surface(shape = RoundedCornerShape(24.dp), color = colors.tertiaryContainer, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(colors.tertiary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CrownIcon(color = colors.tertiary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = uiState.activePlan?.label ?: "MindMingle+",
                                    style = typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onTertiaryContainer
                                )
                                Text(
                                    text = if (uiState.planDaysLeft > 0) {
                                        "Active · ad-free · ${uiState.planDaysLeft} ${if (uiState.planDaysLeft == 1) "day" else "days"} left"
                                    } else {
                                        "Active · ad-free"
                                    },
                                    style = typography.bodySmall,
                                    color = colors.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else {
                    Surface(shape = RoundedCornerShape(24.dp), color = colors.primaryContainer.copy(alpha = 0.7f), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(colors.primary.copy(alpha = 0.85f), colors.secondary.copy(alpha = 0.85f))))
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Upgrade to ", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White.copy(alpha = 0.25f)) {
                                        Text(text = "PRO", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Go ad-free and filter Discover by occupation, experience, interests, languages, distance and lifestyle.",
                                    style = typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(onClick = onUpgradeClick, shape = RoundedCornerShape(50), color = Color.White, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                                        CrownIcon(color = colors.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Upgrade Now", style = typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right column: settings
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Settings & Data", style = typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onBackground)
                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopMenuRow(icon = { HelpCircleIcon(color = colors.onSurface, modifier = Modifier.size(20.dp)) }, title = "Help & Support", onClick = onOpenSupportClick)
                    // Deactivation and deletion both live behind here — neither belongs one click away.
                    DesktopMenuRow(icon = { GearIcon(color = colors.onSurface, modifier = Modifier.size(20.dp)) }, title = "Account Settings", onClick = onOpenAccountSettingsClick)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Orders & Billing", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
                Spacer(modifier = Modifier.height(10.dp))

                Surface(shape = RoundedCornerShape(20.dp), color = colors.surface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                        when {
                            uiState.isLoadingBilling -> {
                                Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                }
                            }
                            uiState.billingError.isNotBlank() -> {
                                Text(text = uiState.billingError, style = typography.bodySmall, color = colors.error)
                            }
                            !uiState.hasOrders -> {
                                Text(
                                    text = "No orders yet. Your MindMingle+ payments will show up here.",
                                    style = typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            else -> {
                                if (uiState.isPremium) {
                                    Text(
                                        text = "Plan active until ${uiState.planRenewsLabel}",
                                        style = typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.tertiary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    uiState.payments.forEach { payment ->
                                        val invoice = uiState.billing?.invoiceFor(payment.paymentId)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                // Admin grants carry no invoice; that row stays inert.
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
                                                Text(
                                                    text = payment.plan?.label ?: payment.planId,
                                                    style = typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.onSurface
                                                )
                                                Text(
                                                    text = "${payment.dateLabel} · ${payment.paymentId}",
                                                    style = typography.bodySmall,
                                                    color = colors.onSurfaceVariant
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
                                                color = colors.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Confirms first — a mis-click here should not end the session.
                    Surface(
                        onClick = { showLogoutConfirm = true },
                        shape = RoundedCornerShape(18.dp),
                        color = colors.errorContainer.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, colors.error.copy(alpha = 0.3f)),
                        modifier = Modifier.widthIn(min = 220.dp).height(52.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                            LogoutIcon(color = colors.error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Log Out", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.error)
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirm) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }
}

@Composable
private fun DesktopStatTile(icon: @Composable () -> Unit, value: String, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(shape = RoundedCornerShape(18.dp), color = colors.surface, shadowElevation = 2.dp, modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
            Text(text = label, style = typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun DesktopMenuRow(icon: @Composable () -> Unit, title: String, onClick: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = colors.surface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(14.dp))
                Text(text = title, style = typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            }
        }
    }
}
