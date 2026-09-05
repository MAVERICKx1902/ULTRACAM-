package com.ultracam.app.ui.glass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ultracam.app.ui.theme.PrismCyan
import com.ultracam.app.ui.theme.PrismMagenta
import com.ultracam.app.ui.theme.PrismViolet

/**
 * The liquid-glass building block: a translucent panel with a gradient sheen
 * that slides with device tilt, and a hair-thin chromatic (prism) border.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    sheen: Float = 0f,
    fillAlpha: Float = 0.12f,
    content: @Composable () -> Unit
) {
    val clampedSheen = sheen.coerceIn(-1f, 1f)
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = fillAlpha + 0.05f),
                        Color.White.copy(alpha = fillAlpha * 0.35f)
                    )
                )
            )
            .drawBehind { drawLiquidSheen(clampedSheen) }
            .border(1.dp, chromaticBorderBrush(), shape)
    ) {
        content()
    }
}

private fun DrawScope.drawLiquidSheen(t: Float) {
    val bandWidth = size.width * 0.6f
    val cx = size.width * (0.5f + t * 0.7f)
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0f)
            ),
            start = Offset(cx - bandWidth / 2f, 0f),
            end = Offset(cx + bandWidth / 2f, size.height * 0.85f)
        )
    )
}

/** Signature chromatic fringe used on every glass edge in the app. */
fun chromaticBorderBrush(): Brush = Brush.linearGradient(
    0.0f to PrismCyan.copy(alpha = 0.34f),
    0.28f to Color.White.copy(alpha = 0.55f),
    0.52f to PrismViolet.copy(alpha = 0.30f),
    0.78f to PrismMagenta.copy(alpha = 0.34f),
    1.0f to Color.White.copy(alpha = 0.45f)
)

fun prismGradientBrush(): Brush = Brush.linearGradient(
    listOf(PrismCyan, PrismViolet, PrismMagenta)
)

/**
 * Round glass button used across the side rail and control clusters.
 * Sinks subtly when pressed, like a droplet.
 */
@Composable
fun GlassCircleButton(
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = Color.White,
    activeTint: Color = PrismCyan,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "glassButtonScale"
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (active) {
                    Brush.radialGradient(
                        listOf(activeTint.copy(alpha = 0.30f), activeTint.copy(alpha = 0.08f))
                    )
                } else {
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = if (active) {
                    Brush.linearGradient(
                        listOf(activeTint.copy(alpha = 0.8f), Color.White.copy(alpha = 0.4f))
                    )
                } else {
                    chromaticBorderBrush()
                },
                shape = CircleShape
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) activeTint else tint
        )
        if (active) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(activeTint)
            )
        }
    }
}
