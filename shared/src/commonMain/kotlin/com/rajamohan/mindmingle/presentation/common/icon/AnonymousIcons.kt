package com.rajamohan.mindmingle.presentation.common.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private fun anonymousIconModifier() = Modifier.size(24.dp)

@Composable
fun MaskIcon(color: Color, modifier: Modifier = anonymousIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.085f

        val mask = Path().apply {
            moveTo(w * 0.10f, h * 0.34f)
            cubicTo(w * 0.30f, h * 0.24f, w * 0.70f, h * 0.24f, w * 0.90f, h * 0.34f)
            cubicTo(w * 0.90f, h * 0.66f, w * 0.72f, h * 0.84f, w * 0.50f, h * 0.84f)
            cubicTo(w * 0.28f, h * 0.84f, w * 0.10f, h * 0.66f, w * 0.10f, h * 0.34f)
            close()
        }
        drawPath(mask, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))

        drawCircle(color = color, radius = w * 0.065f, center = Offset(w * 0.35f, h * 0.47f))
        drawCircle(color = color, radius = w * 0.065f, center = Offset(w * 0.65f, h * 0.47f))

        val smile = Path().apply {
            moveTo(w * 0.38f, h * 0.65f)
            cubicTo(w * 0.45f, h * 0.72f, w * 0.55f, h * 0.72f, w * 0.62f, h * 0.65f)
        }
        drawPath(smile, color = color, style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round))
    }
}

@Composable
fun SkipIcon(color: Color, modifier: Modifier = anonymousIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.1f

        val firstChevron = Path().apply {
            moveTo(w * 0.22f, h * 0.24f)
            lineTo(w * 0.52f, h * 0.5f)
            lineTo(w * 0.22f, h * 0.76f)
        }
        val secondChevron = Path().apply {
            moveTo(w * 0.50f, h * 0.24f)
            lineTo(w * 0.80f, h * 0.5f)
            lineTo(w * 0.50f, h * 0.76f)
        }
        drawPath(firstChevron, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(secondChevron, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun GhostIcon(color: Color, modifier: Modifier = anonymousIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.085f

        val body = Path().apply {
            moveTo(w * 0.20f, h * 0.86f)
            lineTo(w * 0.20f, h * 0.46f)
            cubicTo(w * 0.20f, h * 0.20f, w * 0.80f, h * 0.20f, w * 0.80f, h * 0.46f)
            lineTo(w * 0.80f, h * 0.86f)
            lineTo(w * 0.65f, h * 0.74f)
            lineTo(w * 0.50f, h * 0.86f)
            lineTo(w * 0.35f, h * 0.74f)
            close()
        }
        drawPath(body, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))

        drawCircle(color = color, radius = w * 0.055f, center = Offset(w * 0.40f, h * 0.48f))
        drawCircle(color = color, radius = w * 0.055f, center = Offset(w * 0.60f, h * 0.48f))
    }
}

@Composable
fun RadarIcon(color: Color, modifier: Modifier = anonymousIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        val strokeWidth = w * 0.075f

        drawCircle(color = color, radius = w * 0.42f, center = center, style = Stroke(width = strokeWidth))
        drawCircle(color = color, radius = w * 0.26f, center = center, style = Stroke(width = strokeWidth * 0.8f))
        drawCircle(color = color, radius = w * 0.07f, center = center)

        val sweep = Path().apply {
            moveTo(center.x, center.y)
            lineTo(w * 0.82f, h * 0.22f)
        }
        drawPath(sweep, color = color, style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round))
    }
}

@Composable
fun BurnIcon(color: Color, modifier: Modifier = anonymousIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.08f

        val flame = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            cubicTo(w * 0.74f, h * 0.34f, w * 0.80f, h * 0.56f, w * 0.68f, h * 0.72f)
            cubicTo(w * 0.58f, h * 0.86f, w * 0.42f, h * 0.86f, w * 0.32f, h * 0.72f)
            cubicTo(w * 0.20f, h * 0.56f, w * 0.28f, h * 0.36f, w * 0.5f, h * 0.14f)
            close()
        }
        drawPath(flame, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))

        val core = Path().apply {
            moveTo(w * 0.5f, h * 0.48f)
            cubicTo(w * 0.62f, h * 0.60f, w * 0.60f, h * 0.72f, w * 0.5f, h * 0.76f)
            cubicTo(w * 0.40f, h * 0.72f, w * 0.38f, h * 0.60f, w * 0.5f, h * 0.48f)
            close()
        }
        drawPath(core, color = color, style = Stroke(width = strokeWidth * 0.7f, join = StrokeJoin.Round))
    }
}
