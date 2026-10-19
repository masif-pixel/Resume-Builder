package com.resume.builder.presentation.components

import androidx.compose.ui.res.stringResource
import com.resume.builder.R
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

class SignatureState {
    val strokes: SnapshotStateList<SnapshotStateList<Offset>> = mutableStateListOf()
    val isEmpty: Boolean get() = strokes.none { it.size > 1 }

    fun clear() = strokes.clear()

    /** Renders the strokes into a tightly cropped, transparent bitmap. */
    fun toBitmap(color: Color = Color(0xFF1B1E3F), strokeWidth: Float = 7f): Bitmap? {
        val points = strokes.flatten()
        if (points.size < 2) return null
        val pad = strokeWidth * 2
        val left = points.minOf { it.x } - pad
        val top = points.minOf { it.y } - pad
        val width = (points.maxOf { it.x } + pad - left).toInt().coerceAtLeast(1)
        val height = (points.maxOf { it.y } + pad - top).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        strokes.filter { it.size > 1 }.forEach { stroke ->
            val path = android.graphics.Path()
            path.moveTo(stroke.first().x - left, stroke.first().y - top)
            for (i in 1 until stroke.size) {
                val prev = stroke[i - 1]
                val point = stroke[i]
                path.quadTo(prev.x - left, prev.y - top, (prev.x + point.x) / 2 - left, (prev.y + point.y) / 2 - top)
            }
            canvas.drawPath(path, paint)
        }
        return bitmap
    }
}

@Composable
fun rememberSignatureState() = remember { SignatureState() }

@Composable
fun SignaturePad(state: SignatureState, modifier: Modifier = Modifier, ink: Color = Color(0xFF1B1E3F)) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier.fillMaxWidth().height(200.dp).clip(shape).background(Color.White).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (state.isEmpty) {
            Text(stringResource(R.string.signature_sign_here), color = Color(0xFFB8BCCB), style = MaterialTheme.typography.headlineSmall)
        }
        Canvas(
            Modifier.matchParentSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> state.strokes.add(mutableStateListOf(offset)) },
                    onDrag = { change, _ ->
                        change.consume()
                        state.strokes.lastOrNull()?.add(change.position)
                    },
                )
            },
        ) {
            val baseline = size.height * 0.75f
            drawLine(Color(0xFFE3E5EE), Offset(24f, baseline), Offset(size.width - 24f, baseline), strokeWidth = 2f)
            state.strokes.forEach { stroke ->
                if (stroke.size < 2) return@forEach
                val path = Path().apply {
                    moveTo(stroke.first().x, stroke.first().y)
                    for (i in 1 until stroke.size) {
                        val prev = stroke[i - 1]
                        val point = stroke[i]
                        quadraticTo(prev.x, prev.y, (prev.x + point.x) / 2, (prev.y + point.y) / 2)
                    }
                }
                drawPath(path, ink, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
