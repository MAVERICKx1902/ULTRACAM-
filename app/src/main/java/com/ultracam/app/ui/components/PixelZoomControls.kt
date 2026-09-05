package com.ultracam.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultracam.app.ui.theme.PixelPillBg
import com.ultracam.app.ui.theme.PixelPurpleAccent
import com.ultracam.app.ui.theme.PixelPurpleDark
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

/**
 * Pixel Camera Zoom Control Component:
 * - Floating Zoom Capsule Pill: [ 0.5 | (1x) | 2 | 5 ] with pastel purple active highlight
 * - Animated Arc Zoom Wheel overlay that expands smoothly on drag or toggle
 */
@Composable
fun PixelZoomControls(
    zoom: Float,
    minZoom: Float,
    maxZoom: Float,
    showWheel: Boolean,
    onZoomChange: (Float) -> Unit,
    onToggleWheel: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto collapse wheel after 3 seconds of inactivity
    LaunchedEffect(zoom, showWheel) {
        if (showWheel) {
            delay(3000)
            onToggleWheel(false)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ---- Smooth Expanding Arc Zoom Wheel ----
        AnimatedVisibility(
            visible = showWheel,
            enter = fadeIn(spring(stiffness = 400f)) +
                    scaleIn(initialScale = 0.85f, animationSpec = spring(stiffness = 400f)) +
                    expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut(spring(stiffness = 400f)) +
                    scaleOut(targetScale = 0.85f, animationSpec = spring(stiffness = 400f)) +
                    shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            ZoomWheel(
                zoom = zoom,
                minZoom = minZoom,
                maxZoom = maxZoom,
                visible = true,
                onZoomChange = { newZoom ->
                    onZoomChange(newZoom)
                    onToggleWheel(true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        // ---- Floating Capsule Zoom Pill: [ 0.5 | (1x) | 2 | 5 ] ----
        val presets = listOf(0.5f, 1.0f, 2.0f, 5.0f).filter { it >= minZoom - 0.1f && it <= maxZoom + 0.1f }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(PixelPillBg)
                .pointerInput(minZoom, maxZoom, zoom) {
                    detectHorizontalDragGestures(
                        onDragStart = { onToggleWheel(true) },
                        onDragEnd = { /* timer handles collapse */ }
                    ) { change, dragAmount ->
                        change.consume()
                        val logMin = ln(minZoom.coerceAtLeast(0.5f))
                        val logMax = ln(maxZoom.coerceAtLeast(10f))
                        val currentLog = ln(zoom.coerceIn(minZoom, maxZoom))
                        val logDelta = -dragAmount * 0.007f
                        val nextLog = (currentLog + logDelta).coerceIn(logMin, logMax)
                        onZoomChange(exp(nextLog))
                        onToggleWheel(true)
                    }
                }
                .padding(horizontal = 6.dp, vertical = 5.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { p ->
                    val isSelected = abs(zoom - p) < 0.15f
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) PixelPurpleDark else Color.White.copy(alpha = 0.9f),
                        label = "zoomPillTextColor"
                    )
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) PixelPurpleAccent else Color.Transparent,
                        label = "zoomPillBgColor"
                    )

                    val labelStr = if (p < 1.0f) String.format(Locale.US, "%.1f", p) else "${p.toInt()}x"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(bgColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onZoomChange(p)
                                onToggleWheel(false)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSelected && abs(zoom - p) > 0.05f) String.format(Locale.US, "%.1fx", zoom) else labelStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}
