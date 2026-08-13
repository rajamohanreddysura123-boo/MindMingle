package com.rajamohan.mindmingle.presentation.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Option 4: Modern Monogram 'M' Vector logo mark with a sleek frosted glass border encasing the letter contour.
 * Rendered in application primary color (0xFFD6A87C) on frosted dark glass backdrop.
 */
@Composable
fun MindMingleLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showGlassCard: Boolean = true,
    primaryColor: Color = Color(0xFFD6A87C)
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            if (showGlassCard) {
                // 1. Frosted Dark Glass Fill
                val glassFillGrad = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.02f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                drawRoundRect(
                    brush = glassFillGrad,
                    cornerRadius = CornerRadius(w * 0.22f, h * 0.22f)
                )

                // 2. Glass Rim Border
                val glassBorderGrad = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.25f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                drawRoundRect(
                    brush = glassBorderGrad,
                    cornerRadius = CornerRadius(w * 0.22f, h * 0.22f),
                    style = Stroke(width = w * 0.015f)
                )
            }

            // 3. Monogram 'M' Path
            val strokeW = w * 0.060f

            val mPath = Path().apply {
                moveTo(w * 0.3125f, h * 0.6875f)
                lineTo(w * 0.3125f, h * 0.34375f)
                lineTo(w * 0.5000f, h * 0.53125f)
                lineTo(w * 0.6875f, h * 0.34375f)
                lineTo(w * 0.6875f, h * 0.6875f)
            }

            // Outer Frosted Glass Border contour around 'M'
            drawPath(
                path = mPath,
                color = Color.White.copy(alpha = 0.38f),
                style = Stroke(width = strokeW * 1.38f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Inner Core 'M' in Primary Color
            drawPath(
                path = mPath,
                color = primaryColor,
                style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Full MindMingle Horizontal Brand Header Lockup (Icon + Wordmark).
 */
@Composable
fun MindMingleHeaderLockup(
    modifier: Modifier = Modifier,
    iconSize: Dp = 36.dp,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    primaryAccent: Color = Color(0xFFD6A87C)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        MindMingleLogoMark(size = iconSize, primaryColor = primaryAccent)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = primaryAccent, fontWeight = FontWeight.ExtraBold)) {
                    append("Mind")
                }
                withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.SemiBold)) {
                    append("Mingle")
                }
            },
            style = MaterialTheme.typography.titleLarge,
            fontSize = 22.sp,
            letterSpacing = 0.5.sp
        )
    }
}
