package com.rajamohan.mindmingle.presentation.login.component.mobile

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.window.Dialog
import com.rajamohan.mindmingle.domain.model.CountryCode
import com.rajamohan.mindmingle.domain.model.CountryCodeRepository
import com.rajamohan.mindmingle.presentation.common.icon.ArrowForwardIcon
import com.rajamohan.mindmingle.presentation.common.icon.CheckIcon
import com.rajamohan.mindmingle.presentation.common.icon.ChevronDownIcon
import com.rajamohan.mindmingle.presentation.common.icon.CrossIcon
import com.rajamohan.mindmingle.presentation.common.icon.LockIcon
import com.rajamohan.mindmingle.presentation.common.icon.PencilIcon
import com.rajamohan.mindmingle.presentation.common.icon.ShieldIcon
import com.rajamohan.mindmingle.presentation.theme.Spacing

// Navigation States
sealed class ScreenState {
    object Onboarding : ScreenState()
    object PhoneInput : ScreenState()
    data class OtpVerification(val phone: String, val verificationId: String) : ScreenState()
    /** Desktop's primary sign-in entry — no working Google OAuth on JVM. */
    object EmailInput : ScreenState()
    data class EmailOtpVerification(val email: String) : ScreenState()
    data class ProfileSetup(
        val uid: String,
        val email: String = "",
        val phoneNumber: String = "",
        val prefillName: String = ""
    ) : ScreenState()
    data class Home(
        val uid: String = "",
        val userName: String = "Rajamohan Reddy",
        val userEmail: String = "rajamohan.reddy@gmail.com"
    ) : ScreenState()
    object Admin : ScreenState()
}

/**
 * Animated Phone Number Input Screen
 */
