package com.rajamohan.mindmingle.presentation.welcome.component.desktop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.presentation.welcome.component.shared.EmailSignInButton
import com.rajamohan.mindmingle.presentation.welcome.component.shared.GetStartedButton
import mindmingle.shared.generated.resources.Res
import mindmingle.shared.generated.resources.avatar_man_2
import mindmingle.shared.generated.resources.avatar_man_3
import mindmingle.shared.generated.resources.avatar_rajamohan
import mindmingle.shared.generated.resources.avatar_woman_1
import mindmingle.shared.generated.resources.avatar_woman_2
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Premium Desktop / Widescreen Layout for MindMingle Onboarding
 */
@Composable
fun DesktopWelcomeScreen(
    onGetStartedClick: () -> Unit = {},
    onEmailSignInClick: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── LEFT COLUMN: Desktop Brand & Call-To-Action (50% Width) ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 40.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Logo
                com.rajamohan.mindmingle.presentation.theme.MindMingleHeaderLockup(
                    iconSize = 44.dp
                )

                Column {
                    // Widescreen Headline
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = colors.onBackground, fontWeight = FontWeight.ExtraBold)) {
                                append("Find Your Next\n")
                            }
                            withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.ExtraBold)) {
                                append("Tech")
                            }
                            withStyle(SpanStyle(color = colors.onBackground, fontWeight = FontWeight.ExtraBold)) {
                                append(" Partner")
                            }
                        },
                        style = typography.displayMedium,
                        fontSize = 44.sp,
                        lineHeight = 54.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Connect with top developers, engineers, and co-founders across the globe who speak your language. No photos required.",
                        style = typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        fontSize = 17.sp,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // Desktop Call-to-Action: draggable "swipe to get started" + email sign-in
                    // (no working Google OAuth on JVM, see GoogleAuthLauncher.jvm.kt)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GetStartedButton(
                            onClick = onGetStartedClick,
                            modifier = Modifier.width(280.dp),
                            trackHeight = 60.dp,
                            thumbSize = 48.dp
                        )

                        EmailSignInButton(onClick = onEmailSignInClick)
                    }
                }

                // Desktop Footer Info
                Text(
                    text = "Available on macOS, Windows, Linux, Android & iOS",
                    style = typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            // ── RIGHT COLUMN: Expanded Widescreen Avatar Grid (50% Width) ──
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .shadow(12.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(colors.primaryContainer.copy(alpha = 0.5f), colors.surface)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                DesktopAvatarGrid()
            }
        }
    }
}

@Composable
private fun DesktopAvatarGrid() {
    val colors = MaterialTheme.colorScheme

    val infiniteTransition = rememberInfiniteTransition(label = "desktop_float")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "desktop_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center Main Large Hero Avatar — user's real photo
        DesktopAvatarBubble(
            size = 180.dp,
            imageRes = Res.drawable.avatar_rajamohan,
            modifier = Modifier.scale(pulse),
            borderColor = colors.surface
        )

        // Floating Surrounding Avatars
        DesktopAvatarBubble(
            size = 120.dp,
            imageRes = Res.drawable.avatar_woman_1,
            modifier = Modifier.offset(x = (-150).dp, y = (-120).dp),
            borderColor = colors.primary
        )

        DesktopAvatarBubble(
            size = 110.dp,
            imageRes = Res.drawable.avatar_man_2,
            modifier = Modifier.offset(x = 160.dp, y = (-100).dp)
        )

        DesktopAvatarBubble(
            size = 100.dp,
            imageRes = Res.drawable.avatar_woman_2,
            modifier = Modifier.offset(x = (-160).dp, y = 110.dp)
        )

        DesktopAvatarBubble(
            size = 115.dp,
            imageRes = Res.drawable.avatar_man_3,
            modifier = Modifier.offset(x = 150.dp, y = 120.dp),
            borderColor = colors.primary
        )
    }
}

@Composable
private fun DesktopAvatarBubble(
    size: Dp,
    imageRes: DrawableResource,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .border(if (borderColor != Color.Transparent) 4.dp else 0.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
