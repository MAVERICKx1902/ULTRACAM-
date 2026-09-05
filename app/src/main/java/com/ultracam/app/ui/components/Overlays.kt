package com.ultracam.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultracam.app.camera.CameraMath
import com.ultracam.app.camera.HistogramData
import com.ultracam.app.sensors.Attitude
import com.ultracam.app.ui.FocusFx
import com.ultracam.app.ui.FocusPhase
import com.ultracam.app.ui.GridMode
import com.ultracam.app.ui.glass.FocusRingCanvas
import com.ultracam.app.ui.glass.GlassPanel
import com.ultracam.app.ui.theme.PrismAmber
import com.ultracam.app.ui.theme.PrismCyan
import com.ultracam.app.ui.theme.PrismGreen
import com.ultracam.app.ui.theme.PrismMagenta
import com.ultracam.app.ui.theme.PrismRed
import com.ultracam.app.ui.theme.PrismViolet
import com.ultracam.app.ui.theme.ReadoutTextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// ------------------------------------------------------------------ grid

@Composable
fun GridOverlay(mode: GridMode, modifier: Modifier = Modifier) {
    if (mode == GridMode.OFF) return
    Canvas(modifier) {
        val positions = if (mode == GridMode.THIRDS) {
            listOf(1f / 3f, 2f / 3f)
        } else {
            listOf(0.382f, 0.618f)
        }
        positions.forEach { p ->
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(size.width * p, 0f),
                end = Offset(size.width * p, size.height),
                strokeWidth = 1.2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(0f, size.height * p),
                end = Offset(size.width, size.height * p),
                strokeWidth = 1.2f
            )
        }
        val c = Offset(size.width / 2f, size.height / 2f)
        val arm = 10.dp.toPx()
        drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x - arm, c.y), Offset(c.x - arm / 3, c.y), 2f)
        drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x + arm / 3, c.y), Offset(c.x + arm, c.y), 2f)
        drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x, c.y - arm), Offset(c.x, c.y - arm / 3), 2f)
        drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x, c.y + arm / 3), Offset(c.x, c.y + arm), 2f)
    }
}

// ------------------------------------------------------------------ level

@Composable
fun LevelOverlay(
    attitude: Attitude,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) return
    Canvas(modifier) {
        val pitch = attitude.pitchDeg
        val roll = attitude.rollDeg
        val isLevel = abs(pitch) < 0.8f && abs(roll) < 0.8f
        val color = if (isLevel) PrismGreen else Color.White.copy(alpha = 0.62f)
        rotate(degrees = -roll, pivot = center) {
            val y = center.y + pitch * 4.dp.toPx()
            val gap = 26.dp.toPx()
            val stroke = 2.dp.toPx()
            drawLine(
                color = color,
                start = Offset(size.width * 0.10f, y),
                end = Offset(center.x - gap, y),
                strokeWidth = stroke
            )
            drawLine(
                color = color,
                start = Offset(center.x + gap, y),
                end = Offset(size.width * 0.90f, y),
                strokeWidth = stroke
            )
            if (isLevel) {
                drawLine(
                    color = color.copy(alpha = 0.6f),
                    start = Offset(center.x - gap, y - 8.dp.toPx()),
                    end = Offset(center.x - gap, y + 8.dp.toPx()),
                    strokeWidth = stroke * 0.8f
                )
                drawLine(
                    color = color.copy(alpha = 0.6f),
                    start = Offset(center.x + gap, y - 8.dp.toPx()),
                    end = Offset(center.x + gap, y + 8.dp.toPx()),
                    strokeWidth = stroke * 0.8f
                )
            }
        }
    }
}

// ------------------------------------------------------------------ histogram

