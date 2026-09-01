package com.rajamohan.mindmingle.presentation.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.rajamohan.mindmingle.presentation.account.component.AccountSettingsScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminDashboardScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminDeletionRequestsScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminPlanPricingScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminSubscriberListScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminSupportChatScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminSupportListScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminUserDetailScreen
import com.rajamohan.mindmingle.presentation.admin.component.AdminUserListScreen
import com.rajamohan.mindmingle.presentation.anonymous.component.desktop.DesktopAnonymousChatScreen
import com.rajamohan.mindmingle.presentation.anonymous.component.mobile.AnonymousChatScreen
import com.rajamohan.mindmingle.presentation.chat.component.desktop.DesktopChatScreen
import com.rajamohan.mindmingle.presentation.chat.component.mobile.ChatScreen
import com.rajamohan.mindmingle.presentation.home.component.desktop.DesktopHomeScreen
import com.rajamohan.mindmingle.presentation.home.component.mobile.BottomNavBar
import com.rajamohan.mindmingle.presentation.home.component.mobile.HomeScreen
import com.rajamohan.mindmingle.presentation.home.component.mobile.SwipeDeck
import com.rajamohan.mindmingle.presentation.home.component.mobile.SwipeDeckState
import com.rajamohan.mindmingle.presentation.likes.component.desktop.DesktopLikesScreen
import com.rajamohan.mindmingle.presentation.likes.component.mobile.LikesScreen
import com.rajamohan.mindmingle.presentation.login.component.desktop.DesktopEmailOtpScreen
import com.rajamohan.mindmingle.presentation.login.component.desktop.DesktopEmailSignInScreen
import com.rajamohan.mindmingle.presentation.login.component.mobile.EmailOtpVerificationScreen
import com.rajamohan.mindmingle.presentation.login.component.mobile.EmailPasswordScreen
import com.rajamohan.mindmingle.presentation.premium.component.desktop.DesktopPremiumScreen
import com.rajamohan.mindmingle.presentation.premium.component.mobile.PremiumScreen
import com.rajamohan.mindmingle.presentation.profile.component.desktop.DesktopProfileScreen
import com.rajamohan.mindmingle.presentation.profile.component.mobile.ProfileScreen
import com.rajamohan.mindmingle.presentation.profilesetup.component.desktop.DesktopProfileSetupScreen
import com.rajamohan.mindmingle.presentation.profilesetup.component.mobile.ProfileSetupScreen
import com.rajamohan.mindmingle.presentation.splash.MindMingleSplashScreen
import com.rajamohan.mindmingle.presentation.support.component.SupportChatScreen
import com.rajamohan.mindmingle.presentation.welcome.component.desktop.DesktopWelcomeScreen
import com.rajamohan.mindmingle.presentation.welcome.component.mobile.OnboardingScreen

/**
 * One @Preview per screen in the app.
 *
 * They live in androidMain rather than commonMain on purpose: Android Studio renders
 * `androidx.compose.ui.tooling.preview.Preview` reliably, while commonMain previews depend on
 * which multiplatform plugin build the IDE happens to have. The composables themselves are the
 * real commonMain ones — only the entry point is Android-side.
 *
 * Every preview goes through [PreviewHost], which supplies the theme and a Koin graph whose
 * repository layer is faked. See PreviewHarness.kt for what the fakes return.
 *
 * Phone previews use `widthDp`/`heightDp` at handset size; desktop variants are given a wide
 * canvas because their layouts switch on available width.
 */

private const val PHONE_W = 411
private const val PHONE_H = 891
private const val DESKTOP_W = 1280
private const val DESKTOP_H = 800

// ---------------------------------------------------------------------------------------------
// Entry / onboarding
// ---------------------------------------------------------------------------------------------

