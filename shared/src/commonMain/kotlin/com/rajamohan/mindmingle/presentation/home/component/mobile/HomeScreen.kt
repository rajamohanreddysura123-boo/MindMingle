package com.rajamohan.mindmingle.presentation.home.component.mobile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rajamohan.mindmingle.domain.model.OccupationRepository
import com.rajamohan.mindmingle.domain.model.ProfileOptionsRepository
import com.rajamohan.mindmingle.domain.model.GeoDistance
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.presentation.ads.SponsoredAdCard
import com.rajamohan.mindmingle.presentation.anonymous.component.mobile.AnonymousChatScreen
import com.rajamohan.mindmingle.presentation.chat.component.mobile.ChatScreen
import com.rajamohan.mindmingle.presentation.common.icon.ChatBubbleIcon
import androidx.compose.ui.text.TextStyle
import com.rajamohan.mindmingle.presentation.common.icon.CheckBadgeIcon
import com.rajamohan.mindmingle.presentation.common.icon.LocationPinIcon
import com.rajamohan.mindmingle.presentation.common.icon.EnvelopeIcon
import com.rajamohan.mindmingle.presentation.common.icon.SparkleBurstIcon
import com.rajamohan.mindmingle.presentation.common.icon.TelescopeIcon
import com.rajamohan.mindmingle.domain.model.CountryCode
import com.rajamohan.mindmingle.domain.model.CountryCodeRepository
import com.rajamohan.mindmingle.domain.model.DiscoverFilterDefaults
import com.rajamohan.mindmingle.domain.model.DiscoverFilterOptions
import com.rajamohan.mindmingle.domain.model.ProfileFieldOption
import com.rajamohan.mindmingle.presentation.common.icon.ChevronDownIcon
import com.rajamohan.mindmingle.presentation.home.viewmodel.DiscoverFilters
import com.rajamohan.mindmingle.presentation.home.viewmodel.HomeEvent
import com.rajamohan.mindmingle.presentation.home.viewmodel.HomeViewModel
import com.rajamohan.mindmingle.presentation.likes.component.mobile.LikesScreen
import com.rajamohan.mindmingle.presentation.premium.component.mobile.PremiumScreen
import com.rajamohan.mindmingle.presentation.profile.component.mobile.ProfileScreen
import com.rajamohan.mindmingle.presentation.profilesetup.component.mobile.ProfileSetupScreen
import com.rajamohan.mindmingle.presentation.profilesetup.viewmodel.ProfileSetupOptions
import com.rajamohan.mindmingle.presentation.home.component.desktop.DesktopHomeScreen
import com.rajamohan.mindmingle.presentation.account.component.AccountSettingsScreen
import com.rajamohan.mindmingle.core.push.NotificationDestination
import com.rajamohan.mindmingle.core.push.PendingDestination
import com.rajamohan.mindmingle.presentation.notifications.AlertsViewModel
import com.rajamohan.mindmingle.presentation.common.component.RemoteProfileImage
import com.rajamohan.mindmingle.presentation.support.component.SupportChatScreen
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

internal val avatarPalettes = listOf(
    listOf(Color(0xFF6200EE), Color(0xFFBB86FC)),
    listOf(Color(0xFFFF4081), Color(0xFFFF80AB)),
    listOf(Color(0xFF00C853), Color(0xFFB9F6CA)),
    listOf(Color(0xFF651FFF), Color(0xFFB388FF)),
    listOf(Color(0xFFFF6D00), Color(0xFFFFD180))
)

internal fun avatarGradientFor(uid: String): List<Color> =
    avatarPalettes[(uid.hashCode().let { if (it < 0) -it else it }) % avatarPalettes.size]

/** Desktop breakpoint shared across the app's platform-dispatching entry composables. */
internal val DesktopBreakpoint = 768.dp

@Composable
fun HomeScreen(
    uid: String,
    userName: String = "",
    userEmail: String = "",
    onLogout: () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= DesktopBreakpoint) {
            DesktopHomeScreen(uid = uid, userName = userName, userEmail = userEmail, onLogout = onLogout)
        } else {
            MobileHomeScreen(uid = uid, userName = userName, userEmail = userEmail, onLogout = onLogout)
        }
    }
}

