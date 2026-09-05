package com.ultracam.app.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultracam.app.ui.theme.PixelYellow
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * Pixel Camera Style Smooth Arc Zoom Dial:
 * - High-performance hardware native Canvas rendering (zero GC allocations on draw frames)
 * - Material 3 Expressive spring physics
 * - Smooth gesture drag scrubbing
 */
@Composable
fun ZoomWheel(
    zoom: Float,
    minZoom: Float,
    maxZoom: Float,
    visible: Boolean,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedZoom by animateFloatAsState(
        targetValue = zoom,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f),
        label = "animatedZoom"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.88f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)),
        exit = fadeOut(tween(200, easing = FastOutSlowInEasing)) +
               scaleOut(targetScale = 0.88f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color(0xE6141416))
                .pointerInput(minZoom, maxZoom, animatedZoom) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val logMin = ln(minZoom.coerceAtLeast(0.5f))
                        val logMax = ln(maxZoom.coerceAtLeast(10f))
                        val currentLog = ln(animatedZoom.coerceIn(minZoom, maxZoom))
                        val logDelta = -dragAmount * 0.0055f
                        val nextLog = (currentLog + logDelta).coerceIn(logMin, logMax)
                        onZoomChange(exp(nextLog))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val cx = size.width / 2f
                val arcRadius = size.width * 0.82f
                val cy = arcRadius + 22.dp.toPx()

                // ---- Top Yellow Pointer Triangle ----
                val trianglePath = Path().apply {
                    moveTo(cx, 10.dp.toPx())
                    lineTo(cx - 7.dp.toPx(), 2.dp.toPx())
                    lineTo(cx + 7.dp.toPx(), 2.dp.toPx())
                    close()
                }
                drawPath(trianglePath, color = PixelYellow)

                // ---- Dial Arc Track ----
                drawArc(
                    color = Color.White.copy(alpha = 0.12f),
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - arcRadius, cy - arcRadius),
                    size = Size(arcRadius * 2f, arcRadius * 2f),
                    style = Stroke(width = 40.dp.toPx())
                )

                // ---- Preset Labels & Ticks ----
                val logMin = ln(minZoom.coerceAtLeast(0.5f))
                val logMax = ln(maxZoom.coerceAtLeast(10f))
                val currentLog = ln(animatedZoom.coerceIn(minZoom, maxZoom))

                val presetLabels = listOf(
                    0.5f to ("0.5" to "13 MM"),
                    1.0f to ("1" to "26 MM"),
                    2.0f to ("2" to "52 MM"),
                    3.0f to ("3" to "77 MM"),
                    5.0f to ("5" to "130 MM"),
                    10.0f to ("10" to "260 MM"),
                    15.0f to ("15" to "380 MM")
                ).filter { it.first >= minZoom - 0.1f && it.first <= maxZoom + 0.1f }

                // High-performance Native Android Paint (Zero allocation overhead during draw)
                val textPaint = Paint().apply {
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }

                val mmPaint = Paint().apply {
                    isAntiAlias = true
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                    textSize = 9.sp.toPx()
                }

                val totalTicks = 70
                for (i in 0..totalTicks) {
                    val tickFraction = i.toFloat() / totalTicks
                    val tickLog = logMin + tickFraction * (logMax - logMin)

                    val angleOffsetDeg = (tickLog - currentLog) * 44f
                    if (angleOffsetDeg < -60f || angleOffsetDeg > 60f) continue

                    val angleRad = Math.toRadians((270f + angleOffsetDeg).toDouble())
                    val isMajor = presetLabels.any { abs(ln(it.first) - tickLog) < 0.05f }
                    val tickLen = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
                    val strokeW = if (isMajor) 2.2f else 1.2f
                    val alpha = (1f - (abs(angleOffsetDeg) / 60f)).coerceIn(0.1f, 1f)

                    val startX = (cx + (arcRadius - tickLen / 2f) * cos(angleRad)).toFloat()
                    val startY = (cy + (arcRadius - tickLen / 2f) * sin(angleRad)).toFloat()
                    val endX = (cx + (arcRadius + tickLen / 2f) * cos(angleRad)).toFloat()
                    val endY = (cy + (arcRadius + tickLen / 2f) * sin(angleRad)).toFloat()

                    drawLine(
                        color = Color.White.copy(alpha = alpha * 0.7f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeW
                    )
                }

                // ---- Preset Labels ----
                presetLabels.forEach { (presetZoom, labels) ->
                    val presetLog = ln(presetZoom)
                    val angleOffsetDeg = (presetLog - currentLog) * 44f
                    if (angleOffsetDeg in -55f..55f) {
                        val angleRad = Math.toRadians((270f + angleOffsetDeg).toDouble())
                        val textR = arcRadius - 30.dp.toPx()
                        val tx = (cx + textR * cos(angleRad)).toFloat()
                        val ty = (cy + textR * sin(angleRad)).toFloat()
                        val alpha = (1f - (abs(angleOffsetDeg) / 55f)).coerceIn(0.2f, 1f)

                        val isCurrent = abs(animatedZoom - presetZoom) < 0.1f

                        // Draw Number
                        textPaint.textSize = 13.sp.toPx()
                        textPaint.color = if (isCurrent) PixelYellow.toArgb() else Color.White.copy(alpha = alpha).toArgb()
                        drawContext.canvas.nativeCanvas.drawText(
                            labels.first,
                            tx,
                            ty + 4.dp.toPx(),
                            textPaint
                        )

                        // Draw MM Focal Length
                        mmPaint.color = Color.White.copy(alpha = alpha * 0.5f).toArgb()
                        drawContext.canvas.nativeCanvas.drawText(
                            labels.second,
                            tx,
                            ty + 18.dp.toPx(),
                            mmPaint
                        )
                    }
                }
            }

            // ---- Header Zoom Value Readout ----
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f×", animatedZoom),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PixelYellow,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}