@Composable
fun PhoneInputScreen(
    onSendOtp: (String) -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(CountryCode.defaultIndia) }
    var countryList by remember { mutableStateOf(CountryCodeRepository.fallbackList()) }
    var isCountryPickerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = CountryCodeRepository.getCountryCodes()
        if (loaded.isNotEmpty()) {
            countryList = loaded
            loaded.find { it.code == "IN" }?.let { selectedCountry = it }
        }
    }

    // Pulse animation for header badge
    val infiniteTransition = rememberInfiniteTransition(label = "phone_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isLandscape = maxWidth > maxHeight

            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .safeContentPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = if (isLandscape) 8.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Header Top Bar - Aligned Far Left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButtonPill(onClick = onBack)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Special Header Badge
            AnimatedAuthHeaderBadge()

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Description
            Text(
                text = "Enter Your Mobile Number",
                style = typography.headlineSmall,
                fontWeight = FontWeight.Normal,
                color = colors.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We'll send a 4-digit verification code to verify your phone number",
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Phone Input Field Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(4.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(
                        width = if (phoneNumber.length >= 10) 2.dp else 1.dp,
                        color = if (phoneNumber.length >= 10) colors.primary else colors.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country Code Selector Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isCountryPickerOpen = true }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${selectedCountry.flagEmoji} ${selectedCountry.dialCode}",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ChevronDownIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(10.dp))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(colors.outlineVariant)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Number Input Field
                BasicTextField(
                    value = phoneNumber,
                    onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) phoneNumber = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    textStyle = typography.titleLarge.copy(
                        color = colors.onSurface,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.5.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (phoneNumber.isEmpty()) {
                            Text(
                                text = "98765 43210",
                                style = typography.titleLarge,
                                color = colors.onSurfaceVariant.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 1.5.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Button with Loading & Pulse
            val isValid = phoneNumber.length >= 10

            Surface(
                onClick = {
                    if (isValid && !isLoading) {
                        isLoading = true
                    }
                },
                enabled = isValid,
                shape = RoundedCornerShape(50),
                color = if (isValid) colors.primary else colors.outlineVariant.copy(alpha = 0.4f),
                shadowElevation = if (isValid) 6.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = colors.onPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        LaunchedEffect(Unit) {
                            delay(1200)
                            isLoading = false
                            onSendOtp("${selectedCountry.dialCode} $phoneNumber")
                        }
                    } else {
                        Text(
                            text = "Send OTP",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isValid) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ArrowForwardIcon(
                            color = if (isValid) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isCountryPickerOpen) {
            val filteredCountries = remember(searchQuery, countryList) {
                if (searchQuery.isBlank()) countryList
                else countryList.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.dialCode.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true)
                }
            }

            Dialog(
                onDismissRequest = {
                    isCountryPickerOpen = false
                    searchQuery = ""
                }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = colors.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Country Code",
                                style = typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            CrossIcon(
                                color = colors.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        isCountryPickerOpen = false
                                        searchQuery = ""
                                    }
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search bar input
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search country name or code...",
                                        style = typography.bodyMedium,
                                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                inner()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredCountries.size) { index ->
                                val country = filteredCountries[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedCountry = country
                                            isCountryPickerOpen = false
                                            searchQuery = ""
                                        }
                                        .padding(vertical = 12.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = country.flagEmoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = country.name,
                                        style = typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = country.dialCode,
                                        style = typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Animated OTP Verification Screen
 */
@Composable
fun OtpVerificationScreen(
    phone: String,
    isVerifying: Boolean = false,
    isSuccess: Boolean = false,
    errorMessage: String = "",
    onCodeComplete: (code: String) -> Unit = {},
    onBackToPhone: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    var otpValue by remember { mutableStateOf("") }
    var hasSubmitted by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(30) }

    // Reset the submit guard whenever a fresh error comes back, so the user can retry
    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotBlank()) hasSubmitted = false
    }

    // Countdown Timer Loop
    LaunchedEffect(timerSeconds) {
        if (timerSeconds > 0) {
            delay(1000)
            timerSeconds -= 1
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isLandscape = maxWidth > maxHeight

            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .safeContentPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = if (isLandscape) 8.dp else 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Header Top Bar - Aligned Far Left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButtonPill(onClick = onBackToPhone)
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Animated Lock Graphic Badge
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(colors.primary, colors.tertiary)
                        )
                    ),
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
                text = "OTP Verification",
                style = typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Code sent to ",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = phone,
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
                Spacer(modifier = Modifier.width(6.dp))
                PencilIcon(
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onBackToPhone() }
                        .size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Hidden BasicTextField over 4 PIN Boxes
            Box(contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = otpValue,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            otpValue = it
                            if (it.length == 4 && !hasSubmitted && !isVerifying && !isSuccess) {
                                hasSubmitted = true
                                onCodeComplete(it)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                // 4 Animated PIN Digit Cards
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (index in 0 until 4) {
                        val digit = otpValue.getOrNull(index)?.toString() ?: ""
                        val isFocused = otpValue.length == index || (index == 3 && otpValue.length == 4)

                        val animatedScale by animateFloatAsState(
                            targetValue = if (digit.isNotEmpty()) 1.08f else 1f,
                            label = "pin_scale"
                        )

                        Box(
                            modifier = Modifier
                                .scale(animatedScale)
                                .size(64.dp)
                                .shadow(if (isFocused) 8.dp else 2.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surface)
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = if (isFocused) colors.primary else colors.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit,
                                style = typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    style = typography.bodySmall,
                    color = colors.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Timer & Resend Option
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
                    text = "Didn't receive code? Resend OTP",
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    modifier = Modifier.clickable { timerSeconds = 30 }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Verification Action Button
            val isComplete = otpValue.length == 4

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            color = colors.onPrimary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (isSuccess) {
                        CheckIcon(color = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Verified! Welcome",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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

/**
 * High-tech Animated Header Badge for Mobile Auth Flow
 * Features a glowing pulse radar ring, rotating dashed orbit ring, and a glassmorphism shield core.
 */
@Composable
fun AnimatedAuthHeaderBadge() {
    val colors = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "auth_badge_anim")

    // Continuous rotation angle for outer dashed orbit ring
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )

    // Breathing pulse scale for outer glow radar
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    // Vertical floating animation
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    Box(
        modifier = Modifier
            .size(130.dp)
            .offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Outer Pulse Radar Glow
        Box(
            modifier = Modifier
                .scale(pulseScale)
                .size(110.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.primary.copy(alpha = 0.35f),
                            colors.secondary.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Layer 2: Rotating Dashed Gradient Orbit Ring
        val primaryColor = colors.primary
        val secondaryColor = colors.secondary
        val tertiaryColor = colors.tertiary
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer(rotationZ = rotationAngle)
        ) {
            val strokeWidth = 3.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor,
                        secondaryColor,
                        tertiaryColor,
                        primaryColor
                    )
                ),
                radius = radius,
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 16f), 0f)
                )
            )
        }

        // Layer 3: Central Glassmorphic Shield Badge
        Box(
            modifier = Modifier
                .size(88.dp)
                .shadow(16.dp, CircleShape, spotColor = colors.primary)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colors.primary,
                            colors.secondary,
                            colors.tertiary
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.7f),
                            colors.primary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.scale(1f + (pulseScale - 1f) * 0.3f),
                contentAlignment = Alignment.Center
            ) {
                ShieldIcon(color = Color.White, modifier = Modifier.size(40.dp))
            }
        }
    }
}

/**
 * Pixel-perfect Back Button Pill component with custom Canvas arrow stroke.
 */
@Composable
fun BackButtonPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val primaryColor = colors.primary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                val strokeWidth = 2.dp.toPx()
                val yCenter = size.height / 2
                drawLine(
                    color = primaryColor,
                    start = Offset(size.width * 0.95f, yCenter),
                    end = Offset(size.width * 0.05f, yCenter),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(size.width * 0.05f, yCenter),
                    end = Offset(size.width * 0.45f, yCenter - size.height * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(size.width * 0.05f, yCenter),
                    end = Offset(size.width * 0.45f, yCenter + size.height * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Back",
                style = typography.labelLarge,
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}
