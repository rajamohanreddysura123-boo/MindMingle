package com.rajamohan.mindmingle.presentation.common.icon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn vector icon set for MindMingle. Every icon follows the same convention as the
 * nav-bar icons: proportional coordinates against [size], round stroke caps/joins,
 * single [color] parameter so it always follows the current theme (no baked-in tint).
 */

private fun defaultIconModifier() = Modifier.size(24.dp)

@Composable
fun DeveloperAvatarIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.09f

        // Head
        drawCircle(
            color = color,
            radius = w * 0.17f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.28f),
            style = Stroke(width = strokeWidth)
        )
        // Shoulders
        val shoulders = Path().apply {
            moveTo(w * 0.18f, h * 0.86f)
            cubicTo(w * 0.18f, h * 0.62f, w * 0.34f, h * 0.52f, w * 0.5f, h * 0.52f)
            cubicTo(w * 0.66f, h * 0.52f, w * 0.82f, h * 0.62f, w * 0.82f, h * 0.86f)
        }
        drawPath(shoulders, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
        // Code brackets "< >" on chest
        val bracketW = w * 0.08f
        val bracketStroke = w * 0.055f
        val leftBracket = Path().apply {
            moveTo(w * 0.44f, h * 0.68f)
            lineTo(w * 0.44f - bracketW, h * 0.75f)
            lineTo(w * 0.44f, h * 0.82f)
        }
        val rightBracket = Path().apply {
            moveTo(w * 0.56f, h * 0.68f)
            lineTo(w * 0.56f + bracketW, h * 0.75f)
            lineTo(w * 0.56f, h * 0.82f)
        }
        drawPath(leftBracket, color = color, style = Stroke(width = bracketStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(rightBracket, color = color, style = Stroke(width = bracketStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun TelescopeIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.09f

        val tube = Path().apply {
            moveTo(w * 0.20f, h * 0.30f)
            lineTo(w * 0.78f, h * 0.55f)
            lineTo(w * 0.62f, h * 0.72f)
            lineTo(w * 0.10f, h * 0.44f)
            close()
        }
        drawPath(tube, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))

        val stand = Path().apply {
            moveTo(w * 0.40f, h * 0.68f)
            lineTo(w * 0.24f, h * 0.90f)
            moveTo(w * 0.62f, h * 0.72f)
            lineTo(w * 0.66f, h * 0.90f)
        }
        drawPath(stand, color = color, style = Stroke(width = strokeWidth * 0.8f, cap = StrokeCap.Round))

        drawCircle(color = color, radius = w * 0.045f, center = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.55f))
    }
}

@Composable
fun CrossIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.14f
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.2f), androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.8f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.2f), androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.8f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun ChatBubbleIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.09f

        val bubble = Path().apply {
            moveTo(w * 0.18f, h * 0.22f)
            lineTo(w * 0.82f, h * 0.22f)
            cubicTo(w * 0.88f, h * 0.22f, w * 0.90f, h * 0.26f, w * 0.90f, h * 0.32f)
            lineTo(w * 0.90f, h * 0.62f)
            cubicTo(w * 0.90f, h * 0.68f, w * 0.88f, h * 0.72f, w * 0.82f, h * 0.72f)
            lineTo(w * 0.42f, h * 0.72f)
            lineTo(w * 0.28f, h * 0.88f)
            lineTo(w * 0.30f, h * 0.72f)
            lineTo(w * 0.18f, h * 0.72f)
            cubicTo(w * 0.12f, h * 0.72f, w * 0.10f, h * 0.68f, w * 0.10f, h * 0.62f)
            lineTo(w * 0.10f, h * 0.32f)
            cubicTo(w * 0.10f, h * 0.26f, w * 0.12f, h * 0.22f, w * 0.18f, h * 0.22f)
            close()
        }
        drawPath(bubble, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
    }
}

