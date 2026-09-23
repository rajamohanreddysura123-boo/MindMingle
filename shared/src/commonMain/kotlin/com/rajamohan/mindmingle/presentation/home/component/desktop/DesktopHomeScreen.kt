package com.rajamohan.mindmingle.presentation.home.component.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.presentation.account.component.AccountSettingsScreen
import com.rajamohan.mindmingle.presentation.anonymous.component.desktop.DesktopAnonymousChatScreen
import com.rajamohan.mindmingle.presentation.chat.component.desktop.DesktopChatScreen
import com.rajamohan.mindmingle.presentation.common.component.LogoutConfirmDialog
import com.rajamohan.mindmingle.presentation.common.icon.ChatBubbleIcon
import com.rajamohan.mindmingle.presentation.common.icon.ChevronDownIcon
import com.rajamohan.mindmingle.presentation.common.icon.CrossIcon
import com.rajamohan.mindmingle.presentation.common.icon.HeartIcon
import com.rajamohan.mindmingle.presentation.common.icon.LogoutIcon
import com.rajamohan.mindmingle.presentation.common.icon.MaskIcon
import com.rajamohan.mindmingle.presentation.common.icon.PersonIcon
import com.rajamohan.mindmingle.presentation.common.icon.SparkleBurstIcon
import com.rajamohan.mindmingle.presentation.common.icon.TelescopeIcon
import com.rajamohan.mindmingle.presentation.common.component.RemoteProfileImage
import androidx.compose.foundation.layout.heightIn
import com.rajamohan.mindmingle.presentation.common.component.AppDialog
import com.rajamohan.mindmingle.presentation.home.component.mobile.DistanceLine
import com.rajamohan.mindmingle.presentation.home.component.mobile.ProfileDetailScreen
import com.rajamohan.mindmingle.presentation.home.component.mobile.FilterSheet
import com.rajamohan.mindmingle.presentation.home.component.mobile.PhotoProgressBar
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

    // Bumped whenever Edit Profile actually saves something, so the Profile tab reloads on
    // return rather than showing whatever it had cached from before the edit.
    var profileRefreshToken by remember { mutableStateOf(0) }

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
                onProfileSaved = {
                    showEditProfile = false
                    profileRefreshToken++
                }
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
                    DesktopNavTab.Likes -> DesktopLikesScreen(
                        uid = uid,
                        // Liking back opens the conversation, so the wall hands the tab over
                        // rather than leaving the user to find it themselves.
                        onOpenChat = { activeTab = DesktopNavTab.Chat }
                    )
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
                        onOpenAccountSettingsClick = { showAccountSettings = true },
                        onLikesClick = { activeTab = DesktopNavTab.Likes },
                        onChatsClick = { activeTab = DesktopNavTab.Chat },
                        refreshToken = profileRefreshToken
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
    var detailProfile by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(uid) {
        viewModel.onEvent(HomeEvent.LoadProfiles(uid))
    }

    val visibleProfiles = uiState.profiles.filterNot { connectedUids.contains(it.uid) }

    // Both overlays below (the filter sheet, the profile-detail popup) render as same-window
    // AppDialog content rather than a platform Dialog — see AppDialog's doc for why: Compose
    // Desktop's real Dialog window did not reliably forward mouse-wheel scroll to content inside
    // it. That means each has to be the LAST child of a Box spanning the whole tab, which is what
    // this wrapper is for; a platform Dialog needed no such positioning, since it was a separate
    // window regardless of where in the tree it was composed.
    Box(modifier = Modifier.fillMaxSize()) {
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
                // One card, centred, instead of a wall of small ones.
                //
                // The grid put twelve strangers on screen at 260dp each: too small for the photo to
                // carry anyone, and it turned deciding into scanning. A single large card is what
                // the phone shows and what the decision actually deserves, and it makes Pass mean
                // the same thing on both platforms — the card leaves, the next one takes its place.
                val profile = visibleProfiles.first()

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DesktopDiscoverCard(
                            onOpenDetails = { detailProfile = profile },
                            profile = profile,
                            distanceKm = uiState.distanceToKm(profile),
                            onConnect = {
                                connectedUids = connectedUids + profile.uid
                                viewModel.onEvent(HomeEvent.Connect(fromUid = uid, toUid = profile.uid))
                            },
                            // The view model drops the profile from the list, so the card leaves
                            // the wall without this screen keeping a second set of its own.
                            onPass = { viewModel.onEvent(HomeEvent.PassProfile(profile.uid)) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (visibleProfiles.size > 1) {
                                "${visibleProfiles.size - 1} more waiting"
                            } else {
                                "Last one for now"
                            },
                            style = typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // The mobile detail screen already renders everything a profile holds — bio, interests,
    // every profile-setup answer — so it is presented here rather than written a second time.
    detailProfile?.let { profile ->
        AppDialog(onDismissRequest = { detailProfile = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.background,
                shadowElevation = 18.dp,
                modifier = Modifier.width(560.dp).heightIn(max = 760.dp)
            ) {
                ProfileDetailScreen(
                    profile = profile,
                    distanceKm = profile.distanceKm,
                    onBack = { detailProfile = null }
                )
            }
        }
    }

    // The same sheet the phone uses. Desktop had no filters at all, which meant distance, country
    // and district — the three MindMingle+ filters — were unreachable for anyone on a laptop, and
    // a paying subscriber could not use what they had paid for.
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
    }
}

/**
 * A round step control on the photo. Desktop users look for arrows; the click zones underneath
 * cover everyone who reaches for the photo itself out of phone habit.
 */
@Composable
private fun PhotoArrow(isForward: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.4f),
        modifier = modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            ChevronDownIcon(
                color = Color.White,
                // One glyph, turned: a left chevron is a down chevron rotated a quarter turn, and
                // adding two more icon functions to draw the same shape would be silly.
                modifier = Modifier
                    .size(14.dp)
                    .rotate(if (isForward) -90f else 90f)
            )
        }
    }
}

