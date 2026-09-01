package com.rajamohan.mindmingle.presentation.login.component.desktop

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.presentation.common.icon.CheckIcon
import com.rajamohan.mindmingle.presentation.common.icon.EnvelopeIcon
import com.rajamohan.mindmingle.presentation.theme.MindMingleHeaderLockup
import kotlinx.coroutines.delay

/**
 * Desktop sign-in.
 *
 * ## Why this exists separately from the mobile screens
 * The mailed-code flow was fully built everywhere except the one place that starts it: the Cloud
 * Functions (`requestEmailOtp`/`verifyEmailOtp`), the repository calls, the `AuthEvent`s and the
 * verification screen were all in place, but nothing ever dispatched `RequestEmailOtp`. The only
 * reference to it was the *Resend* button on a screen that could not be reached. Desktop was
 * therefore email + password only.
 *
 * These screens are the missing entry point, in desktop's own two-panel shape rather than a
 * centred phone layout stretched across a 1280px window.
 *
 * ## Why the code is the primary action
 * Desktop has no working Google OAuth on the JVM, so a password was the only way in and every
 * desktop user had to invent and store one. A mailed code needs no stored secret and no password
 * reset path. Password sign-in stays as a secondary option for accounts that already have one.
 */

private val EmailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

@Composable
fun DesktopEmailSignInScreen(
    isSubmitting: Boolean = false,
    errorMessage: String = "",
    onRequestCode: (email: String) -> Unit,
    onPasswordSubmit: (email: String, password: String, isNewAccount: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usePassword by remember { mutableStateOf(false) }
    var isNewAccount by remember { mutableStateOf(false) }

    val isValidEmail = remember(email) { EmailRegex.matches(email.trim()) }
    val canSubmit = if (usePassword) {
        isValidEmail && password.length >= 6 && !isSubmitting
    } else {
        isValidEmail && !isSubmitting
    }

    DesktopAuthShell(
        headline = "Sign in to MindMingle",
        blurb = "Use your work or personal email. We'll send a six-digit code — no password to remember.",
        onBack = onBack
    ) {
        Text(
            text = if (usePassword && isNewAccount) "Create your account" else "Continue with email",
            style = typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when {
                usePassword && isNewAccount -> "Pick a password of at least 6 characters."
                usePassword -> "Enter the password on your account."
                else -> "We'll email you a code that expires in five minutes."
            },
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(26.dp))

        DesktopAuthField(
            value = email,
            onValueChange = { email = it },
            placeholder = "you@example.com",
            keyboardType = KeyboardType.Email
        )

        if (usePassword) {
            Spacer(modifier = Modifier.height(12.dp))
            DesktopAuthField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                keyboardType = KeyboardType.Password,
                isPassword = true
            )
        }

        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = errorMessage, style = typography.bodySmall, color = colors.error)
        }

        Spacer(modifier = Modifier.height(22.dp))

        DesktopAuthButton(
            label = when {
                usePassword && isNewAccount -> "Create account"
                usePassword -> "Sign in"
                else -> "Email me a code"
            },
            enabled = canSubmit,
            isBusy = isSubmitting,
            onClick = {
                if (!canSubmit) return@DesktopAuthButton
                if (usePassword) {
                    onPasswordSubmit(email.trim(), password, isNewAccount)
                } else {
                    onRequestCode(email.trim())
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.outline.copy(alpha = 0.25f)))
            Text(
                text = "or",
                style = typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.outline.copy(alpha = 0.25f)))
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (usePassword) "Email me a code instead" else "Use a password instead",
            style = typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary,
            modifier = Modifier.clickable(enabled = !isSubmitting) {
                usePassword = !usePassword
                password = ""
            }
        )

        if (usePassword) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isNewAccount) "Already have an account? Sign in" else "New here? Create an account",
                style = typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
                modifier = Modifier.clickable(enabled = !isSubmitting) { isNewAccount = !isNewAccount }
            )
        }
    }
}

/**
 * The six-digit code screen, desktop shape.
 *
 * Typing is handled by one invisible [BasicTextField] behind six boxes rather than six fields —
 * a physical keyboard types straight through it, and paste drops all six digits in at once, which
 * is how anyone copying a code out of a mail client will actually do it.
 */
