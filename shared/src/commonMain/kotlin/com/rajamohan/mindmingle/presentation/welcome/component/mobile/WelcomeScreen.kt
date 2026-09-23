package com.rajamohan.mindmingle.presentation.welcome.component.mobile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rajamohan.mindmingle.presentation.theme.Spacing
import com.rajamohan.mindmingle.presentation.welcome.component.desktop.DesktopWelcomeScreen
import com.rajamohan.mindmingle.presentation.welcome.component.shared.GoogleLogoIcon
import mindmingle.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

import com.rajamohan.mindmingle.data.remote.source.GoogleAuthLauncher

@Composable
fun OnboardingScreen(
    /** Desktop only — the widescreen "Get Started" CTA. Mobile signs in with Google alone. */
    onGetStartedClick: () -> Unit = {},
    onGoogleSignInSuccess: (email: String, name: String) -> Unit = { _, _ -> },
    /** Email + password. Desktop's only way in (no working Google OAuth on JVM); on mobile it sits under Google. */
    onEmailSignInClick: () -> Unit = {}
) {
    var isSigningIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf("") }

    val onGoogleSignInClick = {
        signInError = ""
        isSigningIn = true
        GoogleAuthLauncher.launchGoogleSignIn(
            onSuccess = { email, name ->
                isSigningIn = false
                onGoogleSignInSuccess(email, name)
            },
            onError = { message ->
                isSigningIn = false
                signInError = message
                println("Google Sign-In Error: $message")
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 768.dp) {
            // Widescreen / Desktop Design — email OTP instead of Google, which doesn't work on JVM.
            DesktopWelcomeScreen(
                onGetStartedClick = onGetStartedClick,
                onEmailSignInClick = onEmailSignInClick
            )
        } else {
            // Mobile Design — Google, or email + password underneath it.
            MobileWelcomeScreen(
                onGoogleSignInClick = onGoogleSignInClick,
                onEmailSignInClick = onEmailSignInClick
            )
        }

        if (isSigningIn) {
            GoogleSignInLoadingDialog()
        }
    }
}

@Composable
private fun GoogleSignInLoadingDialog() {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            shadowElevation = 12.dp,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 36.dp, vertical = 32.dp)
            ) {
                CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Signing in with Google…",
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
            }
        }
    }
}

@Composable
private fun MobileWelcomeScreen(
    onGoogleSignInClick: () -> Unit = {},
    onEmailSignInClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ---------- Header Logo ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                com.rajamohan.mindmingle.presentation.theme.MindMingleHeaderLockup()
            }

            // ---------- Photo Collage (Fills middle area) ----------
            PhotoCollage(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            )

            // ---------- Bottom Content (Headline, Subtitle, Right-Draggable Button) ----------
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Headline
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.onBackground, fontWeight = FontWeight.Bold)) {
                            append("Find Your Next\n")
                        }
                        withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold)) {
                            append("Tech")
                        }
                        withStyle(SpanStyle(color = colors.onBackground, fontWeight = FontWeight.Bold)) {
                            append(" Partner")
                        }
                    },
                    style = typography.headlineMedium,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle
                Text(
                    text = "Connect on skills, not selfies.\nNo photos required to connect.",
                    style = typography.bodyMedium,
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Google first, email + password as the fallback — phone-number signup is retired.
                Surface(
                    onClick = onGoogleSignInClick,
                    shape = RoundedCornerShape(50),
                    color = colors.surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoogleLogoIcon(modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    onClick = onEmailSignInClick,
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Continue with Email",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PhotoCollage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top Right Avatar
        ProfileAvatar(
            size = 100.dp,
            imageRes = Res.drawable.avatar_woman_2,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = 10.dp)
        )
        // Top Left Avatar
        ProfileAvatar(
            size = 110.dp,
            imageRes = Res.drawable.avatar_woman_1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 10.dp, y = 20.dp),
            borderColor = MaterialTheme.colorScheme.primary
        )
        // Center Small Top Avatar
        ProfileAvatar(
            size = 65.dp,
            imageRes = Res.drawable.avatar_man_3,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 10.dp, y = 60.dp)
        )
        // Center Left Avatar
        ProfileAvatar(
            size = 85.dp,
            imageRes = Res.drawable.avatar_woman_3,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-4).dp, y = 20.dp)
        )
        // Center Right Avatar
        ProfileAvatar(
            size = 95.dp,
            imageRes = Res.drawable.avatar_man_2,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 4.dp, y = (-10).dp),
            borderColor = MaterialTheme.colorScheme.primary
        )
        // Bottom Right Avatar
        ProfileAvatar(
            size = 80.dp,
            imageRes = Res.drawable.avatar_woman_2,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-12).dp, y = (-25).dp)
        )
        // Hero Center Avatar (Largest) — User's real photo
        ProfileAvatar(
            size = 150.dp,
            imageRes = Res.drawable.avatar_woman_3,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 10.dp),
            borderColor = MaterialTheme.colorScheme.surface,
            borderWidth = 4.dp
        )
    }
}

@Composable
private fun ProfileAvatar(
    size: Dp,
    modifier: Modifier = Modifier,
    imageRes: DrawableResource? = null,
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp
) {
    val colors = MaterialTheme.colorScheme
    val hasBorder = borderWidth > 0.dp && borderColor != Color.Transparent

    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .then(
                if (hasBorder) Modifier.border(borderWidth, borderColor, CircleShape) else Modifier
            )
            .background(colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != null) {
            androidx.compose.foundation.Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size * 0.45f)
                    .clip(CircleShape)
                    .background(colors.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }
    }
}



