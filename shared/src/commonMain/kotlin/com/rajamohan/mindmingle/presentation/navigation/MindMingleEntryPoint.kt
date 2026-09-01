package com.rajamohan.mindmingle.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.rajamohan.mindmingle.presentation.admin.AdminEntryPoint
import com.rajamohan.mindmingle.presentation.home.component.mobile.DesktopBreakpoint
import com.rajamohan.mindmingle.presentation.home.component.mobile.HomeScreen
import com.rajamohan.mindmingle.presentation.login.component.desktop.DesktopEmailOtpScreen
import com.rajamohan.mindmingle.presentation.login.component.desktop.DesktopEmailSignInScreen
import com.rajamohan.mindmingle.presentation.login.component.mobile.EmailPasswordScreen
import com.rajamohan.mindmingle.presentation.login.component.mobile.EmailOtpVerificationScreen
import com.rajamohan.mindmingle.presentation.login.component.mobile.ScreenState
import com.rajamohan.mindmingle.presentation.login.viewmodel.AuthEvent
import com.rajamohan.mindmingle.presentation.login.viewmodel.AuthViewModel
import com.rajamohan.mindmingle.presentation.profilesetup.component.mobile.ProfileSetupScreen
import com.rajamohan.mindmingle.presentation.splash.MindMingleSplashScreen
import com.rajamohan.mindmingle.presentation.welcome.component.mobile.OnboardingScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MindMingleEntryPoint() {
    // Coil's loader is built here rather than left to its own defaults: the network fetcher is
    // only auto-registered on JVM targets, so iOS would silently fail every profile photo. One
    // factory in the shared entry point covers Android, iOS and desktop identically.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Same breakpoint the rest of the app dispatches on, so a resized desktop window switches
        // auth layouts at exactly the point it switches Home.
        MindMingleEntryPointContent(isDesktopWidth = maxWidth >= DesktopBreakpoint)
    }
}