@Composable
fun SparkleBurstIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        fun star(centerX: Float, centerY: Float, scale: Float): Path = Path().apply {
            moveTo(centerX, centerY - 0.30f * scale)
            lineTo(centerX + 0.08f * scale, centerY - 0.08f * scale)
            lineTo(centerX + 0.30f * scale, centerY)
            lineTo(centerX + 0.08f * scale, centerY + 0.08f * scale)
            lineTo(centerX, centerY + 0.30f * scale)
            lineTo(centerX - 0.08f * scale, centerY + 0.08f * scale)
            lineTo(centerX - 0.30f * scale, centerY)
            lineTo(centerX - 0.08f * scale, centerY - 0.08f * scale)
            close()
        }

        drawPath(star(cx, cy, w), color = color)
        drawCircle(color = color, radius = w * 0.06f, center = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.24f))
        drawCircle(color = color, radius = w * 0.045f, center = androidx.compose.ui.geometry.Offset(w * 0.80f, h * 0.72f))
    }
}

@Composable
fun HeartIcon(color: Color, modifier: Modifier = defaultIconModifier(), filled: Boolean = true) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val heartPath = Path().apply {
            moveTo(w * 0.5f, h * 0.82f)
            cubicTo(w * 0.15f, h * 0.56f, w * 0.08f, h * 0.32f, w * 0.26f, h * 0.18f)
            cubicTo(w * 0.40f, h * 0.08f, w * 0.50f, h * 0.25f, w * 0.50f, h * 0.28f)
            cubicTo(w * 0.50f, h * 0.25f, w * 0.60f, h * 0.08f, w * 0.74f, h * 0.18f)
            cubicTo(w * 0.92f, h * 0.32f, w * 0.85f, h * 0.56f, w * 0.50f, h * 0.82f)
            close()
        }
        if (filled) {
            drawPath(heartPath, color = color)
        } else {
            drawPath(heartPath, color = color, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun FlameIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val flame = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            cubicTo(w * 0.66f, h * 0.30f, w * 0.78f, h * 0.42f, w * 0.72f, h * 0.62f)
            cubicTo(w * 0.68f, h * 0.76f, w * 0.56f, h * 0.84f, w * 0.5f, h * 0.90f)
            cubicTo(w * 0.44f, h * 0.84f, w * 0.32f, h * 0.76f, w * 0.28f, h * 0.62f)
            cubicTo(w * 0.22f, h * 0.42f, w * 0.34f, h * 0.30f, w * 0.5f, h * 0.08f)
            close()
        }
        drawPath(flame, color = color)
    }
}

@Composable
fun BoltIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bolt = Path().apply {
            moveTo(w * 0.56f, h * 0.06f)
            lineTo(w * 0.22f, h * 0.58f)
            lineTo(w * 0.46f, h * 0.58f)
            lineTo(w * 0.40f, h * 0.94f)
            lineTo(w * 0.80f, h * 0.38f)
            lineTo(w * 0.54f, h * 0.38f)
            close()
        }
        drawPath(bolt, color = color)
    }
}

@Composable
fun GearIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val outerR = w * 0.36f
        val innerR = w * 0.20f
        val toothLen = w * 0.10f
        val strokeWidth = w * 0.08f

        for (i in 0 until 8) {
            val angle = (i * 45f) * (kotlin.math.PI / 180f)
            val x1 = cx + (outerR * kotlin.math.cos(angle)).toFloat()
            val y1 = cy + (outerR * kotlin.math.sin(angle)).toFloat()
            val x2 = cx + ((outerR + toothLen) * kotlin.math.cos(angle)).toFloat()
            val y2 = cy + ((outerR + toothLen) * kotlin.math.sin(angle)).toFloat()
            drawLine(color, androidx.compose.ui.geometry.Offset(x1, y1), androidx.compose.ui.geometry.Offset(x2, y2), strokeWidth, cap = StrokeCap.Round)
        }
        drawCircle(color = color, radius = outerR, center = androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(width = strokeWidth))
        drawCircle(color = color, radius = innerR * 0.35f, center = androidx.compose.ui.geometry.Offset(cx, cy))
    }
}

@Composable
fun PencilIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val body = Path().apply {
            moveTo(w * 0.22f, h * 0.78f)
            lineTo(w * 0.62f, h * 0.18f)
            lineTo(w * 0.82f, h * 0.30f)
            lineTo(w * 0.42f, h * 0.90f)
            lineTo(w * 0.16f, h * 0.90f)
            close()
        }
        drawPath(body, color = color, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round, cap = StrokeCap.Round))
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.54f, h * 0.30f), androidx.compose.ui.geometry.Offset(w * 0.74f, h * 0.42f), w * 0.07f, cap = StrokeCap.Round)
    }
}

