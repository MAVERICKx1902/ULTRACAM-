package com.ultracam.app.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.ultracam.app.ui.theme.PrismAmber
import com.ultracam.app.ui.theme.PrismCyan
import com.ultracam.app.ui.theme.PrismMagenta
import com.ultracam.app.ui.theme.PrismViolet

/**
 * The ULTRACAM sigil: a glass prism splitting one white beam into a spectrum.
 * A slow shimmer sweeps the beams — the "liquid" part of liquid prism.
 */
@Composable
fun PrismLogo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "prismShimmer")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "prismShimmerPhase"
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.44f
        val cy = h * 0.5f

        // ---- glass triangle ----
        val tri = Path().apply {
            moveTo(cx - w * 0.17f, cy - h * 0.30f)
            lineTo(cx - w * 0.17f, cy + h * 0.30f)
            lineTo(cx + w * 0.20f, cy)
            close()
        }
        drawPath(
            path = tri,
            brush = Brush.linearGradient(
                listOf(Color(0x331B2A44), Color(0x55101B2E)),
                start = Offset(cx - w * 0.2f, cy - h * 0.3f),
                end = Offset(cx + w * 0.2f, cy + h * 0.3f)
            )
        )
        drawPath(
            path = tri,
            color = Color(0xFFD9ECFF),
            style = Stroke(width = 2.5f)
        )

        // ---- incoming white beam ----
        val beamY = cy
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0x00F2F8FF), Color(0xFFF2F8FF))
            ),
            topLeft = Offset(0f, beamY - h * 0.022f),
            size = androidx.compose.ui.geometry.Size(cx - w * 0.17f, h * 0.044f)
        )

        // ---- dispersed spectrum ----
        val apex = Offset(cx + w * 0.20f, cy)
        val colors = listOf(PrismCyan, PrismViolet, PrismMagenta, PrismAmber)
        val spread = w * 0.30f
        val phase = shimmer * 2f
        colors.forEachIndexed { i, color ->
            val a = -0.34f + i * 0.225f
            val endY = cy + a * spread
            val glow = 0.45f + 0.55f * (0.5f + 0.5f * kotlin.math.sin(phase + i * 1.3f))
            val beam = Path().apply {
                moveTo(apex.x, apex.y - h * 0.014f)
                lineTo(apex.x, apex.y + h * 0.014f)
                lineTo(w, endY + h * 0.030f)
                lineTo(w, endY - h * 0.030f)
                close()
            }
            drawPath(
                path = beam,
                brush = Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.10f + 0.35f * glow), color.copy(alpha = 0.02f))
                )
            )
        }

        // ---- refraction sparkle at the apex ----
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
            ),
            radius = h * 0.06f * (0.8f + 0.2f * kotlin.math.sin(phase * 2f)),
            center = apex
        )
    }
}


/**
 * Subtle chromatic refraction bands along the viewfinder frame edges —
 * the whole screen becomes the prism.
 */
@Composable
fun PrismEdgeOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val band = size.width * 0.045f
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(PrismCyan.copy(alpha = 0.10f), Color.Transparent)
            ),
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(band, size.height)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, PrismMagenta.copy(alpha = 0.10f))
            ),
            topLeft = Offset(size.width - band, 0f),
            size = androidx.compose.ui.geometry.Size(band, size.height)
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(PrismViolet.copy(alpha = 0.08f), Color.Transparent)
            ),
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(size.width, band)
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, PrismViolet.copy(alpha = 0.08f))
            ),
            topLeft = Offset(0f, size.height - band),
            size = androidx.compose.ui.geometry.Size(size.width, band)
        )
    }
}

/** Slow-drifting aurora blobs behind the onboarding screen. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDrift"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        rotate(degrees = drift * 6f, pivot = center) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(PrismCyan.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(w * 0.25f, h * 0.30f),
                    radius = w * (0.55f + 0.08f * drift)
                ),
                radius = w * (0.55f + 0.08f * drift),
                center = Offset(w * 0.25f, h * 0.30f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(PrismMagenta.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(w * 0.80f, h * 0.65f),
                    radius = w * (0.60f - 0.06f * drift)
                ),
                radius = w * (0.60f - 0.06f * drift),
                center = Offset(w * 0.80f, h * 0.65f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(PrismViolet.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.55f, h * 0.85f),
                    radius = w * (0.50f + 0.05f * drift)
                ),
                radius = w * (0.50f + 0.05f * drift),
                center = Offset(w * 0.55f, h * 0.85f)
            )
        }
    }
}

/** Dashed rotating ring — used as the focusing reticle. */
@Composable
fun FocusRingCanvas(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val stroke = 3f
        drawCircle(
            color = color.copy(alpha = 0.9f),
            radius = size.minDimension / 2f - stroke,
            style = Stroke(
                width = stroke,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(size.minDimension * 0.16f, size.minDimension * 0.08f),
                    phase = progress * 40f
                )
            )
        )
    }
}
