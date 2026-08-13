package com.rajamohan.mindmingle.presentation.home.component.desktop

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.presentation.anonymous.component.desktop.DesktopAnonymousChatScreen
import com.rajamohan.mindmingle.presentation.chat.component.desktop.DesktopChatScreen
import com.rajamohan.mindmingle.presentation.common.icon.ChatBubbleIcon
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
import com.rajamohan.mindmingle.presentation.common.icon.HeartIcon
import com.rajamohan.mindmingle.presentation.common.icon.LogoutIcon
import com.rajamohan.mindmingle.presentation.common.icon.MaskIcon
import com.rajamohan.mindmingle.presentation.common.icon.PersonIcon
import com.rajamohan.mindmingle.presentation.common.icon.SparkleBurstIcon
import com.rajamohan.mindmingle.presentation.common.icon.TelescopeIcon
import com.rajamohan.mindmingle.presentation.home.component.mobile.avatarGradientFor
import com.rajamohan.mindmingle.presentation.home.viewmodel.HomeEvent
import com.rajamohan.mindmingle.presentation.home.viewmodel.HomeViewModel
import com.rajamohan.mindmingle.presentation.likes.component.desktop.DesktopLikesScreen
import com.rajamohan.mindmingle.presentation.premium.component.desktop.DesktopPremiumScreen
import com.rajamohan.mindmingle.presentation.profile.component.desktop.DesktopProfileScreen
import com.rajamohan.mindmingle.presentation.profilesetup.component.desktop.DesktopProfileSetupScreen
import com.rajamohan.mindmingle.presentation.support.component.SupportChatScreen
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

private enum class DesktopNavTab(val label: String) {
    Discover("Discover"),
    Likes("Likes"),
    Chat("Messages"),
    Anonymous("Anonymous"),
    Profile("Profile")
}

/**
 * Desktop-native shell: persistent side rail instead of a bottom pill (there's no
 * thumb to reach for on a mouse/trackpad, and the extra width is free real estate),
 * routing into genuinely desktop-shaped tab content rather than reflowed mobile screens.
 */