@Composable
fun DesktopEmailOtpScreen(
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

    val codeLength = 6
    var code by remember { mutableStateOf("") }
    var hasSubmitted by remember { mutableStateOf(false) }
    var resendSeconds by remember { mutableStateOf(30) }

    // A rejected code has to be retypeable; without this the auto-submit latch stays closed.
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) hasSubmitted = false
    }

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1000)
            resendSeconds -= 1
        }
    }

    DesktopAuthShell(
        headline = "Check your inbox",
        blurb = "The code expires in five minutes. Codes are single use — requesting a new one voids the old.",
        onBack = onBack
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
            contentAlignment = Alignment.Center
        ) {
            if (isSuccess) {
                CheckIcon(color = Color.White, modifier = Modifier.size(32.dp))
            } else {
                EnvelopeIcon(color = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Enter your code",
            style = typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Sent to ", style = typography.bodyMedium, color = colors.onSurfaceVariant)
            Text(text = email, style = typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Box(contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = code,
                onValueChange = { next ->
                    if (next.length <= codeLength && next.all { it.isDigit() }) {
                        code = next
                        if (next.length == codeLength && !hasSubmitted && !isVerifying && !isSuccess) {
                            hasSubmitted = true
                            onCodeComplete(next)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = typography.bodyLarge.copy(color = Color.Transparent),
                cursorBrush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                for (index in 0 until codeLength) {
                    val digit = code.getOrNull(index)?.toString() ?: ""
                    val isNext = code.length == index

                    val boxScale by animateFloatAsState(
                        targetValue = if (digit.isNotEmpty()) 1.06f else 1f,
                        label = "desktop_otp_box_scale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(boxScale)
                            .size(width = 52.dp, height = 60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.4f))
                            .border(
                                width = if (isNext) 2.dp else 1.dp,
                                color = if (isNext) colors.primary else colors.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit,
                            style = typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onSurface
                        )
                    }
                }
            }
        }

        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage, style = typography.bodySmall, color = colors.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        DesktopAuthButton(
            label = if (isSuccess) "Verified — signing you in" else "Verify and continue",
            enabled = code.length == codeLength && !isVerifying && !isSuccess,
            isBusy = isVerifying,
            isDone = isSuccess,
            onClick = {
                if (!hasSubmitted) {
                    hasSubmitted = true
                    onCodeComplete(code)
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (resendSeconds > 0) {
            Text(
                text = buildAnnotatedString {
                    append("Resend code in ")
                    withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold)) {
                        append("00:${resendSeconds.toString().padStart(2, '0')}")
                    }
                },
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        } else {
            Text(
                text = "Didn't get it? Send a new code",
                style = typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                modifier = Modifier.clickable {
                    resendSeconds = 30
                    code = ""
                    hasSubmitted = false
                    onResend()
                }
            )
        }
    }
}

/**
 * Two-panel shell shared by both desktop auth screens: brand on the left, a single focused form
 * card on the right. Matches DesktopWelcomeScreen, which is the screen the user arrives from.
 */
@Composable
private fun DesktopAuthShell(
    headline: String,
    blurb: String,
    onBack: () -> Unit,
    form: @Composable () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 48.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                MindMingleHeaderLockup(iconSize = 44.dp)

                Column {
                    Text(
                        text = headline,
                        style = typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onBackground,
                        fontSize = 40.sp,
                        lineHeight = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = blurb,
                        style = typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        fontSize = 16.sp,
                        lineHeight = 25.sp
                    )
                }

                Text(
                    text = "← Back",
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.clickable { onBack() }
                )
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colors.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.widthIn(max = 460.dp).weight(1f)
            ) {
                Column(modifier = Modifier.padding(40.dp)) {
                    form()
                }
            }
        }
    }
}

@Composable
private fun DesktopAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

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
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = typography.bodyLarge.copy(color = colors.onSurface),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = typography.bodyLarge,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun DesktopAuthButton(
    label: String,
    enabled: Boolean,
    isBusy: Boolean,
    isDone: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = when {
            isDone -> colors.tertiary
            enabled -> colors.primary
            else -> colors.outlineVariant.copy(alpha = 0.4f)
        },
        shadowElevation = if (enabled) 6.dp else 0.dp,
        modifier = Modifier.fillMaxWidth().height(54.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isBusy -> CircularProgressIndicator(
                    color = colors.onPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(22.dp)
                )

                isDone -> {
                    CheckIcon(color = colors.onTertiary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onTertiary)
                }

                else -> Text(
                    text = label,
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