@Composable
fun CheckIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.55f)
            lineTo(size.width * 0.42f, size.height * 0.8f)
            lineTo(size.width * 0.88f, size.height * 0.22f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = size.width * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun CheckBadgeIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = color, radius = w * 0.42f, style = Stroke(width = w * 0.09f))
        val check = Path().apply {
            moveTo(w * 0.30f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.66f)
            lineTo(w * 0.72f, h * 0.36f)
        }
        drawPath(check, color = color, style = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CrownIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val crown = Path().apply {
            moveTo(w * 0.14f, h * 0.78f)
            lineTo(w * 0.10f, h * 0.36f)
            lineTo(w * 0.32f, h * 0.52f)
            lineTo(w * 0.5f, h * 0.20f)
            lineTo(w * 0.68f, h * 0.52f)
            lineTo(w * 0.90f, h * 0.36f)
            lineTo(w * 0.86f, h * 0.78f)
            close()
        }
        drawPath(crown, color = color, style = Stroke(width = w * 0.07f, join = StrokeJoin.Round, cap = StrokeCap.Round))
        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.14f, h * 0.78f), androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.78f), w * 0.07f, cap = StrokeCap.Round)
    }
}

@Composable
fun LogoutIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.09f

        val door = Path().apply {
            moveTo(w * 0.52f, h * 0.15f)
            lineTo(w * 0.24f, h * 0.15f)
            lineTo(w * 0.24f, h * 0.85f)
            lineTo(w * 0.52f, h * 0.85f)
        }
        drawPath(door, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.5f), androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.5f), strokeWidth, cap = StrokeCap.Round)
        val arrowHead = Path().apply {
            moveTo(w * 0.68f, h * 0.34f)
            lineTo(w * 0.86f, h * 0.5f)
            lineTo(w * 0.68f, h * 0.66f)
        }
        drawPath(arrowHead, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun EnvelopeIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.08f
        val body = Path().apply {
            moveTo(w * 0.12f, h * 0.26f)
            lineTo(w * 0.88f, h * 0.26f)
            lineTo(w * 0.88f, h * 0.74f)
            lineTo(w * 0.12f, h * 0.74f)
            close()
        }
        drawPath(body, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
        val flap = Path().apply {
            moveTo(w * 0.12f, h * 0.28f)
            lineTo(w * 0.5f, h * 0.56f)
            lineTo(w * 0.88f, h * 0.28f)
        }
        drawPath(flap, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun PersonIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.09f
        drawCircle(color = color, radius = w * 0.17f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.30f), style = Stroke(width = strokeWidth))
        val shoulders = Path().apply {
            moveTo(w * 0.20f, h * 0.86f)
            cubicTo(w * 0.20f, h * 0.62f, w * 0.35f, h * 0.55f, w * 0.5f, h * 0.55f)
            cubicTo(w * 0.65f, h * 0.55f, w * 0.80f, h * 0.62f, w * 0.80f, h * 0.86f)
        }
        drawPath(shoulders, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    }
}

@Composable
fun ShieldIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.86f, h * 0.22f)
            lineTo(w * 0.86f, h * 0.52f)
            cubicTo(w * 0.86f, h * 0.74f, w * 0.70f, h * 0.88f, w * 0.5f, h * 0.94f)
            cubicTo(w * 0.30f, h * 0.88f, w * 0.14f, h * 0.74f, w * 0.14f, h * 0.52f)
            lineTo(w * 0.14f, h * 0.22f)
            close()
        }
        drawPath(shield, color = color, style = Stroke(width = w * 0.08f, join = StrokeJoin.Round))
    }
}

@Composable
fun BellIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.08f
        val bell = Path().apply {
            moveTo(w * 0.24f, h * 0.66f)
            cubicTo(w * 0.24f, h * 0.40f, w * 0.34f, h * 0.20f, w * 0.5f, h * 0.20f)
            cubicTo(w * 0.66f, h * 0.20f, w * 0.76f, h * 0.40f, w * 0.76f, h * 0.66f)
            lineTo(w * 0.86f, h * 0.78f)
            lineTo(w * 0.14f, h * 0.78f)
            close()
        }
        drawPath(bell, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round, cap = StrokeCap.Round))
        drawArc(
            color = color,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.80f),
            size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.16f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun HelpCircleIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(color = color, radius = w * 0.42f, style = Stroke(width = w * 0.08f))
        val hook = Path().apply {
            moveTo(w * 0.38f, h * 0.38f)
            cubicTo(w * 0.38f, h * 0.28f, w * 0.62f, h * 0.28f, w * 0.62f, h * 0.42f)
            cubicTo(w * 0.62f, h * 0.52f, w * 0.5f, h * 0.52f, w * 0.5f, h * 0.62f)
        }
        drawPath(hook, color = color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round))
        drawCircle(color = color, radius = w * 0.045f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.76f))
    }
}

