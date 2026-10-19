package com.resume.builder.presentation.components

import androidx.compose.ui.res.stringResource
import com.resume.builder.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

val ResumeSwatches: List<Long> = listOf(
    0xFF2F3A4F, 0xFF111827, 0xFF1E3A8A, 0xFF1D4ED8, 0xFF3B82F6, 0xFF0F8B8D, 0xFF0B4F5C, 0xFF3F9A6E,
    0xFF15803D, 0xFF65A30D, 0xFFE7A93B, 0xFFEA580C, 0xFFDC2626, 0xFFBE185D, 0xFF9B51E0, 0xFF6D28D9,
    0xFF6B7280, 0xFFF1F4FB, 0xFFF6F1FD, 0xFFFFFFFF,
)

@Composable
fun SwatchRow(
    selected: Long?,
    onSelect: (Long) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                .clickable(onClick = onCustom),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Colorize, contentDescription = stringResource(R.string.color_custom), tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
        ResumeSwatches.forEach { argb ->
            val color = Color(argb)
            val isSelected = selected == argb
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color)
                    .border(if (isSelected) 3.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Icon(Icons.Rounded.Check, null, tint = if (color.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** HSV wheel + brightness slider + hex input. */
@Composable
fun ColorPickerDialog(
    initial: Color,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2].coerceAtLeast(0.05f)) }
    val current = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    var hexText by remember { mutableStateOf(current.toHex()) }

    fun update(h: Float = hue, s: Float = saturation, v: Float = value) {
        hue = h; saturation = s; value = v
        hexText = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))).toHex()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(
                    Modifier.size(220.dp)
                        .pointerInput(Unit) {
                            fun pick(offset: Offset) {
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val radius = min(size.width, size.height) / 2f
                                val dx = offset.x - c.x
                                val dy = offset.y - c.y
                                val angle = (Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f
                                update(h = angle, s = (hypot(dx, dy) / radius).coerceIn(0f, 1f))
                            }
                            detectTapGestures { pick(it) }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val radius = min(size.width, size.height) / 2f
                                val dx = change.position.x - c.x
                                val dy = change.position.y - c.y
                                val angle = (Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f
                                update(h = angle, s = (hypot(dx, dy) / radius).coerceIn(0f, 1f))
                            }
                        },
                ) {
                    val radius = size.minDimension / 2
                    drawCircle(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)), radius)
                    drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, radius), radius)
                    drawCircle(Color.Black.copy(alpha = 1f - value), radius)
                    val rad = Math.toRadians(hue.toDouble())
                    val knob = Offset(center.x + cos(rad).toFloat() * saturation * radius, center.y + sin(rad).toFloat() * saturation * radius)
                    drawCircle(current, 12.dp.toPx(), knob)
                    drawCircle(Color.White, 12.dp.toPx(), knob, style = Stroke(3.dp.toPx()))
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.color_brightness), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(12.dp))
                    CircleSlider(value = value, onValueChange = { update(v = it) }, valueRange = 0.05f..1f, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(current).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)))
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { text ->
                            hexText = text
                            parseHex(text)?.let { parsed ->
                                val out = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsed.toArgb(), out)
                                hue = out[0]; saturation = out[1]; value = out[2].coerceAtLeast(0.05f)
                            }
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.color_hex)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(current) }) { Text(stringResource(R.string.common_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

fun Color.toHex(): String = "#%06X".format(0xFFFFFF and toArgb())

fun parseHex(text: String): Color? {
    val clean = text.trim().removePrefix("#")
    return when (clean.length) {
        6 -> clean.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
        8 -> clean.toLongOrNull(16)?.let { Color(it) }
        else -> null
    }
}
