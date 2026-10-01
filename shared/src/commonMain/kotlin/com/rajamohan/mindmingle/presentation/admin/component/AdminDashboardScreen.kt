package com.rajamohan.mindmingle.presentation.admin.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminDashboardViewModel
import com.rajamohan.mindmingle.presentation.common.icon.ChatBubbleIcon
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
import com.rajamohan.mindmingle.presentation.common.icon.EnvelopeIcon
import com.rajamohan.mindmingle.presentation.common.icon.FlameIcon
import com.rajamohan.mindmingle.presentation.common.icon.GearIcon
import com.rajamohan.mindmingle.presentation.common.icon.HelpCircleIcon
import com.rajamohan.mindmingle.presentation.common.icon.LogoutIcon
import com.rajamohan.mindmingle.presentation.common.icon.PersonIcon
import com.rajamohan.mindmingle.presentation.home.component.mobile.DesktopBreakpoint
import com.rajamohan.mindmingle.presentation.home.component.mobile.avatarGradientFor
import org.koin.compose.viewmodel.koinViewModel

/** Support Chats is desktop-only — an admin who signs in from the mobile app never sees it, even though the same account has the admins/{uid} doc. */
@Composable
fun AdminDashboardScreen(
    adminUid: String = "",
    adminName: String = "",
    adminEmail: String = "",
    onManageUsers: () -> Unit,
    onManagePricing: () -> Unit,
    onManageSubscribers: () -> Unit,
    onManageSupport: () -> Unit,
    onManageDeletionRequests: () -> Unit,
    onSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val dashboardViewModel: AdminDashboardViewModel = koinViewModel()
    val uiState by dashboardViewModel.uiState.collectAsState()

    var showProfilePopup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        dashboardViewModel.loadStats()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktopWidth = maxWidth >= DesktopBreakpoint
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .widthIn(max = 900.dp)
                .padding(32.dp)
        ) {
            // ── Top Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(
                        text = "MindMingle — Tech Orbit admin",
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }

                // Right side: Profile avatar icon + Sign Out button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ── Admin Profile Avatar Button + Dropdown ──────────────
                    Box {
                        AdminAvatarButton(
                            adminUid = adminUid,
                            adminName = adminName,
                            onClick = { showProfilePopup = true }
                        )

                        DropdownMenu(
                            expanded = showProfilePopup,
                            onDismissRequest = { showProfilePopup = false },
                            offset = DpOffset(x = 0.dp, y = 4.dp)
                        ) {
                            AdminProfileMenuContent(
                                adminUid = adminUid,
                                adminName = adminName,
                                adminEmail = adminEmail,
                                onSignOut = {
                                    showProfilePopup = false
                                    onSignOut()
                                }
                            )
                        }
                    }

                    // Sign Out button
                    Surface(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            LogoutIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Sign Out", style = typography.labelLarge, color = colors.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AdminStatTile(
                        icon = { PersonIcon(color = colors.primary, modifier = Modifier.size(22.dp)) },
                        label = "Total Users",
                        value = uiState.stats.totalUsers.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatTile(
                        icon = { FlameIcon(color = colors.primary, modifier = Modifier.size(22.dp)) },
                        label = "Total Conversations",
                        value = uiState.stats.totalConversations.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatTile(
                        icon = { ChatBubbleIcon(color = colors.primary, modifier = Modifier.size(22.dp)) },
                        label = "Total Messages",
                        value = uiState.stats.totalMessages.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    onClick = onManageUsers,
                    shape = RoundedCornerShape(18.dp),
                    color = colors.primary,
                    modifier = Modifier.height(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    ) {
                        Text(
                            text = "Manage Users",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary
                        )
                    }
                }

                Surface(
                    onClick = onManageSubscribers,
                    shape = RoundedCornerShape(18.dp),
                    color = colors.tertiary,
                    modifier = Modifier.height(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    ) {
                        Text(
                            text = "Subscribers",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onTertiary
                        )
                    }
                }

                Surface(
                    onClick = onManagePricing,
                    shape = RoundedCornerShape(18.dp),
                    color = colors.secondary,
                    modifier = Modifier.height(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    ) {
                        Text(
                            text = "Plan Pricing",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSecondary
                        )
                    }
                }

                Surface(
                    onClick = onManageDeletionRequests,
                    shape = RoundedCornerShape(18.dp),
                    color = colors.errorContainer,
                    modifier = Modifier.height(52.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    ) {
                        Text(
                            text = "Deletion Requests",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onErrorContainer
                        )
                    }
                }

                // Desktop-only: an admin on the mobile app never sees Support Chats.
                if (isDesktopWidth) {
                    Surface(
                        onClick = onManageSupport,
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceVariant,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 28.dp)) {
                            HelpCircleIcon(color = colors.onSurface, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Support Chats",
                                style = typography.titleMedium,
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

// ── Admin Avatar Circle Button ────────────────────────────────────────────

@Composable
private fun AdminAvatarButton(
    adminUid: String,
    adminName: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val initials = adminName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "A" }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.primaryContainer,
        modifier = Modifier
            .size(44.dp)
            .shadow(4.dp, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (adminName.isNotBlank()) {
                Text(
                    text = initials,
                    style = typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onPrimaryContainer,
                    fontSize = 15.sp
                )
            } else {
                DeveloperAvatarIcon(
                    color = colors.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ── Admin Profile Dropdown Menu Content ──────────────────────────────────

@Composable
private fun AdminProfileMenuContent(
    adminUid: String,
    adminName: String,
    adminEmail: String,
    onSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val displayName = adminName.ifBlank { adminEmail.substringBefore("@").ifBlank { "Admin" } }
    val initials = displayName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "A" }

    Column(modifier = Modifier.width(280.dp)) {

        // ── Gradient header + avatar ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .background(
                    Brush.linearGradient(
                        if (adminUid.isNotBlank()) avatarGradientFor(adminUid)
                        else listOf(colors.primary, colors.secondary)
                    )
                )
        ) {
            // Admin badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.primary.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    GearIcon(color = colors.onPrimary, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Admin",
                        style = typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary,
                        fontSize = 10.sp
                    )
                }
            }

            // Avatar circle — sits at bottom centre of the banner, overlapping into the body
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 28.dp)
                    .size(56.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(3.dp, colors.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface
                )
            }
        }

        // Space for the avatar overhang
        Spacer(modifier = Modifier.height(36.dp))

        // ── Name + email ──────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = displayName,
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            if (adminEmail.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EnvelopeIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = adminEmail,
                        style = typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.4f))

        // ── Sign Out row ──────────────────────────────────────────────
        Surface(
            onClick = onSignOut,
            color = colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                LogoutIcon(color = colors.error, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sign Out",
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.error
                )
            }
        }
    }
}

// ── Stat Tile ─────────────────────────────────────────────────────────────

@Composable
private fun AdminStatTile(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
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
        Column(modifier = Modifier.padding(20.dp)) {
            icon()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onSurface
            )
            Text(
                text = label,
                style = typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
