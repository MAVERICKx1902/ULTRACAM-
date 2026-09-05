package com.ultracam.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.ultracam.app.ui.glass.chromaticBorderBrush
import com.ultracam.app.ui.theme.PrismCyan
import com.ultracam.app.ui.theme.PrismMagenta
import com.ultracam.app.ui.theme.PrismViolet
import com.ultracam.app.ui.theme.ReadoutTextStyle
import com.ultracam.app.ui.theme.TinyLabelStyle
import kotlin.math.abs

/**
 * A hand-built glass slider: thin translucent track, prism-gradient active
 * region, glowing droplet thumb. Tapping seeks; dragging scrubs live;
 * onCommit fires when the gesture ends (that is when stills get rebaked).
 */
@Composable
fun GlassSlider(
    label: String,
    valueText: String,
    fraction: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    autoLabel: String? = null,
    onAutoToggle: (() -> Unit)? = null,
    onChange: (Float) -> Unit,
    onCommit: () -> Unit
) {
    var current by remember { mutableStateOf(fraction) }
    var widthPx by remember { mutableStateOf(1f) }

    LaunchedEffect(fraction) {
        if (abs(fraction - current) > 0.002f) current = fraction
    }

    Row(
        modifier.height(38.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TinyLabelStyle,
            modifier = Modifier.width(42.dp)
        )

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .height(38.dp)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = { onCommit() },
                        onDragCancel = { onCommit() },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            val next = (current + amount / widthPx).coerceIn(0f, 1f)
                            current = next
                            onChange(next)
                        }
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val next = (offset.x / widthPx).coerceIn(0f, 1f)
                        current = next
                        onChange(next)
                        onCommit()
                    }
                }
        ) {
            Canvas(Modifier.matchParentSize()) {
                val trackHeight = 4.dp.toPx()
                val cy = size.height / 2f
                val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)

                // full track
                drawRoundRect(
                    color = Color.White.copy(alpha = if (enabled) 0.14f else 0.06f),
                    topLeft = Offset(0f, cy - trackHeight / 2f),
                    size = Size(size.width, trackHeight),
                    cornerRadius = corner
                )

                if (enabled) {
                    val activeW = size.width * current
                    // prism active region
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(PrismCyan, PrismViolet, PrismMagenta)
                        ),
                        topLeft = Offset(0f, cy - trackHeight / 2f),
                        size = Size(activeW, trackHeight),
                        cornerRadius = corner
                    )
                    // thumb droplet
                    val thumbR = 6.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, PrismCyan.copy(alpha = 0.4f)),
                            center = Offset(activeW, cy),
                            radius = thumbR * 1.6f
                        ),
                        radius = thumbR * 1.6f,
                        center = Offset(activeW, cy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = thumbR * 0.62f,
                        center = Offset(activeW, cy)
                    )
                }
            }
        }

        if (autoLabel != null && onAutoToggle != null) {
            val auto = autoLabel == "AUTO"
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .width(46.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (auto) {
                            Color.White.copy(alpha = 0.08f)
                        } else {
                            PrismCyan.copy(alpha = 0.22f)
                        }
                    )
                    .pointerInput(autoLabel) {
                        detectTapGestures { onAutoToggle() }
                    }
                    .border(
                        0.8.dp,
                        if (auto) Color.White.copy(alpha = 0.22f) else PrismCyan.copy(alpha = 0.7f),
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = autoLabel,
                    style = TinyLabelStyle.copy(
                        color = if (auto) Color.White.copy(alpha = 0.6f) else PrismCyan
                    )
                )
            }
        }

        Text(
            text = if (enabled) valueText else "AUTO",
            style = ReadoutTextStyle,
            modifier = Modifier
                .padding(start = 8.dp)
                .width(64.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
