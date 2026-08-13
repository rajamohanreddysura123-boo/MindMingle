package com.rajamohan.mindmingle.presentation.home.component.mobile

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rajamohan.mindmingle.domain.model.OccupationRepository
import com.rajamohan.mindmingle.domain.model.ProfileOptionsRepository
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.presentation.ads.SponsoredAdCard
import com.rajamohan.mindmingle.presentation.anonymous.component.mobile.AnonymousChatScreen
import com.rajamohan.mindmingle.presentation.chat.component.mobile.ChatScreen
import com.rajamohan.mindmingle.presentation.common.icon.ChatBubbleIcon
import com.rajamohan.mindmingle.presentation.common.icon.MaskIcon
import com.rajamohan.mindmingle.presentation.common.icon.CrossIcon
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
import com.rajamohan.mindmingle.presentation.common.icon.SparkleBurstIcon
import com.rajamohan.mindmingle.presentation.common.icon.TelescopeIcon
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
    userName: String = "Rajamohan Reddy",
    userEmail: String = "rajamohan.reddy@gmail.com",
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
    userName: String = "Rajamohan Reddy",
    userEmail: String = "rajamohan.reddy@gmail.com",
    onLogout: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    var activeNav by remember { mutableStateOf(0) }
    var showPremium by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showSupportChat by remember { mutableStateOf(false) }

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

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main Tab Content View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp) // Room for floating bottom nav pill
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
                            onUpgradeClick = { showPremium = true }
                        )
                        1 -> LikesScreen(uid = uid)
                        2 -> ChatScreen(uid = uid)
                        3 -> AnonymousChatScreen(uid = uid)
                        4 -> ProfileScreen(
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

            // Glassmorphic Floating Capsule Bottom Navigation Bar
            FloatingBottomNavBar(
                activeNav = activeNav,
                onNavSelect = { activeNav = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeContentPadding()
                    .padding(start = 28.dp, end = 28.dp, bottom = 20.dp)
            )
        }
    }
}

/**
 * Ultra-sleek Glassmorphic Floating Pill Navigation Bar
 * Matching user provided design exactly
 */
