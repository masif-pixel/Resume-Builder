package com.resume.builder.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.resume.builder.ui.theme.Amber
import com.resume.builder.ui.theme.Indigo
import com.resume.builder.ui.theme.Magenta
import com.resume.builder.ui.theme.Violet
import kotlin.math.cos
import kotlin.math.sin

/** App mark drawn in code so it scales crisply everywhere. */
@Composable
fun BrandLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFFA21CAF)), Offset.Zero, Offset(s, s)),
            cornerRadius = CornerRadius(s * 0.28f),
        )
        val page = Path().apply {
            moveTo(s * 0.30f, s * 0.20f)
            lineTo(s * 0.58f, s * 0.20f)
            lineTo(s * 0.72f, s * 0.34f)
            lineTo(s * 0.72f, s * 0.78f)
            lineTo(s * 0.30f, s * 0.78f)
            close()
        }
        drawPath(page, Color.White)
        drawPath(Path().apply { moveTo(s * 0.58f, s * 0.20f); lineTo(s * 0.58f, s * 0.34f); lineTo(s * 0.72f, s * 0.34f); close() }, Color(0xFFC7D2FE))
        drawCircle(Violet, radius = s * 0.065f, center = Offset(s * 0.40f, s * 0.38f))
        val line = Color(0xFFA5B4FC)
        drawRoundRect(line, Offset(s * 0.50f, s * 0.34f), Size(s * 0.14f, s * 0.03f), CornerRadius(4f))
        drawRoundRect(line, Offset(s * 0.50f, s * 0.40f), Size(s * 0.09f, s * 0.03f), CornerRadius(4f))
        listOf(0.52f, 0.60f, 0.68f).forEachIndexed { i, y ->
            drawRoundRect(line, Offset(s * 0.36f, s * y), Size(s * (if (i == 2) 0.2f else 0.3f), s * 0.03f), CornerRadius(4f))
        }
        sparkle(Offset(s * 0.78f, s * 0.76f), s * 0.09f, Color(0xFFFDE68A))
    }
}

private fun DrawScope.sparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}

@Composable
fun ResumeStackIllustration(modifier: Modifier = Modifier, size: Dp = 200.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val soft = MaterialTheme.colorScheme.primaryContainer
    val lines = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawCircle(soft.copy(alpha = 0.6f), radius = s * 0.42f, center = center)
        rotate(-10f, pivot = center) {
            drawRoundRect(surface, Offset(s * 0.22f, s * 0.2f), Size(s * 0.5f, s * 0.62f), CornerRadius(s * 0.04f))
        }
        rotate(6f, pivot = center) {
            drawRoundRect(primary.copy(alpha = 0.18f), Offset(s * 0.3f, s * 0.16f), Size(s * 0.5f, s * 0.64f), CornerRadius(s * 0.04f))
        }
        val left = s * 0.26f
        val top = s * 0.18f
        drawRoundRect(surface, Offset(left, top), Size(s * 0.48f, s * 0.64f), CornerRadius(s * 0.04f))
        drawRoundRect(primary, Offset(left, top), Size(s * 0.16f, s * 0.64f), CornerRadius(s * 0.04f))
        drawCircle(Color.White, radius = s * 0.045f, center = Offset(left + s * 0.08f, top + s * 0.09f))
        repeat(4) { i ->
            drawRoundRect(Color.White.copy(alpha = 0.7f), Offset(left + s * 0.035f, top + s * (0.2f + i * 0.06f)), Size(s * 0.09f, s * 0.018f), CornerRadius(4f))
        }
        drawRoundRect(primary, Offset(left + s * 0.2f, top + s * 0.05f), Size(s * 0.2f, s * 0.035f), CornerRadius(6f))
        repeat(7) { i ->
            val w = if (i % 3 == 2) 0.15f else 0.24f
            drawRoundRect(lines, Offset(left + s * 0.2f, top + s * (0.13f + i * 0.065f)), Size(s * w, s * 0.02f), CornerRadius(6f))
        }
        sparkle(Offset(s * 0.8f, s * 0.26f), s * 0.05f, Amber)
        sparkle(Offset(s * 0.18f, s * 0.78f), s * 0.035f, Magenta)
    }
}

