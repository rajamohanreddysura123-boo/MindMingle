package com.rajamohan.mindmingle.presentation.home.component.desktop

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rajamohan.mindmingle.domain.model.GeoDistance
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.presentation.account.component.AccountSettingsScreen
import com.rajamohan.mindmingle.presentation.anonymous.component.desktop.DesktopAnonymousChatScreen
import com.rajamohan.mindmingle.presentation.chat.component.desktop.DesktopChatScreen
import com.rajamohan.mindmingle.presentation.common.component.LogoutConfirmDialog
import com.rajamohan.mindmingle.presentation.common.icon.ChatBubbleIcon
import com.rajamohan.mindmingle.presentation.common.icon.HeartIcon
import com.rajamohan.mindmingle.presentation.common.icon.LogoutIcon
import com.rajamohan.mindmingle.presentation.common.icon.MaskIcon
import com.rajamohan.mindmingle.presentation.common.icon.PersonIcon
import com.rajamohan.mindmingle.presentation.common.icon.SparkleBurstIcon
import com.rajamohan.mindmingle.presentation.common.icon.TelescopeIcon
import com.rajamohan.mindmingle.presentation.common.component.RemoteProfileImage
import com.rajamohan.mindmingle.presentation.home.component.mobile.DistanceLine
import com.rajamohan.mindmingle.presentation.home.component.mobile.FilterSheet
import com.rajamohan.mindmingle.presentation.home.component.mobile.PremiumBadge
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
    userName: String = "",
    userEmail: String = "",
    onLogout: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var activeTab by remember { mutableStateOf(DesktopNavTab.Discover) }
    var showPremium by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showSupportChat by remember { mutableStateOf(false) }
    var showAccountSettings by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        if (showPremium) {
            DesktopPremiumScreen(uid = uid, onBack = { showPremium = false })
            return@Surface
        }

        if (showEditProfile) {
            DesktopProfileSetupScreen(
                uid = uid,
                email = userEmail,
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

        if (showAccountSettings) {
            AccountSettingsScreen(
                onBack = { showAccountSettings = false },
                // Deactivated or deleted — either way the session is already gone.
                onAccountClosed = {
                    showAccountSettings = false
                    onLogout()
                }
            )
            return@Surface
        }

        Row(modifier = Modifier.fillMaxSize()) {
            DesktopSideRail(
                activeTab = activeTab,
                onTabSelected = { activeTab = it },
                onLogout = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxHeight()
            )

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (activeTab) {
                    DesktopNavTab.Discover -> DesktopDiscoverContent(
                        uid = uid,
                        onUpgradeClick = { showPremium = true }
                    )
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
                        onOpenSupportClick = { showSupportChat = true },
                        onOpenAccountSettingsClick = { showAccountSettings = true }
                    )
                }
            }
        }
    }

    // The side rail's Log Out asks first, same as the one on the Profile tab.
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
private fun DesktopDiscoverContent(uid: String, onUpgradeClick: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: HomeViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    var connectedUids by remember { mutableStateOf(setOf<String>()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        viewModel.onEvent(HomeEvent.LoadProfiles(uid))
    }

    // The same sheet the phone uses. Desktop had no filters at all, which meant distance, country
    // and district — the three MindMingle+ filters — were unreachable for anyone on a laptop, and
    // a paying subscriber could not use what they had paid for. It is a Dialog, so it needs no
    // desktop-specific layout to be usable at this size.
    if (showFilterSheet) {
        FilterSheet(
            filters = uiState.filters,
            canFilterByDistance = uiState.hasMyLocation,
            onLoadDistricts = { countryCode -> viewModel.districtsFor(countryCode) },
            isPremium = uiState.isPremium,
            onApply = { newFilters ->
                viewModel.onEvent(HomeEvent.ApplyFilters(newFilters))
                showFilterSheet = false
            },
            onReset = { viewModel.onEvent(HomeEvent.ResetFilters) },
            onUpgrade = {
                showFilterSheet = false
                onUpgradeClick()
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    val visibleProfiles = uiState.profiles.filterNot { connectedUids.contains(it.uid) }

    Column(modifier = Modifier.fillMaxSize().safeContentPadding().padding(Spacing.desktopScreenPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Discover Tech Partners", style = typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Browse everyone available right now. Click Connect on who you'd like to work with.",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }

            val activeFilterCount = uiState.filters.activeCount
            Surface(
                onClick = { showFilterSheet = true },
                shape = RoundedCornerShape(14.dp),
                color = if (activeFilterCount > 0) colors.primaryContainer.copy(alpha = 0.6f) else colors.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) {
                    Text(
                        text = if (activeFilterCount > 0) "Filters · $activeFilterCount" else "Filters",
                        style = typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (activeFilterCount > 0) colors.primary else colors.onSurface
                    )
                }
            }
        }

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
                        Text(text = "No more developers nearby", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        Text(text = "New profiles show up as developers join. Check back a little later.", style = typography.bodySmall, color = colors.onSurfaceVariant)
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
                            distanceKm = uiState.distanceToKm(profile),
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

/**
 * Grid card for the desktop Discover wall.
 *
 * Same visual language as the mobile deck card — per-uid gradient hero, experience pill, verified
 * badge, intent pill, interests — at grid scale. A person should look the same on both platforms;
 * before this the desktop card was a flat header and two lines of text and read as a different app.
 *
 * There is no Pass here on purpose: the desktop wall shows every profile at once rather than one
 * at a time, so there is nothing to advance past. Connecting removes the card from the wall.
 */
@Composable
private fun DesktopDiscoverCard(profile: User, distanceKm: Double?, onConnect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.height(392.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Desktop shows the first photo only: the grid puts several cards on screen at once,
            // and per-card photo stepping there would be a lot of controls competing for a click.
            // The whole set is on the profile itself.
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                RemoteProfileImage(
                    url = profile.displayPhotoUrls.firstOrNull().orEmpty(),
                    uid = profile.uid,
                    contentDescription = profile.name.ifBlank { "Profile photo" },
                    placeholderIconSize = 48.dp,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f))))
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = profile.name.ifBlank { "Developer" },
                        style = typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.isPremium) {
                        Spacer(modifier = Modifier.width(4.dp))
                        PremiumBadge(size = 15.dp, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    if (profile.age > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile.age.toString(),
                            style = typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(7.dp))

                Surface(shape = RoundedCornerShape(50), color = colors.primaryContainer.copy(alpha = 0.7f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        SparkleBurstIcon(color = colors.primary, modifier = Modifier.size(12.dp))
                        Text(
                            text = profile.lookingFor.ifBlank { "Open to collaborating" },
                            style = typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                DistanceLine(
                    distanceKm = distanceKm,
                    fallbackLocation = profile.location,
                    style = typography.bodySmall,
                    iconSize = 11.dp
                )

                val metaLines = buildList {
                    if (profile.occupation.isNotBlank()) add(profile.occupation)
                }
                if (metaLines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = metaLines.joinToString("  ·  "),
                        style = typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = profile.bio.ifBlank { "Excited to connect with fellow developers." },
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (profile.interests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        profile.interests.take(3).forEach { interest ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceVariant,
                                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = interest,
                                    style = typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
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
