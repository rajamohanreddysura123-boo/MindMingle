package com.rajamohan.mindmingle.presentation.login.component.mobile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// Navigation States
sealed class ScreenState {
    object Onboarding : ScreenState()
    /** Desktop's primary sign-in entry — no working Google OAuth on JVM. */
    object EmailInput : ScreenState()
    data class EmailOtpVerification(val email: String) : ScreenState()
    data class ProfileSetup(
        val uid: String,
        val email: String = "",
        val prefillName: String = ""
    ) : ScreenState()
    data class Home(
        val uid: String = "",
        val userName: String = "",
        val userEmail: String = ""
    ) : ScreenState()
    object Admin : ScreenState()
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