@Composable
private fun MindMingleEntryPointContent(isDesktopWidth: Boolean) {
    val authViewModel: AuthViewModel = koinViewModel()
    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Onboarding) }
    val authUiState by authViewModel.authUiState.collectAsState()

    // Observe AuthViewModel State for Screen Transitions
    LaunchedEffect(authUiState.isSuccess, authUiState.isEmailOtpSent) {
        if (authUiState.isSuccess) {
            val uid = authUiState.uid.orEmpty()
            currentScreen = if (authUiState.isAdmin) {
                ScreenState.Admin
            } else if (authUiState.profileComplete) {
                ScreenState.Home(
                    uid = uid,
                    userName = authUiState.prefillName.orEmpty(),
                    userEmail = authUiState.email.orEmpty()
                )
            } else {
                ScreenState.ProfileSetup(
                    uid = uid,
                    email = authUiState.email.orEmpty(),
                    prefillName = authUiState.prefillName.orEmpty()
                )
            }
        } else if (authUiState.isEmailOtpSent) {
            currentScreen = ScreenState.EmailOtpVerification(email = authUiState.emailOtpEmail)
        }
    }

    var splashAnimationDone by remember { mutableStateOf(false) }
    if (authUiState.isCheckingSession || !splashAnimationDone) {
        MindMingleSplashScreen(onAnimationComplete = { splashAnimationDone = true })
        return
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            (slideInHorizontally(animationSpec = tween(400)) { fullWidth -> fullWidth } + fadeIn(tween(400)))
                .togetherWith(slideOutHorizontally(animationSpec = tween(400)) { fullWidth -> -fullWidth } + fadeOut(tween(400)))
        },
        label = "oo_navigation"
    ) { targetScreen ->
        when (targetScreen) {
            is ScreenState.Onboarding -> {
                OnboardingScreen(
                    // Desktop's widescreen CTA.
                    onGetStartedClick = {
                        currentScreen = ScreenState.EmailInput
                    },
                    onGoogleSignInSuccess = { email, name ->
                        val formattedName = if (name.isNotBlank()) name else email.substringBefore("@").replace(".", " ").split(" ")
                            .joinToString(" ") { word -> word.replaceFirstChar { char -> char.uppercase() } }
                        authViewModel.onEvent(
                            AuthEvent.GoogleSignInVerified(
                                uid = authViewModel.currentUid() ?: email,
                                email = email,
                                name = formattedName
                            )
                        )
                    },
                    onEmailSignInClick = {
                        currentScreen = ScreenState.EmailInput
                    }
                )
            }
            is ScreenState.EmailInput -> {
                val submitPassword: (String, String, Boolean) -> Unit = { email, password, isNewAccount ->
                    authViewModel.onEvent(
                        if (isNewAccount) {
                            AuthEvent.EmailPasswordSignUp(email = email, password = password)
                        } else {
                            AuthEvent.EmailPasswordSignIn(email = email, password = password)
                        }
                    )
                }

                if (isDesktopWidth) {
                    // Desktop leads with the mailed code; password stays available inside the screen.
                    DesktopEmailSignInScreen(
                        isSubmitting = authUiState.isLoading,
                        errorMessage = authUiState.error,
                        onRequestCode = { email -> authViewModel.onEvent(AuthEvent.RequestEmailOtp(email)) },
                        onPasswordSubmit = submitPassword,
                        onBack = { currentScreen = ScreenState.Onboarding }
                    )
                } else {
                    EmailPasswordScreen(
                        isSubmitting = authUiState.isLoading,
                        errorMessage = authUiState.error,
                        onSubmit = submitPassword,
                        onBack = { currentScreen = ScreenState.Onboarding }
                    )
                }
            }
            is ScreenState.EmailOtpVerification -> {
                val onCode: (String) -> Unit = { code -> authViewModel.onEvent(AuthEvent.VerifyEmailOtp(code)) }
                val onResend: () -> Unit = { authViewModel.onEvent(AuthEvent.RequestEmailOtp(targetScreen.email)) }
                // Back has to clear the session state as well as the screen: leaving the code
                // behind would drop the user straight back onto this screen on the next state emit.
                val onBackToEmail: () -> Unit = {
                    authViewModel.onEvent(AuthEvent.ResetState)
                    currentScreen = ScreenState.EmailInput
                }

                if (isDesktopWidth) {
                    DesktopEmailOtpScreen(
                        email = targetScreen.email,
                        isVerifying = authUiState.isLoading,
                        isSuccess = authUiState.isSuccess,
                        errorMessage = authUiState.error,
                        onCodeComplete = onCode,
                        onResend = onResend,
                        onBack = onBackToEmail
                    )
                } else {
                    EmailOtpVerificationScreen(
                        email = targetScreen.email,
                        isVerifying = authUiState.isLoading,
                        isSuccess = authUiState.isSuccess,
                        errorMessage = authUiState.error,
                        onCodeComplete = onCode,
                        onResend = onResend,
                        onBack = onBackToEmail
                    )
                }
            }
            is ScreenState.ProfileSetup -> {
                ProfileSetupScreen(
                    uid = targetScreen.uid,
                    email = targetScreen.email,
                    prefillName = targetScreen.prefillName,
                    onProfileSaved = { savedName ->
                        currentScreen = ScreenState.Home(
                            uid = targetScreen.uid,
                            userName = savedName,
                            userEmail = targetScreen.email
                        )
                    }
                )
            }
            is ScreenState.Home -> {
                HomeScreen(
                    uid = targetScreen.uid,
                    userName = targetScreen.userName,
                    userEmail = targetScreen.userEmail,
                    onLogout = {
                        authViewModel.onEvent(AuthEvent.ResetState)
                        currentScreen = ScreenState.Onboarding
                    }
                )
            }
            is ScreenState.Admin -> {
                AdminEntryPoint(
                    onSignOut = {
                        authViewModel.onEvent(AuthEvent.ResetState)
                        currentScreen = ScreenState.Onboarding
                    }
                )
            }
        }
    }
}