@Composable
fun LockIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.08f
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.14f),
            size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.42f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        val body = Path().apply {
            moveTo(w * 0.20f, h * 0.42f)
            lineTo(w * 0.80f, h * 0.42f)
            lineTo(w * 0.80f, h * 0.88f)
            lineTo(w * 0.20f, h * 0.88f)
            close()
        }
        drawPath(body, color = color, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
        drawCircle(color = color, radius = w * 0.05f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.62f))
    }
}

@Composable
fun WaveIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val palm = Path().apply {
            moveTo(w * 0.30f, h * 0.85f)
            lineTo(w * 0.28f, h * 0.45f)
            cubicTo(w * 0.28f, h * 0.36f, w * 0.40f, h * 0.36f, w * 0.40f, h * 0.45f)
            lineTo(w * 0.40f, h * 0.30f)
            cubicTo(w * 0.40f, h * 0.21f, w * 0.52f, h * 0.21f, w * 0.52f, h * 0.30f)
            lineTo(w * 0.52f, h * 0.42f)
            cubicTo(w * 0.52f, h * 0.16f, w * 0.66f, h * 0.16f, w * 0.66f, h * 0.42f)
            lineTo(w * 0.66f, h * 0.50f)
            cubicTo(w * 0.66f, h * 0.28f, w * 0.78f, h * 0.30f, w * 0.78f, h * 0.50f)
            lineTo(w * 0.78f, h * 0.66f)
            cubicTo(w * 0.78f, h * 0.78f, w * 0.72f, h * 0.85f, w * 0.64f, h * 0.85f)
            close()
        }
        drawPath(palm, color = color, style = Stroke(width = w * 0.055f, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

@Composable
fun ChevronDownIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.8f, h * 0.35f)
        }
        drawPath(path, color = color, style = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ArrowForwardIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.13f
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.5f)
            lineTo(w * 0.85f, h * 0.5f)
            moveTo(w * 0.55f, h * 0.22f)
            lineTo(w * 0.85f, h * 0.5f)
            lineTo(w * 0.55f, h * 0.78f)
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ExternalLinkIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.11f
        val box = Path().apply {
            moveTo(w * 0.20f, h * 0.35f)
            lineTo(w * 0.20f, h * 0.80f)
            lineTo(w * 0.65f, h * 0.80f)
            lineTo(w * 0.65f, h * 0.55f)
        }
        drawPath(box, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val arrow = Path().apply {
            moveTo(w * 0.45f, h * 0.55f)
            lineTo(w * 0.82f, h * 0.18f)
            moveTo(w * 0.52f, h * 0.16f)
            lineTo(w * 0.82f, h * 0.16f)
            lineTo(w * 0.82f, h * 0.46f)
        }
        drawPath(arrow, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun BackArrowIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.12f
        val path = Path().apply {
            moveTo(w * 0.85f, h * 0.5f)
            lineTo(w * 0.15f, h * 0.5f)
            moveTo(w * 0.45f, h * 0.15f)
            lineTo(w * 0.15f, h * 0.5f)
            lineTo(w * 0.45f, h * 0.85f)
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SendIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.08f, h * 0.5f)
            lineTo(w * 0.90f, h * 0.1f)
            lineTo(w * 0.62f, h * 0.5f)
            lineTo(w * 0.90f, h * 0.9f)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
fun EnterKeyIcon(color: Color, modifier: Modifier = defaultIconModifier()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.12f
        val path = Path().apply {
            moveTo(w * 0.82f, h * 0.22f)
            lineTo(w * 0.82f, h * 0.62f)
            lineTo(w * 0.24f, h * 0.62f)
            moveTo(w * 0.44f, h * 0.42f)
            lineTo(w * 0.20f, h * 0.62f)
            lineTo(w * 0.44f, h * 0.82f)
        }
        drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