@Composable
private fun FloatingBottomNavBar(
    activeNav: Int,
    onNavSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    val containerBg = colors.surfaceContainerHigh.copy(alpha = 0.92f)
    val borderGlow = colors.outline.copy(alpha = 0.25f)
    val activeBgStart = colors.primary.copy(alpha = 0.30f)
    val activeBgEnd = colors.primary.copy(alpha = 0.12f)
    val activeBorderStart = colors.primary.copy(alpha = 0.85f)
    val activeBorderEnd = colors.primary.copy(alpha = 0.30f)

    Surface(
        shape = RoundedCornerShape(36.dp),
        color = containerBg,
        border = BorderStroke(1.2.dp, borderGlow),
        shadowElevation = 20.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icons = listOf(0, 1, 2, 3, 4)

            icons.forEach { index ->
                val isSelected = activeNav == index

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.0f,
                    animationSpec = tween(250)
                )

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(activeBgStart, activeBgEnd)
                                        )
                                    )
                                    .border(
                                        BorderStroke(
                                            1.5.dp,
                                            Brush.linearGradient(
                                                colors = listOf(activeBorderStart, activeBorderEnd)
                                            )
                                        ),
                                        CircleShape
                                    )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onNavSelect(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val iconColor = if (isSelected) colors.primary else colors.onSurface.copy(alpha = 0.55f)

                    when (index) {
                        0 -> HomeNavIcon(color = iconColor)
                        1 -> CompassNavIcon(color = iconColor)
                        2 -> HeartNavIcon(color = iconColor)
                        3 -> MaskIcon(color = iconColor, modifier = Modifier.size(24.dp))
                        4 -> ProfileNavIcon(color = iconColor)
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
private fun CompassNavIcon(color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        val radius = w * 0.42f

        // Outer Ring
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = 2.2.dp.toPx())
        )

        // Diamond Needle
        val needlePath = Path().apply {
            moveTo(w * 0.5f, h * 0.26f) // Top needle
            lineTo(w * 0.65f, h * 0.5f) // Right point
            lineTo(w * 0.5f, h * 0.74f) // Bottom needle
            lineTo(w * 0.35f, h * 0.5f) // Left point
            close()
        }

        drawPath(
            path = needlePath,
            color = color,
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
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
    onUpgradeClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: HomeViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        viewModel.onEvent(HomeEvent.LoadProfiles(uid))
    }

    uiState.matchedUser?.let { matchedUser ->
        MatchDialog(
            matchedUser = matchedUser,
            onDismiss = { viewModel.onEvent(HomeEvent.DismissMatch) }
        )
    }

    if (showFilterSheet) {
        FilterSheet(
            filters = uiState.filters,
            // No coordinates for the signed-in user means no distance to measure against.
            canFilterByDistance = uiState.hasMyLocation,
            isPremium = uiState.isPremium,
            onApply = { newFilters ->
                viewModel.onEvent(HomeEvent.ApplyFilters(newFilters))
                showFilterSheet = false
            },
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
            .safeContentPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = userName.trim().substringBefore(" ").ifBlank { "there" }.let { "Hi, $it" },
                style = typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onBackground,
                fontSize = 22.sp
            )

            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer)
                        .clickable { showFilterSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    FilterIcon(color = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
                }

                if (uiState.filters.activeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                            .border(2.dp, colors.background, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Featured Match Card Container
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            val profile = uiState.currentProfile
            val adGate = uiState.adGate

            when {
                // The ad owns the card slot outright: no profile behind it to peek at, and the
                // Pass/Connect row below is disabled until the gate clears.
                adGate != null -> {
                    SponsoredAdCard(
                        gate = adGate,
                        onContinue = { viewModel.onEvent(HomeEvent.DismissAd) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .shadow(12.dp, RoundedCornerShape(28.dp))
                    )
                }
                uiState.isLoading -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                profile == null -> {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TelescopeIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No more tech partners nearby",
                                style = typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Check back later for new profiles",
                                style = typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .shadow(12.dp, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(colors.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Avatar Header Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(Brush.linearGradient(avatarGradientFor(profile.uid))),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .border(3.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DeveloperAvatarIcon(color = Color.White, modifier = Modifier.size(46.dp))
                                }

                                // Experience Badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Black.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = profile.experienceLevel.ifBlank { "Dev" },
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            // Profile Details
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Text(
                                    text = profile.name,
                                    style = typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = "Looking for: ${profile.lookingFor.ifBlank { "Collaborator" }}",
                                    style = typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )

                                val distanceKm = uiState.distanceToKm(profile)
                                val locationLine = when {
                                    distanceKm != null -> "${distanceKm.toInt()} km away"
                                    profile.location.isNotBlank() -> profile.location
                                    else -> null
                                }
                                if (locationLine != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = locationLine,
                                        style = typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = profile.bio.ifBlank { "Excited to connect with fellow developers." },
                                    style = typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Occupation chip
                                if (profile.occupation.isNotBlank()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = colors.primaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = profile.occupation,
                                                style = typography.labelSmall,
                                                color = colors.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action Buttons (Pass vs Connect)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // Pass Button
                Surface(
                    onClick = { viewModel.onEvent(HomeEvent.Pass) },
                    enabled = profile != null && !uiState.isSwipeLocked,
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CrossIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pass",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // Connect Button
                Surface(
                    onClick = {
                        profile?.let {
                            viewModel.onEvent(HomeEvent.Connect(fromUid = uid, toUid = it.uid))
                        }
                    },
                    enabled = profile != null && !uiState.isSwipeLocked,
                    shape = RoundedCornerShape(20.dp),
                    color = colors.primary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(52.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ChatBubbleIcon(color = colors.onPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connect & Chat",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary
                        )
                    }
                }
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

@Composable
private fun FilterSheet(
    filters: DiscoverFilters,
    canFilterByDistance: Boolean,
    isPremium: Boolean,
    onApply: (DiscoverFilters) -> Unit,
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
    var detailSelections by remember { mutableStateOf(filters.detailFilters) }
    var occupationQuery by remember { mutableStateOf("") }
    var languageQuery by remember { mutableStateOf("") }
    var allOccupations by remember { mutableStateOf(OccupationRepository.defaultShortlist) }
    var interestOptions by remember { mutableStateOf(ProfileSetupOptions.interests) }
    var genderOptions by remember { mutableStateOf(emptyList<String>()) }
    var languageOptions by remember { mutableStateOf(emptyList<String>()) }
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
    val languageResults = remember(languageQuery, languageOptions) {
        if (languageQuery.isBlank()) {
            languageOptions.take(10)
        } else {
            languageOptions.filter { it.contains(languageQuery, ignoreCase = true) }.take(30)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
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
                        modifier = Modifier.clickable {
                            ageRange = DiscoverFilterDefaults.MIN_AGE.toFloat()..DiscoverFilterDefaults.MAX_AGE.toFloat()
                            distanceKm = DiscoverFilterDefaults.MAX_DISTANCE_KM.toFloat()
                            selectedInterests = emptySet()
                            selectedLookingFor = emptySet()
                            selectedOccupations = emptySet()
                            selectedGenders = emptySet()
                            selectedExperience = emptySet()
                            selectedLanguages = emptySet()
                            detailSelections = emptyMap()
                            occupationQuery = ""
                            languageQuery = ""
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
                                    text = "Filter by distance, languages and lifestyle — drinking, education, relationship intent and more.",
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

                Surface(
                    onClick = {
                        onApply(
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
                                detailFilters = detailSelections
                            )
                        )
                    },
                    shape = RoundedCornerShape(50),
                    color = colors.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(text = "Apply Filters", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onPrimary)
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

@Composable
private fun MatchDialog(
    matchedUser: User,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SparkleBurstIcon(color = colors.primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "It's a Match!",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You and ${matchedUser.name} both connected. Start the conversation!",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    color = colors.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Keep Swiping",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary
                        )
                    }
                }
            }
        }
    }
}