/**
 * The desktop Discover card: one person, centred, at a size worth looking at.
 *
 * Same visual language as the mobile deck card, and now the same shape of decision. It replaced a
 * grid of 260dp tiles where the photo was too small to carry anyone and choosing had become
 * scanning.
 *
 * Photos step with the arrows on the hero, or by clicking its left and right thirds — the same
 * zones the phone uses. There is no horizontal swipe here because there is no drag to swipe with:
 * on desktop, Pass and Connect are buttons.
 *
 * Both actions remove the card and the next profile takes the slot. Passing goes through
 * PassProfile(uid) rather than the deck's position-based Pass, because this screen renders a list
 * and not a cursor.
 */
@Composable
private fun DesktopDiscoverCard(
    profile: User,
    distanceKm: Double?,
    onConnect: () -> Unit,
    onPass: () -> Unit,
    onOpenDetails: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val photos = profile.displayPhotoUrls
    var photoIndex by remember(profile.uid) { mutableStateOf(0) }

    fun step(forward: Boolean) {
        if (photos.size < 2) return
        photoIndex = if (forward) {
            (photoIndex + 1) % photos.size
        } else {
            (photoIndex - 1 + photos.size) % photos.size
        }
    }

    Surface(
        // The whole card opens the profile; the photo zones and the two buttons keep their own
        // clicks, so nothing here fights for the same press.
        onClick = onOpenDetails,
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        shadowElevation = 8.dp,
        modifier = Modifier.width(420.dp).height(620.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                RemoteProfileImage(
                    url = photos.getOrNull(photoIndex).orEmpty(),
                    uid = profile.uid,
                    contentDescription = profile.name.ifBlank { "Profile photo" },
                    placeholderIconSize = 72.dp,
                    modifier = Modifier.fillMaxSize()
                )

                if (photos.size > 1) {
                    // Click zones first, arrows on top of them: the zones are what a phone user
                    // reaches for out of habit, the arrows are what a mouse user looks for.
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { step(forward = false) }
                        )
                        Box(modifier = Modifier.weight(1f).fillMaxHeight())
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { step(forward = true) }
                        )
                    }

                    PhotoArrow(
                        isForward = false,
                        onClick = { step(forward = false) },
                        modifier = Modifier.align(Alignment.CenterStart).padding(10.dp)
                    )
                    PhotoArrow(
                        isForward = true,
                        onClick = { step(forward = true) },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(10.dp)
                    )

                    PhotoProgressBar(
                        count = photos.size,
                        activeIndex = photoIndex,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }

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

            // Pass sits beside Connect rather than on the card as a corner "x": the wall is
            // browsed with a mouse, and a dismiss that only appears on hover is a dismiss most
            // people never find. It is the quieter of the two by weight, not by size — skipping
            // is as ordinary an action here as connecting.
            Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                Surface(
                    onClick = onPass,
                    shape = RoundedCornerShape(0.dp),
                    color = colors.surfaceVariant,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CrossIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pass",
                            style = typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = onConnect,
                    shape = RoundedCornerShape(0.dp),
                    color = colors.primary,
                    modifier = Modifier.weight(1.4f).fillMaxHeight()
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
}