@Composable
fun DesktopHomeScreen(
    uid: String,
    userName: String = "Rajamohan Reddy",
    userEmail: String = "rajamohan.reddy@gmail.com",
    onLogout: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var activeTab by remember { mutableStateOf(DesktopNavTab.Discover) }
    var showPremium by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showSupportChat by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        if (showPremium) {
            DesktopPremiumScreen(uid = uid, onBack = { showPremium = false })
            return@Surface
        }

        if (showEditProfile) {
            DesktopProfileSetupScreen(
                uid = uid,
                email = userEmail,
                phoneNumber = "",
                isEditMode = true,
                onBack = { showEditProfile = false },
                onProfileSaved = { showEditProfile = false }
            )
            return@Surface
        }

        if (showSupportChat) {
            SupportChatScreen(uid = uid, userName = userName, onBack = { showSupportChat = false })
            return@Surface
        }

        Row(modifier = Modifier.fillMaxSize()) {
            DesktopSideRail(
                activeTab = activeTab,
                onTabSelected = { activeTab = it },
                onLogout = onLogout,
                modifier = Modifier.fillMaxHeight()
            )

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (activeTab) {
                    DesktopNavTab.Discover -> DesktopDiscoverContent(uid = uid)
                    DesktopNavTab.Likes -> DesktopLikesScreen(uid = uid)
                    DesktopNavTab.Chat -> DesktopChatScreen(uid = uid)
                    DesktopNavTab.Anonymous -> DesktopAnonymousChatScreen(uid = uid)
                    DesktopNavTab.Profile -> DesktopProfileScreen(
                        uid = uid,
                        userName = userName,
                        userEmail = userEmail,
                        onLogout = onLogout,
                        onUpgradeClick = { showPremium = true },
                        onEditProfileClick = { showEditProfile = true },
                        onOpenSupportClick = { showSupportChat = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopSideRail(
    activeTab: DesktopNavTab,
    onTabSelected: (DesktopNavTab) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        modifier = modifier
            .width(220.dp)
            .background(colors.surfaceContainerLow)
            .border(width = 1.dp, color = colors.outlineVariant.copy(alpha = 0.4f))
            .safeContentPadding()
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(text = "MindMingle", style = typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = colors.primary, fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        DesktopNavTab.entries.forEach { tab ->
            val isSelected = tab == activeTab
            Surface(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) colors.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    val iconColor = if (isSelected) colors.primary else colors.onSurfaceVariant
                    when (tab) {
                        DesktopNavTab.Discover -> TelescopeIcon(color = iconColor, modifier = Modifier.size(20.dp))
                        DesktopNavTab.Likes -> HeartIcon(color = iconColor, modifier = Modifier.size(20.dp))
                        DesktopNavTab.Chat -> ChatBubbleIcon(color = iconColor, modifier = Modifier.size(20.dp))
                        DesktopNavTab.Anonymous -> MaskIcon(color = iconColor, modifier = Modifier.size(20.dp))
                        DesktopNavTab.Profile -> PersonIcon(color = iconColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = tab.label,
                        style = typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colors.onBackground else colors.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            onClick = onLogout,
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                LogoutIcon(color = colors.error, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Text(text = "Log Out", style = typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.error)
            }
        }
    }
}

@Composable
private fun DesktopDiscoverContent(uid: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: HomeViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    var connectedUids by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(uid) {
        viewModel.onEvent(HomeEvent.LoadProfiles(uid))
    }

    uiState.matchedUser?.let { matchedUser ->
        DesktopMatchDialog(matchedUser = matchedUser, onDismiss = { viewModel.onEvent(HomeEvent.DismissMatch) })
    }

    val visibleProfiles = uiState.profiles.filterNot { connectedUids.contains(it.uid) }

    Column(modifier = Modifier.fillMaxSize().safeContentPadding().padding(Spacing.desktopScreenPadding)) {
        Text(text = "Discover Tech Partners", style = typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Browse everyone available right now. Click Connect on who you'd like to work with.",
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
            visibleProfiles.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TelescopeIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "No more tech partners nearby", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text(text = "Check back later for new profiles", style = typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 260.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visibleProfiles, key = { it.uid }) { profile ->
                        DesktopDiscoverCard(
                            profile = profile,
                            onConnect = {
                                connectedUids = connectedUids + profile.uid
                                viewModel.onEvent(HomeEvent.Connect(fromUid = uid, toUid = profile.uid))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopDiscoverCard(profile: User, onConnect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.height(340.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(140.dp).background(Brush.linearGradient(avatarGradientFor(profile.uid))),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)).border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    DeveloperAvatarIcon(color = Color.White, modifier = Modifier.size(34.dp))
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                Text(text = profile.name, style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Text(
                    text = profile.experienceLevel.ifBlank { "Dev" },
                    style = typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = profile.bio.ifBlank { "Excited to connect with fellow developers." },
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (profile.occupation.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(6.dp), color = colors.primaryContainer.copy(alpha = 0.6f)) {
                            Text(
                                text = profile.occupation,
                                style = typography.labelSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                onClick = onConnect,
                shape = RoundedCornerShape(0.dp),
                color = colors.primary,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    ChatBubbleIcon(color = colors.onPrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Connect", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun DesktopMatchDialog(matchedUser: User, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = colors.surface, shadowElevation = 16.dp, modifier = Modifier.widthIn(max = 420.dp)) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                SparkleBurstIcon(color = colors.primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "It's a Match!", style = typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You and ${matchedUser.name} both connected. Start the conversation!",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Surface(onClick = onDismiss, shape = RoundedCornerShape(50), color = colors.primary, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(text = "Keep Browsing", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                    }
                }
            }
        }
    }
}