@Preview(name = "Splash", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun SplashPreview() = PreviewHost { MindMingleSplashScreen() }

@Preview(name = "Onboarding", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun OnboardingPreview() = PreviewHost { OnboardingScreen() }

@Preview(name = "Welcome — desktop", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopWelcomePreview() = PreviewHost { DesktopWelcomeScreen() }

// ---------------------------------------------------------------------------------------------
// Sign-in
// ---------------------------------------------------------------------------------------------

@Preview(name = "Email + password", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun EmailPasswordPreview() = PreviewHost {
    EmailPasswordScreen(onSubmit = { _, _, _ -> }, onBack = {})
}

@Preview(name = "Email password — error", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun EmailPasswordErrorPreview() = PreviewHost {
    EmailPasswordScreen(
        errorMessage = "That password does not match this account.",
        onSubmit = { _, _, _ -> },
        onBack = {}
    )
}

@Preview(name = "Email OTP", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun EmailOtpPreview() = PreviewHost {
    EmailOtpVerificationScreen(
        email = "irene@example.com",
        onCodeComplete = {},
        onResend = {},
        onBack = {}
    )
}

/**
 * Desktop sign-in, both steps.
 *
 * These are the screens that make the mailed-code flow reachable at all — everything under them
 * (Cloud Functions, repository, AuthEvents, the mobile verification screen) already existed, but
 * nothing dispatched `RequestEmailOtp`, so desktop could only sign in with a password.
 */
@Preview(name = "Desktop sign-in — code", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopEmailSignInPreview() = PreviewHost {
    DesktopEmailSignInScreen(
        onRequestCode = {},
        onPasswordSubmit = { _, _, _ -> },
        onBack = {}
    )
}

@Preview(name = "Desktop sign-in — error", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopEmailSignInErrorPreview() = PreviewHost {
    DesktopEmailSignInScreen(
        errorMessage = "We couldn't send a code to that address.",
        onRequestCode = {},
        onPasswordSubmit = { _, _, _ -> },
        onBack = {}
    )
}

@Preview(name = "Desktop OTP", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopEmailOtpPreview() = PreviewHost {
    DesktopEmailOtpScreen(
        email = "irene@example.com",
        onCodeComplete = {},
        onResend = {},
        onBack = {}
    )
}

@Preview(name = "Desktop OTP — light", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopEmailOtpLightPreview() = PreviewHost(darkTheme = false) {
    DesktopEmailOtpScreen(
        email = "irene@example.com",
        errorMessage = "Incorrect code",
        onCodeComplete = {},
        onResend = {},
        onBack = {}
    )
}

// ---------------------------------------------------------------------------------------------
// Profile setup
// ---------------------------------------------------------------------------------------------

@Preview(name = "Profile setup", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun ProfileSetupPreview() = PreviewHost {
    ProfileSetupScreen(
        uid = "u0",
        email = "irene@example.com",
        onProfileSaved = {}
    )
}

@Preview(name = "Profile setup — desktop", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopProfileSetupPreview() = PreviewHost {
    DesktopProfileSetupScreen(
        uid = "u0",
        email = "irene@example.com",
        onProfileSaved = {}
    )
}

// ---------------------------------------------------------------------------------------------
// The four tabs
// ---------------------------------------------------------------------------------------------

@Preview(name = "Home — deck + bottom nav", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun HomePreview() = PreviewHost {
    HomeScreen(uid = "u0", userName = "Irene Fox", userEmail = "irene@example.com")
}

@Preview(name = "Home — light", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun HomeLightPreview() = PreviewHost(darkTheme = false) {
    HomeScreen(uid = "u0", userName = "Irene Fox", userEmail = "irene@example.com")
}

/**
 * The swipeable card on its own.
 *
 * Static previews cannot drag, so this is only good for the resting state and the stack behind it.
 * Run it as an interactive preview to exercise the real thing: the tilt and the CONNECT/PASS
 * stamps track the drag, a short drag springs back, and a long one or a flick throws the card off
 * and logs the direction. Nothing here reaches a ViewModel, so it never actually likes anyone.
 */
@Preview(name = "Deck — swipe card", widthDp = PHONE_W, heightDp = 640)
@Composable
private fun SwipeDeckPreview() = PreviewHost {
    val scope = rememberCoroutineScope()
    val state = remember(scope) { SwipeDeckState(scope) }
    var swipes by remember { mutableStateOf(0) }

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            SwipeDeck(
                profile = previewUsers.first(),
                distanceKm = 4.0,
                behindDepth = 2,
                enabled = true,
                state = state,
                // The deck never advances here, so the same card comes back after every swipe —
                // which is what makes it usable as a repeatable gesture test.
                onPass = { swipes++ },
                onConnect = { swipes++ },
                onOpenDetails = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Bottom navigation
// ---------------------------------------------------------------------------------------------

/**
 * The bar on its own, one copy per selected tab.
 *
 * The full-screen Home previews only ever show tab 0 selected, so the pill, the label weight and
 * the icon tint of the other three states never get looked at. Stacking all four states makes a
 * regression in any of them visible without tapping through the running app.
 */
@Composable
private fun BottomNavStates() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            bottomNavTabs.forEach { tab ->
                BottomNavBar(activeNav = tab, onNavSelect = {})
            }
        }
    }
}

private val bottomNavTabs = 0..3

/**
 * The one preview that renders with system bars, and the only one that can catch an inset bug.
 *
 * A `widthDp`/`heightDp` preview reports no window insets at all, so a bar that mistakenly pads
 * itself with the status-bar inset looks perfect here and wrong on every real phone — which is
 * exactly what happened to the nav bar. `showSystemUi` gives the renderer a real device profile
 * with real insets, so the nav bar's bottom inset and the feed's top inset are both exercised.
 */
@Preview(name = "Home — with system bars", showSystemUi = true, device = "id:pixel_7")
@Composable
private fun HomeSystemBarsPreview() = PreviewHost {
    HomeScreen(uid = "u0", userName = "Irene Fox", userEmail = "irene@example.com")
}

@Preview(name = "Bottom nav — all tabs", widthDp = PHONE_W, heightDp = 340)
@Composable
private fun BottomNavPreview() = PreviewHost { BottomNavStates() }

@Preview(name = "Bottom nav — all tabs, light", widthDp = PHONE_W, heightDp = 340)
@Composable
private fun BottomNavLightPreview() = PreviewHost(darkTheme = false) { BottomNavStates() }

/**
 * Interactive preview: run it and the selection actually moves, which is the only way to see the
 * 220ms pill and tint transition without building the app.
 */
@Preview(name = "Bottom nav — interactive", widthDp = PHONE_W, heightDp = 140)
@Composable
private fun BottomNavInteractivePreview() = PreviewHost {
    var selected by remember { mutableStateOf(0) }
    Surface(color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            BottomNavBar(activeNav = selected, onNavSelect = { selected = it })
        }
    }
}

@Preview(name = "Likes", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun LikesPreview() = PreviewHost { LikesScreen(uid = "u0") }

@Preview(name = "Chat list", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun ChatPreview() = PreviewHost { ChatScreen(uid = "u0") }

@Preview(name = "Profile", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun ProfilePreview() = PreviewHost {
    ProfileScreen(uid = "u0", userName = "Irene Fox", userEmail = "irene@example.com")
}

// ---------------------------------------------------------------------------------------------
// Secondary screens
// ---------------------------------------------------------------------------------------------

@Preview(name = "Anonymous chat", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun AnonymousChatPreview() = PreviewHost {
    AnonymousChatScreen(uid = "u0", onBack = {})
}

@Preview(name = "Premium", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun PremiumPreview() = PreviewHost { PremiumScreen(uid = "u0", onBack = {}) }

@Preview(name = "Support chat", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun SupportChatPreview() = PreviewHost {
    SupportChatScreen(uid = "u0", userName = "Irene Fox", onBack = {})
}

@Preview(name = "Account settings", widthDp = PHONE_W, heightDp = PHONE_H)
@Composable
private fun AccountSettingsPreview() = PreviewHost {
    AccountSettingsScreen(onBack = {}, onAccountClosed = {})
}

// ---------------------------------------------------------------------------------------------
// Desktop variants
// ---------------------------------------------------------------------------------------------

@Preview(name = "Home — desktop", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopHomePreview() = PreviewHost {
    DesktopHomeScreen(uid = "u0", userName = "Irene Fox", userEmail = "irene@example.com")
}

@Preview(name = "Likes — desktop", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopLikesPreview() = PreviewHost { DesktopLikesScreen(uid = "u0") }

@Preview(name = "Chat — desktop", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopChatPreview() = PreviewHost { DesktopChatScreen(uid = "u0") }

@Preview(name = "Profile — desktop", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopProfilePreview() = PreviewHost {
    DesktopProfileScreen(uid = "u0", userName = "Irene Fox", userEmail = "irene@example.com")
}

@Preview(name = "Anonymous — desktop", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopAnonymousPreview() = PreviewHost { DesktopAnonymousChatScreen(uid = "u0") }

@Preview(name = "Premium — desktop", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun DesktopPremiumPreview() = PreviewHost { DesktopPremiumScreen(uid = "u0", onBack = {}) }

// ---------------------------------------------------------------------------------------------
// Admin console (desktop-sized — that is where it is used)
// ---------------------------------------------------------------------------------------------

@Preview(name = "Admin dashboard", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun AdminDashboardPreview() = PreviewHost {
    AdminDashboardScreen(
        onManageUsers = {},
        onManagePricing = {},
        onManageSubscribers = {},
        onManageSupport = {},
        onManageDeletionRequests = {},
        onSignOut = {}
    )
}

@Preview(name = "Admin — users", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun AdminUserListPreview() = PreviewHost {
    AdminUserListScreen(onUserClick = {}, onBack = {})
}

@Preview(name = "Admin — user detail", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun AdminUserDetailPreview() = PreviewHost {
    AdminUserDetailScreen(uid = "u1", onBack = {}, onUserDeleted = {})
}

@Preview(name = "Admin — subscribers", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun AdminSubscriberListPreview() = PreviewHost {
    AdminSubscriberListScreen(onSubscriberClick = {}, onBack = {})
}

@Preview(name = "Admin — support list", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun AdminSupportListPreview() = PreviewHost {
    AdminSupportListScreen(onThreadClick = { _, _ -> }, onBack = {})
}

@Preview(name = "Admin — support chat", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun AdminSupportChatPreview() = PreviewHost {
    AdminSupportChatScreen(uid = "u1", userName = "Irene Fox", onBack = {})
}

@Preview(name = "Admin — deletion requests", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun AdminDeletionRequestsPreview() = PreviewHost { AdminDeletionRequestsScreen(onBack = {}) }

@Preview(name = "Admin — plan pricing", widthDp = DESKTOP_W, heightDp = DESKTOP_H)
@Composable
private fun AdminPlanPricingPreview() = PreviewHost { AdminPlanPricingScreen(onBack = {}) }
