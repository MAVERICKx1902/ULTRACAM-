package com.ultracam.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultracam.app.ui.CaptureMode
import com.ultracam.app.ui.theme.PixelPurpleAccent
import com.ultracam.app.ui.theme.PixelPurpleDark

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Pixel Camera horizontal mode carousel (CINEMATIC, VIDEO, PHOTO, PORTRAIT, PRO).
 * Uses Pixel Material 3 pill selection styling and horizontal drag swipe gesture switching.
 */
@Composable
fun PixelModeCarousel(
    currentMode: CaptureMode,
    onSelectMode: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = CaptureMode.entries
    val currentIndex = modes.indexOf(currentMode)

    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp)
            .pointerInput(currentMode) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag < -40f && currentIndex < modes.size - 1) {
                            onSelectMode(modes[currentIndex + 1])
                        } else if (totalDrag > 40f && currentIndex > 0) {
                            onSelectMode(modes[currentIndex - 1])
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                }
            },
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { mode ->
            val isSelected = mode == currentMode
            val textColor by animateColorAsState(
                targetValue = if (isSelected) PixelPurpleDark else Color.White.copy(alpha = 0.85f),
                label = "modeTextColor"
            )
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) PixelPurpleAccent else Color.Transparent,
                label = "modeBgColor"
            )

            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelectMode(mode) }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    color = textColor
                )
            }
        }
    }
}