@Composable
fun HistogramPanel(
    data: HistogramData?,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier, shape = RoundedCornerShape(16.dp)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(7.dp)
        ) {
            val d = data ?: run {
                drawLine(
                    Color.White.copy(alpha = 0.15f),
                    Offset(0f, size.height - 1f),
                    Offset(size.width, size.height - 1f),
                    1.5f
                )
                return@Canvas
            }
            val bins = 48
            val step = 256 / bins
            val values = FloatArray(bins) { i ->
                var sum = 0
                for (k in 0 until step) sum += d.bins[i * step + k]
                sum / step.toFloat()
            }
            var maxV = 1f
            values.forEach { if (it > maxV) maxV = it }
            val barW = size.width / bins
            for (i in 0 until bins) {
                val v = CameraMath.sqrtNorm(values[i], maxV)
                if (v <= 0f) continue
                val h = v * size.height
                val clippedHigh = (i * step) >= 248 && d.clipHighPct > 0.5f
                val clippedLow = (i * step) <= 7 && d.clipLowPct > 0.5f
                val x = i * barW
                val frac = i.toFloat() / bins
                val color = when {
                    clippedHigh -> PrismRed
                    clippedLow -> PrismAmber
                    frac < 0.5f -> colorLerp(PrismCyan, PrismViolet, frac * 2f)
                    else -> colorLerp(PrismViolet, PrismMagenta, (frac - 0.5f) * 2f)
                }
                drawRect(
                    color = color.copy(alpha = 0.9f),
                    topLeft = Offset(x + 0.6f, size.height - h),
                    size = androidx.compose.ui.geometry.Size(barW - 1.2f, h)
                )
            }
            drawLine(
                Color.White.copy(alpha = 0.22f),
                Offset(0f, size.height - 0.5f),
                Offset(size.width, size.height - 0.5f),
                1.2f
            )
        }
    }
}

private fun colorLerp(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f
)

// ------------------------------------------------------------------ peaking

@Composable
fun PeakingLayer(
    bitmap: android.graphics.Bitmap?,
    tick: Int,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) return
    key(tick) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            alpha = 0.85f
        )
    }
}

// ------------------------------------------------------------------ focus reticle

@Composable
fun FocusReticleLayer(
    events: SharedFlow<FocusFx>,
    modifier: Modifier = Modifier
) {
    var fx by remember { mutableStateOf<FocusFx?>(null) }
    LaunchedEffect(Unit) {
        events.collect { fx = it }
    }
    val current = fx ?: return

    val scale = remember { Animatable(1.45f) }
    val reticleAlpha = remember { Animatable(0f) }
    val transition = rememberInfiniteTransition(label = "focusSpin")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "focusSpinPhase"
    )

    LaunchedEffect(current.id, current.phase) {
        when (current.phase) {
            FocusPhase.START -> {
                reticleAlpha.snapTo(1f)
                scale.snapTo(1.45f)
                scale.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
            }
            FocusPhase.LOCKED, FocusPhase.FAILED -> {
                delay(720)
                reticleAlpha.animateTo(0f, tween(320))
            }
        }
    }

    val ringColor = when (current.phase) {
        FocusPhase.START -> PrismCyan
        FocusPhase.LOCKED -> PrismGreen
        FocusPhase.FAILED -> PrismRed
    }

    Box(
        modifier
            .offset {
                IntOffset(
                    (current.x - 44.dp.toPx()).roundToInt(),
                    (current.y - 44.dp.toPx()).roundToInt()
                )
            }
            .size(88.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = reticleAlpha.value
            }
    ) {
        FocusRingCanvas(
            progress = spin,
            color = ringColor,
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
        )
        Canvas(Modifier.fillMaxSize()) {
            val arm = 14.dp.toPx()
            val stroke = 2.4f
            val inset = 16.dp.toPx()
            fun bracket(cx: Float, cy: Float, dx: Float, dy: Float) {
                drawLine(ringColor, Offset(cx, cy), Offset(cx + dx * arm, cy), stroke)
                drawLine(ringColor, Offset(cx, cy), Offset(cx, cy + dy * arm), stroke)
            }
            bracket(inset, inset, 1f, 1f)
            bracket(size.width - inset, inset, -1f, 1f)
            bracket(inset, size.height - inset, 1f, -1f)
            bracket(size.width - inset, size.height - inset, -1f, -1f)
            drawCircle(
                ringColor,
                radius = 2.4f,
                center = Offset(size.width / 2f, size.height / 2f)
            )
        }
    }
}

// ------------------------------------------------------------------ zoom badge

@Composable
fun ZoomBadge(
    zoom: Float,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(zoom) {
        visible = true
        delay(620)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(220)),
        modifier = modifier
    ) {
        Text(
            text = String.format(Locale.US, "%.1f×", zoom),
            style = ReadoutTextStyle.copy(
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.92f)
            ),
            textAlign = TextAlign.Center
        )
    }
}
