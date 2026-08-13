package com.rajamohan.mindmingle.presentation.welcome.component.shared

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/** Right-draggable "swipe to get started" pill — shimmer sweep, ripple rings off the thumb, fires [onClick] past 80% drag. */
@Composable
fun GetStartedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 60.dp,
    thumbSize: Dp = 48.dp
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val density = LocalDensity.current

    val padding = 6.dp

    var dragOffsetPx by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // --- Wave / Pulse Animations ---
    val infiniteTransition = rememberInfiniteTransition(label = "btn_wave")

    // Shimmer sweep: travels left → right across the track
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -0.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Three staggered ripple rings expanding from the thumb
    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, delayMillis = 467, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, delayMillis = 934, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring3"
    )

    BoxWithConstraints(
        modifier = modifier
            .height(trackHeight)
            .clip(RoundedCornerShape(30.dp))
            .background(colors.primary)
    ) {
        val maxDragPx = with(density) { (maxWidth - thumbSize - (padding * 2)).toPx() }
        val thumbSizePx = with(density) { thumbSize.toPx() }
        val paddingPx = with(density) { padding.toPx() }

        val animatedOffsetPx by animateFloatAsState(
            targetValue = dragOffsetPx,
            animationSpec = spring(stiffness = 400f),
            label = "swipe_button_offset"
        )

        val currentPx = if (isDragging) dragOffsetPx else animatedOffsetPx
        val progress = if (maxDragPx > 0f) (currentPx / maxDragPx).coerceIn(0f, 1f) else 0f

        // 1. Shimmer wave sweep across the track
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shimmerX = shimmerProgress * size.width
            val band = size.width * 0.35f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    startX = shimmerX - band,
                    endX = shimmerX + band
                ),
                size = size
            )
        }

        // 2. Ripple rings — three concentric arcs from thumb center
        Canvas(modifier = Modifier.fillMaxSize()) {
            val thumbCx = currentPx + paddingPx + thumbSizePx / 2f
            val thumbCy = size.height / 2f
            val baseR = thumbSizePx / 2f

            listOf(ring1, ring2, ring3).forEach { p ->
                val radius = baseR + p * 28.dp.toPx()
                val alpha = (1f - p).coerceAtLeast(0f) * 0.50f
                if (alpha > 0f) {
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(thumbCx, thumbCy),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                }
            }
        }

        // 3. Label row
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = thumbSize + (padding * 2) + 8.dp, end = padding + 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.alpha(1f - progress)
            ) {
                Text(
                    text = "Get Started",
                    style = typography.labelLarge,
                    color = colors.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        // 4. Draggable thumb
        Box(
            modifier = Modifier
                .padding(padding)
                .offset { IntOffset(currentPx.roundToInt(), 0) }
                .size(thumbSize)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.surface)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, maxDragPx)
                        },
                        onDragEnd = {
                            isDragging = false
                            if (dragOffsetPx >= maxDragPx * 0.8f) {
                                dragOffsetPx = maxDragPx
                                onClick()
                            } else {
                                dragOffsetPx = 0f
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffsetPx = 0f
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            ArrowForwardIcon(
                color = colors.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ArrowForwardIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.14f
        val path = Path().apply {
            // Horizontal line
            moveTo(w * 0.15f, h * 0.5f)
            lineTo(w * 0.85f, h * 0.5f)
            // Arrow head top
            moveTo(w * 0.55f, h * 0.22f)
            lineTo(w * 0.85f, h * 0.5f)
            // Arrow head bottom
            lineTo(w * 0.55f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
