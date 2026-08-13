package com.rajamohan.mindmingle.presentation.profile.component.mobile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rajamohan.mindmingle.core.settings.AppearanceMode
import com.rajamohan.mindmingle.core.settings.AppearanceSettings
import com.rajamohan.mindmingle.domain.model.PaymentRecord
import com.rajamohan.mindmingle.presentation.common.icon.CheckBadgeIcon
import com.rajamohan.mindmingle.presentation.common.icon.CrownIcon
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
import com.rajamohan.mindmingle.presentation.common.icon.FlameIcon
import com.rajamohan.mindmingle.presentation.common.icon.GearIcon
import com.rajamohan.mindmingle.presentation.common.icon.HeartIcon
import com.rajamohan.mindmingle.presentation.common.icon.HelpCircleIcon
import com.rajamohan.mindmingle.presentation.common.icon.LogoutIcon
import com.rajamohan.mindmingle.presentation.common.icon.PencilIcon
import com.rajamohan.mindmingle.presentation.common.icon.BoltIcon
import com.rajamohan.mindmingle.presentation.profile.viewmodel.ProfileViewModel
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    uid: String,
    userName: String = "Rajamohan Reddy",
    userEmail: String = "rajamohan.reddy@gmail.com",
    onLogout: () -> Unit = {},
    onUpgradeClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onOpenSupportClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: ProfileViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uid) {
        viewModel.loadProfile(uid)
    }

    // Second-layer enforcement: catches an admin disabling this account mid-session
    // (the first layer runs at every sign-in — see AuthViewModel.admitIfAllowed).
    LaunchedEffect(uiState.isAccountBlocked) {
        if (uiState.isAccountBlocked) {
            onLogout()
        }
    }

    // Deleting the account also deletes the Auth user, so the session is already gone.
    LaunchedEffect(uiState.isAccountDeleted) {
        if (uiState.isAccountDeleted) {
            onLogout()
        }
    }

    val appearanceMode by AppearanceSettings.mode.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val appearanceOptions = AppearanceMode.entries

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Profile",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                Surface(
                    onClick = { },
                    shape = CircleShape,
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        GearIcon(color = colors.onSurface, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Identity Header Card
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Container with Edit Badge & Verified Tag
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(colors.primary, colors.secondary)
                                    )
                                )
                                .border(3.dp, colors.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            DeveloperAvatarIcon(color = Color.White, modifier = Modifier.size(46.dp))
                        }

                        // Edit Badge
                        Surface(
                            onClick = onEditProfileClick,
                            shape = CircleShape,
                            color = colors.primary,
                            modifier = Modifier
                                .size(30.dp)
                                .offset(x = 2.dp, y = 2.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PencilIcon(color = colors.onPrimary, modifier = Modifier.size(13.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = userName,
                            style = typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        if (uiState.user?.githubUrl?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            CheckBadgeIcon(color = colors.tertiary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = userEmail,
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Identity Verification Progress Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Profile Completeness",
                                    style = typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = "${uiState.profileCompletionPercent}%",
                                    style = typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { uiState.profileCompletionPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = colors.primary,
                                trackColor = colors.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About Card — bio, occupation, interests (dating-app-style profile detail)
            val aboutUser = uiState.user
            if (aboutUser != null && (aboutUser.bio.isNotBlank() || aboutUser.occupation.isNotBlank() || aboutUser.interests.isNotEmpty())) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = colors.surface,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (aboutUser.bio.isNotBlank()) {
                            Text(
                                text = "About",
                                style = typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = aboutUser.bio,
                                style = typography.bodyMedium,
                                color = colors.onSurface
                            )
                        }

                        if (aboutUser.occupation.isNotBlank()) {
                            if (aboutUser.bio.isNotBlank()) Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Occupation",
                                style = typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ProfileTagChip(text = aboutUser.occupation)
                        }

                        if (aboutUser.interests.isNotEmpty()) {
                            if (aboutUser.bio.isNotBlank() || aboutUser.occupation.isNotBlank()) Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Interests",
                                style = typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                aboutUser.interests.forEach { interest -> ProfileTagChip(text = interest, accent = true) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Profile Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileStatCard(
                    number = "${uiState.likesCount}",
                    label = "Likes",
                    icon = { HeartIcon(color = colors.primary, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    number = "${uiState.matchesCount}",
                    label = "Matches",
                    icon = { FlameIcon(color = colors.primary, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    number = "${uiState.user?.interests?.size ?: 0}",
                    label = "Interests",
                    icon = { BoltIcon(color = colors.primary, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Plan card: the upgrade pitch, or the active-plan receipt once MindMingle+ is paid for.
            if (uiState.isPremium) {
                PlusActivePlanCard(
                    planLabel = uiState.activePlan?.label ?: "MindMingle+",
                    daysLeft = uiState.planDaysLeft
                )
            } else {
                UpgradeToPlusBanner(onUpgradeClick = onUpgradeClick)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance Theme Selector
            Text(
                text = "Appearance",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.4f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                appearanceOptions.forEach { mode ->
                    val isSelected = mode == appearanceMode
                    Surface(
                        onClick = { AppearanceSettings.set(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.surface else Color.Transparent,
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = mode.label,
                                style = typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.primary else colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Orders & Billing — every MindMingle+ payment, newest first, straight from the server.
            Text(
                text = "Orders & Billing",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            OrdersCard(
                isLoading = uiState.isLoadingBilling,
                hasOrders = uiState.hasOrders,
                payments = uiState.payments,
                renewsLabel = uiState.planRenewsLabel,
                isPlanActive = uiState.isPremium,
                error = uiState.billingError
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Navigation List
            Text(
                text = "Settings & Data",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileMenuItem(icon = { HelpCircleIcon(color = colors.onSurface, modifier = Modifier.size(20.dp)) }, title = "Help & Support", onClick = onOpenSupportClick)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Log Out Button
            Surface(
                onClick = onLogout,
                shape = RoundedCornerShape(18.dp),
                color = colors.errorContainer.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LogoutIcon(color = colors.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Account deletion. Wipes everything server-side, so it asks twice.
            Surface(
                onClick = { showDeleteConfirm = true },
                enabled = !uiState.isDeletingAccount,
                shape = RoundedCornerShape(18.dp),
                color = colors.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.isDeletingAccount) {
                        CircularProgressIndicator(
                            color = colors.onError,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Delete My Account",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onError
                        )
                    }
                }
            }

            if (uiState.deleteError.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.deleteError,
                    style = typography.bodySmall,
                    color = colors.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteAccount()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = colors.surface, shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Delete your account?",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "This erases your profile, photos, matches, messages, likes and " +
                        "subscription records permanently. It cannot be undone, and any time left " +
                        "on an MindMingle+ plan is lost.",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        color = colors.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Keep account",
                                style = typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(50),
                        color = colors.error,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Delete forever",
                                style = typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onError
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersCard(
    isLoading: Boolean,
    hasOrders: Boolean,
    payments: List<PaymentRecord>,
    renewsLabel: String,
    isPlanActive: Boolean,
    error: String
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    }
                }
                error.isNotBlank() -> {
                    Text(text = error, style = typography.bodySmall, color = colors.error)
                }
                !hasOrders -> {
                    Text(
                        text = "No orders yet. Your MindMingle+ payments will show up here.",
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                else -> {
                    if (isPlanActive) {
                        Text(
                            text = "Plan active until $renewsLabel",
                            style = typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.tertiary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        payments.forEach { payment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
}

@Composable
private fun ProfileTagChip(text: String, accent: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(50),
        color = if (accent) colors.primaryContainer.copy(alpha = 0.5f) else colors.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = text,
            style = typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (accent) colors.primary else colors.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ProfileStatCard(
    number: String,
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = number,
                style = typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onSurface
            )
            Text(
                text = label,
                style = typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }
            ChevronRightGlyph(color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun UpgradeToPlusBanner(onUpgradeClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.primaryContainer.copy(alpha = 0.7f),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.85f),
                            colors.secondary.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Upgrade to ",
                        style = typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "PRO",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Go ad-free and unlock unlimited matches, priority messaging and AI profile insights.",
                    style = typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    onClick = onUpgradeClick,
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CrownIcon(color = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upgrade Now",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlusActivePlanCard(planLabel: String, daysLeft: Int) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.tertiaryContainer,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.tertiary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CrownIcon(color = colors.tertiary, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = planLabel,
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onTertiaryContainer
                )
                Text(
                    text = if (daysLeft > 0) {
                        "Active · ad-free · $daysLeft ${if (daysLeft == 1) "day" else "days"} left"
                    } else {
                        "Active · ad-free"
                    },
                    style = typography.bodySmall,
                    color = colors.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ChevronRightGlyph(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.25f, h * 0.85f)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = w * 0.14f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}