@Composable
private fun MobileHomeScreen(
    uid: String,
    userName: String = "",
    userEmail: String = "",
    onLogout: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var activeNav by remember { mutableStateOf(0) }

    val alertsViewModel: AlertsViewModel = koinViewModel()
    LaunchedEffect(uid) { alertsViewModel.start(uid) }

    // Where a notification tap wanted to go, parked by MainActivity before this composed. Consumed
    // once, so returning to Home later does not re-navigate to a week-old notification.
    var pendingConversationId by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        when (val destination = PendingDestination.consume()) {
            is NotificationDestination.Likes -> activeNav = 1
            is NotificationDestination.Chat -> {
                activeNav = 2
                pendingConversationId = destination.conversationId
            }
            null -> Unit
        }
    }

    var showAnonymousChat by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    var bottomNavHeight by remember { mutableStateOf(0.dp) }
    var showPremium by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showSupportChat by remember { mutableStateOf(false) }
    var showAccountSettings by remember { mutableStateOf(false) }

    var detailTarget by remember { mutableStateOf<ProfileDetailTarget?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        if (showPremium) {
            PremiumScreen(uid = uid, onBack = { showPremium = false })
            return@Surface
        }

        if (showEditProfile) {
            ProfileSetupScreen(
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

        detailTarget?.let { target ->
            ProfileDetailScreen(
                profile = target.profile,
                distanceKm = target.distanceKm,
                onBack = { detailTarget = null }
            )
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

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomNavHeight)
                    .consumeWindowInsets(PaddingValues(bottom = bottomNavHeight))
            ) {
                AnimatedContent(
                    targetState = activeNav,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "home_tab_transition"
                ) { targetNav ->
                    when (targetNav) {
                        0 -> HomeFeedContent(
                            uid = uid,
                            userName = userName,
                            onUpgradeClick = { showPremium = true },
                            onOpenProfile = { profile, distanceKm ->
                                detailTarget = ProfileDetailTarget(profile, distanceKm)
                            }
                        )
                        1 -> LikesScreen(
                            uid = uid,
                            onOpenChat = { activeNav = 2 }
                        )
                        2 -> {
                            if (showAnonymousChat) {
                                AnonymousChatScreen(uid = uid, onBack = { showAnonymousChat = false })
                            } else {
                                ChatScreen(
                                    uid = uid,
                                    onOpenAnonymousChat = { showAnonymousChat = true },
                                    openConversationId = pendingConversationId,
                                    onOpenConversationHandled = { pendingConversationId = "" }
                                )
                            }
                        }
                        3 -> ProfileScreen(
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

            BottomNavBar(
                activeNav = activeNav,
                onNavSelect = { activeNav = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        bottomNavHeight = with(density) { size.height.toDp() }
                    }
            )
        }
    }
}

private data class ProfileDetailTarget(val profile: User, val distanceKm: Double?)

private val bottomNavLabels = listOf("Discover", "Likes", "Chats", "Profile")

@Composable
internal fun BottomNavBar(
    activeNav: Int,
    onNavSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        color = colors.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Hairline separator: the bar and the page background are close in tone in dark mode,
            // and without it the bar has no edge at all.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.outline.copy(alpha = 0.18f))
            )

            // System bars and cutout, bottom and sides only — never `safeDrawingPadding()`.
            //
            // Two separate reasons. First, `safeDrawingPadding()` pads all four edges, and window
            // insets are not relative to where a composable sits: a bar pinned to the bottom of the
            // screen still receives the full status-bar inset as *top* padding. That is where the
            // dead strip above these icons came from — on a device with a 24-48dp status bar the
            // bar was that much taller than its own content, and no dp tweak inside it could show
            // up next to it. Previews never caught it because the renderer reports no insets.
            //
            // Second, `safeDrawing`'s bottom includes the IME. Opening the keyboard in the chat tab
            // would have inflated the bar by the height of the keyboard. `systemBars` has no IME
            // component; the cutout is unioned in for the side insets in landscape.
            //
            // Content is then pinned to the top of what is left, so the remaining slack sits under
            // the labels rather than being split above and below them.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.systemBars
                            .union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    )
                    .height(62.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                bottomNavLabels.forEachIndexed { index, label ->
                    val isSelected = activeNav == index

                    // Everything about the selected state animates, so the move between tabs is a
                    // transition rather than a repaint — the tab content itself already cross-fades.
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) colors.primary else colors.onSurface.copy(alpha = 0.45f),
                        animationSpec = tween(220),
                        label = "nav_icon_color"
                    )
                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) colors.primary else colors.onSurface.copy(alpha = 0.5f),
                        animationSpec = tween(220),
                        label = "nav_label_color"
                    )
                    val pillAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 0.14f else 0f,
                        animationSpec = tween(220),
                        label = "nav_pill_alpha"
                    )
                    val pillScale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.72f,
                        animationSpec = tween(220),
                        label = "nav_pill_scale"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onNavSelect(index)
                            }
                            .padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .widthIn(min = 56.dp)
                                .width(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // The pill is drawn behind the icon and scales in place, so the icon
                            // never moves as the selection travels along the bar.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(pillScale)
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.primary.copy(alpha = pillAlpha))
                            )

                            when (index) {
                                0 -> HomeNavIcon(color = iconColor)
                                1 -> HeartNavIcon(color = iconColor)
                                2 -> EnvelopeIcon(color = iconColor, modifier = Modifier.size(23.dp))
                                3 -> ProfileNavIcon(color = iconColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = label,
                            style = typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = labelColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// Custom High-Precision Vector Icons matching screenshot pixel-for-pixel

@Composable
private fun HomeNavIcon(color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val housePath = Path().apply {
            moveTo(w * 0.5f, h * 0.18f) // Peak
            lineTo(w * 0.84f, h * 0.44f) // Right roof end
            lineTo(w * 0.84f, h * 0.78f) // Right wall
            cubicTo(w * 0.84f, h * 0.86f, w * 0.78f, h * 0.88f, w * 0.72f, h * 0.88f) // Bottom right corner
            lineTo(w * 0.28f, h * 0.88f) // Bottom wall
            cubicTo(w * 0.22f, h * 0.88f, w * 0.16f, h * 0.86f, w * 0.16f, h * 0.78f) // Bottom left corner
            lineTo(w * 0.16f, h * 0.44f) // Left wall
            close()
        }

        drawPath(
            path = housePath,
            color = color,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Door dot
        drawCircle(
            color = color,
            radius = 1.6.dp.toPx(),
            center = Offset(w * 0.64f, h * 0.70f)
        )
    }
}

@Composable
private fun HeartNavIcon(color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val heartPath = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            cubicTo(w * 0.15f, h * 0.56f, w * 0.08f, h * 0.32f, w * 0.26f, h * 0.18f)
            cubicTo(w * 0.40f, h * 0.08f, w * 0.50f, h * 0.25f, w * 0.50f, h * 0.28f)
            cubicTo(w * 0.50f, h * 0.25f, w * 0.60f, h * 0.08f, w * 0.74f, h * 0.18f)
            cubicTo(w * 0.92f, h * 0.32f, w * 0.85f, h * 0.56f, w * 0.50f, h * 0.82f)
            close()
        }

        drawPath(
            path = heartPath,
            color = color,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun ProfileNavIcon(color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Head
        drawCircle(
            color = color,
            radius = w * 0.20f,
            center = Offset(w * 0.5f, h * 0.32f),
            style = Stroke(width = 2.2.dp.toPx())
        )

        // Shoulder Arc
        val shoulderPath = Path().apply {
            moveTo(w * 0.20f, h * 0.84f)
            cubicTo(w * 0.20f, h * 0.62f, w * 0.35f, h * 0.56f, w * 0.50f, h * 0.56f)
            cubicTo(w * 0.65f, h * 0.56f, w * 0.80f, h * 0.62f, w * 0.80f, h * 0.84f)
        }

        drawPath(
            path = shoulderPath,
            color = color,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun HomeFeedContent(
    uid: String,
    userName: String,
    /** Opens the MindMingle+ screen — the filter sheet's locked groups route here. */
    onUpgradeClick: () -> Unit = {},
    /** Opens the tapped profile in full. Distance travels with it — only the deck can measure it. */
    onOpenProfile: (User, Double?) -> Unit = { _, _ -> }
) {
    val colors = MaterialTheme.colorScheme

    val viewModel: HomeViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    // Hoisted above the card: a swipe replaces the card composable, and state kept inside it would
    // be torn down halfway through the fly-off animation.
    val deckScope = rememberCoroutineScope()
    val deckState = remember(deckScope) { SwipeDeckState(deckScope) }

    LaunchedEffect(uid) {
        viewModel.onEvent(HomeEvent.LoadProfiles(uid))
    }

    if (showFilterSheet) {
        FilterSheet(
            filters = uiState.filters,
            // No coordinates for the signed-in user means no distance to measure against.
            canFilterByDistance = uiState.hasMyLocation,
            onLoadDistricts = { countryCode -> viewModel.districtsFor(countryCode) },
            isPremium = uiState.isPremium,
            onApply = { newFilters ->
                viewModel.onEvent(HomeEvent.ApplyFilters(newFilters))
                showFilterSheet = false
            },
            // Reset stays on the sheet so the cleared state is visible; the deck behind it
            // reloads immediately with unfiltered, randomly ordered profiles.
            onReset = { viewModel.onEvent(HomeEvent.ResetFilters) },
            onUpgrade = {
                showFilterSheet = false
                onUpgradeClick()
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
.safeDrawingPadding()
    ) {
        DiscoverHeader(
            userName = userName,
            remaining = (uiState.profiles.size - uiState.currentIndex).coerceAtLeast(0),
            isLoading = uiState.isLoading,
            isRecycledDeck = uiState.isRecycledDeck,
            activeFilterCount = uiState.filters.activeCount,
            onFilterClick = { showFilterSheet = true }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            val profile = uiState.currentProfile
            val adGate = uiState.adGate

            // One slot, four possible occupants. Keeping them inside a single Box means the deck
            // never changes height as it moves between loading, ad, card and empty.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    // The ad owns the card slot outright: no profile behind it to peek at, and
                    // swiping stays locked until the gate clears.
                    adGate != null -> {
                        SponsoredAdCard(
                            gate = adGate,
                            onContinue = { viewModel.onEvent(HomeEvent.DismissAd) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = DeckStackReserve)
                                .shadow(18.dp, RoundedCornerShape(30.dp))
                        )
                    }

                    uiState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp)
                        }
                    }

                    profile == null -> {
                        DeckEmptyState(
                            hasActiveFilters = uiState.filters.activeCount > 0,
                            onResetFilters = { viewModel.onEvent(HomeEvent.ResetFilters) },
                            onOpenFilters = { showFilterSheet = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        // Cards still queued behind this one are drawn as two inset slivers, not as
                        // real profiles: the depth cue is all that is needed, and rendering the next
                        // profiles would leak them before their turn.
                        SwipeDeck(
                            profile = profile,
                            distanceKm = uiState.distanceToKm(profile),
                            behindDepth = uiState.profiles.size - uiState.currentIndex - 1,
                            enabled = !uiState.isSwipeLocked,
                            state = deckState,
                            onPass = { viewModel.onEvent(HomeEvent.Pass) },
                            onConnect = { viewModel.onEvent(HomeEvent.Connect(fromUid = uid, toUid = profile.uid)) },
                            onOpenDetails = { onOpenProfile(profile, uiState.distanceToKm(profile)) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/** Bottom strip left free inside the deck slot so the stacked cards behind stay visible. */
internal val DeckStackReserve = 16.dp

/** Corner radius shared by the front card and the slivers behind it, so the stack lines up. */
internal val DeckCardShape = RoundedCornerShape(30.dp)

/**
 * Greeting, deck count and the filter entry point.
 *
 * The count is the reason the header earns its space: without it the deck gives no sense of how
 * much is left, and the only other place to put it would be on the card itself.
 */
@Composable
private fun DiscoverHeader(
    userName: String,
    remaining: Int,
    isLoading: Boolean,
    isRecycledDeck: Boolean,
    activeFilterCount: Int,
    onFilterClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val firstName = userName.trim().substringBefore(" ").ifBlank { "there" }
    // A second pass is labelled. Silently repeating people looks like the deck is broken; saying
    // so makes it read as deliberate, and it is the only cue that these are not new arrivals.
    val subtitle = when {
        isLoading -> "Finding developers for you…"
        isRecycledDeck && remaining > 0 -> "You've seen everyone — showing them again"
        remaining <= 0 -> "You're all caught up"
        remaining == 1 -> "1 developer left in your deck"
        else -> "$remaining developers in your deck"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hi, $firstName",
                style = typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onBackground,
                fontSize = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box {
            Surface(
                onClick = onFilterClick,
                shape = CircleShape,
                color = colors.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    FilterIcon(color = colors.onPrimaryContainer, modifier = Modifier.size(19.dp))
                }
            }

            // The count, not just a dot: "filters are on" is far less useful than "three groups are on".
            if (activeFilterCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                        .border(2.dp, colors.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeFilterCount.toString(),
                        color = colors.onPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * The two inset slivers under the front card that make the deck read as a stack.
 *
 * [progress] is the magnitude of the current drag, 0..1. The next card rises toward its full size
 * as the front card is pulled away, which is what stands in for an entrance animation once the
 * swipe commits — by then the card behind is already where the new front card will be.
 */
@Composable
internal fun DeckPeekLayer(depth: Int, progress: Float = 0f) {
    val colors = MaterialTheme.colorScheme
    val rise = progress.coerceIn(0f, 1f)

    if (depth >= 2) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 26.dp)
                .clip(DeckCardShape)
                .background(colors.surfaceContainerHigh.copy(alpha = 0.55f + 0.2f * rise))
        )
    }
    if (depth >= 1) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 13.dp, end = 13.dp, bottom = 8.dp)
                .graphicsLayer {
                    scaleX = 1f + 0.035f * rise
                    scaleY = 1f + 0.035f * rise
                }
                .clip(DeckCardShape)
                .background(colors.surfaceContainerHighest.copy(alpha = 0.85f + 0.15f * rise))
        )
    }
}

/**
 * The profile card.
 *
 * The hero keeps the per-uid gradient the app already uses for avatars everywhere else, so a
 * person looks the same here as in chat and likes. Everything under it scrolls: bios and interest
 * lists are user-supplied and a card that clips them silently is worse than one that scrolls.
 */
@Composable
internal fun ProfileDeckCard(
    profile: User,
    distanceKm: Double?,
    modifier: Modifier = Modifier,
    onOpenDetails: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Box(
        modifier = modifier
            .shadow(18.dp, DeckCardShape)
            .clip(DeckCardShape)
            .background(colors.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The hero takes whatever the details do not.
            //
            // A fixed 196dp hero left a card with a short bio nearly half empty on a real phone —
            // the detail column wraps its content but the card fills the deck slot. Weighting the
            // hero instead means a sparse profile gets a tall gradient and a rich one gets a taller
            // detail block, and neither ends in dead space.
            ProfilePhotoHero(
                profile = profile,
                onOpenDetails = onOpenDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 170.dp)
            )

            // Capped so a long bio cannot squeeze the hero out; it scrolls past that point.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = profile.name.ifBlank { "Developer" },
                        style = typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.isPremium) {
                        Spacer(modifier = Modifier.width(5.dp))
                        PremiumBadge(modifier = Modifier.padding(bottom = 3.dp))
                    }
                    if (profile.age > 0) {
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = profile.age.toString(),
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Intent first — it is the one field that decides whether the two are a fit at all.
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        SparkleBurstIcon(color = colors.primary, modifier = Modifier.size(13.dp))
                        Text(
                            text = profile.lookingFor.ifBlank { "Open to collaborating" },
                            style = typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Distance is its own line with a pin, not another clause in the meta string.
                // It is the one fact on the card that decides whether someone bothers, so it gets
                // the treatment every app in the category gives it. Falls back to the stated town
                // when there is no distance to show — one or the other, never both.
                DistanceLine(
                    distanceKm = distanceKm,
                    fallbackLocation = profile.location,
                    style = typography.bodySmall,
                    iconSize = 12.dp
                )

                if (profile.occupation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = profile.occupation,
                        style = typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile.bio.ifBlank { "Excited to connect with fellow developers." },
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    lineHeight = 19.sp
                )

                // Interests were collected at profile setup but never shown on the deck; they are
                // the most concrete thing on the card to open a conversation with.
                if (profile.interests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profile.interests.take(6).forEach { interest ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surfaceVariant,
                                border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = interest,
                                    style = typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The card's photo area.
 *
 * Photos are stepped by tapping the left or right third and never by dragging: the card sits
 * inside [SwipeDeck]'s horizontal `draggable`, and a pager competing for the same gesture would
 * turn every swipe into a coin toss between "next photo" and "pass". A tap in the middle opens
 * the full profile, which is where the rest of someone's answers live.
 *
 * A profile with no photos falls through to the plain gradient the deck used before photos
 * existed, so an incomplete profile still looks deliberate.
 */
@Composable
private fun ProfilePhotoHero(
    profile: User,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photos = profile.displayPhotoUrls
    // Reset to the first photo whenever the card shows a different person: the deck reuses this
    // composable as profiles move through it, so state keyed on nothing would leak an index.
    var photoIndex by remember(profile.uid) { mutableStateOf(0) }
    val currentPhoto = photos.getOrNull(photoIndex).orEmpty()

    Box(modifier = modifier) {
        RemoteProfileImage(
            url = currentPhoto,
            uid = profile.uid,
            contentDescription = profile.name.ifBlank { "Profile photo" },
            placeholderIconSize = 68.dp,
            modifier = Modifier.fillMaxSize()
        )

        // Scrim at the foot so the hero settles into the card surface instead of ending on a hard
        // edge, and a lighter one at the top so the photo dots stay legible over a pale photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = if (photos.size > 1) 0.22f else 0f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(profile.uid, photos.size) {
                    detectTapGestures { offset ->
                        val third = size.width / 3f
                        when {
                            photos.size > 1 && offset.x < third ->
                                photoIndex = (photoIndex - 1 + photos.size) % photos.size
                            photos.size > 1 && offset.x > size.width - third ->
                                photoIndex = (photoIndex + 1) % photos.size
                            else -> onOpenDetails()
                        }
                    }
                }
        )

        if (photos.size > 1) {
            PhotoProgressBar(
                count = photos.size,
                activeIndex = photoIndex,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}

/** One segment per photo, the current one lit — the same cue every app in the category uses. */
@Composable
internal fun PhotoProgressBar(
    count: Int,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Color.White.copy(alpha = if (index == activeIndex) 0.95f else 0.35f)
                    )
            )
        }
    }
}

/**
 * MindMingle+ badge. Reads `users/{uid}.isPremium`, which each client mirrors from its own
 * subscription doc — see HomeViewModel.mirrorPremiumFlag for why it cannot come from the
 * subscription itself.
 */
@Composable
internal fun PremiumBadge(modifier: Modifier = Modifier, size: Dp = 17.dp) {
    CheckBadgeIcon(color = PremiumBadgeBlue, modifier = modifier.size(size))
}

/**
 * "📍 2.4 km away" — one line, one pin, on the card and on the full profile alike.
 *
 * The distance is measured server-side and arrives already rounded; [GeoDistance.formatDistance]
 * only decides how to say it. When there is none — either side missing a location — the person's
 * stated town takes the line rather than leaving a gap, because a card with nothing there reads as
 * a loading state.
 */
@Composable
internal fun DistanceLine(
    distanceKm: Double?,
    fallbackLocation: String,
    style: TextStyle,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    val text = when {
        distanceKm != null -> GeoDistance.formatDistance(distanceKm)
        fallbackLocation.isNotBlank() -> fallbackLocation
        else -> return
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
    ) {
        LocationPinIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(iconSize))
        Text(
            text = text,
            style = style,
            fontWeight = FontWeight.Medium,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Fixed rather than themed: the tick is only read as "verified" in this exact blue. */
internal val PremiumBadgeBlue = Color(0xFF1D9BF0)

/**
 * Shown when the deck runs dry. Which of the two exits is offered depends on why it is empty:
 * with filters on, the deck is probably empty because of them, so clearing them is the fix.
 */
@Composable
private fun DeckEmptyState(
    hasActiveFilters: Boolean,
    onResetFilters: () -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                TelescopeIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(38.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (hasActiveFilters) "Nobody matches those filters" else "No more developers nearby",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (hasActiveFilters) {
                    "Widen your filters to bring more profiles back into the deck."
                } else {
                    "New profiles show up as developers join. Check back a little later."
                },
                style = typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                onClick = if (hasActiveFilters) onResetFilters else onOpenFilters,
                shape = RoundedCornerShape(50),
                color = if (hasActiveFilters) colors.primary else colors.surfaceContainerHigh
            ) {
                Text(
                    text = if (hasActiveFilters) "Reset filters" else "Adjust filters",
                    style = typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (hasActiveFilters) colors.onPrimary else colors.onSurface,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.11f

        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.2f)
            lineTo(w * 0.88f, h * 0.2f)
            lineTo(w * 0.58f, h * 0.55f)
            lineTo(w * 0.58f, h * 0.85f)
            lineTo(w * 0.42f, h * 0.72f)
            lineTo(w * 0.42f, h * 0.55f)
            close()
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * There is no "Apply" button: the choices take effect when the sheet closes, and Reset takes
 * effect the moment it is tapped. One query per visit to this sheet, not one per button press.
 */
@Composable
internal fun FilterSheet(
    filters: DiscoverFilters,
    canFilterByDistance: Boolean,
    /** Fetches the district names for one country; empty when that country has none seeded. */
    onLoadDistricts: suspend (countryCode: String) -> List<String>,
    isPremium: Boolean,
    onApply: (DiscoverFilters) -> Unit,
    onReset: () -> Unit,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var ageRange by remember { mutableStateOf(filters.minAge.toFloat()..filters.maxAge.toFloat()) }
    // The slider parks at its maximum for "any distance", which is what null means in DiscoverFilters.
    var distanceKm by remember {
        mutableStateOf((filters.maxDistanceKm ?: DiscoverFilterDefaults.MAX_DISTANCE_KM).toFloat())
    }
    var selectedInterests by remember { mutableStateOf(filters.interests) }
    var selectedLookingFor by remember { mutableStateOf(filters.lookingFor) }
    var selectedOccupations by remember { mutableStateOf(filters.occupations) }
    var selectedGenders by remember { mutableStateOf(filters.genders) }
    var selectedExperience by remember { mutableStateOf(filters.experienceLevels) }
    var selectedLanguages by remember { mutableStateOf(filters.languages) }
    var selectedCountries by remember { mutableStateOf(filters.countries) }
    var selectedDistricts by remember { mutableStateOf(filters.districts) }
    var detailSelections by remember { mutableStateOf(filters.detailFilters) }
    var occupationQuery by remember { mutableStateOf("") }
    var languageQuery by remember { mutableStateOf("") }
    var countryQuery by remember { mutableStateOf("") }
    var districtQuery by remember { mutableStateOf("") }
    var allOccupations by remember { mutableStateOf(OccupationRepository.defaultShortlist) }
    var interestOptions by remember { mutableStateOf(ProfileSetupOptions.interests) }
    var genderOptions by remember { mutableStateOf(emptyList<String>()) }
    var languageOptions by remember { mutableStateOf(emptyList<String>()) }
    // The same 242-country list the phone number field uses — no second copy, no hardcoding.
    var countryOptions by remember { mutableStateOf(emptyList<CountryCode>()) }
    // Districts arrive per country, and only once one is picked: the list is a Firestore document
    // for the selected country, not a dataset the app carries.
    var districtOptions by remember { mutableStateOf(emptyList<String>()) }
    var detailFields by remember { mutableStateOf(emptyList<ProfileFieldOption>()) }
    // Eight more chip rows would bury the Apply button; they start folded unless already in use.
    var isLifestyleExpanded by remember {
        mutableStateOf(filters.detailFilters.any { (_, values) -> values.isNotEmpty() })
    }

    LaunchedEffect(Unit) {
        allOccupations = OccupationRepository.getOccupations()
        ProfileOptionsRepository.optionsFor(ProfileOptionsRepository.INTERESTS_KEY)
            .takeIf { it.isNotEmpty() }
            ?.let { interestOptions = it }
        // Same source profile setup writes from, so a filter chip can never drift from a stored answer.
        genderOptions = ProfileOptionsRepository.optionsFor("gender")
        languageOptions = ProfileOptionsRepository.optionsFor("languages")
        countryOptions = CountryCodeRepository.getCountryCodes()
        detailFields = ProfileOptionsRepository.fieldsFor(DiscoverFilterOptions.detailFilterKeys)
    }

    val occupationResults = remember(occupationQuery, allOccupations) {
        if (occupationQuery.isBlank()) {
            OccupationRepository.defaultShortlist
        } else {
            allOccupations.filter { it.contains(occupationQuery, ignoreCase = true) }.take(30)
        }
    }

    // 180+ languages can't be chips — search, like occupation.
    // One country selected is what makes a district list meaningful. With none or several, there is
    // nothing sensible to offer, and any districts already chosen are dropped rather than left
    // filtering invisibly.
    val districtCountry = selectedCountries.singleOrNull().orEmpty()
    LaunchedEffect(districtCountry) {
        districtOptions = if (districtCountry.isBlank()) {
            emptyList()
        } else {
            onLoadDistricts(districtCountry)
        }
        if (districtOptions.isEmpty()) selectedDistricts = emptySet()
    }

    val districtResults = remember(districtQuery, districtOptions) {
        if (districtQuery.isBlank()) {
            districtOptions.take(10)
        } else {
            districtOptions.filter { it.contains(districtQuery, ignoreCase = true) }.take(30)
        }
    }

    val countryResults = remember(countryQuery, countryOptions) {
        if (countryQuery.isBlank()) {
            countryOptions.take(10)
        } else {
            countryOptions.filter { it.name.contains(countryQuery, ignoreCase = true) }.take(30)
        }
    }

    val languageResults = remember(languageQuery, languageOptions) {
        if (languageQuery.isBlank()) {
            languageOptions.take(10)
        } else {
            languageOptions.filter { it.contains(languageQuery, ignoreCase = true) }.take(30)
        }
    }

    // Closing the sheet is what applies the selection — see the KDoc above.
    val currentSelection = {
        DiscoverFilters(
            minAge = ageRange.start.toInt(),
            maxAge = ageRange.endInclusive.toInt(),
            interests = selectedInterests,
            lookingFor = selectedLookingFor,
            occupations = selectedOccupations,
            // Slider at its maximum (or no coordinates to measure from) means "any distance".
            maxDistanceKm = distanceKm.toInt()
                .takeIf { canFilterByDistance && it < DiscoverFilterDefaults.MAX_DISTANCE_KM },
            genders = selectedGenders,
            experienceLevels = selectedExperience,
            languages = selectedLanguages,
            countries = selectedCountries,
            districts = selectedDistricts,
            detailFilters = detailSelections
        )
    }

    Dialog(onDismissRequest = { onApply(currentSelection()) }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Filters", style = typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
                    Text(
                        text = "Reset",
                        style = typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        // Clears every filter and reloads the deck straight away: after this the
                        // screen shows whoever is out there, any gender, in random order.
                        modifier = Modifier.clickable {
                            ageRange = DiscoverFilterDefaults.MIN_AGE.toFloat()..DiscoverFilterDefaults.MAX_AGE.toFloat()
                            distanceKm = DiscoverFilterDefaults.MAX_DISTANCE_KM.toFloat()
                            selectedInterests = emptySet()
                            selectedLookingFor = emptySet()
                            selectedOccupations = emptySet()
                            selectedGenders = emptySet()
                            selectedExperience = emptySet()
                            selectedLanguages = emptySet()
                            selectedCountries = emptySet()
                            selectedDistricts = emptySet()
                            detailSelections = emptyMap()
                            occupationQuery = ""
                            languageQuery = ""
                            countryQuery = ""
                            districtQuery = ""
                            onReset()
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    Text(text = "Age Range", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${ageRange.start.toInt()} – ${ageRange.endInclusive.toInt()}",
                        style = typography.bodyMedium,
                        color = colors.onSurface
                    )
                    RangeSlider(
                        value = ageRange,
                        onValueChange = { ageRange = it },
                        valueRange = DiscoverFilterDefaults.MIN_AGE.toFloat()..DiscoverFilterDefaults.MAX_AGE.toFloat(),
                        steps = 51,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )

                    // Hidden rather than disabled when we hold no coordinates for the signed-in user —
                    // a slider that can never affect the deck is worse than no slider. Free users
                    // get the upgrade card at the foot of the sheet instead.
                    if (canFilterByDistance && isPremium) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Distance", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (distanceKm.toInt() >= DiscoverFilterDefaults.MAX_DISTANCE_KM) {
                                "Any distance"
                            } else {
                                "Within ${distanceKm.toInt()} km"
                            },
                            style = typography.bodyMedium,
                            color = colors.onSurface
                        )
                        Slider(
                            value = distanceKm,
                            onValueChange = { distanceKm = it },
                            valueRange = 5f..DiscoverFilterDefaults.MAX_DISTANCE_KM.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                        )
                    }

                    if (genderOptions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Show Me", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            genderOptions.forEach { gender ->
                                SelectableFilterChip(
                                    text = gender,
                                    isSelected = selectedGenders.contains(gender),
                                    onClick = {
                                        selectedGenders = if (selectedGenders.contains(gender)) selectedGenders - gender else selectedGenders + gender
                                    }
                                )
                            }
                        }
                    }

                    // Everything from here down is MindMingle+ — the free sheet stays deliberately
                    // short (age, who to show, what they're after). filterDiscoverProfiles drops
                    // these server-side too, so hiding them is presentation, not enforcement.
                    if (isPremium) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Experience Level", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileSetupOptions.experienceLevels.forEach { level ->
                                SelectableFilterChip(
                                    text = level,
                                    isSelected = selectedExperience.contains(level),
                                    onClick = {
                                        selectedExperience = if (selectedExperience.contains(level)) selectedExperience - level else selectedExperience + level
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Hobbies & Interests", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            interestOptions.forEach { interest ->
                                SelectableFilterChip(
                                    text = interest,
                                    isSelected = selectedInterests.contains(interest),
                                    onClick = {
                                        selectedInterests = if (selectedInterests.contains(interest)) selectedInterests - interest else selectedInterests + interest
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "Looking For", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileSetupOptions.lookingForOptions.forEach { option ->
                            SelectableFilterChip(
                                text = option,
                                isSelected = selectedLookingFor.contains(option),
                                onClick = {
                                    selectedLookingFor = if (selectedLookingFor.contains(option)) selectedLookingFor - option else selectedLookingFor + option
                                }
                            )
                        }
                    }

                    if (isPremium) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "Occupation", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = occupationQuery,
                            onValueChange = { occupationQuery = it },
                            singleLine = true,
                            textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                            cursorBrush = SolidColor(colors.primary),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (occupationQuery.isEmpty()) {
                                    Text(
                                        text = "Search occupation",
                                        style = typography.bodyMedium,
                                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                inner()
                            }
                        )
                    }

                    if (selectedOccupations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedOccupations.sorted().forEach { occupation ->
                                SelectableFilterChip(
                                    text = occupation,
                                    isSelected = true,
                                    onClick = { selectedOccupations = selectedOccupations - occupation }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        occupationResults.filter { it !in selectedOccupations }.take(10).forEach { occupation ->
                            Surface(
                                onClick = { selectedOccupations = selectedOccupations + occupation },
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = occupation,
                                    style = typography.bodyMedium,
                                    color = colors.onSurface,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)
                                )
                            }
                        }
                        if (occupationQuery.isNotBlank() && occupationResults.isEmpty()) {
                            Text(
                                text = "No occupation matches \"$occupationQuery\"",
                                style = typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    }

                    if (languageOptions.isNotEmpty() && isPremium) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Languages", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = languageQuery,
                                onValueChange = { languageQuery = it },
                                singleLine = true,
                                textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                                cursorBrush = SolidColor(colors.primary),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (languageQuery.isEmpty()) {
                                        Text(
                                            text = "Search language",
                                            style = typography.bodyMedium,
                                            color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }

                        if (selectedLanguages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedLanguages.sorted().forEach { language ->
                                    SelectableFilterChip(
                                        text = language,
                                        isSelected = true,
                                        onClick = { selectedLanguages = selectedLanguages - language }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            languageResults.filter { it !in selectedLanguages }.take(10).forEach { language ->
                                Surface(
                                    onClick = { selectedLanguages = selectedLanguages + language },
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = language,
                                        style = typography.bodyMedium,
                                        color = colors.onSurface,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)
                                    )
                                }
                            }
                            if (languageQuery.isNotBlank() && languageResults.isEmpty()) {
                                Text(
                                    text = "No language matches \"$languageQuery\"",
                                    style = typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    if (countryOptions.isNotEmpty() && isPremium) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Country", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = countryQuery,
                                onValueChange = { countryQuery = it },
                                singleLine = true,
                                textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                                cursorBrush = SolidColor(colors.primary),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { inner ->
                                    if (countryQuery.isEmpty()) {
                                        Text(
                                            text = "Search country",
                                            style = typography.bodyMedium,
                                            color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }

                        // Selected countries are chips carrying the flag, so a set of four reads at
                        // a glance without spelling every name out.
                        if (selectedCountries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                countryOptions.filter { it.code in selectedCountries }.forEach { country ->
                                    SelectableFilterChip(
                                        text = "${country.flagEmoji}  ${country.name}",
                                        isSelected = true,
                                        onClick = { selectedCountries = selectedCountries - country.code }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            countryResults.filter { it.code !in selectedCountries }.take(10).forEach { country ->
                                Surface(
                                    onClick = { selectedCountries = selectedCountries + country.code },
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${country.flagEmoji}  ${country.name}",
                                        style = typography.bodyMedium,
                                        color = colors.onSurface,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)
                                    )
                                }
                            }
                            if (countryQuery.isNotBlank() && countryResults.isEmpty()) {
                                Text(
                                    text = "No country matches \"$countryQuery\"",
                                    style = typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        // Districts hang off the chosen country and appear only once there is
                        // exactly one, which is also the only case where the names are unambiguous.
                        if (districtOptions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(text = "District", style = typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = districtQuery,
                                    onValueChange = { districtQuery = it },
                                    singleLine = true,
                                    textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                                    cursorBrush = SolidColor(colors.primary),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (districtQuery.isEmpty()) {
                                            Text(
                                                text = "Search district",
                                                style = typography.bodyMedium,
                                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        inner()
                                    }
                                )
                            }

                            if (selectedDistricts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    selectedDistricts.sorted().forEach { district ->
                                        SelectableFilterChip(
                                            text = district,
                                            isSelected = true,
                                            onClick = { selectedDistricts = selectedDistricts - district }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                districtResults.filter { it !in selectedDistricts }.take(10).forEach { district ->
                                    Surface(
                                        onClick = { selectedDistricts = selectedDistricts + district },
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.surfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = district,
                                            style = typography.bodyMedium,
                                            color = colors.onSurface,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)
                                        )
                                    }
                                }
                                if (districtQuery.isNotBlank() && districtResults.isEmpty()) {
                                    Text(
                                        text = "No district matches \"$districtQuery\"",
                                        style = typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (detailFields.isNotEmpty() && isPremium) {
                        Spacer(modifier = Modifier.height(14.dp))

                        val activeDetailCount = detailSelections.count { (_, values) -> values.isNotEmpty() }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isLifestyleExpanded = !isLifestyleExpanded }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (activeDetailCount > 0) "Lifestyle & Values ($activeDetailCount)" else "Lifestyle & Values",
                                style = typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurfaceVariant
                            )
                            ChevronDownIcon(
                                color = colors.onSurfaceVariant,
                                modifier = Modifier
                                    .size(12.dp)
                                    .rotate(if (isLifestyleExpanded) 180f else 0f)
                            )
                        }

                        if (isLifestyleExpanded) {
                            detailFields.forEach { field ->
                                val selected = detailSelections[field.key].orEmpty()
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = field.label,
                                    style = typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    field.options.forEach { option ->
                                        SelectableFilterChip(
                                            text = option,
                                            isSelected = selected.contains(option),
                                            onClick = {
                                                val updated = if (selected.contains(option)) selected - option else selected + option
                                                detailSelections = detailSelections + (field.key to updated)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!isPremium) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            onClick = onUpgrade,
                            shape = RoundedCornerShape(16.dp),
                            color = colors.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Text(
                                    text = "MindMingle+ filters",
                                    style = typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Filter by occupation, experience, interests, languages, distance and lifestyle — drinking, education, relationship intent and more.",
                                    style = typography.bodySmall,
                                    color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Upgrade →",
                                    style = typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Not an Apply button: the selection is already live once this closes the sheet.
                // It is here so closing is obvious, not because anything needs confirming.
                Surface(
                    onClick = { onApply(currentSelection()) },
                    shape = RoundedCornerShape(50),
                    color = colors.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(text = "Done", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableFilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (isSelected) colors.primary else colors.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Text(
            text = text,
            style = typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) colors.onPrimary else colors.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}