@Composable
fun LetterIllustration(modifier: Modifier = Modifier, size: Dp = 200.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val soft = MaterialTheme.colorScheme.tertiaryContainer
    val lines = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawCircle(soft.copy(alpha = 0.7f), radius = s * 0.42f, center = center)
        drawRoundRect(primary.copy(alpha = 0.85f), Offset(s * 0.16f, s * 0.44f), Size(s * 0.68f, s * 0.38f), CornerRadius(s * 0.04f))
        val letterTop = s * 0.18f
        drawRoundRect(surface, Offset(s * 0.24f, letterTop), Size(s * 0.52f, s * 0.5f), CornerRadius(s * 0.03f))
        drawRoundRect(primary, Offset(s * 0.3f, letterTop + s * 0.06f), Size(s * 0.18f, s * 0.035f), CornerRadius(6f))
        repeat(5) { i ->
            drawRoundRect(lines, Offset(s * 0.3f, letterTop + s * (0.14f + i * 0.06f)), Size(s * (if (i == 4) 0.22f else 0.4f), s * 0.02f), CornerRadius(6f))
        }
        val flap = Path().apply {
            moveTo(s * 0.16f, s * 0.54f)
            lineTo(s * 0.5f, s * 0.72f)
            lineTo(s * 0.84f, s * 0.54f)
            lineTo(s * 0.84f, s * 0.82f)
            lineTo(s * 0.16f, s * 0.82f)
            close()
        }
        drawPath(flap, Brush.linearGradient(listOf(Indigo, Violet), Offset(0f, s * 0.5f), Offset(s, s)))
        sparkle(Offset(s * 0.82f, s * 0.22f), s * 0.05f, Amber)
    }
}

@Composable
fun FolderIllustration(modifier: Modifier = Modifier, size: Dp = 190.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.secondaryContainer
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawCircle(soft.copy(alpha = 0.7f), radius = s * 0.42f, center = center)
        drawRoundRect(primary.copy(alpha = 0.55f), Offset(s * 0.18f, s * 0.3f), Size(s * 0.64f, s * 0.46f), CornerRadius(s * 0.05f))
        drawRoundRect(Color.White, Offset(s * 0.28f, s * 0.24f), Size(s * 0.36f, s * 0.4f), CornerRadius(s * 0.03f))
        drawRoundRect(Color(0xFFFCA5A5), Offset(s * 0.34f, s * 0.3f), Size(s * 0.12f, s * 0.08f), CornerRadius(6f))
        val front = Path().apply {
            moveTo(s * 0.14f, s * 0.44f)
            lineTo(s * 0.86f, s * 0.44f)
            lineTo(s * 0.8f, s * 0.8f)
            lineTo(s * 0.2f, s * 0.8f)
            close()
        }
        drawPath(front, Brush.linearGradient(listOf(Indigo, Violet), Offset(0f, s * 0.4f), Offset(s, s)))
        drawRoundRect(Color.White.copy(alpha = 0.6f), Offset(s * 0.42f, s * 0.56f), Size(s * 0.16f, s * 0.03f), CornerRadius(6f))
    }
}

/** Orbiting dots used while the AI assistant is working. */
@Composable
fun AiOrb(modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val transition = rememberInfiniteTransition(label = "orb")
    val angle by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "angle")
    val pulse by transition.animateFloat(0.85f, 1.1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = this.size.minDimension
            drawCircle(Brush.radialGradient(listOf(primary.copy(alpha = 0.35f), Color.Transparent)), radius = s * 0.36f * pulse)
            drawCircle(primary.copy(alpha = 0.25f), radius = s * 0.46f, style = Stroke(width = 2.dp.toPx()))
            drawCircle(primary.copy(alpha = 0.15f), radius = s * 0.32f, style = Stroke(width = 2.dp.toPx()))
            repeat(8) { i ->
                val a = Math.toRadians((angle + i * 45f).toDouble())
                drawCircle(primary, radius = s * 0.025f, center = Offset(center.x + cos(a).toFloat() * s * 0.46f, center.y + sin(a).toFloat() * s * 0.46f))
                val b = Math.toRadians((-angle * 1.4f + i * 45f + 22f).toDouble())
                drawCircle(Violet.copy(alpha = 0.7f), radius = s * 0.02f, center = Offset(center.x + cos(b).toFloat() * s * 0.32f, center.y + sin(b).toFloat() * s * 0.32f))
            }
        }
        Canvas(Modifier.size(size * 0.3f).rotate(angle / 2)) {
            val s = this.size.minDimension
            drawCircle(Brush.linearGradient(listOf(Indigo, Magenta)), radius = s * 0.5f * pulse)
            sparkle(center, s * 0.3f, Color.White)
        }
    }
}
