package com.rajamohan.mindmingle.presentation.welcome.component.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.2f)),
        modifier = modifier.size(60.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GoogleLogoIcon(modifier = Modifier.size(26.dp))
        }
    }
}

/** Desktop's primary sign-in trigger — no working Google OAuth on JVM, so email + a mailed code instead. */
@Composable
fun EmailSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = colors.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.2f)),
        modifier = modifier.size(60.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EnvelopeIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun EnvelopeIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.09f

        drawPath(
            path = Path().apply {
                moveTo(w * 0.08f, h * 0.22f)
                lineTo(w * 0.92f, h * 0.22f)
                lineTo(w * 0.92f, h * 0.78f)
                lineTo(w * 0.08f, h * 0.78f)
                close()
            },
            color = color,
            style = Stroke(width = strokeWidth, join = StrokeJoin.Round)
        )
        drawPath(
            path = Path().apply {
                moveTo(w * 0.08f, h * 0.24f)
                lineTo(w * 0.5f, h * 0.56f)
                lineTo(w * 0.92f, h * 0.24f)
            },
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun GoogleLogoIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) * 0.40f
        val strokeWidth = radius * 0.42f

        // Red top arc (-45° to -180°)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -45f,
            sweepAngle = -135f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        // Yellow bottom-left arc (-180° to -245°)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = -180f,
            sweepAngle = -65f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        // Green bottom-right arc (35° to 135°)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 35f,
            sweepAngle = 100f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        // Blue right arc (-45° to 35°)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 80f,
            useCenter = false,
            style = Stroke(width = strokeWidth)
        )
        // Blue horizontal bar into center
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(cx, cy),
            end = Offset(cx + radius, cy),
            strokeWidth = strokeWidth
        )
    }
}
