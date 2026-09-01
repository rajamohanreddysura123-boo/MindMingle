package com.rajamohan.mindmingle.presentation.login.component.mobile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.presentation.common.icon.CheckIcon
import com.rajamohan.mindmingle.presentation.common.icon.LockIcon
import com.rajamohan.mindmingle.presentation.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Email + password sign-in. Desktop's only way in (no working Google OAuth on JVM) and mobile's
 * alternative to Google. The same screen registers: [isNewAccount] flips the button and which
 * event the caller fires. Password rules are Firebase's — 6 characters minimum.
 */
@Composable
fun EmailPasswordScreen(
    isSubmitting: Boolean = false,
    errorMessage: String = "",
    onSubmit: (email: String, password: String, isNewAccount: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isNewAccount by remember { mutableStateOf(false) }
    val isValidEmail = remember(email) { Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email) }
    val isValidPassword = password.length >= 6
    val canSubmit = isValidEmail && isValidPassword && !isSubmitting

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    LockIcon(color = Color.White, modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isNewAccount) "Create your account" else "Sign in with Email",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isNewAccount) {
                        "Pick a password of at least 6 characters."
                    } else {
                        "Enter your email and password to continue."
                    },
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = email,
                        onValueChange = { email = it },
                        singleLine = true,
                        textStyle = typography.bodyLarge.copy(color = colors.onSurface),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (email.isEmpty()) {
                                Text(
                                    text = "you@example.com",
                                    style = typography.bodyLarge,
                                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            inner()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        textStyle = typography.bodyLarge.copy(color = colors.onSurface),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (password.isEmpty()) {
                                Text(
                                    text = "Password",
                                    style = typography.bodyLarge,
                                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            inner()
                        }
                    )
                }

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = errorMessage, style = typography.bodySmall, color = colors.error, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(22.dp))

                Surface(
                    onClick = { if (canSubmit) onSubmit(email.trim(), password, isNewAccount) },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(50),
                    color = if (canSubmit) colors.primary else colors.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(22.dp))
                        } else {
                            Text(
                                text = if (isNewAccount) "Create account" else "Continue",
                                style = typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (canSubmit) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = if (isNewAccount) "Already have an account? Sign in" else "New here? Create an account",
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary,
                    modifier = Modifier.clickable(enabled = !isSubmitting) { isNewAccount = !isNewAccount }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Back",
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.clickable { onBack() }
                )
            }
        }
    }
}

/**
 * Verifies the 6-digit code emailed by AuthEvent.RequestEmailOtp (or auto-triggered admin
 * 2FA after a Google sign-in matching the reserved admin email — see AuthViewModel).
 */
@Composable
fun EmailOtpVerificationScreen(
    email: String,
    isVerifying: Boolean = false,
    isSuccess: Boolean = false,
    errorMessage: String = "",
    onCodeComplete: (code: String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var otpValue by remember { mutableStateOf("") }
    var hasSubmitted by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(30) }
    val codeLength = 6

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) hasSubmitted = false
    }

    LaunchedEffect(timerSeconds) {
        if (timerSeconds > 0) {
            delay(1000)
            timerSeconds -= 1
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val isLandscape = maxWidth > maxHeight

            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = if (isLandscape) 8.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    BackButtonPill(onClick = onBack)
                }

                Spacer(modifier = Modifier.height(36.dp))

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSuccess) {
                        CheckIcon(color = Color.White, modifier = Modifier.size(42.dp))
                    } else {
                        LockIcon(color = Color.White, modifier = Modifier.size(42.dp))
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Check Your Email",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(text = "Code sent to ", style = typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(text = email, style = typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
                }

                Spacer(modifier = Modifier.height(40.dp))

                Box(contentAlignment = Alignment.Center) {
                    BasicTextField(
                        value = otpValue,
                        onValueChange = {
                            if (it.length <= codeLength && it.all { char -> char.isDigit() }) {
                                otpValue = it
                                if (it.length == codeLength && !hasSubmitted && !isVerifying && !isSuccess) {
                                    hasSubmitted = true
                                    onCodeComplete(it)
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        for (index in 0 until codeLength) {
                            val digit = otpValue.getOrNull(index)?.toString() ?: ""
                            val isFocused = otpValue.length == index || (index == codeLength - 1 && otpValue.length == codeLength)

                            val animatedScale by animateFloatAsState(
                                targetValue = if (digit.isNotEmpty()) 1.08f else 1f,
                                label = "email_pin_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .scale(animatedScale)
                                    .size(48.dp)
                                    .shadow(if (isFocused) 8.dp else 2.dp, RoundedCornerShape(14.dp))
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surface)
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) colors.primary else colors.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = digit, style = typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = colors.primary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage.isNotBlank()) {
                    Text(text = errorMessage, style = typography.bodySmall, color = colors.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (timerSeconds > 0) {
                    Text(
                        text = buildAnnotatedString {
                            append("Resend code in ")
                            withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold)) {
                                append("00:${timerSeconds.toString().padStart(2, '0')}")
                            }
                        },
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Didn't receive code? Resend",
                        style = typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.clickable {
                            timerSeconds = 30
                            otpValue = ""
                            hasSubmitted = false
                            onResend()
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                val isComplete = otpValue.length == codeLength

                Surface(
                    onClick = {
                        if (isComplete && !hasSubmitted && !isVerifying && !isSuccess) {
                            hasSubmitted = true
                            onCodeComplete(otpValue)
                        }
                    },
                    enabled = isComplete && !isVerifying,
                    shape = RoundedCornerShape(50),
                    color = if (isSuccess) Color(0xFF388E3C) else if (isComplete) colors.primary else colors.outlineVariant.copy(alpha = 0.4f),
                    shadowElevation = if (isComplete) 6.dp else 0.dp,
                    modifier = Modifier.fillMaxWidth().height(58.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                        if (isVerifying) {
                            CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                        } else if (isSuccess) {
                            CheckIcon(color = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Verified! Welcome", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text(
                                text = "Verify & Proceed",
                                style = typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isComplete) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
