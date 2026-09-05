package com.ultracam.app.ui.components

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ultracam.app.ui.glass.chromaticBorderBrush
import com.ultracam.app.ui.theme.PrismCyan
import com.ultracam.app.ui.theme.PrismMagenta
import com.ultracam.app.ui.theme.PrismViolet
import kotlinx.coroutines.delay

/**
 * The big prism shutter: a conic chromatic ring around a droplet core.
 * Tap = shoot. Press & hold (~0.4s) = burst until release.
 */
@Composable
fun ShutterButton(
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    busy: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "busySpin")
    val spinDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinDegrees"
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "shutterScale"
    )

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(400)
            onHoldStart()
        } else {
            onHoldEnd()
        }
    }

    Box(
        modifier
            .size(82.dp)
            .clip(CircleShape)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                clip = true
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onTap),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(82.dp)) {
            val ring = 5.dp.toPx()
            val outer = size.minDimension / 2f
            val inner = outer - ring

            // conic prism ring
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to PrismCyan,
                    0.2f to Color.White,
                    0.42f to PrismViolet,
                    0.62f to PrismMagenta,
                    0.82f to Color.White,
                    1.0f to PrismCyan
                ),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = ring, cap = StrokeCap.Butt),
                topLeft = Offset(ring / 2f, ring / 2f),
                size = Size(size.width - ring, size.height - ring)
            )

            // ring faceplate
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.03f))
                ),
                radius = inner
            )
            drawCircle(
                brush = chromaticBorderBrush(),
                radius = inner,
                style = Stroke(width = 1.4f)
            )

            // droplet core
            val core = inner - 7.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFAFCFF), Color(0xFFD9E6F5)),
                    center = Offset(core * 0.82f, core * 0.72f),
                    radius = core * 1.25f
                ),
                radius = core
            )
            // specular highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                    center = Offset(core * 0.72f, core * 0.60f),
                    radius = core * 0.45f
                ),
                radius = core * 0.45f,
                center = Offset(core * 0.72f, core * 0.60f)
            )

            // busy spinner arc
            if (busy) {
                rotate(degrees = spinDegrees) {
                    drawArc(
                        color = PrismCyan,
                        startAngle = 0f,
                        sweepAngle = 110f,
                        useCenter = false,
                        style = Stroke(width = ring * 0.6f, cap = StrokeCap.Round),
                        topLeft = Offset(ring / 2f, ring / 2f),
                        size = Size(size.width - ring, size.height - ring)
                    )
                }
            }
        }
    }
}
