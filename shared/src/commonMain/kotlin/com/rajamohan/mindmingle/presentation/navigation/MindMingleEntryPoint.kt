package com.rajamohan.mindmingle.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rajamohan.mindmingle.presentation.admin.AdminEntryPoint
import com.rajamohan.mindmingle.presentation.home.component.mobile.HomeScreen
import com.rajamohan.mindmingle.presentation.login.component.mobile.EmailInputScreen
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
                    phoneNumber = authUiState.phone,
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
            // PhoneInput/OtpVerification are retired — nothing routes to them any more, they
            // only stay in the when to keep it exhaustive over ScreenState.
            is ScreenState.Onboarding, is ScreenState.PhoneInput, is ScreenState.OtpVerification -> {
                OnboardingScreen(
                    // Desktop's CTA — mobile no longer shows this button (Google sign-in only).
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
                EmailInputScreen(
                    isSending = authUiState.isLoading,
                    errorMessage = authUiState.error,
                    onSendCode = { email ->
                        authViewModel.onEvent(AuthEvent.RequestEmailOtp(email))
                    },
                    onBack = {
                        currentScreen = ScreenState.Onboarding
                    }
                )
            }
            is ScreenState.EmailOtpVerification -> {
                EmailOtpVerificationScreen(
                    email = targetScreen.email,
                    isVerifying = authUiState.isLoading,
                    isSuccess = authUiState.isSuccess,
                    errorMessage = authUiState.error,
                    onCodeComplete = { code ->
                        authViewModel.onEvent(AuthEvent.VerifyEmailOtp(code))
                    },
                    onResend = {
                        authViewModel.onEvent(AuthEvent.RequestEmailOtp(targetScreen.email))
                    },
                    onBack = {
                        authViewModel.onEvent(AuthEvent.ResetState)
                        currentScreen = ScreenState.EmailInput
                    }
                )
            }
            is ScreenState.ProfileSetup -> {
                ProfileSetupScreen(
                    uid = targetScreen.uid,
                    email = targetScreen.email,
                    phoneNumber = targetScreen.phoneNumber,
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